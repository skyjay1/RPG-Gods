package rpggods.network;

import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.network.NetworkEvent;
import rpggods.RPGGods;
import rpggods.altar.AltarPose;
import rpggods.entity.AltarEntity;

import java.util.function.Supplier;

/**
 * Created when the player closes an AltarScreen GUI.
 * The packet sends the AltarPose, name, and other settings
 * to the server to update the AltarEntity NBT data.
 **/
public class CUpdateAltarPacket {

    protected static final int NAME_LEN = 50;
    protected int entityId;
    protected AltarPose pose = AltarPose.EMPTY;
    protected boolean female = false;
    protected boolean slim = false;
    protected String customName = "";

    public CUpdateAltarPacket() {
    }

    /**
     * @param entityId    the ID of the altar entity
     * @param pose        the StatuePose settings
     * @param female      true if the statue uses the female model
     * @param slim        true if the statue uses the slim model
     * @param customName the String name of the texture to use
     **/
    public CUpdateAltarPacket(final int entityId, final AltarPose pose,
                              final boolean female, final boolean slim, final String customName) {
        this.entityId = entityId;
        this.pose = pose;
        this.female = female;
        this.slim = slim;
        this.customName = customName;
    }

    /**
     * Reads the raw packet data from the data stream.
     *
     * @param buf the PacketBuffer
     * @return a new instance of a CUpdateAltarPacket based on the PacketBuffer
     */
    public static CUpdateAltarPacket fromBytes(final FriendlyByteBuf buf) {
        final int id = buf.readInt();
        final CompoundTag nbt = buf.readNbt();
        final boolean female = buf.readBoolean();
        final boolean slim = buf.readBoolean();
        final String customName = buf.readUtf(NAME_LEN);
        return new CUpdateAltarPacket(id, new AltarPose(nbt), female, slim, customName);
    }

    /**
     * Writes the raw packet data to the data stream.
     *
     * @param msg the CUpdateAltarPacket
     * @param buf the PacketBuffer
     */
    public static void toBytes(final CUpdateAltarPacket msg, final FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeNbt(msg.pose.serializeNBT());
        buf.writeBoolean(msg.female);
        buf.writeBoolean(msg.slim);
        String name = msg.customName;
        if (name.length() > NAME_LEN) {
            name = name.substring(0, NAME_LEN);
        }
        buf.writeUtf(name, NAME_LEN);
    }

    /**
     * Handles the packet when it is received.
     *
     * @param message         the CUpdateStatuePosePacket
     * @param contextSupplier the NetworkEvent.Context supplier
     */
    public static void handlePacket(final CUpdateAltarPacket message, final Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide() == LogicalSide.SERVER) {
            context.enqueueWork(() -> {
                final ServerPlayer player = context.getSender();
                Entity entity = context.getSender().getCommandSenderWorld().getEntity(message.entityId);
                double maxDistance = player.getAttributeValue(ForgeMod.REACH_DISTANCE.get());
                if (entity != null && entity instanceof AltarEntity && player.distanceToSqr(entity) < Math.pow(maxDistance, 2)) {
                    // update pose and name
                    AltarEntity altar = (AltarEntity) entity;
                    altar.setAltarPose(message.pose);
                    if (message.customName != null && !message.customName.isEmpty()) {
                        altar.setCustomName(new TextComponent(message.customName));
                    }
                }
            });
        }
        context.setPacketHandled(true);
    }
}
