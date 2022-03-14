package rpggods.entity;

import com.google.common.collect.Lists;
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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.IInventoryChangedListener;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.SimpleNamedContainerProvider;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.tileentity.SkullTileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.HandSide;
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
import rpggods.event.FavorEventHandler;
import rpggods.favor.IFavor;
import rpggods.gui.AltarContainer;
import rpggods.gui.FavorContainer;

import javax.annotation.Nullable;
import java.util.Optional;

public class AltarEntity extends LivingEntity implements IInventoryChangedListener {

    private static final DataParameter<String> DEITY = EntityDataManager.createKey(AltarEntity.class, DataSerializers.STRING);
    private static final DataParameter<Byte> FLAGS = EntityDataManager.createKey(AltarEntity.class, DataSerializers.BYTE);
    private static final DataParameter<Byte> LOCKED = EntityDataManager.createKey(AltarEntity.class, DataSerializers.BYTE);
    private static final DataParameter<CompoundNBT> POSE = EntityDataManager.createKey(AltarEntity.class, DataSerializers.COMPOUND_NBT);
    private static final DataParameter<Optional<BlockState>> BLOCK = EntityDataManager.createKey(AltarEntity.class, DataSerializers.OPTIONAL_BLOCK_STATE);

    private static final String KEY_DEITY = "Deity";
    private static final String KEY_INVENTORY = "Inventory";
    private static final String KEY_SLOT = "Slot";
    private static final String KEY_FLAGS = "Flags";
    private static final String KEY_LOCKED = "Locked";
    private static final String KEY_POSE = "Pose";
    private static final String KEY_BLOCK = "Block";

    private Optional<BlockState> block = Optional.empty();
    private Optional<ResourceLocation> deity = Optional.empty();

    private static final int INV_SIZE = 7;
    private Inventory inventory;

    @Nullable
    private GameProfile playerProfile = null;
    private String textureName = "";

    private AltarPose pose = new AltarPose();

    private EntitySize smallSize = new EntitySize(0.8F, 1.98F, false);

    public AltarEntity(final EntityType<? extends AltarEntity> entityType, final World world) {
        super(entityType, world);
        this.stepHeight = 0.0F;
        initInventory();
    }

    public static AltarEntity createAltar(final World world, final BlockPos pos, Direction facing, final Altar altar) {
        AltarEntity entity = new AltarEntity(RGRegistry.EntityReg.ALTAR, world);
        entity.setPositionAndRotation(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, facing.getHorizontalAngle(), 0F);
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
            String sDeityId = getDataManager().get(DEITY);
            if(sDeityId != null && !sDeityId.isEmpty()) {
                this.setDeity(Optional.ofNullable(ResourceLocation.tryCreate(sDeityId)));
            } else {
                this.setDeity(Optional.empty());
            }
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
        return Lists.newArrayList(
                this.inventory.getStackInSlot(EquipmentSlotType.MAINHAND.getSlotIndex()),
                this.inventory.getStackInSlot(EquipmentSlotType.OFFHAND.getSlotIndex())
        );
    }

    @Override
    public Iterable<ItemStack> getArmorInventoryList() {
        return Lists.newArrayList(
                this.inventory.getStackInSlot(EquipmentSlotType.FEET.getSlotIndex()),
                this.inventory.getStackInSlot(EquipmentSlotType.LEGS.getSlotIndex()),
                this.inventory.getStackInSlot(EquipmentSlotType.CHEST.getSlotIndex()),
                this.inventory.getStackInSlot(EquipmentSlotType.HEAD.getSlotIndex())
        );
    }

    public ItemStack getItemStackFromSlot(EquipmentSlotType slotIn) {
        return this.inventory.getStackInSlot(slotIn.getSlotIndex());
    }

    public void setItemStackToSlot(EquipmentSlotType slotIn, ItemStack stack) {
        this.inventory.setInventorySlotContents(slotIn.getSlotIndex(), stack);
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
        if(getDeity().isPresent()) {
            RPGGods.LOGGER.debug("Deity data: " + RPGGods.DEITY.get(getDeity().get()));
        }

        // open container
        // open the container GUI
        if(player instanceof ServerPlayerEntity) {
            // attempt to process favor capability
            if(getDeity().isPresent()) {
                LazyOptional<IFavor> favor = player.getCapability(RPGGods.FAVOR);
                if(favor.isPresent()) {
                    ResourceLocation deity = getDeity().get();
                    IFavor ifavor = favor.orElse(RPGGods.FAVOR.getDefaultInstance());
                    ItemStack heldItem = player.getHeldItem(hand);
                    // apply offering
                    Optional<ItemStack> offeringResult = FavorEventHandler.onOffering(deity, player, ifavor, heldItem);
                    // if item changed, update player inventory
                    if(offeringResult.isPresent()) {
                        player.setHeldItem(hand, offeringResult.get());
                        return ActionResultType.CONSUME;
                    }
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
                        new SimpleNamedContainerProvider((id, inv, p) ->
                                new AltarContainer(id, inv, this.inventory, this),
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
        // write inventory
        ListNBT listNBT = new ListNBT();
        // write inventory slots to NBT
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                CompoundNBT slotNBT = new CompoundNBT();
                slotNBT.putByte(KEY_SLOT, (byte) i);
                stack.write(slotNBT);
                listNBT.add(slotNBT);
            }
        }
        compound.put(KEY_INVENTORY, listNBT);
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
        // init inventory
        initInventory();
        // read inventory
        final ListNBT list = compound.getList(KEY_INVENTORY, 10);
        // read inventory slots from NBT
        for (int i = 0; i < list.size(); i++) {
            CompoundNBT slotNBT = list.getCompound(i);
            int slotNum = slotNBT.getByte(KEY_SLOT) & 0xFF;
            if (slotNum >= 0 && slotNum < inventory.getSizeInventory()) {
                inventory.setInventorySlotContents(slotNum, ItemStack.read(slotNBT));
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

    public Inventory getInventory() {
        return this.inventory;
    }

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

    @Override
    public void onInventoryChanged(IInventory inv) {
        ItemStack baseBlockItem = inv.getStackInSlot(INV_SIZE - 1);
        Optional<BlockState> baseBlock = Optional.empty();
        if(baseBlockItem.getItem() instanceof BlockItem) {
            baseBlock = Optional.of(((BlockItem)baseBlockItem.getItem()).getBlock().getDefaultState());
        }
        setBaseBlock(baseBlock);
    }

    @Override
    protected void dropInventory() {
        super.dropInventory();
        if (this.inventory != null) {
            if(!isHandsLocked()) {
                this.entityDropItem(this.inventory.getStackInSlot(EquipmentSlotType.MAINHAND.getSlotIndex()));
                this.entityDropItem(this.inventory.getStackInSlot(EquipmentSlotType.OFFHAND.getSlotIndex()));
            }
            if(!isArmorLocked()) {
                this.entityDropItem(this.inventory.getStackInSlot(EquipmentSlotType.FEET.getSlotIndex()));
                this.entityDropItem(this.inventory.getStackInSlot(EquipmentSlotType.LEGS.getSlotIndex()));
                this.entityDropItem(this.inventory.getStackInSlot(EquipmentSlotType.CHEST.getSlotIndex()));
                this.entityDropItem(this.inventory.getStackInSlot(EquipmentSlotType.HEAD.getSlotIndex()));
            }
            if(!isBlockLocked()) {
                this.entityDropItem(this.inventory.getStackInSlot(INV_SIZE - 1));
            }
        }
    }

    /**
     * Applies the Altar properties to this entity
     * @param altar the Altar with properties to use
     */
    public void applyAltarProperties(final Altar altar) {
        setCustomName(altar.getName().isPresent() ? new StringTextComponent(altar.getName().get()) : null);
        setCustomNameVisible(false);
        setDeity(altar.getDeity());
        setFemale(altar.isFemale());
        setSlim(altar.isSlim());
        setArmorLocked(altar.getItems().isArmorLocked());
        setHandsLocked(altar.getItems().isHandsLocked());
        setBlockLocked(altar.isBlockLocked());
        setAltarPose(altar.getPose());
        setAltarPoseLocked(altar.isPoseLocked());
        for(EquipmentSlotType slot : EquipmentSlotType.values()) {
            setItemStackToSlot(slot, altar.getItems().getItemStackFromSlot(slot).copy());
        }
        this.inventory.setInventorySlotContents(INV_SIZE - 1, new ItemStack(altar.getBlock().getBlock().asItem()));
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
        return new Altar(true /*TODO*/, name, isFemale(), isSlim(), ItemStack.EMPTY, items,
                getBaseBlock().getBlock().getRegistryName(), isBlockLocked(), material, getAltarPose(), isAltarPoseLocked());
    }

    public void setDeity(final Optional<ResourceLocation> deity) {
        this.deity = deity;
        String deityString = "";
        if(deity.isPresent() && !deity.get().toString().isEmpty()) {
            // determine string to save deity name
            ResourceLocation deityId = deity.get();
            deityString = deityId.toString();
            // set custom name
            setCustomName(new TranslationTextComponent(Altar.createTranslationKey(deityId)));
            setCustomNameVisible(false);
        } else {
            setCustomName(null);
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
        setBaseBlock(Optional.ofNullable(block));
    }

    public void setBaseBlock(final Optional<BlockState> block) {
        getDataManager().set(BLOCK, block);
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
