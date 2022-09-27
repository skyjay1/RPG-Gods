package rpggods.network;

import com.mojang.serialization.Codec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;
import rpggods.RPGGods;
import rpggods.deity.Deity;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Called when datapacks are (re)loaded.
 * Sent from the server to the client with a map of
 * ResourceLocation IDs and Deities
 **/
public class SDeityPacket {

    protected static final Codec<Map<ResourceLocation, Deity>> CODEC = Codec.unboundedMap(ResourceLocation.CODEC, Deity.CODEC);

    protected Map<ResourceLocation, Deity> data;

    /**
     * @param data the data map
     **/
    public SDeityPacket(final Map<ResourceLocation, Deity> data) {
        this.data = data;
        if (FMLEnvironment.dist != Dist.CLIENT) {
            // update server-side map
            RPGGods.DEITY_MAP.clear();
            RPGGods.DEITY_MAP.putAll(data);
        }
    }

    /**
     * Reads the raw packet data from the data stream.
     *
     * @param buf the PacketBuffer
     * @return a new instance of a SDeityPacket based on the PacketBuffer
     */
    public static SDeityPacket fromBytes(final FriendlyByteBuf buf) {
        final Map<ResourceLocation, Deity> data = buf.readWithCodec(CODEC);
        return new SDeityPacket(data);
    }

    /**
     * Writes the raw packet data to the data stream.
     *
     * @param msg the SDeityPacket
     * @param buf the PacketBuffer
     */
    public static void toBytes(final SDeityPacket msg, final FriendlyByteBuf buf) {
        buf.writeWithCodec(CODEC, msg.data);
    }

    /**
     * Handles the packet when it is received.
     *
     * @param message         the SDeityPacket
     * @param contextSupplier the NetworkEvent.Context supplier
     */
    public static void handlePacket(final SDeityPacket message, final Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide() == LogicalSide.CLIENT) {
            context.enqueueWork(() -> {
                RPGGods.DEITY_MAP.clear();
                RPGGods.DEITY_MAP.putAll(message.data);
            });
        }
        context.setPacketHandled(true);
    }
}
