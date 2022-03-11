package rpggods.client.render;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.RenderType;
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

public class AltarModel<T extends AltarEntity> extends EntityModel<T> implements IHasArm, IHasHead {


    protected ModelRenderer bipedHead;
    protected ModelRenderer bipedBody;
    protected ModelRenderer bipedBodyChest;
    protected ModelRenderer bipedRightArm;
    protected ModelRenderer bipedLeftArm;
    protected ModelRenderer bipedRightArmSlim;
    protected ModelRenderer bipedLeftArmSlim;
    protected ModelRenderer bipedRightLeg;
    protected ModelRenderer bipedLeftLeg;

    // layers
    protected ModelRenderer bipedHeadwear;
    protected ModelRenderer bipedLeftArmwear;
    protected ModelRenderer bipedRightArmwear;
    protected ModelRenderer bipedLeftArmwearSlim;
    protected ModelRenderer bipedRightArmwearSlim;
    protected ModelRenderer bipedLeftLegwear;
    protected ModelRenderer bipedRightLegwear;
    protected ModelRenderer bipedBodyWear;
//  protected ModelRenderer bipedBodyChestWear;

    private static final EnumMap<ModelPart, Collection<ModelRenderer>> ROTATION_MAP = new EnumMap<>(ModelPart.class);

    public AltarModel() {
        this(0.0F, 0.0F);
    }

    public AltarModel(final float modelSizeIn, final float yOffsetIn) {
        super(RenderType::getEntityCutoutNoCull);
        this.textureWidth = 64;
        this.textureHeight = 64;
        this.isChild = false;
        // head
        this.bipedHead = new ModelRenderer(this, 0, 0);
        this.bipedHead.addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, modelSizeIn);
        this.bipedHead.setRotationPoint(0.0F, 0.0F + yOffsetIn, 0.0F);
        // body
        this.bipedBody = new ModelRenderer(this, 16, 16);
        this.bipedBody.addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, modelSizeIn);
        this.bipedBody.setRotationPoint(0.0F, 0.0F + yOffsetIn, 0.0F);
        this.bipedBodyChest = new ModelRenderer(this);
        this.bipedBodyChest.setRotationPoint(0.0F, 1.0F, -2.0F);
        this.bipedBodyChest.rotateAngleX = -0.2182F;
        this.bipedBodyChest.setTextureOffset(19, 20).addBox(-4.01F, 0.0F, 0.0F, 8.0F, 4.0F, 1.0F, modelSizeIn);
        // full-size arms
        this.bipedLeftArm = new ModelRenderer(this, 32, 48);
        this.bipedLeftArm.addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, modelSizeIn);
        this.bipedLeftArm.setRotationPoint(5.0F, 2.0F + yOffsetIn, 0.0F);
        this.bipedLeftArm.mirror = true;
        this.bipedRightArm = new ModelRenderer(this, 40, 16);
        this.bipedRightArm.addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, modelSizeIn);
        this.bipedRightArm.setRotationPoint(-5.0F, 2.0F + yOffsetIn, 0.0F);
        // slim arms
        this.bipedLeftArmSlim = new ModelRenderer(this, 32, 48);
        this.bipedLeftArmSlim.addBox(-1.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, modelSizeIn);
        this.bipedLeftArmSlim.setRotationPoint(5.0F, 2.5F + yOffsetIn, 0.0F);
        this.bipedLeftArmSlim.mirror = true;
        this.bipedRightArmSlim = new ModelRenderer(this, 40, 16);
        this.bipedRightArmSlim.addBox(-2.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, modelSizeIn);
        this.bipedRightArmSlim.setRotationPoint(-5.0F, 2.5F + yOffsetIn, 0.0F);
        // legs
        this.bipedRightLeg = new ModelRenderer(this, 16, 48);
        this.bipedRightLeg.addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, modelSizeIn);
        this.bipedRightLeg.setRotationPoint(-2.0F, 12.0F + yOffsetIn, 0.0F);
        this.bipedLeftLeg = new ModelRenderer(this, 0, 16);
        //this.bipedLeftLeg.mirror = true;
        this.bipedLeftLeg.addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, modelSizeIn);
        this.bipedLeftLeg.setRotationPoint(2.0F, 12.0F + yOffsetIn, 0.0F);
        // layers
        // head
        this.bipedHeadwear = new ModelRenderer(this, 32, 0);
        this.bipedHeadwear.addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, modelSizeIn + 0.5F);
        this.bipedHeadwear.setRotationPoint(0.0F, 0.0F + yOffsetIn, 0.0F);
        // arms
        this.bipedLeftArmwear = new ModelRenderer(this, 48, 48);
        this.bipedLeftArmwear.addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, modelSizeIn + 0.25F);
        this.bipedLeftArmwear.setRotationPoint(5.0F, 2.0F + yOffsetIn, 0.0F);
        this.bipedRightArmwear = new ModelRenderer(this, 40, 32);
        this.bipedRightArmwear.addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, modelSizeIn + 0.25F);
        this.bipedRightArmwear.setRotationPoint(-5.0F, 2.0F + yOffsetIn, 0.0F); // 10.0F
        // slim arms
        this.bipedLeftArmwearSlim = new ModelRenderer(this, 48, 48);
        this.bipedLeftArmwearSlim.addBox(-1.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, modelSizeIn + 0.25F);
        this.bipedLeftArmwearSlim.setRotationPoint(5.0F, 2.5F + yOffsetIn, 0.0F);
        this.bipedRightArmwearSlim = new ModelRenderer(this, 40, 32);
        this.bipedRightArmwearSlim.addBox(-2.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, modelSizeIn + 0.25F);
        this.bipedRightArmwearSlim.setRotationPoint(-5.0F, 2.5F + yOffsetIn, 0.0F); // 10.0F
        // legs
        this.bipedLeftLegwear = new ModelRenderer(this, 0, 48);
        this.bipedLeftLegwear.addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, modelSizeIn + 0.25F);
        this.bipedLeftLegwear.setRotationPoint(1.9F, 12.0F + yOffsetIn, 0.0F);
        this.bipedRightLegwear = new ModelRenderer(this, 0, 32);
        this.bipedRightLegwear.addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, modelSizeIn + 0.25F);
        this.bipedRightLegwear.setRotationPoint(-1.9F, 12.0F + yOffsetIn, 0.0F);
        // body
        this.bipedBodyWear = new ModelRenderer(this, 16, 32);
        this.bipedBodyWear.addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, modelSizeIn + 0.25F);
        this.bipedBodyWear.setRotationPoint(0.0F, 0.0F + yOffsetIn, 0.0F);
//    this.bipedBodyChestWear = new ModelRenderer(this);
//    this.bipedBodyChestWear.setRotationPoint(0.0F, 1.0F, -2.0F);
//    this.bipedBodyChestWear.rotateAngleX = -0.2182F;
//    this.bipedBodyChestWear.setTextureOffset(19, 36).addBox(-4.01F, 0.0F, 0.0F, 8.0F, 4.0F, 1.0F, modelSizeIn + 0.25F);

        ROTATION_MAP.put(ModelPart.HEAD, ImmutableList.of(this.bipedHead, this.bipedHeadwear));
        ROTATION_MAP.put(ModelPart.BODY, ImmutableList.of(this.bipedBody, this.bipedBodyChest, this.bipedBodyWear/*, this.bipedBodyChestWear*/));
        ROTATION_MAP.put(ModelPart.LEFT_ARM, ImmutableList.of(this.bipedLeftArm, this.bipedLeftArmSlim, this.bipedLeftArmwear, this.bipedLeftArmwearSlim));
        ROTATION_MAP.put(ModelPart.RIGHT_ARM, ImmutableList.of(this.bipedRightArm, this.bipedRightArmSlim, this.bipedRightArmwear, this.bipedRightArmwearSlim));
        ROTATION_MAP.put(ModelPart.LEFT_LEG, ImmutableList.of(this.bipedLeftLeg, this.bipedLeftLegwear));
        ROTATION_MAP.put(ModelPart.RIGHT_LEG, ImmutableList.of(this.bipedRightLeg, this.bipedRightLegwear));
    }

    protected Iterable<ModelRenderer> getParts() {
        return ImmutableList.of(this.bipedHead, this.bipedBody, this.bipedBodyChest, this.bipedHeadwear, this.bipedBodyWear, this.bipedLeftLeg, this.bipedRightLeg, this.bipedLeftLegwear, this.bipedRightLegwear);
    }

    protected Iterable<ModelRenderer> getSlimArms() {
        return ImmutableList.of(this.bipedLeftArmSlim, this.bipedRightArmSlim, this.bipedLeftArmwearSlim, this.bipedRightArmwearSlim);
    }

    protected Iterable<ModelRenderer> getArms() {
        return ImmutableList.of(this.bipedLeftArm, this.bipedRightArm, this.bipedLeftArmwear, this.bipedRightArmwear);
    }


    @Override
    public void setRotationAngles(T entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        final AltarPose pose = entityIn.getAltarPose();
        for(final Map.Entry<ModelPart, Collection<ModelRenderer>> e : ROTATION_MAP.entrySet()) {
            // set the rotations for each part in the list
            final Vector3f rotations = pose.get(e.getKey());
            for(final ModelRenderer m : e.getValue()) {
                m.rotateAngleX = rotations.getX();
                m.rotateAngleY = rotations.getY();
                m.rotateAngleZ = rotations.getZ();
            };
        }
        // reset body rotations
        this.bipedBody.rotateAngleX = 0.0F;
        this.bipedBody.rotateAngleY = 0.0F;
        this.bipedBody.rotateAngleZ = 0.0F;
        this.bipedBodyWear.rotateAngleX = 0.0F;
        this.bipedBodyWear.rotateAngleY = 0.0F;
        this.bipedBodyWear.rotateAngleZ = 0.0F;
        this.bipedBodyChest.rotateAngleX = -0.2182F;
        this.bipedBodyChest.rotateAngleY = 0.0F;
        this.bipedBodyChest.rotateAngleZ = 0.0F;
    }

    public void translateRotateAroundBody(final Vector3f bodyTranslation, final Vector3f bodyRotation,
                                          final MatrixStack matrixStackIn, final float partialTicks) {
        // translate based on offset
        final double scale = 1.0D / Math.PI;
        matrixStackIn.translate(bodyTranslation.getX() * scale, bodyTranslation.getY() * scale, bodyTranslation.getZ() * scale);
        // rotate entire model around body rotations
        if (bodyRotation.getZ() != 0.0F) {
            matrixStackIn.rotate(Vector3f.ZP.rotation(bodyRotation.getZ()));
        }
        if (bodyRotation.getY() != 0.0F) {
            matrixStackIn.rotate(Vector3f.YP.rotation(bodyRotation.getY()));
        }
        if (bodyRotation.getX() != 0.0F) {
            matrixStackIn.rotate(Vector3f.XP.rotation(bodyRotation.getX()));
        }
    }

    @Override
    public void render(MatrixStack matrixStackIn, IVertexBuilder bufferIn, int packedLightIn, int packedOverlayIn, float red,
                       float green, float blue, float alpha) {
        // nothing here
    }

    public void render(final T entity, MatrixStack matrixStackIn, IVertexBuilder bufferIn, int packedLightIn, int packedOverlayIn,
                       final boolean female, final boolean slim, float red, float green, float blue, float alpha) {
        // update which parts can be shown for male/female
        this.bipedBodyChest.showModel = female;
        // determine which parts this block will be rendering
        final Iterable<ModelRenderer> parts = getParts();
        final Iterable<ModelRenderer> arms = (slim ? getSlimArms() : getArms());
        parts.forEach(m -> m.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha));
        arms.forEach(m -> m.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha));
    }

    @Override
    public ModelRenderer getModelHead() {
        return this.bipedHead;
    }

    @Override
    public void translateHand(HandSide sideIn, MatrixStack matrixStackIn) {
        this.getArmForSide(sideIn).translateRotate(matrixStackIn);
    }

    protected ModelRenderer getArmForSide(HandSide side) {
        return side == HandSide.LEFT ? this.bipedLeftArmSlim : this.bipedRightArmSlim;
    }
}