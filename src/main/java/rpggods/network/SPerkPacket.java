package rpggods.network;

import com.google.common.collect.Lists;
import com.mojang.serialization.DataResult;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.network.NetworkEvent;
import rpggods.RPGGods;
import rpggods.deity.Deity;
import rpggods.perk.Affinity;
import rpggods.perk.Perk;
import rpggods.perk.PerkData;

import java.util.EnumMap;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Called when datapacks are (re)loaded.
 * Sent from the server to the client with a single ResourceLocation ID
 * and the corresponding Offering as it was read from JSON.
 **/
public class SPerkPacket {

    protected ResourceLocation perkName;
    protected Perk perk;

    /**
     * @param perkNameIn the ResourceLocation ID of the Deity
     * @param perkIn     the Perk
     **/
    public SPerkPacket(final ResourceLocation perkNameIn, final Perk perkIn) {
        this.perkName = perkNameIn;
        this.perk = perkIn;
    }

    /**
     * Reads the raw packet data from the data stream.
     *
     * @param buf the PacketBuffer
     * @return a new instance of a SPerkPacket based on the PacketBuffer
     */
    public static SPerkPacket fromBytes(final PacketBuffer buf) {
        final ResourceLocation sName = buf.readResourceLocation();
        final CompoundNBT sNBT = buf.readNbt();
        final Optional<Perk> sEffect = RPGGods.PERK.readObject(sNBT)
                .resultOrPartial(error -> RPGGods.LOGGER.error("Failed to read Perk from NBT for packet\n" + error));
        return new SPerkPacket(sName, sEffect.orElse(Perk.EMPTY));
    }

    /**
     * Writes the raw packet data to the data stream.
     *
     * @param msg the SPerkPacket
     * @param buf the PacketBuffer
     */
    public static void toBytes(final SPerkPacket msg, final PacketBuffer buf) {
        DataResult<INBT> nbtResult = RPGGods.PERK.writeObject(msg.perk);
        INBT tag = nbtResult.resultOrPartial(error -> RPGGods.LOGGER.error("Failed to write Perk to NBT for packet\n" + error)).get();
        buf.writeResourceLocation(msg.perkName);
        buf.writeNbt((CompoundNBT) tag);
    }

    /**
     * Handles the packet when it is received.
     *
     * @param message         the SPerkPacket
     * @param contextSupplier the NetworkEvent.Context supplier
     */
    public static void handlePacket(final SPerkPacket message, final Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide() == LogicalSide.CLIENT) {
            context.enqueueWork(() -> {
                Perk perk = message.perk;
                RPGGods.PERK.put(message.perkName, perk);
                // add Perk to Deity
                RPGGods.DEITY.computeIfAbsent(perk.getDeity(), Deity::new).add(message.perkName, perk);
                // add Perk to Affinity map if applicable
                for(PerkData action : perk.getActions()) {
                    if(action.getAffinity().isPresent()) {
                        Affinity affinity = action.getAffinity().get();
                        RPGGods.AFFINITY.computeIfAbsent(affinity.getEntity(), id -> new EnumMap<>(Affinity.Type.class))
                                .computeIfAbsent(affinity.getType(), id -> Lists.newArrayList()).add(message.perkName);
                    }
                }
            });
        }
        context.setPacketHandled(true);
    }
}
