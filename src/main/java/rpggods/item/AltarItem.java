package rpggods.item;

import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import rpggods.RGRegistry;
import rpggods.RPGGods;
import rpggods.entity.AltarEntity;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class AltarItem extends Item {

    public static final String KEY_ALTAR = "altar";

    public AltarItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        String sAltarId = stack.getOrCreateTag().getString(KEY_ALTAR);
        // create translation key using altar information
        if(sAltarId != null && !sAltarId.isEmpty()) {
            ResourceLocation altarId = ResourceLocation.tryParse(sAltarId);
            // determine if altar is a deity
            Optional<rpggods.deity.Altar> altar = RPGGods.ALTAR.get(altarId);
            if(altar.isPresent() && altar.get().getDeity().isPresent()) {
                return Component.translatable("item.rpggods.altar_x",
                        Component.translatable(rpggods.deity.Altar.createTranslationKey(altarId)));
            }
        }
        // fallback when no altar information provided
        return Component.translatable(this.getDescriptionId(stack));
    }

    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        String sAltarId = stack.getOrCreateTag().getString(KEY_ALTAR);
        // create translation key using altar information
        if(!sAltarId.isEmpty()) {
            ResourceLocation altarId = ResourceLocation.tryParse(sAltarId);
            // determine if altar is not a deity but has a name
            Optional<rpggods.deity.Altar> altar = RPGGods.ALTAR.get(altarId);
            if(altar.isPresent() && !altar.get().getDeity().isPresent() && altar.get().getName().isPresent()) {
                tooltip.add(Component.literal(altar.get().getName().get()));
            }
        }
    }

    public static void addAltarItems(List<ItemStack> items) {
        // add altar item stacks for each registered altar
        for(ResourceLocation altarId : RPGGods.ALTAR.getKeys()) {
            ItemStack itemStack = new ItemStack(RGRegistry.ALTAR_ITEM.get());
            itemStack.getOrCreateTag().putString(AltarItem.KEY_ALTAR, altarId.toString());
            items.add(itemStack);
        }
    }

    // unused
    public static void addStatueItemsOnly(List<ItemStack> items) {
        // add altar item stacks for each registered altar that does not have a deity
        for(ResourceLocation altarId : RPGGods.ALTAR.getKeys()) {
            if(!RPGGods.ALTAR.get(altarId).get().getDeity().isPresent()) {
                ItemStack itemStack = new ItemStack(RGRegistry.ALTAR_ITEM.get());
                itemStack.getOrCreateTag().putString(AltarItem.KEY_ALTAR, altarId.toString());
                items.add(itemStack);
            }
        }
    }

    @Override
    public void fillItemCategory(CreativeModeTab group, NonNullList<ItemStack> items) {
        if (this.allowedIn(group)) {
            addAltarItems(items);
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Direction direction = context.getClickedFace();
        if (direction == Direction.DOWN) {
            return InteractionResult.FAIL;
        } else {
            Level world = context.getLevel();
            BlockPlaceContext blockitemusecontext = new BlockPlaceContext(context);
            BlockPos blockpos = blockitemusecontext.getClickedPos();
            ItemStack itemstack = context.getItemInHand();
            Vec3 vector3d = Vec3.atBottomCenterOf(blockpos);
            AABB axisalignedbb = RGRegistry.ALTAR_TYPE.get().getDimensions().makeBoundingBox(vector3d.x(), vector3d.y(), vector3d.z());
            if (world.noCollision(null, axisalignedbb) && world.getEntities(null, axisalignedbb).isEmpty()) {
                if (world instanceof ServerLevel) {
                    ServerLevel serverworld = (ServerLevel)world;
                    // determine altar properties to apply
                    String sAltarId = context.getItemInHand().getOrCreateTag().getString(KEY_ALTAR);
                    ResourceLocation altarId = ResourceLocation.tryParse(sAltarId);
                    // crate altar entity
                    AltarEntity altarEntity = AltarEntity.createAltar(world, blockpos, context.getHorizontalDirection().getOpposite(), altarId);
                    if (altarEntity == null) {
                        return InteractionResult.FAIL;
                    }
                    serverworld.addFreshEntity(altarEntity);
                    world.playSound(null, altarEntity.getX(), altarEntity.getY(), altarEntity.getZ(), SoundEvents.ARMOR_STAND_PLACE, SoundSource.BLOCKS, 0.75F, 0.8F);
                }

                itemstack.shrink(1);
                return InteractionResult.sidedSuccess(world.isClientSide);
            } else {
                return InteractionResult.FAIL;
            }
        }
    }

}
