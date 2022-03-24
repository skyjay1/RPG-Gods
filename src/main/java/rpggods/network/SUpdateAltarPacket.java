package rpggods.network;

import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.network.NetworkEvent;
import rpggods.RPGGods;
import rpggods.entity.AltarEntity;

import java.util.function.Supplier;

/**
 * Created when an AltarEntity joins the world.
 * The packet sends the AltarEntity inventory to the client
 * to prevent de-sync.
 **/
public class SUpdateAltarPacket {

    protected int entityId;
    protected ItemStack block;

    public SUpdateAltarPacket() {
    }


    /**
     * @param entityId The ID of the AltarEntity
     * @param block the AltarEntity block
     */
    public SUpdateAltarPacket(final int entityId, final ItemStack block) {
        this.entityId = entityId;
        this.block = block;
    }

    /**
     * Reads the raw packet data from the data stream.
     *
     * @param buf the PacketBuffer
     * @return a new instance of a SUpdateAltarPacket based on the PacketBuffer
     */
    public static SUpdateAltarPacket fromBytes(final PacketBuffer buf) {
        final int id = buf.readInt();
        final ItemStack item = buf.readItem();
        return new SUpdateAltarPacket(id, item);
    }

    /**
     * Writes the raw packet data to the data stream.
     *
     * @param msg the SUpdateAltarPacket
     * @param buf the PacketBuffer
     */
    public static void toBytes(final SUpdateAltarPacket msg, final PacketBuffer buf) {
        buf.writeInt(msg.entityId);
        buf.writeItem(msg.block);
    }

    /**
     * Handles the packet when it is received.
     *
     * @param message         the SUpdateAltarPacket
     * @param contextSupplier the NetworkEvent.Context supplier
     */
    public static void handlePacket(final SUpdateAltarPacket message, final Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide() == LogicalSide.CLIENT) {
            context.enqueueWork(() -> {
                // locate the entity by ID
                World world = net.minecraft.client.Minecraft.getInstance().level;
                Entity entity = world.getEntity(message.entityId);
                if (entity != null && entity instanceof AltarEntity) {
                    AltarEntity altar = (AltarEntity) entity;
                    // update block slot
                    altar.setBlockSlot(message.block);
                }
            });
        }
        context.setPacketHandled(true);
    }
}
