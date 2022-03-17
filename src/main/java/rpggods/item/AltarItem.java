package rpggods.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import rpggods.RGRegistry;
import rpggods.RPGGods;
import rpggods.deity.Altar;
import rpggods.entity.AltarEntity;

import javax.annotation.Nullable;
import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;

import net.minecraft.item.Item.Properties;

public class AltarItem extends Item {

    public static final String KEY_ALTAR = "altar";

    public AltarItem(Properties properties) {
        super(properties);
    }

    @Override
    public ITextComponent getName(ItemStack stack) {
        String sAltarId = stack.getOrCreateTag().getString(KEY_ALTAR);
        // create translation key using altar information
        if(sAltarId != null && !sAltarId.isEmpty()) {
            ResourceLocation altarId = ResourceLocation.tryParse(sAltarId);
            // determine if altar is a deity
            Optional<Altar> altar = RPGGods.ALTAR.get(altarId);
            if(altar.isPresent() && altar.get().getDeity().isPresent()) {
                return new TranslationTextComponent("item.rpggods.altar_x",
                        new TranslationTextComponent(Altar.createTranslationKey(altarId)));
            }
        }
        // fallback when no altar information provided
        return new TranslationTextComponent(this.getDescriptionId(stack));
    }

    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        String sAltarId = stack.getOrCreateTag().getString(KEY_ALTAR);
        // create translation key using altar information
        if(!sAltarId.isEmpty()) {
            ResourceLocation altarId = ResourceLocation.tryParse(sAltarId);
            // determine if altar is not a deity but has a name
            Optional<Altar> altar = RPGGods.ALTAR.get(altarId);
            if(altar.isPresent() && !altar.get().getDeity().isPresent() && altar.get().getName().isPresent()) {
                tooltip.add(new StringTextComponent(altar.get().getName().get()));
            }
        }
    }



    @Override
    public void fillItemCategory(ItemGroup group, NonNullList<ItemStack> items) {
        if (this.allowdedIn(group)) {
            // add altar item stacks for each registered altar
            for(ResourceLocation altarId : RPGGods.ALTAR.getKeys()) {
                ItemStack itemStack = new ItemStack(this);
                itemStack.getOrCreateTag().putString(KEY_ALTAR, altarId.toString());
                items.add(itemStack);
            }
        }
    }

    @Override
    public ActionResultType useOn(ItemUseContext context) {
        Direction direction = context.getClickedFace();
        if (direction == Direction.DOWN) {
            return ActionResultType.FAIL;
        } else {
            World world = context.getLevel();
            BlockItemUseContext blockitemusecontext = new BlockItemUseContext(context);
            BlockPos blockpos = blockitemusecontext.getClickedPos();
            ItemStack itemstack = context.getItemInHand();
            Vector3d vector3d = Vector3d.atBottomCenterOf(blockpos);
            AxisAlignedBB axisalignedbb = RGRegistry.EntityReg.ALTAR.getDimensions().makeBoundingBox(vector3d.x(), vector3d.y(), vector3d.z());
            if (world.noCollision((Entity)null, axisalignedbb, (entity) -> {
                return true;
            }) && world.getEntities((Entity)null, axisalignedbb).isEmpty()) {
                if (world instanceof ServerWorld) {
                    ServerWorld serverworld = (ServerWorld)world;
                    // determine altar properties to apply
                    String sAltarId = context.getItemInHand().getOrCreateTag().getString(KEY_ALTAR);
                    ResourceLocation altarId = ResourceLocation.tryParse(sAltarId);
                    Altar altar = Altar.EMPTY;
                    if(altarId != null) {
                        altar = RPGGods.ALTAR.get(altarId).orElse(Altar.EMPTY);
                    }
                    // crate altar entity
                    AltarEntity altarEntity = AltarEntity.createAltar(world, blockpos, context.getHorizontalDirection(), altar);
                    if (altarEntity == null) {
                        return ActionResultType.FAIL;
                    }
                    serverworld.addFreshEntityWithPassengers(altarEntity);
                    float f = (float) MathHelper.floor((MathHelper.wrapDegrees(context.getRotation() - 180.0F) + 22.5F) / 45.0F) * 45.0F;
                    altarEntity.moveTo(altarEntity.getX(), altarEntity.getY(), altarEntity.getZ(), f, 0.0F);
                    world.addFreshEntity(altarEntity);
                    world.playSound((PlayerEntity)null, altarEntity.getX(), altarEntity.getY(), altarEntity.getZ(), SoundEvents.ARMOR_STAND_PLACE, SoundCategory.BLOCKS, 0.75F, 0.8F);
                }

                itemstack.shrink(1);
                return ActionResultType.sidedSuccess(world.isClientSide);
            } else {
                return ActionResultType.FAIL;
            }
        }
    }

}
