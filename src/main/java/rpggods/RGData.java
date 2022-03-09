package rpggods;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.network.PacketDistributor;
import rpggods.favor.Favor;
import rpggods.favor.IFavor;
import rpggods.network.SAltarPacket;
import rpggods.network.SOfferingPacket;
import rpggods.network.SPerkPacket;
import rpggods.network.SSacrificePacket;

public final class RGData {

    /**
     * Used to sync datapack data from the server to each client
     * @param event the player login event
     **/
    @SubscribeEvent
    public static void onPlayerLogin(final PlayerEvent.PlayerLoggedInEvent event) {
        PlayerEntity player = event.getPlayer();
        if (player instanceof ServerPlayerEntity) {
            RPGGods.ALTAR.getEntries().forEach(e -> RPGGods.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) player), new SAltarPacket(e.getKey(), e.getValue().get())));
            RPGGods.OFFERING.getEntries().forEach(e -> RPGGods.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) player), new SOfferingPacket(e.getKey(), e.getValue().get())));
            RPGGods.SACRIFICE.getEntries().forEach(e -> RPGGods.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) player), new SSacrificePacket(e.getKey(), e.getValue().get())));
            RPGGods.PERK.getEntries().forEach(e -> RPGGods.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) player), new SPerkPacket(e.getKey(), e.getValue().get())));
        }
    }

    /**
     * Used to sync datapack info when resources are reloaded
     * @param event the reload listener event
     **/
    @SubscribeEvent
    public static void onReloadListeners(final AddReloadListenerEvent event) {
        RPGGods.LOGGER.debug("onReloadListeners");
        event.addListener(RPGGods.ALTAR);
        event.addListener(RPGGods.OFFERING);
        event.addListener(RPGGods.SACRIFICE);
        event.addListener(RPGGods.PERK);
    }

    /**
     * Used to attach Favor to players
     * @param event the capability attach event (Entity)
     */
    @SubscribeEvent
    public static void onAttachCapabilities(final AttachCapabilitiesEvent<Entity> event) {
        if(event.getObject() instanceof PlayerEntity) {
            event.addCapability(IFavor.REGISTRY_NAME, new Favor.Provider());
        }
    }

    /**
     * Used to ensure that favor persists across deaths
     * @param event the player clone event
     */
    @SubscribeEvent
    public static void onPlayerClone(final PlayerEvent.Clone event) {
        LazyOptional<IFavor> original = event.getOriginal().getCapability(RPGGods.FAVOR);
        LazyOptional<IFavor> copy = event.getPlayer().getCapability(RPGGods.FAVOR);
        if(original.isPresent() && copy.isPresent()) {
            copy.ifPresent(f -> f.deserializeNBT(original.orElseGet(() -> RPGGods.FAVOR.getDefaultInstance()).serializeNBT()));
        }
    }
}
