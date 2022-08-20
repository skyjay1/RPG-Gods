package rpggods.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.network.NetworkHooks;
import rpggods.RPGGods;
import rpggods.favor.FavorLevel;
import rpggods.favor.IFavor;
import rpggods.gui.FavorContainer;

public class ScrollItem extends Item {

    public ScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // prevent use when there are no registered deities
        if(RPGGods.DEITY_HELPER.isEmpty()) {
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        }
        // begin using item (to enable texture change) and open GUI
        player.startUsingItem(hand);
        if(player instanceof ServerPlayer) {
            LazyOptional<IFavor> favor = RPGGods.getFavor(player);
            if(favor.isPresent()) {
                IFavor ifavor = favor.orElse(null);
                // prevent use when favor is disabled
                if(!ifavor.isEnabled()) {
                    return InteractionResultHolder.pass(player.getItemInHand(hand));
                }
                // prevent use when there are no unlocked deities
                boolean unlocked = false;
                for(FavorLevel l : ifavor.getAllFavor().values()) {
                    if(l.isEnabled()) {
                        unlocked = true;
                        break;
                    }
                }
                if(!unlocked) {
                    return InteractionResultHolder.pass(player.getItemInHand(hand));
                }
                // open Favor GUI
                NetworkHooks.openGui((ServerPlayer)player,
                        new SimpleMenuProvider((id, inventory, p) ->
                                new FavorContainer(id, inventory, ifavor, null),
                                TextComponent.EMPTY),
                        buf -> {
                            buf.writeNbt(ifavor.serializeNBT());
                            buf.writeBoolean(false);
                        }
                );
            }
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack item) {
        return UseAnim.BLOCK;
    }

    public int getUseDuration(ItemStack item) {
        return 72000;
    }
}
