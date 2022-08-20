package rpggods.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import rpggods.RGRegistry;
import rpggods.RPGGods;
import rpggods.block.BrazierBlock;
import rpggods.deity.Deity;
import rpggods.entity.AltarEntity;
import rpggods.event.FavorEventHandler;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public class BrazierBlockEntity extends BlockEntity implements Container, Nameable {

    private static final String KEY_COOLDOWN = "Cooldown";
    private static final String KEY_OWNER = "Owner";

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(1, ItemStack.EMPTY);
    private LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> createUnSidedHandler());

    protected static final int MAX_COOLDOWN = 6;
    protected int cooldownTime = -1;
    protected long tickedGameTime;
    protected UUID owner;
    protected Component name;

    public BrazierBlockEntity(BlockPos pos, BlockState state) {
        super(RGRegistry.BRAZIER_TYPE.get(), pos, state);
    }

    // TICK AND COOLDOWN

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T blockEntity) {
        if(blockEntity instanceof BrazierBlockEntity brazier && level instanceof ServerLevel) {
            --brazier.cooldownTime;
            brazier.tickedGameTime = level.getGameTime();
            if (!brazier.isOnCooldown()) {
                brazier.setCooldown(0);
                brazier.tryBurnOffering((ServerLevel)level, blockPos, blockState);
            }
        }
    }

    public void tryBurnOffering(ServerLevel level, BlockPos blockPos, BlockState blockState) {
        // do not burn offering when block is not lit
        if(blockState.getValue(BrazierBlock.WATERLOGGED) || !blockState.getValue(BrazierBlock.LIT)) {
            return;
        }
        // do not burn offering when inventory is empty
        if(null == getInventory() || getInventory().isEmpty() || getItem(0).isEmpty()) {
            return;
        }
        // do not burn offering when owner is missing or offline
        final Player player = getOwnerPlayer(level);
        if(null == player) {
            return;
        }
        // update cooldown
        setCooldown(MAX_COOLDOWN);
        // locate the nearest altar
        final AABB aabb = new AABB(blockPos).inflate(1.0D);
        final Vec3 vec = Vec3.atCenterOf(blockPos);
        final AltarEntity altar = level.getNearestEntity(AltarEntity.class, TargetingConditions.forNonCombat(), null, vec.x, vec.y, vec.z, aabb);
        // do not burn offering when no deity altar is found
        if(null == altar || !altar.getDeity().isPresent()) {
            return;
        }
        // burn the offering
        final ItemStack offering = removeItem(0, 1);
        final ResourceLocation deity = altar.getDeity().get();
        RPGGods.getFavor(player).ifPresent(favor -> {
            Optional<ItemStack> result = FavorEventHandler.onOffering(Optional.of(altar), deity, player, favor, offering, true);
            if(!result.isPresent()) {
                // the offering failed, drop the item stack instead
                Containers.dropItemStack(level, vec.x, vec.y + 0.5D, vec.z, offering);
                level.sendParticles(ParticleTypes.ANGRY_VILLAGER, vec.x, vec.y + 0.5D, vec.z, 3, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        });
        // update block entity
        setChanged();
    }

    public void setCooldown(int cooldown) {
        this.cooldownTime = cooldown;
    }

    public boolean isOnCooldown() {
        return this.cooldownTime > 0;
    }

    // OWNER

    public void setOwner(@Nullable final UUID owner) {
        this.owner = owner;
        this.setChanged();
    }

    @Nullable
    public UUID getOwner() {
        return this.owner;
    }

    @Nullable
    public Player getOwnerPlayer(final Level level) {
        if(null == this.owner) {
            return null;
        }
        return level.getPlayerByUUID(this.owner);
    }

    // CLIENT-SERVER SYNC

    /**
     * Called when the chunk is saved
     * @return the compound tag to use in #handleUpdateTag
     */
    @Override
    public CompoundTag getUpdateTag() {
        return ContainerHelper.saveAllItems(super.getUpdateTag(), inventory);
    }

    /**
     * Called when the chunk is loaded
     * @param tag the compound tag
     */
    @Override
    public void handleUpdateTag(final CompoundTag tag) {
        super.handleUpdateTag(tag);
        ContainerHelper.loadAllItems(tag, inventory);
        inventoryChanged();
    }

    // INVENTORY //

    public NonNullList<ItemStack> getInventory() {
        return this.inventory;
    }

    public void dropAllItems() {
        if (this.level != null && !this.level.isClientSide()) {
            Containers.dropContents(this.level, this.getBlockPos(), this.getInventory());
        }
        this.inventoryChanged();
    }

    public void inventoryChanged() {
        if (getLevel() != null && !getLevel().isClientSide()) {
            getLevel().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
        ItemStack itemStack = getItem(0);
        this.name = itemStack.hasCustomHoverName() ? itemStack.getHoverName() : null;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        this.inventoryChanged();
    }

    @Override
    public void clearContent() {
        this.inventory.clear();
        this.setChanged();
    }

    @Override
    public int getContainerSize() {
        return this.inventory.size();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return isEmpty() || getItem(0).getCount() < getMaxStackSize();
    }

    @Override
    public boolean isEmpty() {
        return this.inventory.get(0).isEmpty();
    }

    /**
     * Returns the stack in the given slot.
     */
    public ItemStack getItem(int index) {
        return index >= 0 && index < this.inventory.size() ? this.inventory.get(index) : ItemStack.EMPTY;
    }

    /**
     * Removes up to a specified number of items from an inventory slot and returns them in a new stack.
     */
    @Override
    public ItemStack removeItem(int index, int count) {
        ItemStack itemStack = ContainerHelper.removeItem(this.inventory, index, count);
        return itemStack;
    }

    /**
     * Removes a stack from the given slot and returns it.
     */
    @Override
    public ItemStack removeItemNoUpdate(int index) {
        ItemStack itemStack = ContainerHelper.takeItem(this.inventory, index);
        return itemStack;
    }

    /**
     * Sets the given item stack to the specified slot in the inventory (can be crafting or armor sections).
     */
    @Override
    public void setItem(int index, ItemStack stack) {
        if (index >= 0 && index < this.inventory.size()) {
            this.inventory.set(index, stack);
            this.inventoryChanged();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.level.getBlockEntity(this.worldPosition) != this) {
            return false;
        } else {
            return !(player.distanceToSqr((double) this.worldPosition.getX() + 0.5D, (double) this.worldPosition.getY() + 0.5D,
                    (double) this.worldPosition.getZ() + 0.5D) > 64.0D);
        }
    }

    // NBT AND SAVING

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.cooldownTime = tag.getInt(KEY_COOLDOWN);
        if(tag.contains(KEY_OWNER)) {
            setOwner(tag.getUUID(KEY_OWNER));
        }
        this.inventory.clear();
        ContainerHelper.loadAllItems(tag, this.inventory);
        this.inventoryChanged();
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(KEY_COOLDOWN, this.cooldownTime);
        if(this.owner != null) {
            tag.putUUID(KEY_OWNER, this.owner);
        }
        ContainerHelper.saveAllItems(tag, this.inventory, true);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // NAMEABLE

    protected Component getDefaultName() {
        return new TranslatableComponent("container.brazier");
    }

    @Override
    public Component getName() {
        return this.name != null ? this.name : this.getDefaultName();
    }

    @Nullable
    @Override
    public Component getCustomName() {
        return this.name;
    }

    // CAPABILITY

    protected IItemHandler createUnSidedHandler() {
        return new InvWrapper(this);
    }

    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (!this.remove && cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY ) {
            return itemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandler.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        itemHandler = LazyOptional.of(() -> createUnSidedHandler());
    }
}
