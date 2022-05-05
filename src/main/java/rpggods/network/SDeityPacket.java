package rpggods.network;

import com.mojang.serialization.DataResult;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;
import rpggods.RPGGods;
import rpggods.deity.Deity;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Called when datapacks are (re)loaded.
 * Sent from the server to the client with a single ResourceLocation ID
 * and the corresponding Deity as it was read from JSON.
 **/
public class SDeityPacket {

    protected ResourceLocation deityId;
    protected Deity deity;

    /**
     * @param deityNameIn the ResourceLocation ID of the Deity
     * @param dietyIn     the Deity
     **/
    public SDeityPacket(final ResourceLocation deityNameIn, final Deity dietyIn) {
        this.deityId = deityNameIn;
        this.deity = dietyIn;
    }

    /**
     * Reads the raw packet data from the data stream.
     *
     * @param buf the PacketBuffer
     * @return a new instance of a SDeityPacket based on the PacketBuffer
     */
    public static SDeityPacket fromBytes(final FriendlyByteBuf buf) {
        final ResourceLocation sName = buf.readResourceLocation();
        final CompoundTag sNBT = buf.readNbt();
        final Optional<Deity> sEffect = RPGGods.DEITY.readObject(sNBT)
                .resultOrPartial(error -> RPGGods.LOGGER.error("Failed to read Deity from NBT for packet\n" + error));
        return new SDeityPacket(sName, sEffect.orElse(Deity.EMPTY));
    }

    /**
     * Writes the raw packet data to the data stream.
     *
     * @param msg the SDeityPacket
     * @param buf the PacketBuffer
     */
    public static void toBytes(final SDeityPacket msg, final FriendlyByteBuf buf) {
        DataResult<Tag> nbtResult = RPGGods.DEITY.writeObject(msg.deity);
        Tag tag = nbtResult.resultOrPartial(error -> RPGGods.LOGGER.error("Failed to write Deity to NBT for packet\n" + error)).get();
        buf.writeResourceLocation(msg.deityId);
        buf.writeNbt((CompoundTag) tag);
    }

    /**
     * Handles the packet when it is received.
     *
     * @param message         the SPerkPacket
     * @param contextSupplier the NetworkEvent.Context supplier
     */
    public static void handlePacket(final SDeityPacket message, final Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide() == LogicalSide.CLIENT) {
            context.enqueueWork(() -> {
                Deity deity = message.deity;
                RPGGods.DEITY.put(message.deityId, deity);
            });
        }
        context.setPacketHandled(true);
    }
}