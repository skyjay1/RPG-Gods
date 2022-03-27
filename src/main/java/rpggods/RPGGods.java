package rpggods;

import com.google.common.collect.Lists;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rpggods.deity.Altar;
import rpggods.deity.Deity;
import rpggods.deity.Offering;
import rpggods.deity.Sacrifice;
import rpggods.event.FavorEventHandler;
import rpggods.favor.Favor;
import rpggods.favor.IFavor;
import rpggods.network.CUpdateAltarPacket;
import rpggods.network.SAltarPacket;
import rpggods.network.SOfferingPacket;
import rpggods.network.SPerkPacket;
import rpggods.network.SSacrificePacket;
import rpggods.network.SUpdateAltarPacket;
import rpggods.network.SUpdateSittingPacket;
import rpggods.perk.Affinity;
import rpggods.perk.Perk;
import rpggods.perk.PerkAction;
import rpggods.tameable.ITameable;
import rpggods.tameable.Tameable;
import rpggods.util.GenericJsonReloadListener;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Mod(RPGGods.MODID)
public class RPGGods {
    public static final String MODID = "rpggods";

    private static final ForgeConfigSpec.Builder CONFIG_BUILDER = new ForgeConfigSpec.Builder();
    public static RGConfig CONFIG = new RGConfig(CONFIG_BUILDER);
    private static final ForgeConfigSpec CONFIG_SPEC = CONFIG_BUILDER.build();

    @CapabilityInject(IFavor.class)
    public static final Capability<IFavor> FAVOR = null;

    @CapabilityInject(ITameable.class)
    public static final Capability<ITameable> TAMEABLE = null;

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(MODID, "channel"),
            () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

    // Map of Deity ID to Deity
    public static final Map<ResourceLocation, Deity> DEITY = new HashMap<>();
    // Map of Entity ID to Affinity
    public static final Map<ResourceLocation, Map<Affinity.Type, List<ResourceLocation>>> AFFINITY = new HashMap<>();
    // Reloadable data resource listeners
    public static final GenericJsonReloadListener<Altar> ALTAR = new GenericJsonReloadListener<>("deity/altar", Altar.class, Altar.CODEC,
            l -> l.getEntries().forEach(e -> {
                RPGGods.CHANNEL.send(PacketDistributor.ALL.noArg(), new SAltarPacket(e.getKey(), e.getValue().get()));
                e.getValue().ifPresent(a -> a.getDeity().ifPresent(d -> RPGGods.DEITY.computeIfAbsent(d, Deity::new).add(e.getKey(), a)));
            }));
    public static final GenericJsonReloadListener<Offering> OFFERING = new GenericJsonReloadListener<>("deity/offering", Offering.class, Offering.CODEC,
            l -> l.getEntries().forEach(e -> {
                RPGGods.CHANNEL.send(PacketDistributor.ALL.noArg(), new SOfferingPacket(e.getKey(), e.getValue().get()));
                e.getValue().ifPresent(o -> RPGGods.DEITY.computeIfAbsent(Offering.getDeity(e.getKey()), Deity::new).add(e.getKey(), o));
            }));
    public static final GenericJsonReloadListener<Sacrifice> SACRIFICE = new GenericJsonReloadListener<>("deity/sacrifice", Sacrifice.class, Sacrifice.CODEC,
            l -> l.getEntries().forEach(e -> {
                RPGGods.CHANNEL.send(PacketDistributor.ALL.noArg(), new SSacrificePacket(e.getKey(), e.getValue().get()));
                e.getValue().ifPresent(s -> RPGGods.DEITY.computeIfAbsent(Sacrifice.getDeity(e.getKey()), Deity::new).add(e.getKey(), s));
            }));
    public static final GenericJsonReloadListener<Perk> PERK = new GenericJsonReloadListener<>("deity/perk", Perk.class, Perk.CODEC,
            l -> l.getEntries().forEach(e -> {
                RPGGods.CHANNEL.send(PacketDistributor.ALL.noArg(), new SPerkPacket(e.getKey(), e.getValue().get()));
                e.getValue().ifPresent(p -> {
                    // add Perk to Deity
                    RPGGods.DEITY.computeIfAbsent(p.getDeity(), Deity::new).add(e.getKey(), p);
                    // add Perk to Affinity map if applicable
                    for(PerkAction action : p.getActions()) {
                        if(action.getAffinity().isPresent()) {
                            Affinity affinity = action.getAffinity().get();
                            RPGGods.AFFINITY.computeIfAbsent(affinity.getEntity(), id -> new EnumMap<>(Affinity.Type.class))
                                    .computeIfAbsent(affinity.getType(), id -> Lists.newArrayList()).add(e.getKey());
                        }
                    }
                });
            }));

    public static final Logger LOGGER = LogManager.getFormatterLogger(RPGGods.MODID);

    public RPGGods() {
        LOGGER.debug("registerListeners");
        // Mod event bus listeners
        FMLJavaModLoadingContext.get().getModEventBus().register(RGRegistry.ItemReg.class);
        FMLJavaModLoadingContext.get().getModEventBus().register(RGRegistry.EntityReg.class);
        FMLJavaModLoadingContext.get().getModEventBus().register(RGRegistry.AttributesReg.class);
        FMLJavaModLoadingContext.get().getModEventBus().register(RGRegistry.ContainerReg.class);
        FMLJavaModLoadingContext.get().getModEventBus().register(RGRegistry.RecipeReg.class);
        FMLJavaModLoadingContext.get().getModEventBus().register(RGRegistry.LootModifierReg.class);
        FMLJavaModLoadingContext.get().getModEventBus().register(RGRegistry.ClientReg.class);
        FMLJavaModLoadingContext.get().getModEventBus().register(FavorEventHandler.ModEvents.class);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(RPGGods::setup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(RPGGods::loadConfig);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(RPGGods::reloadConfig);
        // Config file
        RPGGods.LOGGER.debug("registerConfig");
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CONFIG_SPEC);
        // Required for data pack sync and favor capability
        MinecraftForge.EVENT_BUS.register(RGData.class);
        // Events that affect Favor and Perks
        MinecraftForge.EVENT_BUS.register(FavorEventHandler.ForgeEvents.class);
        // Events that are client-only
        DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> {
            RPGGods.LOGGER.debug("registerClientEvents");
            MinecraftForge.EVENT_BUS.register(FavorEventHandler.ClientEvents.class);
        });
        // Packets
        LOGGER.debug("registerNetwork");
        int messageId = 0;
        CHANNEL.registerMessage(messageId++, SAltarPacket.class, SAltarPacket::toBytes, SAltarPacket::fromBytes, SAltarPacket::handlePacket, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(messageId++, SOfferingPacket.class, SOfferingPacket::toBytes, SOfferingPacket::fromBytes, SOfferingPacket::handlePacket, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(messageId++, SSacrificePacket.class, SSacrificePacket::toBytes, SSacrificePacket::fromBytes, SSacrificePacket::handlePacket, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(messageId++, SPerkPacket.class, SPerkPacket::toBytes, SPerkPacket::fromBytes, SPerkPacket::handlePacket, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(messageId++, SUpdateSittingPacket.class, SUpdateSittingPacket::toBytes, SUpdateSittingPacket::fromBytes, SUpdateSittingPacket::handlePacket, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(messageId++, SUpdateAltarPacket.class, SUpdateAltarPacket::toBytes, SUpdateAltarPacket::fromBytes, SUpdateAltarPacket::handlePacket, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(messageId++, CUpdateAltarPacket.class, CUpdateAltarPacket::toBytes, CUpdateAltarPacket::fromBytes, CUpdateAltarPacket::handlePacket, Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    public static void setup(final FMLCommonSetupEvent event) {
        // register capability
        CapabilityManager.INSTANCE.register(IFavor.class, new Favor.Storage(), Favor::new);
        CapabilityManager.INSTANCE.register(ITameable.class, new Tameable.Storage(), Tameable::new);
    }

    public static void loadConfig(final ModConfig.Loading event) {
        CONFIG.bake();
    }

    public static void reloadConfig(final ModConfig.Reloading event) {
        CONFIG.bake();
    }
}
