package rpggods.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import rpggods.RPGGods;
import rpggods.deity.Altar;
import rpggods.entity.AltarEntity;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

public class GlowBlock extends Block implements SimpleWaterloggedBlock {

    public static final IntegerProperty LIGHT_LEVEL = IntegerProperty.create("light", 0, 15);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public GlowBlock(final Properties prop) {
        super(prop);
        this.registerDefaultState(this.getStateDefinition().any()
                .setValue(WATERLOGGED, false)
                .setValue(LIGHT_LEVEL, 15));
    }

    protected static boolean removeGlowBlock(final Level worldIn, final BlockState state, final BlockPos pos, final int flag) {
        // remove this block and replace with air or water
        final BlockState replaceWith = state.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.defaultFluidState().createLegacyBlock()
                : Blocks.AIR.defaultBlockState();
        // replace with air OR water depending on waterlogged state
        return worldIn.setBlock(pos, replaceWith, flag);
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED).add(LIGHT_LEVEL);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : Fluids.EMPTY.defaultFluidState();
    }

    @Override
    public void onPlace(final BlockState state, final Level worldIn, final BlockPos pos, final BlockState oldState, final boolean isMoving) {
        state.setValue(WATERLOGGED, oldState.getFluidState().is(FluidTags.WATER));
        // schedule next tick
        worldIn.scheduleTick(pos, this, 4);
        if (state.getValue(BlockStateProperties.WATERLOGGED)) {
            worldIn.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(worldIn));
        }
    }

    @Override
    public void tick(final BlockState state, final ServerLevel worldIn, final BlockPos pos, final Random rand) {
        super.tick(state, worldIn, pos, rand);
        // schedule next tick
        worldIn.scheduleTick(pos, this, 4);
        if (state.getValue(BlockStateProperties.WATERLOGGED)) {
            worldIn.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(worldIn));
        }
        // check for altar entity
        AABB aabb = new AABB(pos);
        List<AltarEntity> list = worldIn.getEntitiesOfClass(AltarEntity.class, aabb);
        // check if any altar entity has light level
        boolean hasAltar = false;
        for(AltarEntity altarEntity : list) {
            Altar altar = RPGGods.ALTAR.get(altarEntity.getAltar()).orElse(Altar.EMPTY);
            if(altar.getLightLevel() > 0) {
                hasAltar = true;
                break;
            }
        }
        // remove this block if no altar found with valid light level
        if(!hasAltar) {
            removeGlowBlock(worldIn, state, pos, UPDATE_ALL);
        }
    }

    @Override
    protected boolean isAir(BlockState state) {
        return !state.getValue(WATERLOGGED);
    }

    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter worldIn, final BlockPos pos, final CollisionContext cxt) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getCollisionShape(final BlockState state, final BlockGetter worldIn, final BlockPos pos, final CollisionContext cxt) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getOcclusionShape(final BlockState state, final BlockGetter worldIn, final BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getInteractionShape(final BlockState state, final BlockGetter worldIn, final BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    public ItemStack getCloneItemStack(final BlockGetter worldIn, final BlockPos pos, final BlockState state) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canBeReplaced(final BlockState state, final BlockPlaceContext useContext) {
        return true;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return defaultBlockState();
    }

    @Override
    public RenderShape getRenderShape(final BlockState state) {
        return RenderShape.INVISIBLE;
    }
}
