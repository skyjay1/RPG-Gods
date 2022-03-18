package rpggods;

import net.minecraft.client.gui.ScreenManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.ShapelessRecipe;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.common.loot.GlobalLootModifierSerializer;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.ObjectHolder;
import rpggods.entity.AltarEntity;
import rpggods.favor.IFavor;
import rpggods.gui.AltarContainer;
import rpggods.gui.FavorContainer;
import rpggods.item.AltarItem;
import rpggods.loot.AutosmeltOrCobbleModifier;
import rpggods.loot.CropMultiplierModifier;
import rpggods.recipe.AltarRecipe;

import java.util.Optional;
import java.util.UUID;

public final class RGRegistry {

    public static final class EntityReg {

        public static EntityType<AltarEntity> ALTAR = EntityType.Builder
                .of(AltarEntity::new, EntityClassification.MISC)
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
    public static final class ItemReg {

        @ObjectHolder("altar")
        public static final Item ALTAR = null;

        @SubscribeEvent
        public static void registerItems(final RegistryEvent.Register<Item> event) {
            RPGGods.LOGGER.debug("registerItems");
            event.getRegistry().register(new AltarItem(new Item.Properties().tab(ItemGroup.TAB_MISC))
                    .setRegistryName(RPGGods.MODID, "altar"));
        }
    }

    @ObjectHolder(RPGGods.MODID)
    public static final class RecipeReg {
        @ObjectHolder(AltarRecipe.CATEGORY)
        public static final IRecipeSerializer<ShapelessRecipe> ALTAR_RECIPE_SERIALIZER = null;

        @SubscribeEvent
        public static void registerRecipeSerializers(final RegistryEvent.Register<IRecipeSerializer<?>> event) {
            RPGGods.LOGGER.debug("registerRecipeSerializers");
            event.getRegistry().register(new AltarRecipe.Factory().setRegistryName(RPGGods.MODID, AltarRecipe.CATEGORY));
        }

    }

    @ObjectHolder(RPGGods.MODID)
    public static final class ContainerReg {

        @ObjectHolder("altar_container")
        public static final ContainerType<AltarContainer> ALTAR_CONTAINER = null;
        @ObjectHolder("favor_container")
        public static final ContainerType<FavorContainer> FAVOR_CONTAINER = null;

        @SubscribeEvent
        public static void registerContainers(final RegistryEvent.Register<ContainerType<?>> event) {
            RPGGods.LOGGER.debug("registerContainers");
            // Altar screen requires UUID of altar entity
            ContainerType<AltarContainer> altarContainer = IForgeContainerType.create((windowId, inv, data) -> {
                final int entityId = data.readInt();
                Entity entity = inv.player.level.getEntity(entityId);
                AltarEntity altarEntity = (AltarEntity) entity;
                return new AltarContainer(windowId, inv, altarEntity.getInventory(), altarEntity); // TODO
            });
            // Favor screen requires Favor as a Compound Tag and Deity ID as a ResourceLocation
            ContainerType<FavorContainer> favorContainer = IForgeContainerType.create((windowId, inv, data) -> {
                final IFavor favor = RPGGods.FAVOR.getDefaultInstance();
                RPGGods.FAVOR.readNBT(favor, null, data.readNbt());
                ResourceLocation deityId = data.readResourceLocation();
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
            registerEntityRenderers();
            registerContainerRenders();
        }

        public static void registerEntityRenderers() {
            RPGGods.LOGGER.debug("registerEntityRenderers");
            RenderingRegistry.registerEntityRenderingHandler(EntityReg.ALTAR, rpggods.client.render.AltarRenderer::new);
        }

        private static void registerContainerRenders() {
            RPGGods.LOGGER.debug("registerContainerRenders");
            ScreenManager.register(RGRegistry.ContainerReg.ALTAR_CONTAINER, rpggods.client.screen.AltarScreen::new);
            ScreenManager.register(RGRegistry.ContainerReg.FAVOR_CONTAINER, rpggods.client.screen.FavorScreen::new);
        }
    }
}
