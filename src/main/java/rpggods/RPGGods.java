package rpggods;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rpggods.deity.Altar;
import rpggods.deity.Deity;
import rpggods.deity.DeityHelper;
import rpggods.deity.Offering;
import rpggods.deity.Sacrifice;
import rpggods.favor.IFavor;
import rpggods.network.CUpdateAltarPacket;
import rpggods.network.SAltarPacket;
import rpggods.network.SDeityPacket;
import rpggods.network.SOfferingPacket;
import rpggods.network.SPerkPacket;
import rpggods.network.SSacrificePacket;
import rpggods.network.SUpdateAltarPacket;
import rpggods.network.SUpdateSittingPacket;
import rpggods.perk.Affinity;
import rpggods.perk.Perk;
import rpggods.tameable.ITameable;
import rpggods.util.CodecJsonDataManager;

import javax.annotation.Nullable;
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

    public static Capability<IFavor> FAVOR = CapabilityManager.get(new CapabilityToken<>(){});

    public static Capability<ITameable> TAMEABLE = CapabilityManager.get(new CapabilityToken<>(){});

    private static final String PROTOCOL_VERSION = "3";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(MODID, "channel"),
            () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

    // Map of Deity ID to Deity
    public static final Map<ResourceLocation, DeityHelper> DEITY_HELPER = new HashMap<>();
    // Map of Entity ID to Affinity
    public static final Map<ResourceLocation, Map<Affinity.Type, List<ResourceLocation>>> AFFINITY = new HashMap<>();

    // Reloadable data resource listeners
    protected static final CodecJsonDataManager<Altar> ALTAR_JSON_MANAGER = new CodecJsonDataManager<>("deity/altar", Altar.CODEC);
    public static final Map<ResourceLocation, Altar> ALTAR_MAP = new HashMap<>();

    protected static final CodecJsonDataManager<Deity> DEITY_JSON_MANAGER = new CodecJsonDataManager<>("deity/deity", Deity.CODEC);
    public static final Map<ResourceLocation, Deity> DEITY_MAP = new HashMap<>();

    protected static final CodecJsonDataManager<Offering> OFFERING_JSON_MANAGER = new CodecJsonDataManager<>("deity/offering", Offering.CODEC);
    public static final Map<ResourceLocation, Offering> OFFERING_MAP = new HashMap<>();

    protected static final CodecJsonDataManager<Perk> PERK_JSON_MANAGER = new CodecJsonDataManager<>("deity/perk", Perk.CODEC);
    public static final Map<ResourceLocation, Perk> PERK_MAP = new HashMap<>();

    protected static final CodecJsonDataManager<Sacrifice> SACRIFICE_JSON_MANAGER = new CodecJsonDataManager<>("deity/sacrifice", Sacrifice.CODEC);
    public static final Map<ResourceLocation, Sacrifice> SACRIFICE_MAP = new HashMap<>();

    public static final Logger LOGGER = LogManager.getFormatterLogger(RPGGods.MODID);

    public RPGGods() {
        // Deferred registers
        RGRegistry.register();
        // Mod event bus listeners
        FMLJavaModLoadingContext.get().getModEventBus().register(RGRegistry.ClientReg.class);
        FMLJavaModLoadingContext.get().getModEventBus().register(RGEvents.ModEvents.class);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(RPGGods::setup);
        // Config file
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CONFIG_SPEC);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(RPGGods::loadConfig);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(RPGGods::reloadConfig);
        // Required for data pack sync and favor capability
        MinecraftForge.EVENT_BUS.register(RGData.class);
        // Events that affect Favor and Perks
        MinecraftForge.EVENT_BUS.register(RGEvents.ForgeEvents.class);
        // Events that are client-only
        DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> {
            MinecraftForge.EVENT_BUS.register(RGEvents.ClientEvents.class);
        });
        // Packets
        int messageId = 0;
        CHANNEL.registerMessage(messageId++, SDeityPacket.class, SDeityPacket::toBytes, SDeityPacket::fromBytes, SDeityPacket::handlePacket, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(messageId++, SAltarPacket.class, SAltarPacket::toBytes, SAltarPacket::fromBytes, SAltarPacket::handlePacket, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(messageId++, SOfferingPacket.class, SOfferingPacket::toBytes, SOfferingPacket::fromBytes, SOfferingPacket::handlePacket, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(messageId++, SSacrificePacket.class, SSacrificePacket::toBytes, SSacrificePacket::fromBytes, SSacrificePacket::handlePacket, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(messageId++, SPerkPacket.class, SPerkPacket::toBytes, SPerkPacket::fromBytes, SPerkPacket::handlePacket, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(messageId++, SUpdateSittingPacket.class, SUpdateSittingPacket::toBytes, SUpdateSittingPacket::fromBytes, SUpdateSittingPacket::handlePacket, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(messageId++, SUpdateAltarPacket.class, SUpdateAltarPacket::toBytes, SUpdateAltarPacket::fromBytes, SUpdateAltarPacket::handlePacket, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(messageId++, CUpdateAltarPacket.class, CUpdateAltarPacket::toBytes, CUpdateAltarPacket::fromBytes, CUpdateAltarPacket::handlePacket, Optional.of(NetworkDirection.PLAY_TO_SERVER));

        // data managers
        ALTAR_JSON_MANAGER.subscribeAsSyncable(CHANNEL, SAltarPacket::new);
        DEITY_JSON_MANAGER.subscribeAsSyncable(CHANNEL, SDeityPacket::new);
        OFFERING_JSON_MANAGER.subscribeAsSyncable(CHANNEL, SOfferingPacket::new);
        PERK_JSON_MANAGER.subscribeAsSyncable(CHANNEL, SPerkPacket::new);
        SACRIFICE_JSON_MANAGER.subscribeAsSyncable(CHANNEL, SSacrificePacket::new);
    }

    public static void setup(final FMLCommonSetupEvent event) {

    }

    public static void loadConfig(final ModConfigEvent.Loading event) {
        CONFIG.bake();
    }

    public static void reloadConfig(final ModConfigEvent.Reloading event) {
        CONFIG.bake();
    }

    /**
     * Determines the favor to use for the given entity
     * @param entity the entity, or null for global favor
     * @return a lazy optional containing the favor, or empty
     */
    public static LazyOptional<IFavor> getFavor(@Nullable final Entity entity) {
        if(CONFIG.useGlobalFavor()) {
            return LazyOptional.of(() -> RGSavedData.get(ServerLifecycleHooks.getCurrentServer()).getFavor());
        }
        if(CONFIG.useTeamFavor() && entity != null && entity.getTeam() != null ) {
            return LazyOptional.of(() -> RGSavedData.get(ServerLifecycleHooks.getCurrentServer()).getTeamFavor(entity.getTeam().getName()));
        }
        if(entity != null) {
            return entity.getCapability(FAVOR);
        }
        return LazyOptional.empty();
    }
}
