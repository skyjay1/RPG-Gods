package rpggods.client.render;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.model.AgeableModel;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.entity.model.IHasArm;
import net.minecraft.client.renderer.entity.model.IHasHead;
import net.minecraft.client.renderer.model.Model;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.HandSide;
import net.minecraft.util.math.vector.Vector3f;
import rpggods.altar.AltarPose;
import rpggods.altar.ModelPart;
import rpggods.entity.AltarEntity;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

public class AltarModel extends AltarArmorModel {

    protected ModelRenderer bodyChest;
    protected ModelRenderer rightArmSlim;
    protected ModelRenderer leftArmSlim;

    // layers
    protected ModelRenderer headwear;
    protected ModelRenderer leftArmwear;
    protected ModelRenderer rightArmwear;
    protected ModelRenderer leftArmwearSlim;
    protected ModelRenderer rightArmwearSlim;
    protected ModelRenderer leftLegwear;
    protected ModelRenderer rightLegwear;
    protected ModelRenderer bodyWear;

    private static final EnumMap<ModelPart, Collection<ModelRenderer>> ROTATION_MAP = new EnumMap<>(ModelPart.class);

    public AltarModel() {
        this(0.0F, 0.0F);
    }

    public AltarModel(final float modelSizeIn, final float yOffsetIn) {
        super(modelSizeIn, 64, 64);
        this.texWidth = 64;
        this.texHeight = 64;
        this.young = false;
        // head
        this.head = new ModelRenderer(this, 0, 0);
        this.head.addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, modelSizeIn);
        this.head.setPos(0.0F, 0.0F + yOffsetIn, 0.0F);
        // body
        this.body = new ModelRenderer(this, 16, 16);
        this.body.addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, modelSizeIn);
        this.body.setPos(0.0F, 0.0F + yOffsetIn, 0.0F);
        this.bodyChest = new ModelRenderer(this);
        this.bodyChest.setPos(0.0F, 1.0F, -2.0F);
        this.bodyChest.xRot = -0.2182F;
        this.bodyChest.texOffs(19, 20).addBox(-4.01F, 0.0F, 0.0F, 8.0F, 4.0F, 1.0F, modelSizeIn);
        // full-size arms
        this.leftArm = new ModelRenderer(this, 32, 48);
        this.leftArm.addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, modelSizeIn);
        this.leftArm.setPos(5.0F, 2.0F + yOffsetIn, 0.0F);
        this.leftArm.mirror = true;
        this.rightArm = new ModelRenderer(this, 40, 16);
        this.rightArm.addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, modelSizeIn);
        this.rightArm.setPos(-5.0F, 2.0F + yOffsetIn, 0.0F);
        // slim arms
        this.leftArmSlim = new ModelRenderer(this, 32, 48);
        this.leftArmSlim.addBox(-1.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, modelSizeIn);
        this.leftArmSlim.setPos(5.0F, 2.5F + yOffsetIn, 0.0F);
        this.leftArmSlim.mirror = true;
        this.rightArmSlim = new ModelRenderer(this, 40, 16);
        this.rightArmSlim.addBox(-2.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, modelSizeIn);
        this.rightArmSlim.setPos(-5.0F, 2.5F + yOffsetIn, 0.0F);
        // legs
        this.rightLeg = new ModelRenderer(this, 16, 48);
        this.rightLeg.addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, modelSizeIn);
        this.rightLeg.setPos(-2.0F, 12.0F + yOffsetIn, 0.0F);
        this.leftLeg = new ModelRenderer(this, 0, 16);
        //this.leftLeg.mirror = true;
        this.leftLeg.addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, modelSizeIn);
        this.leftLeg.setPos(2.0F, 12.0F + yOffsetIn, 0.0F);
        // layers
        // head
        this.headwear = new ModelRenderer(this, 32, 0);
        this.headwear.addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, modelSizeIn + 0.5F);
        this.headwear.setPos(0.0F, 0.0F + yOffsetIn, 0.0F);
        // arms
        this.leftArmwear = new ModelRenderer(this, 48, 48);
        this.leftArmwear.addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, modelSizeIn + 0.25F);
        this.leftArmwear.setPos(5.0F, 2.0F + yOffsetIn, 0.0F);
        this.rightArmwear = new ModelRenderer(this, 40, 32);
        this.rightArmwear.addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, modelSizeIn + 0.25F);
        this.rightArmwear.setPos(-5.0F, 2.0F + yOffsetIn, 0.0F); // 10.0F
        // slim arms
        this.leftArmwearSlim = new ModelRenderer(this, 48, 48);
        this.leftArmwearSlim.addBox(-1.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, modelSizeIn + 0.25F);
        this.leftArmwearSlim.setPos(5.0F, 2.5F + yOffsetIn, 0.0F);
        this.rightArmwearSlim = new ModelRenderer(this, 40, 32);
        this.rightArmwearSlim.addBox(-2.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, modelSizeIn + 0.25F);
        this.rightArmwearSlim.setPos(-5.0F, 2.5F + yOffsetIn, 0.0F); // 10.0F
        // legs
        this.leftLegwear = new ModelRenderer(this, 0, 48);
        this.leftLegwear.addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, modelSizeIn + 0.25F);
        this.leftLegwear.setPos(1.9F, 12.0F + yOffsetIn, 0.0F);
        this.rightLegwear = new ModelRenderer(this, 0, 32);
        this.rightLegwear.addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, modelSizeIn + 0.25F);
        this.rightLegwear.setPos(-1.9F, 12.0F + yOffsetIn, 0.0F);
        // body
        this.bodyWear = new ModelRenderer(this, 16, 32);
        this.bodyWear.addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, modelSizeIn + 0.25F);
        this.bodyWear.setPos(0.0F, 0.0F + yOffsetIn, 0.0F);

        ROTATION_MAP.put(ModelPart.HEAD, ImmutableList.of(this.head, this.headwear));
        ROTATION_MAP.put(ModelPart.BODY, ImmutableList.of(this.body, this.bodyChest, this.bodyWear));
        ROTATION_MAP.put(ModelPart.LEFT_ARM, ImmutableList.of(this.leftArm, this.leftArmSlim, this.leftArmwear, this.leftArmwearSlim));
        ROTATION_MAP.put(ModelPart.RIGHT_ARM, ImmutableList.of(this.rightArm, this.rightArmSlim, this.rightArmwear, this.rightArmwearSlim));
        ROTATION_MAP.put(ModelPart.LEFT_LEG, ImmutableList.of(this.leftLeg, this.leftLegwear));
        ROTATION_MAP.put(ModelPart.RIGHT_LEG, ImmutableList.of(this.rightLeg, this.rightLegwear));
    }

    protected Iterable<ModelRenderer> getParts() {
        return ImmutableList.of(this.head, this.body, this.bodyChest, this.headwear, this.bodyWear, this.leftLeg, this.rightLeg, this.leftLegwear, this.rightLegwear);
    }

    protected Iterable<ModelRenderer> getSlimArms() {
        return ImmutableList.of(this.leftArmSlim, this.rightArmSlim, this.leftArmwearSlim, this.rightArmwearSlim);
    }

    protected Iterable<ModelRenderer> getArms() {
        return ImmutableList.of(this.leftArm, this.rightArm, this.leftArmwear, this.rightArmwear);
    }

    @Override
    public void setupAnim(AltarEntity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        final AltarPose pose = entityIn.getAltarPose();
        for(final Map.Entry<ModelPart, Collection<ModelRenderer>> e : ROTATION_MAP.entrySet()) {
            // set the rotations for each part in the list
            final Vector3f rotations = pose.get(e.getKey());
            for(final ModelRenderer m : e.getValue()) {
                m.xRot = rotations.x();
                m.yRot = rotations.y();
                m.zRot = rotations.z();
            };
        }
        // reset body rotations
        this.body.xRot = 0.0F;
        this.body.yRot = 0.0F;
        this.body.zRot = 0.0F;
        this.bodyWear.xRot = 0.0F;
        this.bodyWear.yRot = 0.0F;
        this.bodyWear.zRot = 0.0F;
        this.bodyChest.xRot = -0.2182F;
        this.bodyChest.yRot = 0.0F;
        this.bodyChest.zRot = 0.0F;
    }

    public void translateRotateAroundBody(final Vector3f bodyTranslation, final Vector3f bodyRotation,
                                          final MatrixStack matrixStackIn, final float partialTicks) {
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
    public void renderToBuffer(MatrixStack matrixStackIn, IVertexBuilder bufferIn, int packedLightIn, int packedOverlayIn, float red,
                       float green, float blue, float alpha) {
        // nothing here
    }

    public void render(final AltarEntity entity, MatrixStack matrixStackIn, IVertexBuilder bufferIn, int packedLightIn, int packedOverlayIn,
                       final boolean female, final boolean slim, float red, float green, float blue, float alpha) {
        // update which parts can be shown for male/female
        this.bodyChest.visible = female;
        // determine which parts this block will be rendering
        final Iterable<ModelRenderer> parts = getParts();
        final Iterable<ModelRenderer> arms = (slim ? getSlimArms() : getArms());
        parts.forEach(m -> m.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha));
        arms.forEach(m -> m.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha));
    }

    @Override
    public ModelRenderer getHead() {
        return this.head;
    }

    @Override
    public void translateToHand(HandSide sideIn, MatrixStack matrixStackIn) {
        this.getArmForSide(sideIn).translateAndRotate(matrixStackIn);
    }

    protected ModelRenderer getArmForSide(HandSide side) {
        return side == HandSide.LEFT ? this.leftArmSlim : this.rightArmSlim;
    }
}