package rpggods.network;

import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.network.NetworkEvent;
import rpggods.RPGGods;
import rpggods.entity.AltarEntity;
import rpggods.tameable.ITameable;

import java.util.function.Supplier;

/**
 * Sent when a mob with Tameable capability changes its sitting state
 **/
public class SUpdateSittingPacket {

    protected int entityId;
    protected boolean sitting;

    public SUpdateSittingPacket() {
    }


    /**
     * @param entityId The ID of the entity
     * @param sitting True if the entity is now sitting
     */
    public SUpdateSittingPacket(final int entityId, final boolean sitting) {
        this.entityId = entityId;
        this.sitting = sitting;
    }

    /**
     * Reads the raw packet data from the data stream.
     *
     * @param buf the PacketBuffer
     * @return a new instance of a SUpdateSittingPacket based on the PacketBuffer
     */
    public static SUpdateSittingPacket fromBytes(final PacketBuffer buf) {
        final int id = buf.readInt();
        final boolean sitting = buf.readBoolean();
        return new SUpdateSittingPacket(id, sitting);
    }

    /**
     * Writes the raw packet data to the data stream.
     *
     * @param msg the SUpdateSittingPacket
     * @param buf the PacketBuffer
     */
    public static void toBytes(final SUpdateSittingPacket msg, final PacketBuffer buf) {
        buf.writeInt(msg.entityId);
        buf.writeBoolean(msg.sitting);
    }

    /**
     * Handles the packet when it is received.
     *
     * @param message         the SUpdateSittingPacket
     * @param contextSupplier the NetworkEvent.Context supplier
     */
    public static void handlePacket(final SUpdateSittingPacket message, final Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide() == LogicalSide.CLIENT) {
            context.enqueueWork(() -> {
                // locate the entity by ID
                World world = net.minecraft.client.Minecraft.getInstance().level;
                Entity entity = world.getEntity(message.entityId);
                if (entity != null) {
                    LazyOptional<ITameable> tameable = entity.getCapability(RPGGods.TAMEABLE);
                    if(tameable.isPresent()) {
                        tameable.orElse(null).setSitting(message.sitting);
                    }
                }
            });
        }
        context.setPacketHandled(true);
    }
}
