package rpggods.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemStack;
import rpggods.blockentity.BrazierBlockEntity;

public class BrazierBlockEntityRenderer implements BlockEntityRenderer<BrazierBlockEntity> {

    protected final BlockEntityRendererProvider.Context context;

    public BrazierBlockEntityRenderer(final BlockEntityRendererProvider.Context context) {
        this.context = context;
    }

    @Override
    public void render(BrazierBlockEntity vaseBlockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
        final ItemStack itemstack = vaseBlockEntity.getItem(0);
        if (!itemstack.isEmpty()) {
            final float scale = 0.315F;
            poseStack.pushPose();
            // transforms
            poseStack.translate(0.5D, 0.85D, 0.5D);
            poseStack.scale(scale, scale, scale);
            // render the item stack
            Minecraft.getInstance().getItemRenderer().renderStatic(itemstack, ItemTransforms.TransformType.FIXED, packedLight,
                    OverlayTexture.NO_OVERLAY, poseStack, bufferSource, 0);
            // finish rendering
            poseStack.popPose();
        }
    }
}
