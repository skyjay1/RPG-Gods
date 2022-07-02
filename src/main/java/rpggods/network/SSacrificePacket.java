package rpggods.network;

import com.mojang.serialization.DataResult;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;
import rpggods.RPGGods;
import rpggods.deity.DeityHelper;
import rpggods.deity.Sacrifice;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Called when datapacks are (re)loaded.
 * Sent from the server to the client with a single ResourceLocation ID
 * and the corresponding Sacrifice as it was read from JSON.
 **/
public class SSacrificePacket {

    protected ResourceLocation sacrificeId;
    protected Sacrifice sacrifice;

    /**
     * @param sacrificeId the ResourceLocation ID of the Sacrifice
     * @param sacrificeIn     the Sacrifice
     **/
    public SSacrificePacket(final ResourceLocation sacrificeId, final Sacrifice sacrificeIn) {
        this.sacrificeId = sacrificeId;
        this.sacrifice = sacrificeIn;
    }

    /**
     * Reads the raw packet data from the data stream.
     *
     * @param buf the PacketBuffer
     * @return a new instance of a SSacrificePacket based on the PacketBuffer
     */
    public static SSacrificePacket fromBytes(final FriendlyByteBuf buf) {
        final ResourceLocation sName = buf.readResourceLocation();
        final CompoundTag sNBT = buf.readNbt();
        final Optional<Sacrifice> sEffect = RPGGods.SACRIFICE.readObject(sNBT)
                .resultOrPartial(error -> RPGGods.LOGGER.error("Failed to read Sacrifice from NBT for packet\n" + error));
        return new SSacrificePacket(sName, sEffect.orElse(Sacrifice.EMPTY));
    }

    /**
     * Writes the raw packet data to the data stream.
     *
     * @param msg the SSacrificePacket
     * @param buf the PacketBuffer
     */
    public static void toBytes(final SSacrificePacket msg, final FriendlyByteBuf buf) {
        DataResult<Tag> nbtResult = RPGGods.SACRIFICE.writeObject(msg.sacrifice);
        Tag tag = nbtResult.resultOrPartial(error -> RPGGods.LOGGER.error("Failed to write Sacrifice to NBT for packet\n" + error)).get();
        buf.writeResourceLocation(msg.sacrificeId);
        buf.writeNbt((CompoundTag) tag);
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
                RPGGods.SACRIFICE.put(message.sacrificeId, message.sacrifice);
                ResourceLocation deityId = Sacrifice.getDeity(message.sacrificeId);
                RPGGods.DEITY_HELPER.computeIfAbsent(deityId, DeityHelper::new).add(message.sacrificeId, message.sacrifice);
            });
        }
        context.setPacketHandled(true);
    }
}
