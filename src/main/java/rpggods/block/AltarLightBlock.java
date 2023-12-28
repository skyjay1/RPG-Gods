package rpggods.block;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
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
import rpggods.data.deity.Altar;
import rpggods.entity.AltarEntity;

import javax.annotation.Nullable;
import java.util.List;

public class AltarLightBlock extends LightBlock implements SimpleWaterloggedBlock {

    public AltarLightBlock(final Properties prop) {
        super(prop);
        this.registerDefaultState(this.getStateDefinition().any()
                .setValue(WATERLOGGED, false)
                .setValue(LEVEL, 15));
    }

    protected static boolean removeAltarLightBlock(final Level worldIn, final BlockState state, final BlockPos pos, final int flag) {
        // replace with air OR water depending on waterlogged state
        final BlockState replaceWith = state.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.defaultFluidState().createLegacyBlock()
                : Blocks.AIR.defaultBlockState();
        return worldIn.setBlock(pos, replaceWith, flag);
    }

    @Override
    public void onPlace(final BlockState state, final Level level, final BlockPos pos, final BlockState oldState, final boolean isMoving) {
        state.setValue(WATERLOGGED, oldState.getFluidState().is(FluidTags.WATER));
        // schedule next tick
        level.scheduleTick(pos, this, 5);
        if (state.getValue(BlockStateProperties.WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
    }

    @Override
    public void tick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource rand) {
        super.tick(state, level, pos, rand);
        // schedule next tick
        level.scheduleTick(pos, this, 5);
        if (state.getValue(BlockStateProperties.WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        // check for altar entity
        AABB aabb = new AABB(pos);
        List<AltarEntity> list = level.getEntitiesOfClass(AltarEntity.class, aabb);
        // check if any altar entity has light level
        boolean hasAltar = false;
        for(AltarEntity altarEntity : list) {
            Altar altar = RPGGods.ALTAR_MAP.getOrDefault(altarEntity.getAltar(), Altar.EMPTY);
            if(altar.getLightLevel() > 0) {
                hasAltar = true;
                break;
            }
        }
        // remove this block if no altar found with valid light level
        if(!hasAltar) {
            removeAltarLightBlock(level, state, pos, UPDATE_ALL);
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
