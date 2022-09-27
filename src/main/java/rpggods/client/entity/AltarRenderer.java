package rpggods.client.entity;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import com.mojang.math.Vector3f;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.common.MinecraftForge;
import rpggods.RPGGods;
import rpggods.util.altar.AltarPose;
import rpggods.util.altar.HumanoidPart;
import rpggods.client.screen.AltarScreen;
import rpggods.entity.AltarEntity;

import java.util.HashMap;
import java.util.Map;

public class AltarRenderer extends LivingEntityRenderer<AltarEntity, AltarModel> {

    protected static final ResourceLocation STEVE_TEXTURE = new ResourceLocation(RPGGods.MODID, "textures/altar/steve.png");
    protected static final ResourceLocation ALEX_TEXTURE = new ResourceLocation(RPGGods.MODID, "textures/altar/alex.png");

    public static final ModelLayerLocation ALTAR_MODEL_RESOURCE = new ModelLayerLocation(new ResourceLocation(RPGGods.MODID, "altar"), "altar");
    public static final ModelLayerLocation ALTAR_INNER_ARMOR_RESOURCE = new ModelLayerLocation(new ResourceLocation(RPGGods.MODID, "altar"), "inner_armor");
    public static final ModelLayerLocation ALTAR_OUTER_ARMOR_RESOURCE = new ModelLayerLocation(new ResourceLocation(RPGGods.MODID, "altar"), "outer_armor");

    private final Map<ResourceLocation, ResourceLocation> DEITY_TEXTURES = new HashMap<>();

    public AltarRenderer(final EntityRendererProvider.Context context) {
        super(context, new AltarModel(context.bakeLayer(ALTAR_MODEL_RESOURCE)), 0.5F);
        this.addLayer(new HumanoidArmorLayer<>(this,
                new AltarArmorModel(context.bakeLayer(ALTAR_INNER_ARMOR_RESOURCE)),
                new AltarArmorModel(context.bakeLayer(ALTAR_OUTER_ARMOR_RESOURCE))));
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
        this.addLayer(new ElytraLayer<>(this, context.getModelSet()));
        this.addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getItemInHandRenderer()));
    }

    @Override
    public void render(AltarEntity entityIn, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource bufferIn, int packedLightIn) {
        // intentional omission of super call
        // pre-render event
        if (MinecraftForge.EVENT_BUS.post(new RenderLivingEvent.Pre<>(entityIn, this, partialTicks, poseStack, bufferIn, packedLightIn))) {
            return;
        }
        poseStack.pushPose();
        // rotate around entity body rotation
        poseStack.mulPose(Vector3f.YN.rotationDegrees(entityIn.yBodyRot));

        // render base
        float baseHeight = -0.5F;
        ItemStack blockItem = entityIn.getBlockBySlot();
        if(!blockItem.isEmpty()) {
            final Block block = Block.byItem(blockItem.getItem());
            if(block != null) {
                baseHeight = 0.0F;
                poseStack.pushPose();
                poseStack.translate(-0.5D, 0.0D, -0.5D);
                Minecraft.getInstance().getBlockRenderer().renderSingleBlock(block.defaultBlockState(),
                        poseStack, bufferIn, packedLightIn, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
                poseStack.popPose();
            }
        }

        // prepare to render model
        AltarPose pose = entityIn.getAltarPose();
        getModel().young = false;
        getModel().setupAnim(entityIn, entityIn.animationPosition, entityIn.animationSpeed, entityIn.tickCount, entityIn.getYHeadRot(), entityIn.getViewXRot(partialTicks));

        // determine render type
        Minecraft minecraft = Minecraft.getInstance();
        boolean flag = this.isBodyVisible(entityIn);
        boolean flag1 = !flag && !entityIn.isInvisibleTo(minecraft.player);
        boolean flag2 = minecraft.shouldEntityAppearGlowing(entityIn);
        RenderType rendertype = this.getRenderType(entityIn, flag, flag1, flag2);

        // rotate around body and translate according to pose offsets
        getModel().translateRotateAroundBody(pose.get(HumanoidPart.OFFSET), pose.get(HumanoidPart.BODY), poseStack, partialTicks);
        // translate and rotate so the model is not upside-down
        poseStack.translate(0.0F, 2.0F + baseHeight, 0.0F);
        poseStack.mulPose(Vector3f.XP.rotationDegrees(180.0F));
        if (rendertype != null) {
            VertexConsumer ivertexbuilder = bufferIn.getBuffer(rendertype);
            getModel().render(entityIn, poseStack, ivertexbuilder, packedLightIn, OverlayTexture.NO_OVERLAY,
                    entityIn.isFemale(), entityIn.isSlim(), 1.0F, 1.0F, 1.0F, 1.0F);
        }

        // render layers
        if (!entityIn.isSpectator()) {
            for(RenderLayer layerrenderer : this.layers) {
                layerrenderer.render(poseStack, bufferIn, packedLightIn, entityIn, entityIn.animationPosition, entityIn.animationSpeed, partialTicks, entityIn.tickCount, entityIn.getYHeadRot(), entityIn.getViewXRot(partialTicks));
            }
        }
        poseStack.popPose();

        // render nametag
        net.minecraftforge.client.event.RenderNameTagEvent renderNameTagEvent = new net.minecraftforge.client.event.RenderNameTagEvent(entityIn, entityIn.getDisplayName(), this, poseStack, bufferIn, packedLightIn, partialTicks);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(renderNameTagEvent);
        if (renderNameTagEvent.getResult() != net.minecraftforge.eventbus.api.Event.Result.DENY && (renderNameTagEvent.getResult() == net.minecraftforge.eventbus.api.Event.Result.ALLOW || this.shouldShowName(entityIn))) {
            this.renderNameTag(entityIn, renderNameTagEvent.getContent(), poseStack, bufferIn, packedLightIn);
        }

        // post-render event
        MinecraftForge.EVENT_BUS.post(new RenderLivingEvent.Post<>(entityIn, this, partialTicks, poseStack, bufferIn, packedLightIn));
    }

    @Override
    public boolean shouldShowName(final AltarEntity entityIn) {
        final Minecraft mc = Minecraft.getInstance();
        if(mc.screen instanceof AltarScreen) {
            return false;
        }
        final Vec3 pos = entityIn.position().add(0, entityIn.getType().getDimensions().height / 2D, 0);
        return super.shouldShowName(entityIn) && mc.crosshairPickEntity == entityIn;
    }

    @Override
    public ResourceLocation getTextureLocation(final AltarEntity entity) {
        // return deity texture
        if(entity.getDeity().isPresent() && !entity.getDeity().get().toString().isEmpty()) {
            return DEITY_TEXTURES.computeIfAbsent(entity.getDeity().get(),
                    deity -> new ResourceLocation(deity.getNamespace(), "textures/altar/" + deity.getPath() + ".png"));
        }
        // return player texture
        final GameProfile gameProfile = entity.getPlayerProfile();
        if(gameProfile != null) {
            Minecraft minecraft = Minecraft.getInstance();
            Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> map = minecraft.getSkinManager().getInsecureSkinInformation(gameProfile);
            if(map.containsKey(MinecraftProfileTexture.Type.SKIN)) {
                return minecraft.getSkinManager().registerTexture(map.get(MinecraftProfileTexture.Type.SKIN), MinecraftProfileTexture.Type.SKIN);
            }
        }
        // return default texture
        // TODO: return dynamic texture based on material
        return entity.isSlim() ? ALEX_TEXTURE : STEVE_TEXTURE;
    }

    @Override
    protected RenderType getRenderType(final AltarEntity entityIn, boolean isVisible, boolean isVisibleToPlayer, boolean isGlowing) {
        // TODO: optimize, allow for player skins, etc.
        return super.getRenderType(entityIn, isVisible, isVisibleToPlayer, isGlowing);
    }
}
