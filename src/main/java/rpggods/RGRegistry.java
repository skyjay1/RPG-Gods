package rpggods;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import rpggods.block.AltarLightBlock;
import rpggods.block.BrazierBlock;
import rpggods.block.entity.BrazierBlockEntity;
import rpggods.entity.AltarEntity;
import rpggods.data.favor.Favor;
import rpggods.data.favor.IFavor;
import rpggods.menu.AltarContainerMenu;
import rpggods.menu.FavorContainerMenu;
import rpggods.item.AltarItem;
import rpggods.item.ScrollItem;
import rpggods.util.AutosmeltOrCobbleModifier;
import rpggods.util.CropMultiplierModifier;
import rpggods.util.ShapedAltarRecipe;
import rpggods.util.ShapelessAltarRecipe;
import rpggods.data.tameable.ITameable;
import rpggods.util.AltarStructureProcessor;

public final class RGRegistry {

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, RPGGods.MODID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RPGGods.MODID);
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, RPGGods.MODID);
    private static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, RPGGods.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, RPGGods.MODID);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, RPGGods.MODID);
    private static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIER_SERIALIZERS = DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, RPGGods.MODID);

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
            // register loc processor
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
    public static final RegistryObject<AltarLightBlock> LIGHT_BLOCK = BLOCKS.register("light", () ->
            new AltarLightBlock(BlockBehaviour.Properties.of(Material.AIR)
                .strength(-1F).noCollission().randomTicks()
                .lightLevel(b -> b.getValue(AltarLightBlock.LEVEL))));
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
    public static final RegistryObject<MenuType<AltarContainerMenu>> ALTAR_CONTAINER = MENU_TYPES.register("altar_container", () ->
        IForgeMenuType.create((windowId, inv, data) -> {
            final int entityId = data.readInt();
            Entity entity = inv.player.level.getEntity(entityId);
            AltarEntity altarEntity = (AltarEntity) entity;
            return new AltarContainerMenu(windowId, inv, altarEntity.getInventory(), altarEntity);
        })
    );
    public static final RegistryObject<MenuType<FavorContainerMenu>> FAVOR_CONTAINER = MENU_TYPES.register("favor_container", () ->
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
            return new FavorContainerMenu(windowId, inv, favor, deityId);
        })
    );
    //// LOOT MODIFER SERIALIZERS ////
    public static final RegistryObject<Codec<? extends AutosmeltOrCobbleModifier>> AUTOSMELT_LOOT_MODIFIER =
            LOOT_MODIFIER_SERIALIZERS.register("autosmelt_or_cobble", AutosmeltOrCobbleModifier.CODEC_SUPPLIER);
    public static final RegistryObject<Codec<? extends CropMultiplierModifier>> CROP_LOOT_MODIFIER =
            LOOT_MODIFIER_SERIALIZERS.register("crop_multiplier", CropMultiplierModifier.CODEC_SUPPLIER);

}
