package rpggods;

import com.google.common.collect.Lists;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Material;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.common.loot.GlobalLootModifierSerializer;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.ObjectHolder;
import rpggods.block.GlowBlock;
import rpggods.entity.AltarEntity;
import rpggods.favor.Favor;
import rpggods.favor.IFavor;
import rpggods.gui.AltarContainer;
import rpggods.gui.FavorContainer;
import rpggods.item.AltarItem;
import rpggods.item.ScrollItem;
import rpggods.loot.AutosmeltOrCobbleModifier;
import rpggods.loot.CropMultiplierModifier;
import rpggods.recipe.ShapedAltarRecipe;
import rpggods.recipe.ShapelessAltarRecipe;
import rpggods.tameable.ITameable;

import java.util.Collections;
import java.util.List;

public final class RGRegistry {

    public static final class CapabilityReg {

        @SubscribeEvent
        public static void registerCapabilities(final RegisterCapabilitiesEvent event) {
            event.register(IFavor.class);
            event.register(ITameable.class);
        }
    }

    public static final class EntityReg {

        public static EntityType<AltarEntity> ALTAR = EntityType.Builder
                .of(AltarEntity::new, MobCategory.MISC)
                .sized(0.8F, 2.48F).clientTrackingRange(10)
                .build("altar");

        @SubscribeEvent
        public static void registerEntities(final RegistryEvent.Register<EntityType<?>> event) {
            RPGGods.LOGGER.debug("registerEntities");
            event.getRegistry().register(ALTAR.setRegistryName(RPGGods.MODID, "altar"));
        }

    }

    public static final class AttributesReg {

        @SubscribeEvent
        public static void registerAttributes(final EntityAttributeCreationEvent event) {
            RPGGods.LOGGER.debug("registerAttributes");
            event.put(EntityReg.ALTAR, AltarEntity.registerAttributes().build());
        }
    }

    @ObjectHolder(RPGGods.MODID)
    public static final class BlockReg {
        @ObjectHolder("light")
        public static final Block LIGHT = null;

        @SubscribeEvent
        public static void registerBlocks(final RegistryEvent.Register<Block> event) {
            RPGGods.LOGGER.debug("registerBlocks");
            event.getRegistry().register(new GlowBlock(BlockBehaviour.Properties.of(Material.AIR)
                    .strength(-1F).noCollission().randomTicks().lightLevel(b -> b.getValue(GlowBlock.LIGHT_LEVEL)))
                    .setRegistryName(RPGGods.MODID, "light"));
        }
    }

    @ObjectHolder(RPGGods.MODID)
    public static final class ItemReg {

        @ObjectHolder("altar")
        public static final Item ALTAR = null;

        @ObjectHolder("scroll")
        public static final Item SCROLL = null;

        @SubscribeEvent
        public static void registerItems(final RegistryEvent.Register<Item> event) {
            RPGGods.LOGGER.debug("registerItems");
            event.getRegistry().register(new AltarItem(new Item.Properties().tab(CreativeModeTab.TAB_MISC))
                    .setRegistryName(RPGGods.MODID, "altar"));
            event.getRegistry().register(new ScrollItem(new Item.Properties().tab(CreativeModeTab.TAB_MISC))
                    .setRegistryName(RPGGods.MODID, "scroll"));
            event.getRegistry().register(new BlockItem(BlockReg.LIGHT, new Item.Properties())
                    .setRegistryName(RPGGods.MODID, "light"));
        }
    }

    @ObjectHolder(RPGGods.MODID)
    public static final class RecipeReg {
        @ObjectHolder(ShapelessAltarRecipe.NAME)
        public static final RecipeSerializer<ShapelessRecipe> SHAPELESS_ALTAR_RECIPE_SERIALIZER = null;

        @ObjectHolder(ShapedAltarRecipe.NAME)
        public static final RecipeSerializer<ShapedRecipe> SHAPED_ALTAR_RECIPE_SERIALIZER = null;

        @SubscribeEvent
        public static void registerRecipeSerializers(final RegistryEvent.Register<RecipeSerializer<?>> event) {
            RPGGods.LOGGER.debug("registerRecipeSerializers");
            event.getRegistry().register(new ShapelessAltarRecipe.Factory().setRegistryName(RPGGods.MODID, ShapelessAltarRecipe.NAME));
            event.getRegistry().register(new ShapedAltarRecipe.Factory().setRegistryName(RPGGods.MODID, ShapedAltarRecipe.NAME));
        }

    }

    @ObjectHolder(RPGGods.MODID)
    public static final class ContainerReg {

        @ObjectHolder("altar_container")
        public static final MenuType<AltarContainer> ALTAR_CONTAINER = null;
        @ObjectHolder("favor_container")
        public static final MenuType<FavorContainer> FAVOR_CONTAINER = null;

        @SubscribeEvent
        public static void registerContainers(final RegistryEvent.Register<MenuType<?>> event) {
            RPGGods.LOGGER.debug("registerContainers");
            // Altar screen requires UUID of altar entity
            MenuType<AltarContainer> altarContainer = IForgeMenuType.create((windowId, inv, data) -> {
                final int entityId = data.readInt();
                Entity entity = inv.player.level.getEntity(entityId);
                AltarEntity altarEntity = (AltarEntity) entity;
                return new AltarContainer(windowId, inv, altarEntity.getInventory(), altarEntity);
            });
            // Favor screen requires Favor as a Compound Tag and Deity ID as a ResourceLocation
            MenuType<FavorContainer> favorContainer = IForgeMenuType.create((windowId, inv, data) -> {
                CompoundTag nbt = data.readNbt();
                // load favor capability
                LazyOptional<IFavor> ifavor = inv.player.getCapability(RPGGods.FAVOR);
                IFavor favor = ifavor.orElse(Favor.EMPTY);
                if(favor != Favor.EMPTY && nbt != null) {
                    favor.deserializeNBT(nbt);
                }
                // load deity
                boolean hasDeity = data.readBoolean();
                ResourceLocation deityId = null;
                if(hasDeity) {
                    deityId = data.readResourceLocation();
                }
                return new FavorContainer(windowId, inv, favor, deityId);
            });
            event.getRegistry().register(altarContainer.setRegistryName(RPGGods.MODID, "altar_container"));
            event.getRegistry().register(favorContainer.setRegistryName(RPGGods.MODID, "favor_container"));
        }
    }

    public static final class LootModifierReg {
        @SubscribeEvent
        public static void registerLootModifiers(final RegistryEvent.Register<GlobalLootModifierSerializer<?>> event) {
            event.getRegistry().registerAll(
                    new AutosmeltOrCobbleModifier.Serializer().setRegistryName(RPGGods.MODID, "autosmelt_or_cobble"),
                    new CropMultiplierModifier.Serializer().setRegistryName(RPGGods.MODID, "crop_multiplier")
            );
        }
    }

    public static final class ClientReg {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> registerContainerRenders());
            event.enqueueWork(() -> registerModelProperties());
        }

        @SubscribeEvent
        public static void registerEntityLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
            RPGGods.LOGGER.debug("registerEntityRenderers");
            // create cube deformations
            net.minecraft.client.model.geom.builders.CubeDeformation inner = new net.minecraft.client.model.geom.builders.CubeDeformation(0.25F);
            net.minecraft.client.model.geom.builders.CubeDeformation outer = new net.minecraft.client.model.geom.builders.CubeDeformation(0.5F);
            // register layer definitions
            event.registerLayerDefinition(rpggods.client.render.AltarRenderer.ALTAR_MODEL_RESOURCE, () -> rpggods.client.render.AltarModel.createBodyLayer());
            event.registerLayerDefinition(rpggods.client.render.AltarRenderer.ALTAR_INNER_ARMOR_RESOURCE, () -> rpggods.client.render.AltarArmorModel.createBodyLayer(inner));
            event.registerLayerDefinition(rpggods.client.render.AltarRenderer.ALTAR_OUTER_ARMOR_RESOURCE, () -> rpggods.client.render.AltarArmorModel.createBodyLayer(outer));
        }

        @SubscribeEvent
        public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
            RPGGods.LOGGER.debug("registerEntityRenderers");
            event.registerEntityRenderer(EntityReg.ALTAR, rpggods.client.render.AltarRenderer::new);
        }

        private static void registerContainerRenders() {
            RPGGods.LOGGER.debug("registerContainerRenders");
            MenuScreens.register(RGRegistry.ContainerReg.ALTAR_CONTAINER, rpggods.client.screen.AltarScreen::new);
            MenuScreens.register(RGRegistry.ContainerReg.FAVOR_CONTAINER, rpggods.client.screen.FavorScreen::new);
        }

        private static List<ResourceLocation> altars = Lists.newArrayList();

        private static void registerModelProperties() {
            RPGGods.LOGGER.debug("registerModelProperties");
            // Scroll properites
            ItemProperties.register(ItemReg.SCROLL, new ResourceLocation("open"),
                    (item, world, entity, i) -> (entity != null && entity.isUsingItem() && entity.getUseItem() == item) ? 1.0F : 0.0F);
            // Altar properties
            ItemProperties.register(ItemReg.ALTAR, new ResourceLocation("index"), (item, world, entity, i) -> {
                // determine index of altar in list
                if(altars.isEmpty()) {
                    altars = Lists.newArrayList(RPGGods.ALTAR.getKeys());
                    Collections.sort(altars, ResourceLocation::compareNamespaced);
                }
                ResourceLocation deity = ResourceLocation.tryParse(item.getOrCreateTag().getString(AltarItem.KEY_ALTAR));
                int index = 0;
                int size = altars.size();
                for(int j = 0; j < size; j++) {
                    if(altars.get(j).equals(deity)) {
                        index = j;
                        break;
                    }
                }
                return (float) (index + 1) / (float) Math.max(1, size);
            });
        }
    }
}
