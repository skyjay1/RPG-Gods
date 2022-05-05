package rpggods.client.render;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.HumanoidArm;
import rpggods.altar.AltarPose;
import rpggods.altar.HumanoidPart;
import rpggods.entity.AltarEntity;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

public class AltarModel extends AltarArmorModel {

    protected ModelPart bodyChest;
    protected ModelPart rightArmSlim;
    protected ModelPart leftArmSlim;
    protected ModelPart leftSleeveSlim;
    protected ModelPart rightSleeveSlim;
    protected ModelPart leftSleeve;
    protected ModelPart rightSleeve;
    protected ModelPart leftPants;
    protected ModelPart rightPants;
    protected ModelPart jacket;

    private static final EnumMap<HumanoidPart, Collection<ModelPart>> ROTATION_MAP = new EnumMap<>(HumanoidPart.class);

    public AltarModel(final ModelPart root) {
        super(root);
        this.young = false;
        this.bodyChest = root.getChild("chest");
        this.leftArmSlim = root.getChild("left_arm_slim");
        this.rightArmSlim = root.getChild("right_arm_slim");
        this.leftSleeve = root.getChild("left_sleeve");
        this.rightSleeve = root.getChild("right_sleeve");
        this.leftSleeveSlim = root.getChild("left_sleeve_slim");
        this.rightSleeveSlim = root.getChild("right_sleeve_slim");
        this.leftPants = root.getChild("left_pants");
        this.rightPants = root.getChild("right_pants");
        this.jacket = root.getChild("jacket");

        ROTATION_MAP.put(HumanoidPart.HEAD, ImmutableList.of(this.head, this.hat));
        ROTATION_MAP.put(HumanoidPart.BODY, ImmutableList.of(this.body, this.bodyChest, this.jacket));
        ROTATION_MAP.put(HumanoidPart.LEFT_ARM, ImmutableList.of(this.leftArm, this.leftArmSlim, this.leftSleeve, this.leftSleeveSlim));
        ROTATION_MAP.put(HumanoidPart.RIGHT_ARM, ImmutableList.of(this.rightArm, this.rightArmSlim, this.rightSleeve, this.rightSleeveSlim));
        ROTATION_MAP.put(HumanoidPart.LEFT_LEG, ImmutableList.of(this.leftLeg, this.leftPants));
        ROTATION_MAP.put(HumanoidPart.RIGHT_LEG, ImmutableList.of(this.rightLeg, this.rightPants));
    }

    public static LayerDefinition createBodyLayer() {
        CubeDeformation cubeDeformation = CubeDeformation.NONE;
        MeshDefinition meshdefinition = HumanoidModel.createMesh(cubeDeformation, 0.0F);
        PartDefinition partdefinition = meshdefinition.getRoot();
        partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(32, 48).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, cubeDeformation), PartPose.offset(5.0F, 2.0F, 0.0F));
        partdefinition.addOrReplaceChild("left_sleeve", CubeListBuilder.create().texOffs(48, 48).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, cubeDeformation.extend(0.25F)), PartPose.offset(5.0F, 2.0F, 0.0F));
        partdefinition.addOrReplaceChild("right_sleeve", CubeListBuilder.create().texOffs(40, 32).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, cubeDeformation.extend(0.25F)), PartPose.offset(-5.0F, 2.0F, 0.0F));
        partdefinition.addOrReplaceChild("left_pants", CubeListBuilder.create().texOffs(0, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, cubeDeformation.extend(0.25F)), PartPose.offset(1.9F, 12.0F, 0.0F));
        partdefinition.addOrReplaceChild("right_pants", CubeListBuilder.create().texOffs(0, 32).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, cubeDeformation.extend(0.25F)), PartPose.offset(-1.9F, 12.0F, 0.0F));
        partdefinition.addOrReplaceChild("jacket", CubeListBuilder.create().texOffs(16, 32).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, cubeDeformation.extend(0.25F)), PartPose.ZERO);
        partdefinition.addOrReplaceChild("left_arm_slim", CubeListBuilder.create().texOffs(32, 48).addBox(-1.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, cubeDeformation), PartPose.offset(5.0F, 2.5F, 0.0F));
        partdefinition.addOrReplaceChild("right_arm_slim", CubeListBuilder.create().texOffs(40, 16).addBox(-2.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, cubeDeformation), PartPose.offset(-5.0F, 2.5F, 0.0F));
        partdefinition.addOrReplaceChild("left_sleeve_slim", CubeListBuilder.create().texOffs(48, 48).addBox(-1.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, cubeDeformation.extend(0.25F)), PartPose.offset(5.0F, 2.5F, 0.0F));
        partdefinition.addOrReplaceChild("right_sleeve_slim", CubeListBuilder.create().texOffs(40, 32).addBox(-2.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, cubeDeformation.extend(0.25F)), PartPose.offset(-5.0F, 2.5F, 0.0F));
        partdefinition.addOrReplaceChild("chest", CubeListBuilder.create().texOffs(19, 20).addBox(-4.01F, 0.0F, 0.0F, 8.0F, 4.0F, 1.0F, cubeDeformation), PartPose.offsetAndRotation(0.0F, 1.0F, -2.0F, -0.2182F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    protected Iterable<ModelPart> getParts() {
        return ImmutableList.of(this.head, this.body, this.bodyChest, this.hat, this.jacket, this.leftLeg, this.rightLeg, this.leftPants, this.rightPants);
    }

    protected Iterable<ModelPart> getSlimArms() {
        return ImmutableList.of(this.leftArmSlim, this.rightArmSlim, this.leftSleeveSlim, this.rightSleeveSlim);
    }

    protected Iterable<ModelPart> getArms() {
        return ImmutableList.of(this.leftArm, this.rightArm, this.leftSleeve, this.rightSleeve);
    }

    @Override
    public void setupAnim(AltarEntity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        final AltarPose pose = entityIn.getAltarPose();
        for(final Map.Entry<HumanoidPart, Collection<ModelPart>> e : ROTATION_MAP.entrySet()) {
            // set the rotations for each part in the list
            final Vector3f rotations = pose.get(e.getKey());
            for(final ModelPart m : e.getValue()) {
                m.xRot = rotations.x();
                m.yRot = rotations.y();
                m.zRot = rotations.z();
            };
        }
        // reset body rotations
        this.body.xRot = 0.0F;
        this.body.yRot = 0.0F;
        this.body.zRot = 0.0F;
        this.jacket.xRot = 0.0F;
        this.jacket.yRot = 0.0F;
        this.jacket.zRot = 0.0F;
        this.bodyChest.xRot = -0.2182F;
        this.bodyChest.yRot = 0.0F;
        this.bodyChest.zRot = 0.0F;
    }

    public void translateRotateAroundBody(final Vector3f bodyTranslation, final Vector3f bodyRotation,
                                          final PoseStack matrixStackIn, final float partialTicks) {
        // translate based on offset
        final double scale = 1.0D / Math.PI;
        matrixStackIn.translate(bodyTranslation.x() * scale, bodyTranslation.y() * scale, bodyTranslation.z() * scale);
        // rotate entire model around body rotations
        if (bodyRotation.z() != 0.0F) {
            matrixStackIn.mulPose(Vector3f.ZP.rotation(bodyRotation.z()));
        }
        if (bodyRotation.y() != 0.0F) {
            matrixStackIn.mulPose(Vector3f.YP.rotation(bodyRotation.y()));
        }
        if (bodyRotation.x() != 0.0F) {
            matrixStackIn.mulPose(Vector3f.XP.rotation(bodyRotation.x()));
        }
    }

    @Override
    public void renderToBuffer(PoseStack matrixStackIn, VertexConsumer bufferIn, int packedLightIn, int packedOverlayIn, float red,
                       float green, float blue, float alpha) {
        // nothing here
    }

    public void render(final AltarEntity entity, PoseStack matrixStackIn, VertexConsumer bufferIn, int packedLightIn, int packedOverlayIn,
                       final boolean female, final boolean slim, float red, float green, float blue, float alpha) {
        // update which parts can be shown for male/female
        this.bodyChest.visible = female;
        // determine which parts this block will be rendering
        final Iterable<ModelPart> parts = getParts();
        final Iterable<ModelPart> arms = (slim ? getSlimArms() : getArms());
        parts.forEach(m -> m.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha));
        arms.forEach(m -> m.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha));
    }

    @Override
    public ModelPart getHead() {
        return this.head;
    }

    @Override
    public void translateToHand(HumanoidArm sideIn, PoseStack matrixStackIn) {
        this.getArmForSide(sideIn).translateAndRotate(matrixStackIn);
    }

    protected ModelPart getArmForSide(HumanoidArm side) {
        return side == HumanoidArm.LEFT ? this.leftArmSlim : this.rightArmSlim;
    }
}