package rpggods;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rpggods.deity.Altar;
import rpggods.deity.Offering;
import rpggods.deity.Sacrifice;
import rpggods.favor.Favor;
import rpggods.favor.IFavor;
import rpggods.network.CUpdateAltarPacket;
import rpggods.network.SAltarPacket;
import rpggods.network.SOfferingPacket;
import rpggods.network.SPerkPacket;
import rpggods.network.SSacrificePacket;
import rpggods.perk.Perk;
import rpggods.util.GenericJsonReloadListener;

import java.util.Optional;

@Mod(RPGGods.MODID)
public class RPGGods {
    public static final String MODID = "rpggods";

    @CapabilityInject(IFavor.class)
    public static final Capability<IFavor> FAVOR = null;

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(MODID, "channel"),
            () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

    public static final GenericJsonReloadListener<Altar> ALTAR = new GenericJsonReloadListener<>("deity/altar", Altar.class, Altar.CODEC,
            l -> l.getEntries().forEach(e -> RPGGods.CHANNEL.send(PacketDistributor.ALL.noArg(), new SAltarPacket(e.getKey(), e.getValue().get()))));
    public static final GenericJsonReloadListener<Offering> OFFERING = new GenericJsonReloadListener<>("deity/offering", Offering.class, Offering.CODEC,
            l -> l.getEntries().forEach(e -> RPGGods.CHANNEL.send(PacketDistributor.ALL.noArg(), new SOfferingPacket(e.getKey(), e.getValue().get()))));
    public static final GenericJsonReloadListener<Sacrifice> SACRIFICE = new GenericJsonReloadListener<>("deity/sacrifice", Sacrifice.class, Sacrifice.CODEC,
            l -> l.getEntries().forEach(e -> RPGGods.CHANNEL.send(PacketDistributor.ALL.noArg(), new SSacrificePacket(e.getKey(), e.getValue().get()))));
    public static final GenericJsonReloadListener<Perk> PERK = new GenericJsonReloadListener<>("deity/perk", Perk.class, Perk.CODEC,
            l -> l.getEntries().forEach(e -> RPGGods.CHANNEL.send(PacketDistributor.ALL.noArg(), new SPerkPacket(e.getKey(), e.getValue().get()))));

    public static final Logger LOGGER = LogManager.getFormatterLogger(RPGGods.MODID);

    public RPGGods() {
        LOGGER.debug("registerListeners");
        // Mod event bus listeners
        FMLJavaModLoadingContext.get().getModEventBus().register(RGRegistry.ItemReg.class);
        FMLJavaModLoadingContext.get().getModEventBus().register(RGRegistry.EntityReg.class);
        FMLJavaModLoadingContext.get().getModEventBus().register(RGRegistry.AttributesReg.class);
        FMLJavaModLoadingContext.get().getModEventBus().register(RGRegistry.ContainerReg.class);
        FMLJavaModLoadingContext.get().getModEventBus().register(RGRegistry.RecipeReg.class);
        FMLJavaModLoadingContext.get().getModEventBus().register(RGRegistry.ClientReg.class);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(RPGGods::setup);
        // Forge event bus listeners
        // Required for data pack sync and favor capability
        MinecraftForge.EVENT_BUS.register(RGData.class);

        LOGGER.debug("registerNetwork");
        int messageId = 0;
        CHANNEL.registerMessage(messageId++, SAltarPacket.class, SAltarPacket::toBytes, SAltarPacket::fromBytes, SAltarPacket::handlePacket, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(messageId++, SOfferingPacket.class, SOfferingPacket::toBytes, SOfferingPacket::fromBytes, SOfferingPacket::handlePacket, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(messageId++, SSacrificePacket.class, SSacrificePacket::toBytes, SSacrificePacket::fromBytes, SSacrificePacket::handlePacket, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(messageId++, SPerkPacket.class, SPerkPacket::toBytes, SPerkPacket::fromBytes, SPerkPacket::handlePacket, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(messageId++, CUpdateAltarPacket.class, CUpdateAltarPacket::toBytes, CUpdateAltarPacket::fromBytes, CUpdateAltarPacket::handlePacket, Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    public static void setup(final FMLCommonSetupEvent event) {
        // register capability
        CapabilityManager.INSTANCE.register(IFavor.class, new Favor.Storage(), Favor::new);
    }


}
