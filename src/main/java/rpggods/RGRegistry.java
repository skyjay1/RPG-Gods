package rpggods;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.material.MapColor;
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
    private static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(BuiltInRegistries.CREATIVE_MODE_TAB.key(), RPGGods.MODID);
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, RPGGods.MODID);
    private static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, RPGGods.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, RPGGods.MODID);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, RPGGods.MODID);
    private static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIER_SERIALIZERS = DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, RPGGods.MODID);
    private static final DeferredRegister<StructureProcessorType<?>> STRUCTURE_PROCESSORS = DeferredRegister.create(BuiltInRegistries.STRUCTURE_PROCESSOR.key(), RPGGods.MODID);

    public static void register() {
        BlockReg.register();
        ItemReg.register();
        CreativeTabReg.register();
        EntityReg.register();
        BlockEntityReg.register();
        MenuReg.register();
        RecipeReg.register();
        LootModifierReg.register();
        CapabilityReg.register();
        StructureProcessorReg.register();
    }

    public static final class BlockReg {
        private static void register() {
            BLOCKS.register(FMLJavaModLoadingContext.get().getModEventBus());
        }

        public static final RegistryObject<AltarLightBlock> LIGHT = BLOCKS.register("light", () ->
                new AltarLightBlock(BlockBehaviour.Properties.of()
                        .strength(-1F).mapColor(MapColor.NONE).noCollission().randomTicks()
                        .lightLevel(b -> b.getValue(AltarLightBlock.LEVEL))));
        public static final RegistryObject<Block> BRAZIER = BLOCKS.register("brazier", () ->
                new BrazierBlock(BlockBehaviour.Properties.of()
                        .strength(3.0F).mapColor(MapColor.METAL).sound(SoundType.METAL)
                        .lightLevel(b -> b.getValue(BrazierBlock.LIT) ? 15 : 0)));

    }

    public static final class ItemReg {
        public static final RegistryObject<AltarItem> ALTAR = ITEMS.register("altar", () -> new AltarItem(new Item.Properties()));
        public static final RegistryObject<ScrollItem> SCROLL = ITEMS.register("scroll", () -> new ScrollItem(new Item.Properties()));
        public static final RegistryObject<BlockItem> BRAZIER = ITEMS.register("brazier", () -> new BlockItem(BlockReg.BRAZIER.get(), new Item.Properties()));

        private static void register() {
            ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        }
    }

    public static final class CreativeTabReg {
        private static void register() {
            CREATIVE_MODE_TABS.register(FMLJavaModLoadingContext.get().getModEventBus());
        }
    }

    public static final class EntityReg {

        private static void register() {
            ENTITY_TYPES.register(FMLJavaModLoadingContext.get().getModEventBus());
            FMLJavaModLoadingContext.get().getModEventBus().addListener(RGRegistry.EntityReg::registerEntityAttributes);
        }

        private static void registerEntityAttributes(final EntityAttributeCreationEvent event) {
            event.put(ALTAR.get(), AltarEntity.registerAttributes().build());
        }

        public static final RegistryObject<EntityType<? extends AltarEntity>> ALTAR = ENTITY_TYPES.register("altar", () ->
                EntityType.Builder
                        .of(AltarEntity::new, MobCategory.MISC)
                        .sized(0.8F, 2.48F).clientTrackingRange(10)
                        .build("altar"));
    }

    public static final class BlockEntityReg {

        private static void register() {
            BLOCK_ENTITY_TYPES.register(FMLJavaModLoadingContext.get().getModEventBus());
        }

        public static final RegistryObject<BlockEntityType<? extends BrazierBlockEntity>> BRAZIER = BLOCK_ENTITY_TYPES.register("brazier", () ->
                BlockEntityType.Builder.of(BrazierBlockEntity::new, BlockReg.BRAZIER.get()) .build(null)
        );
    }

    public static final class MenuReg {

        private static void register() {
            MENU_TYPES.register(FMLJavaModLoadingContext.get().getModEventBus());
        }

        public static final RegistryObject<MenuType<AltarContainerMenu>> ALTAR_CONTAINER = MENU_TYPES.register("altar_container", () ->
                IForgeMenuType.create((windowId, inv, data) -> {
                    final int entityId = data.readInt();
                    Entity entity = inv.player.level().getEntity(entityId);
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
    }

    public static final class RecipeReg {

        private static void register() {
            RECIPE_SERIALIZERS.register(FMLJavaModLoadingContext.get().getModEventBus());
        }

        public static final RegistryObject<ShapelessAltarRecipe.Serializer> SHAPELESS_ALTAR_RECIPE_SERIALIZER =
                RECIPE_SERIALIZERS.register(ShapelessAltarRecipe.NAME, () -> new ShapelessAltarRecipe.Serializer());
        public static final RegistryObject<ShapedAltarRecipe.Serializer> SHAPED_ALTAR_RECIPE_SERIALIZER =
                RECIPE_SERIALIZERS.register(ShapedAltarRecipe.NAME, () -> new ShapedAltarRecipe.Serializer());
    }

    public static final class LootModifierReg {

        private static void register() {
            LOOT_MODIFIER_SERIALIZERS.register(FMLJavaModLoadingContext.get().getModEventBus());
        }

        public static final RegistryObject<Codec<? extends AutosmeltOrCobbleModifier>> AUTOSMELT_LOOT_MODIFIER =
                LOOT_MODIFIER_SERIALIZERS.register("autosmelt_or_cobble", AutosmeltOrCobbleModifier.CODEC_SUPPLIER);
        public static final RegistryObject<Codec<? extends CropMultiplierModifier>> CROP_LOOT_MODIFIER =
                LOOT_MODIFIER_SERIALIZERS.register("crop_multiplier", CropMultiplierModifier.CODEC_SUPPLIER);
    }

    public static final class CapabilityReg {
        private static void register() {
            FMLJavaModLoadingContext.get().getModEventBus().addListener(RGRegistry.CapabilityReg::registerCapabilities);
        }

        private static void registerCapabilities(final RegisterCapabilitiesEvent event) {
            event.register(IFavor.class);
            event.register(ITameable.class);
        }

        // TODO attach capability event
    }

    public static final class StructureProcessorReg {
        private static void register() {
            STRUCTURE_PROCESSORS.register(FMLJavaModLoadingContext.get().getModEventBus());
        }

        public static final RegistryObject<StructureProcessorType<AltarStructureProcessor>> ALTAR = STRUCTURE_PROCESSORS.register("altar", () ->
                () -> AltarStructureProcessor.CODEC);
    }
}
