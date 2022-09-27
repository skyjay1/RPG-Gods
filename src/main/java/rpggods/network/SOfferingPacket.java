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
import rpggods.deity.Offering;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Called when datapacks are (re)loaded.
 * Sent from the server to the client with a map of
 * ResourceLocation IDs and Offerings
 **/
public class SOfferingPacket {

    protected static final Codec<Map<ResourceLocation, Offering>> CODEC = Codec.unboundedMap(ResourceLocation.CODEC, Offering.CODEC);

    protected Map<ResourceLocation, Offering> data;

    /**
     * @param data the data map
     **/
    public SOfferingPacket(final Map<ResourceLocation, Offering> data) {
        this.data = data;
        if (FMLEnvironment.dist != Dist.CLIENT) {
            // update server-side map
            updateOfferings(data);
        }
    }

    /**
     * Reads the raw packet data from the data stream.
     *
     * @param buf the PacketBuffer
     * @return a new instance of a SOfferingPacket based on the PacketBuffer
     */
    public static SOfferingPacket fromBytes(final FriendlyByteBuf buf) {
        final Map<ResourceLocation, Offering> data = buf.readWithCodec(CODEC);
        return new SOfferingPacket(data);
    }

    /**
     * Writes the raw packet data to the data stream.
     *
     * @param msg the SOfferingPacket
     * @param buf the PacketBuffer
     */
    public static void toBytes(final SOfferingPacket msg, final FriendlyByteBuf buf) {
        buf.writeWithCodec(CODEC, msg.data);
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
                updateOfferings(message.data);
            });
        }
        context.setPacketHandled(true);
    }

    private static void updateOfferings(final Map<ResourceLocation, Offering> data) {
        // add data to map
        RPGGods.OFFERING_MAP.clear();
        RPGGods.OFFERING_MAP.putAll(data);
        // clear all deity helper offerings
        for(DeityHelper helper : RPGGods.DEITY_HELPER.values()) {
            helper.offeringMap.clear();
        }
        // add offerings to deity helper
        for(Map.Entry<ResourceLocation, Offering> entry : RPGGods.OFFERING_MAP.entrySet()) {
            RPGGods.DEITY_HELPER.computeIfAbsent(Offering.getDeity(entry.getKey()), DeityHelper::new).add(entry.getKey(), entry.getValue());
        }
    }
}
