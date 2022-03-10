package rpggods.client.render;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.LivingRenderer;
import net.minecraft.client.renderer.entity.layers.BipedArmorLayer;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.entity.layers.HeadLayer;
import net.minecraft.client.renderer.entity.layers.HeldItemLayer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.entity.model.ArmorStandArmorModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.common.MinecraftForge;
import rpggods.RPGGods;
import rpggods.altar.AltarPose;
import rpggods.altar.ModelPart;
import rpggods.client.screen.AltarScreen;
import rpggods.deity.Altar;
import rpggods.entity.AltarEntity;

import java.util.Map;

public class AltarRenderer<T extends AltarEntity, M extends AltarModel<T>> extends LivingRenderer<T, M> {

    private static final ResourceLocation TEXTURE = new ResourceLocation("greek", "textures/altar/zeus.png");
    protected static final ResourceLocation STEVE_TEXTURE = new ResourceLocation(RPGGods.MODID, "textures/altar/steve.png");
    protected static final ResourceLocation ALEX_TEXTURE = new ResourceLocation(RPGGods.MODID, "textures/altar/alex.png");

    public AltarRenderer(final EntityRendererManager renderManagerIn) {
        super(renderManagerIn, (M) new AltarModel<T>(0.0F, 0.0F), 0.5F);
        // TODO: fix armor layer
        //this.addLayer(new BipedArmorLayer<T, AltarArmorModel<T>, AltarArmorModel<T>>(this, new AltarArmorModel(0.5F), new AltarArmorModel(1.0F)));
        this.addLayer(new HeldItemLayer<>(this));
        this.addLayer(new ElytraLayer<>(this));
        this.addLayer(new HeadLayer<>(this));
    }

    @Override
    public void render(T entityIn, float entityYaw, float partialTicks, MatrixStack matrixStackIn,
                       IRenderTypeBuffer bufferIn, int packedLightIn) {
        // intentional omission of super call
        // pre-render event
        if (MinecraftForge.EVENT_BUS.post(new RenderLivingEvent.Pre<T, M>(entityIn, this, partialTicks, matrixStackIn, bufferIn, packedLightIn))) {
            return;
        }

        // render base
        float baseHeight = -0.5F;
        final BlockState base = entityIn.getBaseBlock();
        if(base != null && base.getMaterial() != Material.AIR) {
            baseHeight = 0.0F;
            matrixStackIn.push();
            matrixStackIn.translate(-0.5D, 0.0D, -0.5D);
            Minecraft.getInstance().getBlockRendererDispatcher().renderBlock(base,
                    matrixStackIn, bufferIn, packedLightIn, OverlayTexture.NO_OVERLAY, EmptyModelData.INSTANCE);
            matrixStackIn.pop();
        }

        // prepare to render model
        AltarPose pose = entityIn.getAltarPose();
        getEntityModel().isChild = false;
        getEntityModel().setRotationAngles(entityIn, 0F, 0F, 0F, 0F, 0F);

        // determine render type
        Minecraft minecraft = Minecraft.getInstance();
        boolean flag = this.isVisible(entityIn);
        boolean flag1 = !flag && !entityIn.isInvisibleToPlayer(minecraft.player);
        boolean flag2 = minecraft.isEntityGlowing(entityIn);
        RenderType rendertype = this.func_230496_a_(entityIn, flag, flag1, flag2);

        // render model
        matrixStackIn.push();
        getEntityModel().translateRotateAroundBody(pose.get(ModelPart.OFFSET), pose.get(ModelPart.BODY), matrixStackIn, partialTicks);
        matrixStackIn.translate(0.0F, 2.0F + baseHeight, 0.0F);
        matrixStackIn.rotate(Vector3f.XP.rotationDegrees(180.0F));
        if (rendertype != null) {
            IVertexBuilder ivertexbuilder = bufferIn.getBuffer(rendertype);
            int i = getPackedOverlay(entityIn, this.getOverlayProgress(entityIn, partialTicks));
            getEntityModel().render(entityIn, matrixStackIn, ivertexbuilder, packedLightIn, OverlayTexture.NO_OVERLAY,
                    entityIn.isFemale(), entityIn.isSlim(), 1.0F, 1.0F, 1.0F, 1.0F);
        }

        // render layers
        if (!entityIn.isSpectator()) {
            for(LayerRenderer<T, M> layerrenderer : this.layerRenderers) {
                layerrenderer.render(matrixStackIn, bufferIn, packedLightIn, entityIn, 0F, 0F, partialTicks, 0F, 0F, 0F);
            }
        }
        matrixStackIn.pop();

        // render nametag
        net.minecraftforge.client.event.RenderNameplateEvent renderNameplateEvent = new net.minecraftforge.client.event.RenderNameplateEvent(entityIn, entityIn.getDisplayName(), this, matrixStackIn, bufferIn, packedLightIn, partialTicks);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(renderNameplateEvent);
        if (renderNameplateEvent.getResult() != net.minecraftforge.eventbus.api.Event.Result.DENY && (renderNameplateEvent.getResult() == net.minecraftforge.eventbus.api.Event.Result.ALLOW || this.canRenderName(entityIn))) {
            this.renderName(entityIn, renderNameplateEvent.getContent(), matrixStackIn, bufferIn, packedLightIn);
        }

        // post-render event
        MinecraftForge.EVENT_BUS.post(new RenderLivingEvent.Post<T, M>(entityIn, this, partialTicks, matrixStackIn, bufferIn, packedLightIn));
    }

    @Override
    public boolean canRenderName(final AltarEntity entityIn) {
        final Minecraft mc = Minecraft.getInstance();
        if(mc.currentScreen instanceof AltarScreen) {
            return false;
        }
        final Vector3d pos = entityIn.getPositionVec().add(0, entityIn.getType().getSize().height / 2D, 0);
        return isWithinDistanceToRenderName(pos, 6.0D);
    }

    @Override
    public ResourceLocation getEntityTexture(final T entity) {
        // return deity texture
        if(entity.getDeity().isPresent()) {
            ResourceLocation deity = entity.getDeity().get();
            return new ResourceLocation(deity.getNamespace(), "textures/altar/" + deity.getPath() + ".png");
        }
        // return player texture
        final GameProfile gameProfile = entity.getPlayerProfile();
        final boolean slim = entity.isSlim();
        if(gameProfile != null) {
            Minecraft minecraft = Minecraft.getInstance();
            Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> map = minecraft.getSkinManager().loadSkinFromCache(gameProfile);
            if(map.containsKey(MinecraftProfileTexture.Type.SKIN)) {
                return minecraft.getSkinManager().loadSkin(map.get(MinecraftProfileTexture.Type.SKIN), MinecraftProfileTexture.Type.SKIN);
            }
        }
        // return default texture
        // TODO: return dynamic texture based on material
        return slim ? ALEX_TEXTURE : STEVE_TEXTURE;
    }

    @Override
    protected RenderType func_230496_a_(final T entityIn, boolean isVisible, boolean isVisibleToPlayer, boolean isGlowing) {
        // TODO: optimize, allow for player skins, etc.
        return super.func_230496_a_(entityIn, isVisible, isVisibleToPlayer, isGlowing);
    }

    public boolean isWithinDistanceToRenderName(final Vector3d pos, final double dis) {
        final Minecraft mc = Minecraft.getInstance();
        final EntityRendererManager renderManager = mc.getRenderManager();
        return renderManager.getDistanceToCamera(pos.x, pos.y, pos.z) < (dis * dis)
                && mc.objectMouseOver != null
                && mc.objectMouseOver.getType() == RayTraceResult.Type.ENTITY
                && mc.objectMouseOver.getHitVec().squareDistanceTo(pos) < Math.pow(0.9D, 2);
    }
}
