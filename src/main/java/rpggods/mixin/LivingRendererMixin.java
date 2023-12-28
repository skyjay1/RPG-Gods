package rpggods.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.util.LazyOptional;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import rpggods.RPGGods;
import rpggods.data.tameable.ITameable;

@Mixin(LivingEntityRenderer.class)
public class LivingRendererMixin {

    @ModifyVariable(
            at=@At("STORE"),
            method="render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            index=7)
    private boolean shouldSit(boolean shouldSit, LivingEntity entity, float entityYaw, float partialTicks, PoseStack matrixStack, MultiBufferSource buffer, int packedLight) {
        LazyOptional<ITameable> tameable = entity.getCapability(RPGGods.TAMEABLE);
        if(tameable.isPresent() && tameable.orElse(null).isSitting()) {
            return true;
        }
        return shouldSit;
    }
}
