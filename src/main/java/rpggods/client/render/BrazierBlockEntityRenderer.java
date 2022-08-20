package rpggods.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import rpggods.blockentity.BrazierBlockEntity;

public class BrazierBlockEntityRenderer implements BlockEntityRenderer<BrazierBlockEntity> {

    protected final BlockEntityRendererProvider.Context context;

    public BrazierBlockEntityRenderer(final BlockEntityRendererProvider.Context context) {
        this.context = context;
    }

    @Override
    public void render(BrazierBlockEntity blockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
        final ItemStack itemstack = blockEntity.getItem(0);
        if (!itemstack.isEmpty()) {
            final float ticks = blockEntity.getLevel().getGameTime() + blockEntity.getBlockPos().hashCode() + partialTicks;
            final float speed = 0.125F;
            final float scale = 1.0F; //0.68F;
            poseStack.pushPose();
            // transforms
            final float offsetY = 0.065F * Mth.cos(ticks * speed);
            poseStack.translate(0.5D, 0.95D + offsetY, 0.5D);
            poseStack.scale(scale, scale, scale);
            poseStack.mulPose(Vector3f.YP.rotation(ticks * speed * Mth.PI * 0.125F));
            // render the item stack
            Minecraft.getInstance().getItemRenderer().renderStatic(itemstack, ItemTransforms.TransformType.GROUND, packedLight,
                    OverlayTexture.NO_OVERLAY, poseStack, bufferSource, 0);
            // finish rendering
            poseStack.popPose();
        }
    }
}
