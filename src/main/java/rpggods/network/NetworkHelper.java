package rpggods.network;

import net.minecraft.world.World;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.Optional;

public class NetworkHelper {

    public static Optional<World> getClientWorld(final NetworkEvent.Context context) {
        if(context.getDirection().getReceptionSide() == LogicalSide.CLIENT) {
            return Optional.ofNullable(net.minecraft.client.Minecraft.getInstance().level);
        }
        return Optional.empty();
    }
}
