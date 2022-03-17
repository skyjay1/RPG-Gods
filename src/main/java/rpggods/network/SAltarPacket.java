package rpggods.network;

import com.mojang.serialization.DataResult;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.network.NetworkEvent;
import rpggods.RPGGods;
import rpggods.deity.Altar;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Called when datapacks are (re)loaded.
 * Sent from the server to the client with a single ResourceLocation ID
 * and the corresponding Altar as it was read from JSON.
 **/
public class SAltarPacket {

    protected ResourceLocation deityName;
    protected Altar altar;

    /**
     * @param deityNameIn the ResourceLocation ID of the Deity
     * @param altarIn     the Altar
     **/
    public SAltarPacket(final ResourceLocation deityNameIn, final Altar altarIn) {
        this.deityName = deityNameIn;
        this.altar = altarIn;
    }

    /**
     * Reads the raw packet data from the data stream.
     *
     * @param buf the PacketBuffer
     * @return a new instance of a SAltarPacket based on the PacketBuffer
     */
    public static SAltarPacket fromBytes(final PacketBuffer buf) {
        final ResourceLocation sName = buf.readResourceLocation();
        final CompoundNBT sNBT = buf.readNbt();
        final Optional<Altar> sEffect = RPGGods.ALTAR.readObject(sNBT)
                .resultOrPartial(error -> RPGGods.LOGGER.error("Failed to read Altar from NBT for packet\n" + error));
        return new SAltarPacket(sName, sEffect.orElse(Altar.EMPTY));
    }

    /**
     * Writes the raw packet data to the data stream.
     *
     * @param msg the SAltarPacket
     * @param buf the PacketBuffer
     */
    public static void toBytes(final SAltarPacket msg, final PacketBuffer buf) {
        DataResult<INBT> nbtResult = RPGGods.ALTAR.writeObject(msg.altar);
        INBT tag = nbtResult.resultOrPartial(error -> RPGGods.LOGGER.error("Failed to write Altar to NBT for packet\n" + error)).get();
        buf.writeResourceLocation(msg.deityName);
        buf.writeNbt((CompoundNBT) tag);
    }

    /**
     * Handles the packet when it is received.
     *
     * @param message         the SAltarPacket
     * @param contextSupplier the NetworkEvent.Context supplier
     */
    public static void handlePacket(final SAltarPacket message, final Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide() == LogicalSide.CLIENT) {
            context.enqueueWork(() -> {
                RPGGods.ALTAR.put(message.deityName, message.altar);
            });
        }
        context.setPacketHandled(true);
    }
}
