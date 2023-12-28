package rpggods.network;

import com.mojang.serialization.Codec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;
import rpggods.RPGGods;
import rpggods.data.deity.Altar;
import rpggods.data.deity.DeityWrapper;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Called when datapacks are (re)loaded.
 * Sent from the server to the client with a map of
 * ResourceLocation IDs and Altars
 **/
// TODO use datapack registry instead
public class SAltarPacket {

    protected static final Codec<Map<ResourceLocation, Altar>> CODEC = Codec.unboundedMap(ResourceLocation.CODEC, Altar.CODEC);

    protected Map<ResourceLocation, Altar> data;

    /**
     * @param data the data map
     **/
    public SAltarPacket(final Map<ResourceLocation, Altar> data) {
        this.data = data;
        if (FMLEnvironment.dist != Dist.CLIENT) {
            // update server-side map
            updateAltars(data);
        }
    }

    /**
     * Reads the raw packet data from the data stream.
     *
     * @param buf the PacketBuffer
     * @return a new instance of a SAltarPacket based on the PacketBuffer
     */
    public static SAltarPacket fromBytes(final FriendlyByteBuf buf) {
        final Map<ResourceLocation, Altar> data = buf.readWithCodec(CODEC);
        return new SAltarPacket(data);
    }

    /**
     * Writes the raw packet data to the data stream.
     *
     * @param msg the SAltarPacket
     * @param buf the PacketBuffer
     */
    public static void toBytes(final SAltarPacket msg, final FriendlyByteBuf buf) {
        buf.writeWithCodec(CODEC, msg.data);
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
                updateAltars(message.data);
            });
        }
        context.setPacketHandled(true);
    }

    private static void updateAltars(final Map<ResourceLocation, Altar> data) {
        // update maps
        RPGGods.ALTAR_MAP.clear();
        RPGGods.ALTAR_MAP.putAll(data);
        // clear all deity helper altars
        for(DeityWrapper helper : RPGGods.DEITY_HELPER.values()) {
            helper.altarList.clear();
        }
        // add altars to deity helper
        for(Map.Entry<ResourceLocation, Altar> entry : RPGGods.ALTAR_MAP.entrySet()) {
            entry.getValue().getDeity().ifPresent(deity ->
                    RPGGods.DEITY_HELPER.computeIfAbsent(deity, DeityWrapper::new).add(entry.getKey(), entry.getValue())
            );
        }
    }
}
