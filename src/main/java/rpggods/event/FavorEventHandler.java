package rpggods.event;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import rpggods.entity.AltarEntity;
import rpggods.favor.IFavor;

public class FavorEventHandler {

    /**
     * Called when the player attempts to give an offering
     * @param deity the deity ID
     * @param player the player
     * @param favor the player's favor
     * @param item the item being offered
     * @return true if the offering was accepted and the ItemStack size is changed
     */
    public static boolean onOffering(final ResourceLocation deity, final PlayerEntity player, final IFavor favor, final ItemStack item) {
        return false;
    }

    /**
     * Called when the player kills a living entity
     * @param player the player
     * @param favor the player's favor
     * @param entity the entity that was killed
     * @return true if the player's favor was modified
     */
    public static boolean onSacrifice(final PlayerEntity player, final IFavor favor, final LivingEntity entity) {
        return false;
    }

    public static class ModEvents {

    }

    public static class ForgeEvents {

    }
}
