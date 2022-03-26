package rpggods.entity;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.DataResult;
import net.minecraft.block.BlockState;
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
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTDynamicOps;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.SkullTileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.HandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.concurrent.TickDelayedTask;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import rpggods.RGRegistry;
import rpggods.RPGGods;
import rpggods.altar.AltarItems;
import rpggods.altar.AltarPose;
import rpggods.deity.Altar;
import rpggods.deity.Deity;
import rpggods.event.FavorEventHandler;
import rpggods.favor.IFavor;
import rpggods.gui.AltarContainer;
import rpggods.gui.FavorContainer;
import rpggods.item.AltarItem;
import rpggods.network.SUpdateAltarPacket;

import javax.annotation.Nullable;
import java.util.Optional;

public class AltarEntity extends LivingEntity implements IInventoryChangedListener {

    private static final DataParameter<String> ALTAR = EntityDataManager.defineId(AltarEntity.class, DataSerializers.STRING);
    private static final DataParameter<String> DEITY = EntityDataManager.defineId(AltarEntity.class, DataSerializers.STRING);
    private static final DataParameter<Byte> FLAGS = EntityDataManager.defineId(AltarEntity.class, DataSerializers.BYTE);
    private static final DataParameter<Byte> LOCKED = EntityDataManager.defineId(AltarEntity.class, DataSerializers.BYTE);
    private static final DataParameter<CompoundNBT> POSE = EntityDataManager.defineId(AltarEntity.class, DataSerializers.COMPOUND_TAG);

    private static final String KEY_ALTAR = "Altar";
    private static final String KEY_DEITY = "Deity";
    private static final String KEY_INVENTORY = "Inventory";
    private static final String KEY_SLOT = "Slot";
    private static final String KEY_FLAGS = "Flags";
    private static final String KEY_LOCKED = "Locked";
    private static final String KEY_POSE = "Pose";

    private Optional<ResourceLocation> deity = Optional.empty();

    public static final int INV_SIZE = 7;
    private Inventory inventory;

    @Nullable
    private GameProfile playerProfile = null;

    private AltarPose pose = AltarPose.EMPTY;

    private EntitySize smallSize = new EntitySize(0.8F, 1.98F, false);

    public AltarEntity(final EntityType<? extends AltarEntity> entityType, final World world) {
        super(entityType, world);
        this.maxUpStep = 0.0F;
        initInventory();
    }

    public static AltarEntity createAltar(final World world, final BlockPos pos, Direction facing, final ResourceLocation altar) {
        AltarEntity entity = new AltarEntity(RGRegistry.EntityReg.ALTAR, world);
        float f = facing.toYRot();
        entity.absMoveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, f, 0);
        entity.yHeadRot = f;
        entity.yBodyRot = f;
        entity.applyAltarProperties(altar);
        return entity;
    }

    public static AttributeModifierMap.MutableAttribute registerAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 0.25F)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0F);
    }

    @Override
    public void defineSynchedData() {
        super.defineSynchedData();
        this.getEntityData().define(ALTAR, "");
        this.getEntityData().define(DEITY, "");
        this.getEntityData().define(FLAGS, (byte) 0);
        this.getEntityData().define(LOCKED, (byte) 0);
        this.getEntityData().define(POSE, new CompoundNBT());
    }

    @Override
    public void onSyncedDataUpdated(DataParameter<?> key) {
        super.onSyncedDataUpdated(key);
        if(key.equals(DEITY)) {
            String sDeityId = getEntityData().get(DEITY);
            if(sDeityId != null && !sDeityId.isEmpty() && sDeityId.contains(":")) {
                this.setDeity(Optional.ofNullable(ResourceLocation.tryParse(sDeityId)));
            } else {
                this.setDeity(Optional.empty());
            }
        }
        if(key.equals(POSE)) {
            this.setAltarPose(new AltarPose(getEntityData().get(POSE)));
        }
    }

    @Override
    public Iterable<ItemStack> getHandSlots() {
        return Lists.newArrayList(
                this.inventory.getItem(EquipmentSlotType.MAINHAND.getFilterFlag()),
                this.inventory.getItem(EquipmentSlotType.OFFHAND.getFilterFlag())
        );
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return Lists.newArrayList(
                this.inventory.getItem(EquipmentSlotType.FEET.getFilterFlag()),
                this.inventory.getItem(EquipmentSlotType.LEGS.getFilterFlag()),
                this.inventory.getItem(EquipmentSlotType.CHEST.getFilterFlag()),
                this.inventory.getItem(EquipmentSlotType.HEAD.getFilterFlag())
        );
    }

    @Override
    public Iterable<ItemStack> getAllSlots() {
        return Iterables.concat(this.getHandSlots(), this.getArmorSlots(), Lists.newArrayList(getBlockBySlot()));
    }

    public ItemStack getItemBySlot(EquipmentSlotType slotIn) {
        return this.inventory.getItem(slotIn.getFilterFlag());
    }

    public ItemStack getBlockBySlot() { return this.inventory.getItem(INV_SIZE - 1); }

    public void setItemSlot(EquipmentSlotType slotIn, ItemStack stack) {
        this.inventory.setItem(slotIn.getFilterFlag(), stack);
    }

    public void setBlockSlot(ItemStack stack) {
        this.inventory.setItem(INV_SIZE - 1, stack);
    }

    @Override
    public HandSide getMainArm() {
        return HandSide.RIGHT;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entityIn) {
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    public boolean attackable() {
        return false;
    }

    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source.bypassArmor(), amount);
    }

    public EntitySize getDimensions(Pose poseIn) {
        return (getBlockBySlot().isEmpty()) ? smallSize : this.getType().getDimensions();
    }

    public void tick() {
        if(firstTick) {
            if(level.isClientSide) {
                // update game profile
                setCustomName(getCustomName());
            }
        }
        super.tick();
    }

    public ActionResultType interactAt(PlayerEntity player, Vector3d vec, Hand hand) {
        if(player instanceof ServerPlayerEntity && hand == Hand.MAIN_HAND) {
            // check if altar is deity
            if(getDeity().isPresent()) {
                // attempt to process favor capability
                LazyOptional<IFavor> favor = player.getCapability(RPGGods.FAVOR);
                if(favor.isPresent()) {
                    ResourceLocation deity = getDeity().get();
                    IFavor ifavor = favor.orElse(RPGGods.FAVOR.getDefaultInstance());
                    // detect item in mainhand
                    ItemStack heldItem = player.getItemInHand(hand);
                    // attempt to process held item as offering
                    Optional<ItemStack> offeringResult = FavorEventHandler.onOffering(Optional.of(this), deity, player, ifavor, heldItem);
                    // if item changed, update player inventory
                    if(offeringResult.isPresent()) {
                        player.setItemInHand(hand, offeringResult.get());
                        return ActionResultType.CONSUME;
                    }
                    // no offering result, open favor GUI
                    NetworkHooks.openGui((ServerPlayerEntity)player,
                            new SimpleNamedContainerProvider((id, inventory, p) ->
                                    new FavorContainer(id, inventory, ifavor, deity),
                                    StringTextComponent.EMPTY),
                            buf -> {
                                buf.writeNbt(ifavor.serializeNBT());
                                buf.writeResourceLocation(deity);
                            }
                    );
                    return ActionResultType.SUCCESS;
                }
            } else if(!(isArmorLocked() && isHandsLocked() && isBlockLocked() && isAltarPoseLocked())) {
                // open altar GUI
                NetworkHooks.openGui((ServerPlayerEntity)player,
                        new SimpleNamedContainerProvider((id, inv, p) ->
                                new AltarContainer(id, inv, this.inventory, this),
                                StringTextComponent.EMPTY),
                        buf -> {
                            buf.writeInt(this.getId());
                        }
                );
                return ActionResultType.SUCCESS;
            }
        }

        return ActionResultType.PASS;
    }

    @Override
    public void addAdditionalSaveData(CompoundNBT compound) {
        super.addAdditionalSaveData(compound);
        // write deity
        if(getDeity().isPresent()) {
            compound.putString(KEY_DEITY, getDeity().get().toString());
        }
        // write altar
        compound.putString(KEY_ALTAR, getAltar().toString());
        // write inventory
        ListNBT listNBT = new ListNBT();
        // write inventory slots to NBT
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                CompoundNBT slotNBT = new CompoundNBT();
                slotNBT.putByte(KEY_SLOT, (byte) i);
                stack.save(slotNBT);
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
    }

    @Override
    public void readAdditionalSaveData(CompoundNBT compound) {
        super.readAdditionalSaveData(compound);
        // read deity
        if(compound.contains(KEY_DEITY)) {
            String deity = compound.getString(KEY_DEITY);
            if(deity != null && !deity.isEmpty()) {
                setDeity(Optional.of(ResourceLocation.tryParse(deity)));
            }
        }
        // read altar
        setAltar(ResourceLocation.tryParse(compound.getString(KEY_ALTAR)));
        // init inventory
        initInventory();
        // read inventory
        final ListNBT list = compound.getList(KEY_INVENTORY, 10);
        // read inventory slots from NBT
        for (int i = 0; i < list.size(); i++) {
            CompoundNBT slotNBT = list.getCompound(i);
            int slotNum = slotNBT.getByte(KEY_SLOT) & 0xFF;
            if (slotNum >= 0 && slotNum < inventory.getContainerSize()) {
                inventory.setItem(slotNum, ItemStack.of(slotNBT));
            }
        }
        this.inventory.setChanged();
        // read flags
        setFlags(compound.getByte(KEY_FLAGS));
        // read locked flags
        setLocked(compound.getByte(KEY_LOCKED));
        // read pose
        setAltarPose(new AltarPose(compound.getCompound(KEY_POSE)));
    }

    public Inventory getInventory() {
        return this.inventory;
    }

    public void initInventory() {
        Inventory simplecontainer = this.inventory;
        this.inventory = new Inventory(INV_SIZE);
        if (simplecontainer != null) {
            simplecontainer.removeListener(this);
            int i = Math.min(simplecontainer.getContainerSize(), this.inventory.getContainerSize());

            for(int j = 0; j < i; ++j) {
                ItemStack itemstack = simplecontainer.getItem(j);
                if (!itemstack.isEmpty()) {
                    this.inventory.setItem(j, itemstack.copy());
                }
            }
        }

        this.inventory.addListener(this);
        this.containerChanged(this.inventory);
    }

    @Override
    public void containerChanged(IInventory inv) {
        this.refreshDimensions();
        // send packet to client to notify change
        if(!this.level.isClientSide) {
            ItemStack block = getBlockBySlot();
            RPGGods.CHANNEL.send(PacketDistributor.ALL.noArg(), new SUpdateAltarPacket(this.getId(), block));
        }
    }

    @Override
    protected void dropEquipment() {
        // drop altar
        final ItemStack altarItem = new ItemStack(RGRegistry.ItemReg.ALTAR);
        altarItem.getOrCreateTag().putString(AltarItem.KEY_ALTAR, getAltar().toString());
        this.spawnAtLocation(altarItem);
        // drop inventory
        if (this.inventory != null) {
            if(!isHandsLocked()) {
                this.spawnAtLocation(getItemBySlot(EquipmentSlotType.MAINHAND));
                this.spawnAtLocation(getItemBySlot(EquipmentSlotType.OFFHAND));
            }
            if(!isArmorLocked()) {
                this.spawnAtLocation(getItemBySlot(EquipmentSlotType.FEET));
                this.spawnAtLocation(getItemBySlot(EquipmentSlotType.LEGS));
                this.spawnAtLocation(getItemBySlot(EquipmentSlotType.CHEST));
                this.spawnAtLocation(getItemBySlot(EquipmentSlotType.HEAD));
            }
            if(!isBlockLocked()) {
                this.spawnAtLocation(getBlockBySlot());
            }
        }
    }

    /**
     * Applies the Altar properties to this entity
     * @param altarId the Altar with properties to use
     */
    public void applyAltarProperties(final ResourceLocation altarId) {
        // query altar by id
        Altar altar = RPGGods.ALTAR.get(altarId).orElse(Altar.EMPTY);
        // apply properties
        setDeity(altar.getDeity());
        setFemale(altar.isFemale());
        setSlim(altar.isSlim());
        setArmorLocked(altar.getItems().isArmorLocked());
        setHandsLocked(altar.getItems().isHandsLocked());
        setBlockLocked(altar.getItems().isBlockLocked());
        setAltarPose(altar.getPose());
        setAltarPoseLocked(altar.isPoseLocked());
        for(EquipmentSlotType slot : EquipmentSlotType.values()) {
            setItemSlot(slot, altar.getItems().getItemStackFromSlot(slot).copy());
        }
        setBlockSlot(new ItemStack(altar.getItems().getBlock().asItem()));
        // custom name
        if(altar.getDeity().isPresent()) {
            setCustomName(Deity.getName(altarId));
        } else if(altar.getName().isPresent()) {
            setCustomName(new StringTextComponent(altar.getName().get()));
        }
        // save altar id
        setAltar(altarId);
    }

    /**
     * Creates a new Altar instance with the same properties as found in this entity.
     */
    public Altar createAltarProperties() {
        AltarItems items;
        items = new AltarItems(
                getItemBySlot(EquipmentSlotType.HEAD),
                getItemBySlot(EquipmentSlotType.CHEST),
                getItemBySlot(EquipmentSlotType.LEGS),
                getItemBySlot(EquipmentSlotType.FEET),
                getItemBySlot(EquipmentSlotType.MAINHAND),
                getItemBySlot(EquipmentSlotType.OFFHAND),
                ForgeRegistries.BLOCKS.getValue(getBlockBySlot().getItem().getRegistryName()),
                isArmorLocked(), isHandsLocked(), isBlockLocked());
        Optional<String> name = hasCustomName() ? Optional.of(getCustomName().getString()) : Optional.empty();
        boolean enabled = true; // TODO
        ResourceLocation material = Altar.MATERIAL; // TODO
        return new Altar(enabled, name, isFemale(), isSlim(), ItemStack.EMPTY, items,
                material, getAltarPose(), isAltarPoseLocked());
    }

    public void setAltar(final ResourceLocation altar) {
        getEntityData().set(ALTAR, altar.toString());
    }

    public ResourceLocation getAltar() {
        String altar = getEntityData().get(ALTAR);
        if(altar != null && !altar.isEmpty() && altar.contains(":")) {
            return ResourceLocation.tryParse(altar);
        }
        return new ResourceLocation("null");
    }

    public void setDeity(final Optional<ResourceLocation> deity) {
        this.deity = deity;
        String deityString = "";
        if(deity.isPresent() && !deity.get().toString().isEmpty()) {
            // determine string to save deity name
            ResourceLocation deityId = deity.get();
            deityString = deityId.toString();
        }
        // update data manager
        getEntityData().set(DEITY, deityString);
    }

    public Optional<ResourceLocation> getDeity() {
        return this.deity;
    }

    public boolean isNameLocked() {
        return getDeity().isPresent();
    }

    private void setPlayerProfile(@Nullable GameProfile profile) {
        this.playerProfile = profile;
        this.updatePlayerProfile();
    }

    private void updatePlayerProfile() {
        this.playerProfile = SkullTileEntity.updateGameprofile(this.playerProfile);
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
        return getEntityData().get(LOCKED).byteValue();
    }

    private void setLocked(byte b) {
        getEntityData().set(LOCKED, b);
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
        if(name != null) {
            String sName = name.getContents();
            if(sName.length() > 0 && sName.length() <= 16 && !sName.contains(":")) {
                final CompoundNBT profileNBT = new CompoundNBT();
                profileNBT.putString("Name", sName);
                setPlayerProfile(NBTUtil.readGameProfile(profileNBT));
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
        return getEntityData().get(FLAGS).byteValue();
    }

    private void setFlags(byte b) {
        getEntityData().set(FLAGS, b);
    }

    public AltarPose getAltarPose() {
        return pose;
    }

    public void setAltarPose(final AltarPose pose) {
        this.pose = pose;
        this.getEntityData().set(POSE, this.pose.serializeNBT());
    }

    @Nullable
    public GameProfile getPlayerProfile() { return this.playerProfile; }
}
