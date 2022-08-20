package rpggods;

import com.google.common.collect.Lists;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.common.loot.GlobalLootModifierSerializer;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import rpggods.block.BrazierBlock;
import rpggods.block.GlowBlock;
import rpggods.blockentity.BrazierBlockEntity;
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
import rpggods.util.AltarStructureProcessor;

import java.util.Collections;
import java.util.List;

public final class RGRegistry {

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, RPGGods.MODID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RPGGods.MODID);
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITIES, RPGGods.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITIES, RPGGods.MODID);
    private static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.CONTAINERS, RPGGods.MODID);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, RPGGods.MODID);
    private static final DeferredRegister<GlobalLootModifierSerializer<?>> LOOT_MODIFIER_SERIALIZERS = DeferredRegister.create(ForgeRegistries.Keys.LOOT_MODIFIER_SERIALIZERS, RPGGods.MODID);

    public static void register() {
        // deferred registers
        BLOCKS.register(FMLJavaModLoadingContext.get().getModEventBus());
        ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        ENTITY_TYPES.register(FMLJavaModLoadingContext.get().getModEventBus());
        BLOCK_ENTITY_TYPES.register(FMLJavaModLoadingContext.get().getModEventBus());
        MENU_TYPES.register(FMLJavaModLoadingContext.get().getModEventBus());
        RECIPE_SERIALIZERS.register(FMLJavaModLoadingContext.get().getModEventBus());
        LOOT_MODIFIER_SERIALIZERS.register(FMLJavaModLoadingContext.get().getModEventBus());
        // event listeners
        FMLJavaModLoadingContext.get().getModEventBus().addListener(RGRegistry::registerEntityAttributes);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(RGRegistry::registerCapabilities);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(RGRegistry::registerStructureProcessors);
    }

    public static void registerEntityAttributes(final EntityAttributeCreationEvent event) {
        event.put(ALTAR_TYPE.get(), AltarEntity.registerAttributes().build());
    }

    public static void registerCapabilities(final RegisterCapabilitiesEvent event) {
        event.register(IFavor.class);
        event.register(ITameable.class);
    }

    //// STRUCTURE PROCESSORS ////
    public static StructureProcessorType<AltarStructureProcessor> ALTAR_STRUCTURE_PROCESSOR;

    public static void registerStructureProcessors(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // register altar processor
            ResourceLocation altarProcessorId = new ResourceLocation(RPGGods.MODID, "altar");
            ALTAR_STRUCTURE_PROCESSOR = StructureProcessorType.register(altarProcessorId.toString(), AltarStructureProcessor.CODEC);
        });
    }

    //// ENTITIES ////
    public static final RegistryObject<EntityType<? extends AltarEntity>> ALTAR_TYPE = ENTITY_TYPES.register("altar", () ->
            EntityType.Builder
            .of(AltarEntity::new, MobCategory.MISC)
            .sized(0.8F, 2.48F).clientTrackingRange(10)
            .build("altar"));

    //// BLOCKS ////
    public static final RegistryObject<GlowBlock> LIGHT_BLOCK = BLOCKS.register("light", () ->
            new GlowBlock(BlockBehaviour.Properties.of(Material.AIR)
                .strength(-1F).noCollission().randomTicks()
                .lightLevel(b -> b.getValue(GlowBlock.LIGHT_LEVEL))));
    public static final RegistryObject<Block> BRAZIER_BLOCK = BLOCKS.register("brazier", () ->
            new BrazierBlock(BlockBehaviour.Properties.of(Material.METAL)
                    .strength(3.0F).sound(SoundType.METAL)
                    .lightLevel(b -> b.getValue(BrazierBlock.LIT) ? 15 : 0)));

    //// ITEMS ////
    public static final RegistryObject<AltarItem> ALTAR_ITEM = ITEMS.register("altar", () -> new AltarItem(new Item.Properties().tab(CreativeModeTab.TAB_MISC)));
    public static final RegistryObject<ScrollItem> SCROLL_ITEM = ITEMS.register("scroll", () -> new ScrollItem(new Item.Properties().tab(CreativeModeTab.TAB_MISC)));
    public static final RegistryObject<BlockItem> LIGHT_ITEM = ITEMS.register("light", () -> new BlockItem(LIGHT_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> BRAZIER_ITEM = ITEMS.register("brazier", () -> new BlockItem(BRAZIER_BLOCK.get(), new Item.Properties().tab(CreativeModeTab.TAB_MISC)));

    //// BLOCK ENTITIES ////
    public static final RegistryObject<BlockEntityType<? extends BrazierBlockEntity>> BRAZIER_TYPE = BLOCK_ENTITY_TYPES.register("brazier", () ->
        BlockEntityType.Builder.of(BrazierBlockEntity::new, RGRegistry.BRAZIER_BLOCK.get()) .build(null)
    );

    //// RECIPES ////
    public static final RegistryObject<ShapelessAltarRecipe.Serializer> SHAPELESS_ALTAR_RECIPE_SERIALIZER =
            RECIPE_SERIALIZERS.register(ShapelessAltarRecipe.NAME, () -> new ShapelessAltarRecipe.Serializer());
    public static final RegistryObject<ShapedAltarRecipe.Serializer> SHAPED_ALTAR_RECIPE_SERIALIZER =
            RECIPE_SERIALIZERS.register(ShapedAltarRecipe.NAME, () -> new ShapedAltarRecipe.Serializer());

    //// MENU TYPES ////
    public static final RegistryObject<MenuType<AltarContainer>> ALTAR_CONTAINER = MENU_TYPES.register("altar_container", () ->
        IForgeMenuType.create((windowId, inv, data) -> {
            final int entityId = data.readInt();
            Entity entity = inv.player.level.getEntity(entityId);
            AltarEntity altarEntity = (AltarEntity) entity;
            return new AltarContainer(windowId, inv, altarEntity.getInventory(), altarEntity);
        })
    );
    public static final RegistryObject<MenuType<FavorContainer>> FAVOR_CONTAINER = MENU_TYPES.register("favor_container", () ->
        IForgeMenuType.create((windowId, inv, data) -> {
            CompoundTag nbt = data.readNbt();
            // load favor capability
            LazyOptional<IFavor> ifavor = RPGGods.getFavor(inv.player);
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
        })
    );

    //// LOOT MODIFER SERIALIZERS ////
    public static final RegistryObject<AutosmeltOrCobbleModifier.Serializer> AUTOSMELT_LOOT_MODIFIER =
            LOOT_MODIFIER_SERIALIZERS.register("autosmelt_or_cobble", () -> new AutosmeltOrCobbleModifier.Serializer());
    public static final RegistryObject<CropMultiplierModifier.Serializer> CROP_LOOT_MODIFIER =
            LOOT_MODIFIER_SERIALIZERS.register("crop_multiplier", () -> new CropMultiplierModifier.Serializer());

    //// CLIENT MOD BUS HANDLER ////
    public static final class ClientReg {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> registerContainerRenders());
            event.enqueueWork(() -> registerModelProperties());
            event.enqueueWork(() -> registerRenderLayers());
        }

        @SubscribeEvent
        public static void registerEntityLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
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
            event.registerEntityRenderer(ALTAR_TYPE.get(), rpggods.client.render.AltarRenderer::new);
            event.registerBlockEntityRenderer(BRAZIER_TYPE.get(), rpggods.client.render.BrazierBlockEntityRenderer::new);
        }

        private static void registerContainerRenders() {
            MenuScreens.register(RGRegistry.ALTAR_CONTAINER.get(), rpggods.client.screen.AltarScreen::new);
            MenuScreens.register(RGRegistry.FAVOR_CONTAINER.get(), rpggods.client.screen.FavorScreen::new);
        }

        private static void registerRenderLayers() {
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(RGRegistry.BRAZIER_BLOCK.get(), net.minecraft.client.renderer.RenderType.cutout());
        }

        private static List<ResourceLocation> altars = Lists.newArrayList();

        private static void registerModelProperties() {
            // Scroll properites
            ItemProperties.register(SCROLL_ITEM.get(), new ResourceLocation("open"),
                    (item, world, entity, i) -> (entity != null && entity.isUsingItem() && entity.getUseItem() == item) ? 1.0F : 0.0F);
            // Altar properties
            ItemProperties.register(ALTAR_ITEM.get(), new ResourceLocation("index"), (item, world, entity, i) -> {
                // determine index of altar in list
                if(altars.isEmpty() || (world != null && world.getGameTime() % 100 == 0)) {
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
