package rpggods.network;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.network.NetworkEvent;
import rpggods.RPGGods;
import rpggods.altar.AltarPose;
import rpggods.entity.AltarEntity;

import java.util.function.Supplier;

/**
 * Created when the player closes a StatueContainer GUI.
 * The packet sends the BlockPos, StatuePose, and other settings
 * to the server to update the StatueTileEntity NBT data.
 **/
public class CUpdateAltarPacket {

    protected static final int NAME_LEN = 50;
    protected int entityId;
    protected AltarPose pose = AltarPose.EMPTY;
    protected boolean female = false;
    protected boolean slim = false;
    protected String textureName = "";

    public CUpdateAltarPacket() {
    }

    /**
     * @param entityId    the ID of the altar entity
     * @param pose        the StatuePose settings
     * @param female      true if the statue uses the female model
     * @param slim        true if the statue uses the slim model
     * @param textureName the String name of the texture to use
     **/
    public CUpdateAltarPacket(final int entityId, final AltarPose pose,
                              final boolean female, final boolean slim, final String textureName) {
        this.entityId = entityId;
        this.pose = pose;
        this.female = female;
        this.slim = slim;
        this.textureName = textureName;
    }

    /**
     * Reads the raw packet data from the data stream.
     *
     * @param buf the PacketBuffer
     * @return a new instance of a CUpdateAltarPacket based on the PacketBuffer
     */
    public static CUpdateAltarPacket fromBytes(final PacketBuffer buf) {
        final int id = buf.readInt();
        final CompoundNBT nbt = buf.readCompoundTag();
        final boolean female = buf.readBoolean();
        final boolean slim = buf.readBoolean();
        final String textureName = buf.readString(NAME_LEN);
        return new CUpdateAltarPacket(id, new AltarPose(nbt), female, slim, textureName);
    }

    /**
     * Writes the raw packet data to the data stream.
     *
     * @param msg the CUpdateAltarPacket
     * @param buf the PacketBuffer
     */
    public static void toBytes(final CUpdateAltarPacket msg, final PacketBuffer buf) {
        buf.writeInt(msg.entityId);
        buf.writeCompoundTag(msg.pose.serializeNBT());
        buf.writeBoolean(msg.female);
        buf.writeBoolean(msg.slim);
        String name = msg.textureName;
        if (name.length() > NAME_LEN) {
            name = name.substring(0, NAME_LEN);
        }
        buf.writeString(name, NAME_LEN);
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
                final ServerPlayerEntity player = context.getSender();
                Entity entity = context.getSender().getEntityWorld().getEntityByID(message.entityId);
                RPGGods.LOGGER.debug("Updating entity id " + message.entityId + ": " + (entity != null ? entity.toString() : "NOT FOUND"));
                double maxDistance = player.getAttributeValue(ForgeMod.REACH_DISTANCE.get());
                if(entity != null && entity instanceof AltarEntity && player.getDistanceSq(entity) < Math.pow(maxDistance, 2)) {
                    // update pose and name
                    AltarEntity altar = (AltarEntity) entity;
                    altar.setAltarPose(message.pose);
                    if(message.textureName != null && !message.textureName.isEmpty()) {
                        altar.setCustomName(new StringTextComponent(message.textureName));
                    }
                }
            });
        }
        context.setPacketHandled(true);
    }
}
