package rpggods.network;

import com.mojang.serialization.Codec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;
import rpggods.RPGGods;
import rpggods.deity.DeityHelper;
import rpggods.deity.Sacrifice;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Called when datapacks are (re)loaded.
 * Sent from the server to the client with a map of
 * ResourceLocation IDs and Sacrifices
 **/
public class SSacrificePacket {

    protected static final Codec<Map<ResourceLocation, Sacrifice>> CODEC = Codec.unboundedMap(ResourceLocation.CODEC, Sacrifice.CODEC);

    protected Map<ResourceLocation, Sacrifice> data;

    /**
     * @param data the data map
     **/
    public SSacrificePacket(final Map<ResourceLocation, Sacrifice> data) {
        this.data = data;
        if (FMLEnvironment.dist != Dist.CLIENT) {
            // update server-side map
            updateSacrifices(data);
        }
    }

    /**
     * Reads the raw packet data from the data stream.
     *
     * @param buf the PacketBuffer
     * @return a new instance of a SSacrificePacket based on the PacketBuffer
     */
    public static SSacrificePacket fromBytes(final FriendlyByteBuf buf) {
        final Map<ResourceLocation, Sacrifice> data = buf.readWithCodec(CODEC);
        return new SSacrificePacket(data);
    }

    /**
     * Writes the raw packet data to the data stream.
     *
     * @param msg the SSacrificePacket
     * @param buf the PacketBuffer
     */
    public static void toBytes(final SSacrificePacket msg, final FriendlyByteBuf buf) {
        buf.writeWithCodec(CODEC, msg.data);
    }

    /**
     * Handles the packet when it is received.
     *
     * @param message         the SSacrificePacket
     * @param contextSupplier the NetworkEvent.Context supplier
     */
    public static void handlePacket(final SSacrificePacket message, final Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide() == LogicalSide.CLIENT) {
            context.enqueueWork(() -> {
                updateSacrifices(message.data);
            });
        }
        context.setPacketHandled(true);
    }

    private static void updateSacrifices(final Map<ResourceLocation, Sacrifice> data) {
        // add data to map
        RPGGods.SACRIFICE_MAP.clear();
        RPGGods.SACRIFICE_MAP.putAll(data);
        // clear all deity helper sacrifices
        for(DeityHelper helper : RPGGods.DEITY_HELPER.values()) {
            helper.sacrificeMap.clear();
        }
        // add offerings to deity helper
        for(Map.Entry<ResourceLocation, Sacrifice> entry : RPGGods.SACRIFICE_MAP.entrySet()) {
            RPGGods.DEITY_HELPER.computeIfAbsent(Sacrifice.getDeity(entry.getKey()), DeityHelper::new).add(entry.getKey(), entry.getValue());
        }
    }
}
