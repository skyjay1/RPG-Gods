package rpggods.client.render;

import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.util.math.vector.Vector3f;
import rpggods.altar.AltarPose;
import rpggods.altar.ModelPart;
import rpggods.entity.AltarEntity;

import java.util.Collection;
import java.util.Map;

public class AltarArmorModel extends BipedModel<AltarEntity> {

    public AltarArmorModel(float modelSize) {
        this(modelSize, 64, 32);
    }

    protected AltarArmorModel(float modelSize, int textureWidthIn, int textureHeightIn) {
        super(modelSize, 0.0F, textureWidthIn, textureHeightIn);
    }

    /**
     * Sets this entity's model rotation angles
     */
    @Override
    public void setupAnim(AltarEntity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        final AltarPose pose = entityIn.getAltarPose();

        Vector3f rot = pose.get(ModelPart.HEAD);
        this.head.xRot = rot.x();
        this.head.yRot = rot.y();
        this.head.zRot = rot.z();
        this.head.setPos(0.0F, 1.0F, 0.0F);
        this.body.xRot = 0.0F;
        this.body.yRot = 0.0F;
        this.body.zRot = 0.0F;
        rot = pose.get(ModelPart.LEFT_ARM);
        this.leftArm.xRot = rot.x();
        this.leftArm.yRot = rot.y();
        this.leftArm.zRot = rot.z();
        rot = pose.get(ModelPart.RIGHT_ARM);
        this.rightArm.xRot = rot.x();
        this.rightArm.yRot = rot.y();
        this.rightArm.zRot = rot.z();
        rot = pose.get(ModelPart.LEFT_LEG);
        this.leftLeg.xRot = rot.x();
        this.leftLeg.yRot = rot.y();
        this.leftLeg.zRot = rot.z();
        this.leftLeg.setPos(1.9F, 11.0F, 0.0F);
        rot = pose.get(ModelPart.RIGHT_LEG);
        this.rightLeg.xRot = rot.x();
        this.rightLeg.yRot = rot.y();
        this.rightLeg.zRot = rot.z();
        this.rightLeg.setPos(-1.9F, 11.0F, 0.0F);
        this.hat.copyFrom(this.head);
    }
}