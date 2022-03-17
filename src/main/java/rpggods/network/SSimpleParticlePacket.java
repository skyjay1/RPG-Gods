/*
package rpggods.network;

import net.minecraft.command.impl.ParticleCommand;
import net.minecraft.network.PacketBuffer;
import net.minecraft.particles.IParticleData;
import net.minecraft.particles.ParticleType;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Random;
import java.util.function.Supplier;

public class SSimpleParticlePacket {
    protected ResourceLocation particleId;
    protected BlockPos pos = BlockPos.ZERO;
    protected byte count = 0;

    public SSimpleParticlePacket() {
    }


    public SSimpleParticlePacket(final ResourceLocation particleId, final BlockPos pos, final int count) {
        this.particleId = particleId;
        this.pos = pos;
        this.count = (byte) count;
    }

    */
/**
     * Reads the raw packet data from the data stream.
     *
     * @param buf the PacketBuffer
     * @return a new instance of a SSimpleParticlesPacket based on the PacketBuffer
     *//*

    public static SSimpleParticlePacket fromBytes(final PacketBuffer buf) {
        final ResourceLocation id = buf.readResourceLocation();
        final BlockPos pos = buf.readBlockPos();
        final int count = buf.readByte();
        return new SSimpleParticlePacket(id, pos, count);
    }

    */
/**
     * Writes the raw packet data to the data stream.
     *
     * @param msg the SSimpleParticlesPacket
     * @param buf the PacketBuffer
     *//*

    public static void toBytes(final SSimpleParticlePacket msg, final PacketBuffer buf) {
        buf.writeResourceLocation(msg.particleId);
        buf.writeBlockPos(msg.pos);
        buf.writeByte(msg.count);
    }

    */
/**
     * Handles the packet when it is received.
     *
     * @param message         the SSimpleParticlesPacket
     * @param contextSupplier the NetworkEvent.Context supplier
     *//*

    public static void handlePacket(final SSimpleParticlePacket message, final Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide() == LogicalSide.CLIENT) {
            context.enqueueWork(() -> {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                ParticleType particle = ForgeRegistries.PARTICLE_TYPES.getValue(message.particleId);
                final Random rand = mc.player.getRNG();
                for (int i = 0; i < message.count; ++i) {
                    double x2 = message.pos.getX() + rand.nextDouble();
                    double y2 = message.pos.getY() + rand.nextDouble();
                    double z2 = message.pos.getZ() + rand.nextDouble();
                    mc.world.addParticle(particle, x2, y2, z2, 0, 0, 0);
                }
            });
        }
        context.setPacketHandled(true);
    }
}
*/
