package rpggods.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import rpggods.RGRegistry;
import rpggods.RPGGods;
import rpggods.deity.Altar;
import rpggods.entity.AltarEntity;

public class AltarItem extends Item {
    public AltarItem(Properties properties) {
        super(properties);
    }

    @Override
    public ActionResultType onItemUse(ItemUseContext context) {
        Direction direction = context.getFace();
        if (direction == Direction.DOWN) {
            return ActionResultType.FAIL;
        } else {
            World world = context.getWorld();
            BlockItemUseContext blockitemusecontext = new BlockItemUseContext(context);
            BlockPos blockpos = blockitemusecontext.getPos();
            ItemStack itemstack = context.getItem();
            Vector3d vector3d = Vector3d.copyCenteredHorizontally(blockpos);
            AxisAlignedBB axisalignedbb = RGRegistry.EntityReg.ALTAR.getSize().func_242285_a(vector3d.getX(), vector3d.getY(), vector3d.getZ());
            if (world.hasNoCollisions((Entity)null, axisalignedbb, (entity) -> {
                return true;
            }) && world.getEntitiesWithinAABBExcludingEntity((Entity)null, axisalignedbb).isEmpty()) {
                // DEBUG:
                ResourceLocation DEBUG = new ResourceLocation("greek:artemis");
                Altar altar = RPGGods.ALTAR.get(DEBUG).orElse(Altar.EMPTY);
                RPGGods.LOGGER.debug("Creating altar from '" + DEBUG  + "' " + altar);

                if (world instanceof ServerWorld) {
                    ServerWorld serverworld = (ServerWorld)world;
                    AltarEntity altarEntity = AltarEntity.createAltar(world, blockpos, altar);
                    if (altarEntity == null) {
                        return ActionResultType.FAIL;
                    }
                    serverworld.func_242417_l(altarEntity);
                    float f = (float) MathHelper.floor((MathHelper.wrapDegrees(context.getPlacementYaw() - 180.0F) + 22.5F) / 45.0F) * 45.0F;
                    altarEntity.setLocationAndAngles(altarEntity.getPosX(), altarEntity.getPosY(), altarEntity.getPosZ(), f, 0.0F);
                    world.addEntity(altarEntity);
                    world.playSound((PlayerEntity)null, altarEntity.getPosX(), altarEntity.getPosY(), altarEntity.getPosZ(), SoundEvents.ENTITY_ARMOR_STAND_PLACE, SoundCategory.BLOCKS, 0.75F, 0.8F);
                }

                itemstack.shrink(1);
                return ActionResultType.func_233537_a_(world.isRemote);
            } else {
                return ActionResultType.FAIL;
            }
        }
    }

}
