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
import rpggods.perk.Perk;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Called when datapacks are (re)loaded.
 * Sent from the server to the client with a map of
 * ResourceLocation IDs and Perks
 **/
public class SPerkPacket {

    protected static final Codec<Map<ResourceLocation, Perk>> CODEC = Codec.unboundedMap(ResourceLocation.CODEC, Perk.CODEC);

    protected Map<ResourceLocation, Perk> data;

    /**
     * @param data the data map
     **/
    public SPerkPacket(final Map<ResourceLocation, Perk> data) {
        this.data = data;
        if (FMLEnvironment.dist != Dist.CLIENT) {
            // update server-side map
            updatePerks(data);
        }
    }

    /**
     * Reads the raw packet data from the data stream.
     *
     * @param buf the PacketBuffer
     * @return a new instance of a SPerkPacket based on the PacketBuffer
     */
    public static SPerkPacket fromBytes(final FriendlyByteBuf buf) {
        final Map<ResourceLocation, Perk> data = buf.readWithCodec(CODEC);
        return new SPerkPacket(data);
    }

    /**
     * Writes the raw packet data to the data stream.
     *
     * @param msg the SPerkPacket
     * @param buf the PacketBuffer
     */
    public static void toBytes(final SPerkPacket msg, final FriendlyByteBuf buf) {
        buf.writeWithCodec(CODEC, msg.data);
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
                updatePerks(message.data);
            });
        }
        context.setPacketHandled(true);
    }

    private static void updatePerks(final Map<ResourceLocation, Perk> data) {
        // update map
        RPGGods.PERK_MAP.clear();
        RPGGods.PERK_MAP.putAll(data);
        // clear affinity map
        RPGGods.AFFINITY.clear();
        // clear all deity helper perks
        for(DeityHelper helper : RPGGods.DEITY_HELPER.values()) {
            helper.perkList.clear();
            helper.perkByConditionMap.clear();
            helper.perkByTypeMap.clear();
        }
        // add perks to deity helper
        for(Map.Entry<ResourceLocation, Perk> entry : RPGGods.PERK_MAP.entrySet()) {
            RPGGods.DEITY_HELPER.computeIfAbsent(entry.getValue().getDeity(), DeityHelper::new).add(entry.getKey(), entry.getValue());
        }
    }
}
