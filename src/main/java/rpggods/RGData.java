package rpggods;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
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
     * Used to sync datapack info when resources are reloaded
     * @param event the reload listener event
     **/
    @SubscribeEvent
    public static void onReloadListeners(final AddReloadListenerEvent event) {
        event.addListener(RPGGods.ALTAR_JSON_MANAGER);
        event.addListener(RPGGods.DEITY_JSON_MANAGER);
        event.addListener(RPGGods.OFFERING_JSON_MANAGER);
        event.addListener(RPGGods.PERK_JSON_MANAGER);
        event.addListener(RPGGods.SACRIFICE_JSON_MANAGER);
    }

    /**
     * Used to attach Favor to players
     * @param event the capability attach event (Entity)
     */
    @SubscribeEvent
    public static void onAttachCapabilities(final AttachCapabilitiesEvent<Entity> event) {
        if(event.getObject() instanceof Player) {
            event.addCapability(IFavor.REGISTRY_NAME, new Favor.Provider((Player)event.getObject()));
        } else if(event.getObject() instanceof Mob
                && !(event.getObject() instanceof TamableAnimal)
                && !(event.getObject() instanceof AbstractHorse)) {
            event.addCapability(ITameable.REGISTRY_NAME, new Tameable.Provider(event.getObject()));
        }
    }

    /**
     * Used to ensure that favor persists across deaths
     * @param event the player clone event
     */
    @SubscribeEvent
    public static void onPlayerClone(final PlayerEvent.Clone event) {
        // revive capabilities in order to copy to the clone
        event.getOriginal().reviveCaps();
        LazyOptional<IFavor> original = event.getOriginal().getCapability(RPGGods.FAVOR);
        LazyOptional<IFavor> copy = event.getEntity().getCapability(RPGGods.FAVOR);
        if(original.isPresent() && copy.isPresent()) {
            copy.ifPresent(f -> f.deserializeNBT(original.orElse(Favor.EMPTY).serializeNBT()));
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
        if(event.getEntity().isAlive() && !event.getEntity().level.isClientSide) {
            // sync altar entity
            if(event.getTarget() instanceof AltarEntity) {
                int entityId = event.getTarget().getId();
                ItemStack block = ((AltarEntity)event.getTarget()).getBlockBySlot();
                RPGGods.CHANNEL.send(PacketDistributor.ALL.noArg(), new SUpdateAltarPacket(entityId, block));
            }
            // sync tameable entity
            LazyOptional<ITameable> tameable = event.getTarget().getCapability(RPGGods.TAMEABLE);
            if(tameable.isPresent()) {
                ITameable t = tameable.orElse(Tameable.EMPTY);
                t.setSittingWithUpdate(event.getTarget(), t.isSitting());
            }
        }
    }
}
