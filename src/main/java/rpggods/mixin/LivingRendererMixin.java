package rpggods.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.util.LazyOptional;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import rpggods.RPGGods;
import rpggods.tameable.ITameable;

@Mixin(LivingEntityRenderer.class)
public class LivingRendererMixin {

    @ModifyVariable(
            at=@At("STORE"),
            method="render(Lnet/minecraft/entity/LivingEntity;FFLcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;I)V",
            index=7)
    private boolean shouldSit(boolean shouldSit, LivingEntity entity, float entityYaw, float partialTicks, PoseStack matrixStack, MultiBufferSource buffer, int packedLight) {
        LazyOptional<ITameable> tameable = entity.getCapability(RPGGods.TAMEABLE);
        if(tameable.isPresent() && tameable.orElse(null).isSitting()) {
            return true;
        }
        return shouldSit;
    }


    /*
        @ModifyVariable(
            at=@At(value = "FIELD", target = "Lnet/minecraft/client/renderer/entity/model/EntityModel;riding:Z",
                    shift = At.Shift.AFTER, opcode = Opcodes.PUTFIELD),
            method="render(Lnet/minecraft/entity/LivingEntity;FFLcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;I)V",
            print=true)
    private boolean isModelRiding(boolean shouldSit, LivingEntity entity, float entityYaw, float partialTicks, MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight) {
        return true;
    }
     */
}
