package rpggods.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.FloatTag;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerListener;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import rpggods.RGRegistry;
import rpggods.RPGGods;
import rpggods.deity.Altar;
import rpggods.util.altar.AltarItems;
import rpggods.util.altar.AltarPose;
import rpggods.block.GlowBlock;
import rpggods.deity.Deity;
import rpggods.deity.DeityHelper;
import rpggods.RGEvents;
import rpggods.favor.Favor;
import rpggods.favor.IFavor;
import rpggods.menu.AltarContainerMenu;
import rpggods.menu.FavorContainerMenu;
import rpggods.item.AltarItem;
import rpggods.network.SUpdateAltarPacket;
import rpggods.perk.PerkCondition;

import javax.annotation.Nullable;
import java.util.Optional;

public class AltarEntity extends LivingEntity implements ContainerListener {

    private static final EntityDataAccessor<String> ALTAR = SynchedEntityData.defineId(AltarEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DEITY = SynchedEntityData.defineId(AltarEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Byte> FLAGS = SynchedEntityData.defineId(AltarEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> LOCKED = SynchedEntityData.defineId(AltarEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<CompoundTag> POSE = SynchedEntityData.defineId(AltarEntity.class, EntityDataSerializers.COMPOUND_TAG);

    private static final String KEY_ALTAR = "Altar";
    private static final String KEY_DEITY = "Deity";
    private static final String KEY_INVENTORY = "Inventory";
    private static final String KEY_SLOT = "Slot";
    private static final String KEY_FLAGS = "Flags";
    private static final String KEY_LOCKED = "Locked";
    private static final String KEY_POSE = "Pose";

    public static final int INV_SIZE = 7;
    private SimpleContainer inventory;

    private AltarPose pose = AltarPose.EMPTY;
    private EntityDimensions smallSize = new EntityDimensions(0.8F, 1.98F, false);
    private Optional<ResourceLocation> deity = Optional.empty();
    @Nullable
    private GameProfile playerProfile = null;

    public long lastHit;

    public AltarEntity(final EntityType<? extends AltarEntity> entityType, final Level world) {
        super(entityType, world);
        this.maxUpStep = 0.0F;
        initInventory();
    }

    public static AltarEntity createAltar(final Level world, final BlockPos pos, Direction facing, final ResourceLocation altar) {
        AltarEntity entity = new AltarEntity(RGRegistry.ALTAR_TYPE.get(), world);
        float f = facing.toYRot();
        entity.absMoveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, f, 0);
        entity.yHeadRot = f;
        entity.yBodyRot = f;
        entity.applyAltarProperties(altar);
        return entity;
    }

    public static AttributeSupplier.Builder registerAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0F);
    }

    @Override
    public void defineSynchedData() {
        super.defineSynchedData();
        this.getEntityData().define(ALTAR, "");
        this.getEntityData().define(DEITY, "");
        this.getEntityData().define(FLAGS, (byte) 0);
        this.getEntityData().define(LOCKED, (byte) 0);
        this.getEntityData().define(POSE, new CompoundTag());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (key.equals(DEITY)) {
            String sDeityId = getEntityData().get(DEITY);
            if (!sDeityId.isEmpty() && sDeityId.contains(":")) {
                this.setDeity(Optional.ofNullable(ResourceLocation.tryParse(sDeityId)));
            } else {
                this.setDeity(Optional.empty());
            }
        }
        if (key.equals(POSE)) {
            this.setAltarPose(new AltarPose(getEntityData().get(POSE)));
        }
    }

    @Override
    public Iterable<ItemStack> getHandSlots() {
        return Lists.newArrayList(
                this.inventory.getItem(EquipmentSlot.MAINHAND.getFilterFlag()),
                this.inventory.getItem(EquipmentSlot.OFFHAND.getFilterFlag())
        );
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return Lists.newArrayList(
                this.inventory.getItem(EquipmentSlot.FEET.getFilterFlag()),
                this.inventory.getItem(EquipmentSlot.LEGS.getFilterFlag()),
                this.inventory.getItem(EquipmentSlot.CHEST.getFilterFlag()),
                this.inventory.getItem(EquipmentSlot.HEAD.getFilterFlag())
        );
    }

    @Override
    public Iterable<ItemStack> getAllSlots() {
        return Iterables.concat(this.getHandSlots(), this.getArmorSlots(), Lists.newArrayList(getBlockBySlot()));
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slotIn) {
        return this.inventory.getItem(slotIn.getFilterFlag());
    }

    public ItemStack getBlockBySlot() {
        return this.inventory.getItem(INV_SIZE - 1);
    }

    @Override
    public void setItemSlot(EquipmentSlot slotIn, ItemStack stack) {
        this.inventory.setItem(slotIn.getFilterFlag(), stack);
    }

    public void setBlockSlot(ItemStack stack) {
        this.inventory.setItem(INV_SIZE - 1, stack);
    }

    @Override
    public boolean canTakeItem(ItemStack p_213365_1_) {
        EquipmentSlot slot = Mob.getEquipmentSlotForItem(p_213365_1_);
        boolean locked = (slot.getType() == EquipmentSlot.Type.ARMOR ? isArmorLocked() : isHandsLocked());
        return this.getItemBySlot(slot).isEmpty() && !locked;
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
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

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public void kill() {
        this.remove(Entity.RemovalReason.KILLED);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level.isClientSide && !this.isRemoved()) {
            if (DamageSource.OUT_OF_WORLD.equals(source)) {
                this.kill();
                return false;
            } else if (!this.isInvulnerableTo(source)) {
                if (source.isExplosion()) {
                    this.brokenByAnything(source);
                    this.kill();
                    return false;
                } else if (DamageSource.IN_FIRE.equals(source)) {
                    if (this.isOnFire()) {
                        this.causeDamage(source, 0.15F);
                    } else {
                        this.setSecondsOnFire(5);
                    }

                    return false;
                } else if (DamageSource.ON_FIRE.equals(source) && this.getHealth() > 0.5F) {
                    this.causeDamage(source, 4.0F);
                    return false;
                } else {
                    boolean flag = source.getDirectEntity() instanceof AbstractArrow;
                    boolean flag1 = flag && ((AbstractArrow) source.getDirectEntity()).getPierceLevel() > 0;
                    boolean flag2 = "player".equals(source.getMsgId());
                    if (!flag2 && !flag) {
                        return false;
                    } else if (source.getEntity() instanceof Player && !((Player) source.getEntity()).getAbilities().mayBuild) {
                        return false;
                    } else if (source.isCreativePlayer()) {
                        this.playBrokenSound();
                        this.showBreakingParticles();
                        this.kill();
                        return flag1;
                    } else {
                        long i = this.level.getGameTime();
                        if (i - this.lastHit > 5L && !flag) {
                            this.level.broadcastEntityEvent(this, (byte) 32);
                            this.lastHit = i;
                        } else {
                            this.brokenByPlayer(source);
                            this.showBreakingParticles();
                            this.kill();
                        }

                        return true;
                    }
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public EntityDimensions getDimensions(Pose poseIn) {
        return (getBlockBySlot().isEmpty()) ? smallSize : this.getType().getDimensions();
    }

    @Override
    public void tick() {
        // client-side tick logic
        if (firstTick && level.isClientSide) {
                // update game profile
                setCustomName(getCustomName());
        }
        // parent tick
        super.tick();
        // server-side tick logic
        if(!level.isClientSide) {
            // check if altar has a deity
            if(getDeity().isPresent()) {
                // check if there are any perk conditions for "ritual"
                DeityHelper helper = RPGGods.DEITY_HELPER.computeIfAbsent(getDeity().get(), DeityHelper::new);
                if(!helper.perkByConditionMap.getOrDefault(PerkCondition.Type.RITUAL, ImmutableList.of()).isEmpty()) {
                    // onPerformRitual
                    RGEvents.performRitual(this, getDeity().get());
                }
            }
            // attempt to place light block
            if(tickCount % 4 == 1) {
                Altar altar = RPGGods.ALTAR_MAP.getOrDefault(getAltar(), Altar.EMPTY);
                int lightLevel = altar.getLightLevel();
                // check light level
                if(lightLevel > 0) {
                    BlockPos posIn = getOnPos().above();
                    BlockState blockIn = level.getBlockState(posIn);
                    // check if current block can be replaced
                    if((blockIn.getMaterial() == Material.AIR || blockIn.getMaterial().isLiquid())
                            && !RGRegistry.LIGHT_BLOCK.get().defaultBlockState().is(blockIn.getBlock())) {
                        // determine waterlog value
                        boolean waterlogged = blockIn.getFluidState().isSource() && blockIn.getFluidState().is(FluidTags.WATER);
                        // create light block
                        BlockState lightBlock = RGRegistry.LIGHT_BLOCK.get()
                                .defaultBlockState()
                                .setValue(GlowBlock.LIGHT_LEVEL, lightLevel)
                                .setValue(GlowBlock.WATERLOGGED, waterlogged);
                        // place light block
                        level.setBlock(posIn, lightBlock, Block.UPDATE_ALL);
                    }
                }
            }
        }
    }

    private void showBreakingParticles() {
        if (this.level instanceof ServerLevel) {
            ((ServerLevel) this.level).sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()), this.getX(), this.getY(0.66D), this.getZ(), 10, (double) (this.getBbWidth() / 4.0F), (double) (this.getBbHeight() / 4.0F), (double) (this.getBbWidth() / 4.0F), 0.05D);
        }
    }

    private void causeDamage(DamageSource source, float amount) {
        float f = this.getHealth();
        f = f - amount;
        if (f <= 0.5F) {
            this.brokenByAnything(source);
            this.kill();
        } else {
            this.setHealth(f);
        }
    }

    private void brokenByPlayer(DamageSource p_213815_1_) {
        // drop altar
        final ItemStack altarItem = new ItemStack(RGRegistry.ALTAR_ITEM.get());
        altarItem.getOrCreateTag().putString(AltarItem.KEY_ALTAR, getAltar().toString());
        Block.popResource(level, blockPosition().above(), altarItem);
        // drop other
        this.brokenByAnything(p_213815_1_);
    }

    private void brokenByAnything(DamageSource source) {
        this.playBrokenSound();
        this.dropAllDeathLoot(source);
        BlockPos pos = blockPosition().above();
        // drop inventory
        if (this.inventory != null) {
            if (!isHandsLocked()) {
                Block.popResource(level, pos, getItemBySlot(EquipmentSlot.MAINHAND));
                Block.popResource(level, pos, getItemBySlot(EquipmentSlot.OFFHAND));
                setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            }
            if (!isArmorLocked()) {
                Block.popResource(level, pos, getItemBySlot(EquipmentSlot.FEET));
                Block.popResource(level, pos, getItemBySlot(EquipmentSlot.LEGS));
                Block.popResource(level, pos, getItemBySlot(EquipmentSlot.CHEST));
                Block.popResource(level, pos, getItemBySlot(EquipmentSlot.HEAD));
                setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
                setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
                setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
                setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            }
            if (!isBlockLocked()) {
                Block.popResource(level, pos, getBlockBySlot());
                setBlockSlot(ItemStack.EMPTY);
            }
        }

    }

    private void playBrokenSound() {
        this.level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.STONE_BREAK, this.getSoundSource(), 1.0F, 1.0F);
    }

    @Override
    public InteractionResult interactAt(Player player, Vec3 vec, InteractionHand hand) {
        if (player instanceof ServerPlayer && hand == InteractionHand.MAIN_HAND && this.isAlive()) {
            // check if altar is deity
            if (getDeity().isPresent()) {
                // attempt to process favor capability
                LazyOptional<IFavor> favor = RPGGods.getFavor(player);
                if (favor.isPresent()) {
                    ResourceLocation deity = getDeity().get();
                    IFavor ifavor = favor.orElse(Favor.EMPTY);
                    boolean enabled = ifavor.getFavor(deity).isEnabled();
                    if(!enabled) {
                        // send feedback about disabled deity
                        Component message = new TranslatableComponent("favor.locked");
                        player.displayClientMessage(message, true);
                        return InteractionResult.PASS;
                    }
                    // detect item in mainhand
                    ItemStack heldItem = player.getItemInHand(hand);
                    if(!heldItem.isEmpty()) {
                        // attempt to process held item as offering
                        Optional<ItemStack> offeringResult = RGEvents.onOffering(Optional.of(this), deity, player, ifavor, heldItem, false);
                        // if offering succeeded, update player inventory
                        if (offeringResult.isPresent()) {
                            player.setItemInHand(hand, offeringResult.get());
                            return InteractionResult.CONSUME;
                        }
                    }
                    // no offering result, open favor GUI
                    NetworkHooks.openGui((ServerPlayer) player,
                            new SimpleMenuProvider((id, inventory, p) ->
                                    new FavorContainerMenu(id, inventory, ifavor, deity),
                                    TextComponent.EMPTY),
                            buf -> {
                                buf.writeNbt(ifavor.serializeNBT());
                                buf.writeBoolean(true);
                                buf.writeResourceLocation(deity);
                            }
                    );
                    return InteractionResult.SUCCESS;
                }
            } else if (!(isArmorLocked() && isHandsLocked() && isBlockLocked() && isAltarPoseLocked())) {
                // open altar GUI
                NetworkHooks.openGui((ServerPlayer) player,
                        new SimpleMenuProvider((id, inv, p) ->
                                new AltarContainerMenu(id, inv, this.inventory, this),
                                TextComponent.EMPTY),
                        buf -> {
                            buf.writeInt(this.getId());
                        }
                );
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        // write deity
        if (getDeity().isPresent()) {
            compound.putString(KEY_DEITY, getDeity().get().toString());
        }
        // write altar
        compound.putString(KEY_ALTAR, getAltar().toString());
        // write inventory
        ListTag listNBT = new ListTag();
        // write inventory slots to NBT
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                CompoundTag slotNBT = new CompoundTag();
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
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        // read deity
        if (compound.contains(KEY_DEITY)) {
            String deity = compound.getString(KEY_DEITY);
            if (!deity.isEmpty()) {
                setDeity(Optional.of(ResourceLocation.tryParse(deity)));
            }
        }
        // read altar
        if(compound.contains(KEY_ALTAR)) {
            setAltar(ResourceLocation.tryParse(compound.getString(KEY_ALTAR)));
        }
        // init inventory
        initInventory();
        // read inventory
        final ListTag list = compound.getList(KEY_INVENTORY, 10);
        // read inventory slots from NBT
        for (int i = 0; i < list.size(); i++) {
            CompoundTag slotNBT = list.getCompound(i);
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

    public SimpleContainer getInventory() {
        return this.inventory;
    }

    public void initInventory() {
        SimpleContainer simplecontainer = this.inventory;
        this.inventory = new SimpleContainer(INV_SIZE);
        if (simplecontainer != null) {
            simplecontainer.removeListener(this);
            int i = Math.min(simplecontainer.getContainerSize(), this.inventory.getContainerSize());

            for (int j = 0; j < i; ++j) {
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
    public void refreshDimensions() {
        double d0 = this.getX();
        double d1 = this.getY();
        double d2 = this.getZ();
        super.refreshDimensions();
        this.setPos(d0, d1, d2);
    }

    @Override
    public void containerChanged(Container inv) {
        this.refreshDimensions();
        // send packet to client to notify change
        if (!this.level.isClientSide) {
            ItemStack block = getBlockBySlot();
            RPGGods.CHANNEL.send(PacketDistributor.ALL.noArg(), new SUpdateAltarPacket(this.getId(), block));
        }
    }

    /**
     * Applies the Altar properties to this entity
     *
     * @param altarId the Altar with properties to use
     */
    public void applyAltarProperties(final ResourceLocation altarId) {
        // query altar by id
        Altar altar = RPGGods.ALTAR_MAP.getOrDefault(altarId, Altar.EMPTY);
        // apply properties
        setDeity(altar.getDeity());
        setFemale(altar.isFemale());
        setSlim(altar.isSlim());
        setArmorLocked(altar.getItems().isArmorLocked());
        setHandsLocked(altar.getItems().isHandsLocked());
        setBlockLocked(altar.getItems().isBlockLocked());
        setAltarPose(altar.getPose());
        setAltarPoseLocked(altar.isPoseLocked());
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            setItemSlot(slot, altar.getItems().getItemStackFromSlot(slot).copy());
        }
        setBlockSlot(new ItemStack(altar.getItems().getBlock().asItem()));
        // custom name
        if (altar.getDeity().isPresent()) {
            setCustomName(DeityHelper.getName(altarId));
        } else if (altar.getName().isPresent()) {
            setCustomName(new TextComponent(altar.getName().get()));
        }
        // save altar id
        setAltar(altarId);
    }

    /**
     * Directly writes properties from an Altar to a Compound NBT Tag
     * @param altarId the Altar ID
     * @param altar the Altar as retrieved from the altar map
     * @param rotation the structure rotation
     * @return a CompoundTag that represents the Altar applied to an AltarEntity
     */
    public static CompoundTag writeAltarProperties(final ResourceLocation altarId, final Altar altar, Rotation rotation) {
        // create compound tag
        CompoundTag compoundTag = new CompoundTag();
        if(null == altar) {
            return compoundTag;
        }
        // write altar properties to the tag
        // write altar
        compoundTag.putString(KEY_ALTAR, altarId.toString());
        // determine custom name
        Component customName = altar.getName().isPresent() ? new TextComponent(altar.getName().get()) : TextComponent.EMPTY;
        // write deity
        Optional<Deity> deity = Optional.empty();
        if (altar.getDeity().isPresent() && !altar.getDeity().get().toString().isEmpty()) {
            // determine string to save deity name
            ResourceLocation deityId = altar.getDeity().get();
            deity = Optional.ofNullable(RPGGods.DEITY_MAP.get(deityId));
            customName = DeityHelper.getName(altarId);
            compoundTag.putString(KEY_DEITY, deityId.toString());
        }
        // write custom name
        if(customName != TextComponent.EMPTY) {
            compoundTag.putString("CustomName", Component.Serializer.toJson(customName));
            compoundTag.putBoolean("CustomNameVisible", true);
        }
        // write inventory
        ListTag listNBT = new ListTag();
        // write inventory slots to NBT
        for(EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = altar.getItems().getItemStackFromSlot(slot);
            if (!stack.isEmpty()) {
                CompoundTag slotNBT = new CompoundTag();
                slotNBT.putByte(KEY_SLOT, (byte) slot.getFilterFlag());
                stack.save(slotNBT);
                listNBT.add(slotNBT);
            }
        }
        // write block to NBT
        ItemStack stack = new ItemStack(altar.getItems().getBlock());
        if (!stack.isEmpty()) {
            CompoundTag slotNBT = new CompoundTag();
            slotNBT.putByte(KEY_SLOT, (byte) (INV_SIZE - 1));
            stack.save(slotNBT);
            listNBT.add(slotNBT);
        }
        compoundTag.put(KEY_INVENTORY, listNBT);
        // determine flags
        byte flags = 0;
        if(altar.isFemale()) flags = (byte) (flags | 0x01);
        if(altar.isSlim()) flags = (byte) (flags | 0x02);
        // write flags
        compoundTag.putByte(KEY_FLAGS, flags);
        // determine locked flags
        byte locked = 0;
        if(altar.getItems().isArmorLocked()) locked = (byte) (locked | 0x01);
        if(altar.getItems().isHandsLocked()) locked = (byte) (locked | 0x02);
        if(altar.getItems().isBlockLocked()) locked = (byte) (locked | 0x04);
        if(altar.isPoseLocked()) locked = (byte) (locked | 0x08);
        // write locked flags
        compoundTag.putByte(KEY_LOCKED, locked);
        // write pose
        compoundTag.put(KEY_POSE, altar.getPose().serializeNBT());
        // write rotation
        compoundTag.put("Rotation", getRotationTag(rotation));
        return compoundTag;
    }

    private static ListTag getRotationTag(final Rotation rotation) {
        ListTag rotationTag = new ListTag();
        float yRot = 0;
        switch(rotation) {
            case NONE:
                yRot = 90;
                break;
            case CLOCKWISE_90:
                yRot = 180;
                break;
            case CLOCKWISE_180:
                yRot = -90;
                break;
            case COUNTERCLOCKWISE_90:
                yRot = 0;
                break;
        }
        rotationTag.add(FloatTag.valueOf(yRot));
        rotationTag.add(FloatTag.valueOf(0));
        return rotationTag;
    }

    /**
     * @return a new Altar instance with the same properties as found in this entity.
     */
    public rpggods.deity.Altar createAltarProperties() {
        AltarItems items;
        items = new AltarItems(
                getItemBySlot(EquipmentSlot.HEAD),
                getItemBySlot(EquipmentSlot.CHEST),
                getItemBySlot(EquipmentSlot.LEGS),
                getItemBySlot(EquipmentSlot.FEET),
                getItemBySlot(EquipmentSlot.MAINHAND),
                getItemBySlot(EquipmentSlot.OFFHAND),
                ForgeRegistries.BLOCKS.getValue(getBlockBySlot().getItem().getRegistryName()),
                isArmorLocked(), isHandsLocked(), isBlockLocked());
        Optional<String> name = hasCustomName() ? Optional.of(getCustomName().getString()) : Optional.empty();
        boolean enabled = true; // TODO
        ResourceLocation material = rpggods.deity.Altar.MATERIAL; // TODO
        int lightLevel = 0; // TODO
        return new rpggods.deity.Altar(enabled, name, isFemale(), isSlim(), lightLevel, items,
                material, getAltarPose(), isAltarPoseLocked());
    }

    public void setAltar(final ResourceLocation altar) {
        getEntityData().set(ALTAR, altar.toString());
    }

    public ResourceLocation getAltar() {
        String altar = getEntityData().get(ALTAR);
        if (!altar.isEmpty() && altar.contains(":")) {
            return ResourceLocation.tryParse(altar);
        }
        return new ResourceLocation("null");
    }

    public void setDeity(final Optional<ResourceLocation> deity) {
        this.deity = deity;
        String deityString = "";
        if (deity.isPresent() && !deity.get().toString().isEmpty()) {
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
        SkullBlockEntity.updateGameprofile(this.playerProfile, g -> AltarEntity.this.playerProfile = g);
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
        return getEntityData().get(LOCKED);
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
    public void setCustomName(final Component name) {
        super.setCustomName(name);
        // attempt to use custom name to set texture
        if (name != null) {
            String sName = name.getContents();
            if (sName.length() > 0 && sName.length() <= 16 && !sName.contains(":")) {
                final CompoundTag profileNBT = new CompoundTag();
                profileNBT.putString("Name", sName);
                setPlayerProfile(NbtUtils.readGameProfile(profileNBT));
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
    public GameProfile getPlayerProfile() {
        return this.playerProfile;
    }
}
