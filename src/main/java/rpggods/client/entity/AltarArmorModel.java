package rpggods.client.entity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import com.mojang.math.Vector3f;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import rpggods.altar.AltarPose;
import rpggods.altar.HumanoidPart;
import rpggods.entity.AltarEntity;

public class AltarArmorModel extends HumanoidModel<AltarEntity> {

    protected AltarArmorModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer(CubeDeformation cubeDeformation) {
        MeshDefinition meshdefinition = HumanoidModel.createMesh(cubeDeformation, 0.0F);
        return LayerDefinition.create(meshdefinition, 64, 32);
    }

    /**
     * Sets this entity's model rotation angles
     */
    @Override
    public void setupAnim(AltarEntity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        final AltarPose pose = entityIn.getAltarPose();

        Vector3f rot = pose.get(HumanoidPart.HEAD);
        this.head.xRot = rot.x();
        this.head.yRot = rot.y();
        this.head.zRot = rot.z();
        this.head.setPos(0.0F, 1.0F, 0.0F);
        this.body.xRot = 0.0F;
        this.body.yRot = 0.0F;
        this.body.zRot = 0.0F;
        rot = pose.get(HumanoidPart.LEFT_ARM);
        this.leftArm.xRot = rot.x();
        this.leftArm.yRot = rot.y();
        this.leftArm.zRot = rot.z();
        rot = pose.get(HumanoidPart.RIGHT_ARM);
        this.rightArm.xRot = rot.x();
        this.rightArm.yRot = rot.y();
        this.rightArm.zRot = rot.z();
        rot = pose.get(HumanoidPart.LEFT_LEG);
        this.leftLeg.xRot = rot.x();
        this.leftLeg.yRot = rot.y();
        this.leftLeg.zRot = rot.z();
        this.leftLeg.setPos(1.9F, 11.0F, 0.0F);
        rot = pose.get(HumanoidPart.RIGHT_LEG);
        this.rightLeg.xRot = rot.x();
        this.rightLeg.yRot = rot.y();
        this.rightLeg.zRot = rot.z();
        this.rightLeg.setPos(-1.9F, 11.0F, 0.0F);
        this.hat.copyFrom(this.head);
    }
}