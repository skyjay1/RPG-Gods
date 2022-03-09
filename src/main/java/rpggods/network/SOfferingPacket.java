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
import rpggods.deity.Offering;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Called when datapacks are (re)loaded.
 * Sent from the server to the client with a single ResourceLocation ID
 * and the corresponding Offering as it was read from JSON.
 **/
public class SOfferingPacket {

    protected ResourceLocation deityName;
    protected Offering offering;

    /**
     * @param deityNameIn the ResourceLocation ID of the Deity
     * @param offeringIn     the Offering
     **/
    public SOfferingPacket(final ResourceLocation deityNameIn, final Offering offeringIn) {
        this.deityName = deityNameIn;
        this.offering = offeringIn;
    }

    /**
     * Reads the raw packet data from the data stream.
     *
     * @param buf the PacketBuffer
     * @return a new instance of a SOfferingPacket based on the PacketBuffer
     */
    public static SOfferingPacket fromBytes(final PacketBuffer buf) {
        final ResourceLocation sName = buf.readResourceLocation();
        final CompoundNBT sNBT = buf.readCompoundTag();
        final Optional<Offering> sEffect = RPGGods.OFFERING.readObject(sNBT)
                .resultOrPartial(error -> RPGGods.LOGGER.error("Failed to read Offering from NBT for packet\n" + error));
        return new SOfferingPacket(sName, sEffect.orElse(Offering.EMPTY));
    }

    /**
     * Writes the raw packet data to the data stream.
     *
     * @param msg the SOfferingPacket
     * @param buf the PacketBuffer
     */
    public static void toBytes(final SOfferingPacket msg, final PacketBuffer buf) {
        DataResult<INBT> nbtResult = RPGGods.OFFERING.writeObject(msg.offering);
        INBT tag = nbtResult.resultOrPartial(error -> RPGGods.LOGGER.error("Failed to write Offering to NBT for packet\n" + error)).get();
        buf.writeResourceLocation(msg.deityName);
        buf.writeCompoundTag((CompoundNBT) tag);
    }

    /**
     * Handles the packet when it is received.
     *
     * @param message         the SOfferingPacket
     * @param contextSupplier the NetworkEvent.Context supplier
     */
    public static void handlePacket(final SOfferingPacket message, final Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide() == LogicalSide.CLIENT) {
            context.enqueueWork(() -> {
                RPGGods.OFFERING.put(message.deityName, message.offering);
            });
        }
        context.setPacketHandled(true);
    }
}
