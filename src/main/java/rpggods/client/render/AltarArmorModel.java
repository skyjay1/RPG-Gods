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

public class AltarArmorModel<T extends AltarEntity> extends BipedModel<T> {

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
    public void setRotationAngles(T entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        final AltarPose pose = entityIn.getAltarPose();

        Vector3f rot = pose.get(ModelPart.HEAD);
        this.bipedHead.rotateAngleX = rot.getX();
        this.bipedHead.rotateAngleY = rot.getY();
        this.bipedHead.rotateAngleZ = rot.getZ();
        this.bipedHead.setRotationPoint(0.0F, 1.0F, 0.0F);
        this.bipedBody.rotateAngleX = 0.0F;
        this.bipedBody.rotateAngleY = 0.0F;
        this.bipedBody.rotateAngleZ = 0.0F;
        rot = pose.get(ModelPart.LEFT_ARM);
        this.bipedLeftArm.rotateAngleX = rot.getX();
        this.bipedLeftArm.rotateAngleY = rot.getY();
        this.bipedLeftArm.rotateAngleZ = rot.getZ();
        rot = pose.get(ModelPart.RIGHT_ARM);
        this.bipedRightArm.rotateAngleX = rot.getX();
        this.bipedRightArm.rotateAngleY = rot.getY();
        this.bipedRightArm.rotateAngleZ = rot.getZ();
        rot = pose.get(ModelPart.LEFT_LEG);
        this.bipedLeftLeg.rotateAngleX = rot.getX();
        this.bipedLeftLeg.rotateAngleY = rot.getY();
        this.bipedLeftLeg.rotateAngleZ = rot.getZ();
        this.bipedLeftLeg.setRotationPoint(1.9F, 11.0F, 0.0F);
        rot = pose.get(ModelPart.RIGHT_LEG);
        this.bipedRightLeg.rotateAngleX = rot.getX();
        this.bipedRightLeg.rotateAngleY = rot.getY();
        this.bipedRightLeg.rotateAngleZ = rot.getZ();
        this.bipedRightLeg.setRotationPoint(-1.9F, 11.0F, 0.0F);
        this.bipedHeadwear.copyModelAngles(this.bipedHead);
    }
}