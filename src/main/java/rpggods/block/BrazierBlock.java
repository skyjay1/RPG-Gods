package rpggods.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import rpggods.RGRegistry;
import rpggods.RPGGods;
import rpggods.block.entity.BrazierBlockEntity;

public class BrazierBlock extends Block implements EntityBlock, SimpleWaterloggedBlock {

    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    protected static final VoxelShape SHAPE = Shapes.or(
            Block.box(3.0D, 6.0D, 3.0D, 13.0D, 12.0D, 13.0D),
            Shapes.joinUnoptimized(
                    Block.box(1.0D, 8.0D, 1.0D, 15.0D, 14.0D, 15.0D),
                    Block.box(3.0D, 10.0D, 3.0D, 13.0D, 14.0D, 13.0D), BooleanOp.ONLY_FIRST));

    public BrazierBlock(final Properties properties) {
        super(properties);
        this.registerDefaultState(this.getStateDefinition().any()
                .setValue(LIT, true)
                .setValue(POWERED, false)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT, WATERLOGGED, POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluid = context.getLevel().getFluidState(context.getClickedPos());
        boolean waterlogged = fluid.is(FluidTags.WATER);
        return this.defaultBlockState().setValue(WATERLOGGED, waterlogged).setValue(LIT, !waterlogged);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos blockPos, BlockState blockState, @Nullable LivingEntity livingEntity, ItemStack itemStack) {
        // update the player owner in the block entity
        if(livingEntity instanceof Player) {
            BlockEntity blockEntity = level.getBlockEntity(blockPos);
            if(blockEntity instanceof BrazierBlockEntity brazier) {
                brazier.setOwner(livingEntity.getUUID());
            }
        }
    }

    @Override
    public void onPlace(BlockState blockState, Level level, BlockPos blockPos, BlockState oldState, boolean isMoving) {
        if (!oldState.is(blockState.getBlock())) {
            this.checkPoweredState(level, blockPos, blockState);
        }
    }

    @Override
    public BlockState updateShape(BlockState stateIn, Direction facing, BlockState facingState, LevelAccessor level,
                                  BlockPos currentPos, BlockPos facingPos) {
        boolean lit = stateIn.getValue(LIT);
        if (stateIn.getValue(WATERLOGGED)) {
            level.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
            lit = false;
        }
        return stateIn.setValue(LIT, lit);
    }

    @Override
    public InteractionResult use(final BlockState state, final Level level, final BlockPos pos,
                                 final Player player, final InteractionHand hand, final BlockHitResult hit) {
        final ItemStack heldItem = player.getItemInHand(hand);
        final BlockEntity blockEntity = level.getBlockEntity(pos);
        // process held item
        if (blockEntity instanceof BrazierBlockEntity brazierBlockEntity) {
            ItemStack invStack = brazierBlockEntity.getItem(0);
            // process flint and steel when not waterlogged and not lit
            if(heldItem.is(Items.FLINT_AND_STEEL) && !state.getValue(WATERLOGGED) && !state.getValue(LIT)) {
                level.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_CLIENTS);
                level.playSound(player, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                player.swing(hand);
                return InteractionResult.CONSUME;
            }
            // determine whether to insert or remove item
            boolean invEmpty = invStack.isEmpty();
            boolean heldEmpty = heldItem.isEmpty();
            // remove one item from held item stack and add it to the block entity inventory
            if (invEmpty && !heldEmpty) {
                ItemStack copy = heldItem.split(1);
                brazierBlockEntity.setItem(0, copy);
            }
            // remove and drop inventory item
            if(!invEmpty && heldEmpty) {
                invStack = brazierBlockEntity.removeItem(0, 1);
                Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, invStack);
            }
            return InteractionResult.CONSUME;
        }
        return InteractionResult.sidedSuccess(level.isClientSide() && !heldItem.isEmpty());
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            // drop items from inventory
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!level.isClientSide() && blockEntity instanceof BrazierBlockEntity brazierBlockEntity) {
                brazierBlockEntity.dropAllItems();
                level.updateNeighbourForOutputSignal(pos, this);
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos blockPos, BlockState blockState, FluidState fluidState) {
        if (!blockState.getValue(BlockStateProperties.WATERLOGGED) && fluidState.is(FluidTags.WATER)) {
            boolean flag = blockState.getValue(LIT);
            if (flag) {
                if (!level.isClientSide()) {
                    level.playSound(null, blockPos, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 1.0F, 1.0F);
                }
                level.gameEvent(null, GameEvent.BLOCK_CHANGE, blockPos);
            }

            level.setBlock(blockPos, blockState.setValue(WATERLOGGED, Boolean.TRUE).setValue(LIT, Boolean.FALSE), 3);
            level.scheduleTick(blockPos, fluidState.getType(), fluidState.getType().getTickDelay(level));
            return true;
        } else {
            return false;
        }
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : Fluids.EMPTY.defaultFluidState();
    }

    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter worldIn, final BlockPos pos, final CollisionContext cxt) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return RGRegistry.BlockEntityReg.BRAZIER.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState blockState, BlockEntityType<T> blockEntityType) {
        if(blockEntityType == RGRegistry.BlockEntityReg.BRAZIER.get() && RPGGods.CONFIG.isBrazierEnabled()) {
            return BrazierBlockEntity::tick;
        }
        return null;
    }

    @Override
    public void animateTick(BlockState blockState, Level level, BlockPos blockPos, RandomSource random) {
        if(blockState.getValue(LIT) && random.nextInt(3) == 0) {
            Vec3 vec = Vec3.atCenterOf(blockPos);
            level.addParticle(ParticleTypes.SMOKE,
                    vec.x + 0.8D * (random.nextDouble() - 0.5D),
                    vec.y + 0.35D + 0.5D * (random.nextDouble()),
                    vec.z + 0.8D * (random.nextDouble() - 0.5D),
                    0, 0, 0);
        }
    }

    // Redstone methods

    @Override
    public void neighborChanged(BlockState blockState, Level level, BlockPos blockPos, Block neighborBlock, BlockPos neighborPos, boolean b) {
        this.checkPoweredState(level, blockPos, blockState);
    }

    private void checkPoweredState(Level level, BlockPos blockPos, BlockState blockState) {
        boolean powered = level.hasNeighborSignal(blockPos);
        if (powered != blockState.getValue(POWERED)) {
            level.setBlock(blockPos, blockState.setValue(POWERED, powered), Block.UPDATE_INVISIBLE);
        }
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level worldIn, BlockPos pos) {
        final BlockEntity blockEntity = worldIn.getBlockEntity(pos);
        if (blockEntity instanceof Container container) {
            return AbstractContainerMenu.getRedstoneSignalFromContainer(container);
        }
        return 0;
    }
}
