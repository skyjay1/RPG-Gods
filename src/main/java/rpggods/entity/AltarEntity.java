package rpggods.entity;

import com.mojang.authlib.GameProfile;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Pose;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.IInventoryChangedListener;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.SimpleNamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.tileentity.SkullTileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.HandSide;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.network.NetworkHooks;
import rpggods.RGRegistry;
import rpggods.RPGGods;
import rpggods.altar.AltarItems;
import rpggods.altar.AltarPose;
import rpggods.deity.Altar;
import rpggods.favor.Favor;
import rpggods.favor.IFavor;
import rpggods.gui.AltarContainer;
import rpggods.gui.FavorContainer;

import javax.annotation.Nullable;
import java.util.Optional;

public class AltarEntity extends LivingEntity /* implements IInventoryChangedListener */ {

    private static final DataParameter<String> DEITY = EntityDataManager.createKey(AltarEntity.class, DataSerializers.STRING);
    private static final DataParameter<Byte> FLAGS = EntityDataManager.createKey(AltarEntity.class, DataSerializers.BYTE);
    private static final DataParameter<Byte> LOCKED = EntityDataManager.createKey(AltarEntity.class, DataSerializers.BYTE);
    private static final DataParameter<CompoundNBT> POSE = EntityDataManager.createKey(AltarEntity.class, DataSerializers.COMPOUND_NBT);
    private static final DataParameter<Optional<BlockState>> BLOCK = EntityDataManager.createKey(AltarEntity.class, DataSerializers.OPTIONAL_BLOCK_STATE);

    private static final String KEY_DEITY = "Deity";
    private static final String KEY_ARMOR = "ArmorItems";
    private static final String KEY_HANDS = "HandItems";
    private static final String KEY_FLAGS = "Flags";
    private static final String KEY_LOCKED = "Locked";
    private static final String KEY_POSE = "Pose";
    private static final String KEY_BLOCK = "Block";

    private NonNullList<ItemStack> handItems = NonNullList.withSize(2, ItemStack.EMPTY);
    private NonNullList<ItemStack> armorItems = NonNullList.withSize(4, ItemStack.EMPTY);
    private Optional<BlockState> block = Optional.empty();
    private Optional<ResourceLocation> deity = Optional.empty();

    //private static final int INV_SIZE = 7;
    //private Inventory inventory;

    @Nullable
    private GameProfile playerProfile = null;
    private String textureName = "";

    private AltarPose pose = new AltarPose();

    private EntitySize smallSize = new EntitySize(0.8F, 1.98F, false);

    public AltarEntity(final EntityType<? extends AltarEntity> entityType, final World world) {
        super(entityType, world);
        this.stepHeight = 0.0F;
    }

    public static AltarEntity createAltar(final World world, final BlockPos pos, final Altar altar) {
        AltarEntity entity = RGRegistry.EntityReg.ALTAR.create(world);
        entity.setPosition(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        entity.applyAltarProperties(altar);
        return entity;
    }

    public static AttributeModifierMap.MutableAttribute registerAttributes() {
        return LivingEntity.registerAttributes()
                .createMutableAttribute(Attributes.MAX_HEALTH, 0.25F)
                .createMutableAttribute(Attributes.KNOCKBACK_RESISTANCE, 1.0F);
    }

    @Override
    public void registerData() {
        super.registerData();
        this.getDataManager().register(DEITY, "");
        this.getDataManager().register(FLAGS, (byte) 0);
        this.getDataManager().register(LOCKED, (byte) 0);
        this.getDataManager().register(POSE, new CompoundNBT());
        this.getDataManager().register(BLOCK, Optional.of(Blocks.AIR.getDefaultState()));
    }

    @Override
    public void notifyDataManagerChange(DataParameter<?> key) {
        super.notifyDataManagerChange(key);
        if(key.equals(DEITY)) {
            this.setDeity(Optional.ofNullable(ResourceLocation.tryCreate(getDataManager().get(DEITY))));
        }
        if(key.equals(POSE)) {
            this.setAltarPose(new AltarPose(getDataManager().get(POSE)));
        }
        if(key.equals(BLOCK)) {
            this.setBaseBlock(getDataManager().get(BLOCK).orElse(null));
        }
    }

    @Override
    public Iterable<ItemStack> getHeldEquipment() {
        return handItems;
    }

    @Override
    public Iterable<ItemStack> getArmorInventoryList() {
        return armorItems;
    }

    public ItemStack getItemStackFromSlot(EquipmentSlotType slotIn) {
        switch(slotIn.getSlotType()) {
            case HAND:
                return handItems.get(slotIn.getIndex());
            case ARMOR:
                return armorItems.get(slotIn.getIndex());
        }
        return ItemStack.EMPTY;
    }

    public void setItemStackToSlot(EquipmentSlotType slotIn, ItemStack stack) {
        int index = slotIn.getIndex();
        switch(slotIn.getSlotType()) {
            case HAND:
                handItems.set(index, stack);
                break;
            case ARMOR:
                armorItems.set(index, stack);
                break;
        }
    }

    @Override
    public HandSide getPrimaryHand() {
        return HandSide.RIGHT;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    protected void collideWithEntity(Entity entityIn) {
    }

    @Override
    protected void collideWithNearbyEntities() {
    }

    @Override
    public boolean attackable() {
        return false;
    }

    public EntitySize getSize(Pose poseIn) {
        return (getBaseBlock() != null && getBaseBlock().getMaterial() != Material.AIR) ? this.getType().getSize() : smallSize;
    }

    public ActionResultType applyPlayerInteraction(PlayerEntity player, Vector3d vec, Hand hand) {
        RPGGods.LOGGER.debug("Name: " + (hasCustomName() ? getCustomName().getUnformattedComponentText() : "<null>"));
        RPGGods.LOGGER.debug("Deity: " + getDeity());

        // open container
        // open the container GUI
        if(player instanceof ServerPlayerEntity) {
            // attempt to process favor capability
            if(getDeity().isPresent()) {
                LazyOptional<IFavor> favor = player.getCapability(RPGGods.FAVOR);
                if(favor.isPresent()) {
                    ResourceLocation deity = getDeity().get();
                    IFavor ifavor = favor.orElse(RPGGods.FAVOR.getDefaultInstance());

                    // open favor GUI
                    NetworkHooks.openGui((ServerPlayerEntity)player,
                            new SimpleNamedContainerProvider((id, inventory, p) ->
                                    new FavorContainer(id, inventory, ifavor, deity),
                                    StringTextComponent.EMPTY),
                            buf -> {
                                buf.writeCompoundTag(ifavor.serializeNBT());
                                buf.writeResourceLocation(deity);
                            }
                    );
                }

            } else {
                // open altar GUI
                NetworkHooks.openGui((ServerPlayerEntity)player,
                        new SimpleNamedContainerProvider((id, inventory, p) ->
                                new AltarContainer(id, inventory, null, this),
                                StringTextComponent.EMPTY),
                        buf -> {
                            buf.writeInt(this.getEntityId());
                        }
                );
            }
        }

        return ActionResultType.SUCCESS;
    }

    @Override
    public void writeAdditional(CompoundNBT compound) {
        super.writeAdditional(compound);
        // write deity
        if(getDeity().isPresent()) {
            compound.putString(KEY_DEITY, getDeity().get().toString());
        }
        // write armor items
        ListNBT armor = new ListNBT();
        for(ItemStack stack : this.armorItems) {
            CompoundNBT compoundnbt = new CompoundNBT();
            if (!stack.isEmpty()) {
                stack.write(compoundnbt);
            }
            armor.add(compoundnbt);
        }
        compound.put(KEY_ARMOR, armor);
        // write hand items
        ListNBT hands = new ListNBT();
        for(ItemStack stack : this.handItems) {
            CompoundNBT compoundNBT = new CompoundNBT();
            if (!stack.isEmpty()) {
                stack.write(compoundNBT);
            }

            hands.add(compoundNBT);
        }
        compound.put(KEY_HANDS, hands);
        // write flags
        compound.putByte(KEY_FLAGS, getFlags());
        // write locked flags
        compound.putByte(KEY_LOCKED, getLocked());
        // write pose
        compound.put(KEY_POSE, getAltarPose().serializeNBT());
        // write block
        compound.put(KEY_BLOCK, NBTUtil.writeBlockState(getBaseBlock()));
    }

    @Override
    public void readAdditional(CompoundNBT compound) {
        super.readAdditional(compound);
        // read deity
        if(compound.contains(KEY_DEITY)) {
            setDeity(Optional.of(ResourceLocation.tryCreate(compound.getString(KEY_DEITY))));
        }
        // read armor
        if (compound.contains(KEY_ARMOR, 9)) {
            ListNBT armor = compound.getList(KEY_ARMOR, 10);

            for(int i = 0; i < this.armorItems.size(); ++i) {
                this.armorItems.set(i, ItemStack.read(armor.getCompound(i)));
            }
        }
        // read hands
        if (compound.contains(KEY_HANDS, 9)) {
            ListNBT hands = compound.getList(KEY_HANDS, 10);
            for(int j = 0; j < this.handItems.size(); ++j) {
                this.handItems.set(j, ItemStack.read(hands.getCompound(j)));
            }
        }
        // read flags
        setFlags(compound.getByte(KEY_FLAGS));
        // read locked flags
        setLocked(compound.getByte(KEY_LOCKED));
        // read pose
        setAltarPose(new AltarPose(compound.getCompound(KEY_POSE)));
        // read block
        setBaseBlock(NBTUtil.readBlockState(compound.getCompound(KEY_BLOCK)));
    }

    /**
     * Applies the Altar properties to this entity
     * @param altar the Altar with properties to use
     */
    public void applyAltarProperties(final Altar altar) {
        altar.getName().ifPresent(name -> setCustomName(new StringTextComponent(name)));
        setCustomNameVisible(false);
        setDeity(altar.getDeity());
        setFemale(altar.isFemale());
        setSlim(altar.isSlim());
        setArmorLocked(altar.getItems().isArmorLocked());
        setHandsLocked(altar.getItems().isHandsLocked());
        setBaseBlock(altar.getBlock());
        setBlockLocked(altar.isBlockLocked());
        setAltarPose(altar.getPose());
        setAltarPoseLocked(altar.isPoseLocked());
        for(EquipmentSlotType slot : EquipmentSlotType.values()) {
            setItemStackToSlot(slot, altar.getItems().getItemStackFromSlot(slot));
        }
    }

    /**
     * Creates a new Altar instance with the same properties as found in this entity.
     */
    public Altar createAltarProperties() {
        AltarItems items = new AltarItems(
                getItemStackFromSlot(EquipmentSlotType.HEAD),
                getItemStackFromSlot(EquipmentSlotType.CHEST),
                getItemStackFromSlot(EquipmentSlotType.LEGS),
                getItemStackFromSlot(EquipmentSlotType.FEET),
                getItemStackFromSlot(EquipmentSlotType.MAINHAND),
                getItemStackFromSlot(EquipmentSlotType.OFFHAND),
                isArmorLocked(), isHandsLocked());
        Optional<String> name = hasCustomName() ? Optional.of(getCustomName().getString()) : Optional.empty();
        boolean enabled = true; // TODO
        ResourceLocation material = Altar.MATERIAL; // TODO
        return new Altar(true /*TODO*/, name, isFemale(), isSlim(), items, getBaseBlock(), isBlockLocked(),
                material, getAltarPose(), isAltarPoseLocked());
    }

    public void setDeity(final Optional<ResourceLocation> deity) {
        this.deity = deity;
        String deityString = "";
        if(deity.isPresent()) {
            // determine string to save deity name
            ResourceLocation deityId = deity.get();
            deityString = deityId.toString();
            // set custom name
            setCustomName(new TranslationTextComponent(Altar.createTranslationKey(deityId)));
            setCustomNameVisible(false);
        }
        // update data manager
        getDataManager().set(DEITY, deityString);
    }

    public Optional<ResourceLocation> getDeity() {
        return this.deity;
    }

    public boolean isNameLocked() {
        return getDeity().isPresent();
    }

    public BlockState getBaseBlock() {
        return getDataManager().get(BLOCK).orElse(Blocks.AIR.getDefaultState());
    }

    public void setBaseBlock(final BlockState block) {
        getDataManager().set(BLOCK, Optional.ofNullable(block));
        recalculateSize();
    }

    private void setPlayerProfile(@Nullable GameProfile profile) {
        this.playerProfile = profile;
        this.updatePlayerProfile();
    }

    private void updatePlayerProfile() {
        this.playerProfile = SkullTileEntity.updateGameProfile(this.playerProfile);
    }

    public boolean isArmorLocked() {
        return (getLocked() & 0x01) > 0;
    }

    public boolean isHandsLocked() {
        return (getLocked() & 0x02) > 0;
    }

    public boolean isBlockLocked() {
        return (getLocked() & 0x04) > 0;
    }

    public boolean isAltarPoseLocked() {
        return (getLocked() & 0x08) > 0;
    }

    public void setArmorLocked(boolean armorLocked) {
        byte locked = getLocked();
        setLocked((byte) (armorLocked ? (locked | 0x01) : (locked & ~0x01)));
    }

    public void setHandsLocked(boolean handsLocked) {
        byte locked = getLocked();
        setLocked((byte) (handsLocked ? (locked | 0x02) : (locked & ~0x02)));
    }

    public void setAltarPoseLocked(final boolean poseLocked) {
        byte locked = getLocked();
        setLocked((byte) (poseLocked ? (locked | 0x08) : (locked & ~0x08)));
    }

    public void setBlockLocked(boolean blockLocked) {
        byte locked = getLocked();
        setLocked((byte) (blockLocked ? (locked | 0x04) : (locked & ~0x04)));
    }

    private byte getLocked() {
        return getDataManager().get(LOCKED).byteValue();
    }

    private void setLocked(byte b) {
        getDataManager().set(LOCKED, b);
    }

    public void setFemale(boolean female) {
        byte flags = getFlags();
        setFlags((byte) (female ? (flags | 0x01) : (flags & ~0x01)));
    }

    public void setSlim(boolean slim) {
        byte flags = getFlags();
        setFlags((byte) (slim ? (flags | 0x02) : (flags & ~0x02)));
    }

    @Override
    public void setCustomName(final ITextComponent name) {
        super.setCustomName(name);
        // attempt to use custom name to set texture
        if(name != null && !getDeity().isPresent()) {
            String sName = name.getUnformattedComponentText();
            if(sName.length() <= 16) {
                this.textureName = sName;
            }
        }
    }

    public boolean isFemale() {
        return (getFlags() & 0x01) > 0;
    }

    public boolean isSlim() {
        return (getFlags() & 0x02) > 0;
    }

    private byte getFlags() {
        return getDataManager().get(FLAGS).byteValue();
    }

    private void setFlags(byte b) {
        getDataManager().set(FLAGS, b);
    }

    public AltarPose getAltarPose() {
        return pose;
    }

    public void setAltarPose(final AltarPose pose) {
        this.pose = pose;
        this.getDataManager().set(POSE, this.pose.serializeNBT());
    }

    @Nullable
    public GameProfile getPlayerProfile() { return this.playerProfile; }

    /*
    public void initInventory() {
        Inventory simplecontainer = this.inventory;
        this.inventory = new Inventory(INV_SIZE);
        if (simplecontainer != null) {
            simplecontainer.removeListener(this);
            int i = Math.min(simplecontainer.getSizeInventory(), this.inventory.getSizeInventory());

            for(int j = 0; j < i; ++j) {
                ItemStack itemstack = simplecontainer.getStackInSlot(j);
                if (!itemstack.isEmpty()) {
                    this.inventory.setInventorySlotContents(j, itemstack.copy());
                }
            }
        }

        this.inventory.addListener(this);
        this.onInventoryChanged(this.inventory);
    }
     */
}
