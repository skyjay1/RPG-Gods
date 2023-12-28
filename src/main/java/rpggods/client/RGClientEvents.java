package rpggods.client;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import rpggods.RGRegistry;
import rpggods.RPGGods;
import rpggods.item.AltarItem;

import java.util.ArrayList;
import java.util.List;

public final class RGClientEvents {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> registerContainerRenders());
        event.enqueueWork(() -> registerModelProperties());
    }

    @SubscribeEvent
    public static void registerEntityLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        // create cube deformations
        net.minecraft.client.model.geom.builders.CubeDeformation inner = new net.minecraft.client.model.geom.builders.CubeDeformation(0.25F);
        net.minecraft.client.model.geom.builders.CubeDeformation outer = new net.minecraft.client.model.geom.builders.CubeDeformation(0.5F);
        // register layer definitions
        event.registerLayerDefinition(rpggods.client.entity.AltarRenderer.ALTAR_MODEL_RESOURCE, () -> rpggods.client.entity.AltarModel.createBodyLayer());
        event.registerLayerDefinition(rpggods.client.entity.AltarRenderer.ALTAR_INNER_ARMOR_RESOURCE, () -> rpggods.client.entity.AltarArmorModel.createBodyLayer(inner));
        event.registerLayerDefinition(rpggods.client.entity.AltarRenderer.ALTAR_OUTER_ARMOR_RESOURCE, () -> rpggods.client.entity.AltarArmorModel.createBodyLayer(outer));
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(RGRegistry.ALTAR_TYPE.get(), rpggods.client.entity.AltarRenderer::new);
        event.registerBlockEntityRenderer(RGRegistry.BRAZIER_TYPE.get(), rpggods.client.blockentity.BrazierBlockEntityRenderer::new);
    }

    private static void registerContainerRenders() {
        MenuScreens.register(RGRegistry.ALTAR_CONTAINER.get(), rpggods.client.screen.AltarScreen::new);
        MenuScreens.register(RGRegistry.FAVOR_CONTAINER.get(), rpggods.client.screen.FavorScreen::new);
    }

    private static List<ResourceLocation> altars = new ArrayList<>();

    private static void registerModelProperties() {
        // Scroll properites
        ItemProperties.register(RGRegistry.SCROLL_ITEM.get(), new ResourceLocation("open"),
                (item, world, entity, i) -> (entity != null && entity.isUsingItem() && entity.getUseItem() == item) ? 1.0F : 0.0F);
        // Altar properties
        // TODO custom item model loader instead
        ItemProperties.register(RGRegistry.ALTAR_ITEM.get(), new ResourceLocation("index"), (item, world, entity, i) -> {
            // determine index of altar in list
            if (altars.isEmpty() || (world != null && world.getGameTime() % 100 == 0)) {
                altars = new ArrayList<>(RPGGods.ALTAR_MAP.keySet());
                altars.sort(ResourceLocation::compareNamespaced);
            }
            ResourceLocation deity = ResourceLocation.tryParse(item.getOrCreateTag().getString(AltarItem.KEY_ALTAR));
            int index = 0;
            int size = altars.size();
            for (int j = 0; j < size; j++) {
                if (altars.get(j).equals(deity)) {
                    index = j;
                    break;
                }
            }
            return (float) (index + 1) / (float) Math.max(1, size);
        });
    }
}
