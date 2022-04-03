package rpggods.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.SimpleNamedContainerProvider;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.UseAction;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.network.NetworkHooks;
import rpggods.RPGGods;
import rpggods.favor.IFavor;
import rpggods.gui.FavorContainer;

public class ScrollItem extends Item {

    public ScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    public ActionResult<ItemStack> use(World level, PlayerEntity player, Hand hand) {
        // prevent use when there are no registered deities
        if(RPGGods.DEITY.isEmpty()) {
            return ActionResult.pass(player.getItemInHand(hand));
        }
        // begin using item (to enable texture change) and open GUI
        player.startUsingItem(hand);
        if(player instanceof ServerPlayerEntity) {
            LazyOptional<IFavor> favor = player.getCapability(RPGGods.FAVOR);
            if(favor.isPresent()) {
                IFavor ifavor = favor.orElse(null);
                NetworkHooks.openGui((ServerPlayerEntity)player,
                        new SimpleNamedContainerProvider((id, inventory, p) ->
                                new FavorContainer(id, inventory, ifavor, null),
                                StringTextComponent.EMPTY),
                        buf -> {
                            buf.writeNbt(ifavor.serializeNBT());
                            buf.writeBoolean(false);
                        }
                );
            }
        }
        return ActionResult.success(player.getItemInHand(hand));
    }

    @Override
    public UseAction getUseAnimation(ItemStack item) {
        return UseAction.BLOCK;
    }

    public int getUseDuration(ItemStack item) {
        return 72000;
    }
}
