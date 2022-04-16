package rpggods;

import net.minecraft.entity.Entity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.network.PacketDistributor;
import rpggods.entity.AltarEntity;
import rpggods.favor.Favor;
import rpggods.favor.FavorCommand;
import rpggods.favor.IFavor;
import rpggods.network.SUpdateAltarPacket;
import rpggods.tameable.ITameable;
import rpggods.tameable.Tameable;

public final class RGData {

    @SubscribeEvent
    public static void onAddCommands(final RegisterCommandsEvent event) {
        FavorCommand.register(event.getDispatcher());
    }

    /**
     * Used to sync datapack data from the server to each client
     * @param event the player login event
     **/
    @SubscribeEvent
    public static void onPlayerLogin(final PlayerEvent.PlayerLoggedInEvent event) {
        PlayerEntity player = event.getPlayer();
        if (player instanceof ServerPlayerEntity) {
            RPGGods.DEITY.syncOnReload();
            RPGGods.ALTAR.syncOnReload();
            RPGGods.OFFERING.syncOnReload();
            RPGGods.SACRIFICE.syncOnReload();
            RPGGods.PERK.syncOnReload();
        }
    }

    /**
     * Used to sync datapack info when resources are reloaded
     * @param event the reload listener event
     **/
    @SubscribeEvent
    public static void onReloadListeners(final AddReloadListenerEvent event) {
        RPGGods.LOGGER.debug("onReloadListeners");
        RPGGods.DEITY_HELPER.forEach((id, deity) -> deity.clear());
        RPGGods.AFFINITY.clear();
        event.addListener(RPGGods.DEITY);
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
        } else if(event.getObject() instanceof MobEntity
                && !(event.getObject() instanceof TameableEntity)
                && !(event.getObject() instanceof AbstractHorseEntity)) {
            event.addCapability(ITameable.REGISTRY_NAME, new Tameable.Provider());
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

    /**
     * Used to ensure that
     * 1. AltarEntity syncs inventory to client and
     * 2. Tameable entities sync sitting state to client
     * @param event the Player Start Tracking Entity event
     */
    @SubscribeEvent
    public static void onStartTracking(final PlayerEvent.StartTracking event) {
        if(event.getPlayer().isAlive() && !event.getPlayer().level.isClientSide) {
            // sync altar entity
            if(event.getTarget() instanceof AltarEntity) {
                int entityId = event.getTarget().getId();
                ItemStack block = ((AltarEntity)event.getTarget()).getBlockBySlot();
                RPGGods.CHANNEL.send(PacketDistributor.ALL.noArg(), new SUpdateAltarPacket(entityId, block));
            }
            // sync tameable entity
            LazyOptional<ITameable> tameable = event.getTarget().getCapability(RPGGods.TAMEABLE);
            if(tameable.isPresent()) {
                ITameable t = tameable.orElse(null);
                t.setSittingWithUpdate(event.getTarget(), t.isSitting());
            }
        }
    }
}
