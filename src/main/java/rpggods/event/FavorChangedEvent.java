package rpggods.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.util.StringRepresentable;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Cancelable;
import rpggods.favor.FavorLevel;

/**
 * This event is fired when a player's favor is about to change.
 * This event is not {@link Cancelable}.
 * This event does not have a result. {@link HasResult}.
 * This event is fired on the {@link MinecraftForge#EVENT_BUS}.
 */
public class FavorChangedEvent extends PlayerEvent {

    private final ResourceLocation deity;
    private final long oldFavor;
    private final Source source;
    private final long newFavor;
    private final boolean isLevelChange;

    /**
     * @param playerIn  the player whose favor is changing
     * @param deityIn   the deity whose favor is changing
     * @param prevFavor the amount of favor before this event
     * @param curFavor  the amount of favor after this event
     * @param sourceIn  the source of the change in favor
     */
    private FavorChangedEvent(final Player playerIn, final ResourceLocation deityIn,
                             final long prevFavor, final long curFavor, final Source sourceIn) {
        super(playerIn);
        deity = deityIn;
        oldFavor = prevFavor;
        newFavor = curFavor;
        source = sourceIn;
        isLevelChange = FavorLevel.calculateLevel(curFavor) != FavorLevel.calculateLevel(prevFavor);
    }

    /**
     * @return the Deity whose favor is changing
     **/
    public ResourceLocation getDeity() {
        return deity;
    }

    /**
     * @return the amount of favor from before this event
     **/
    public long getOldFavor() {
        return oldFavor;
    }

    /**
     * @return the new favor amount
     **/
    public long getNewFavor() {
        return newFavor;
    }

    /**
     * @return the Source of the change in favor
     **/
    public Source getSource() {
        return source;
    }

    /**
     * @return true if this change in favor also changes the favor level
     **/
    public boolean isLevelChange() {
        return isLevelChange;
    }


    public static class Pre extends FavorChangedEvent {

        private long newFavorToApply;
        private boolean isLevelChangeToApply;

        /**
         * @param playerIn  the player whose favor is changing
         * @param deityIn   the deity whose favor is changing
         * @param prevFavor the amount of favor before this event
         * @param curFavor  the amount of favor after this event
         * @param sourceIn  the source of the change in favor
         */
        public Pre(Player playerIn, ResourceLocation deityIn, long prevFavor, long curFavor, Source sourceIn) {
            super(playerIn, deityIn, prevFavor, curFavor, sourceIn);
            setNewFavor(curFavor);
        }

        /**
         * @param favor the favor amount that should be applied instead
         */
        public void setNewFavor(final long favor) {
            newFavorToApply = favor;
            isLevelChangeToApply = FavorLevel.calculateLevel(newFavorToApply) != FavorLevel.calculateLevel(getOldFavor());
        }

        /**
         * @return the amount of favor that will be applied after this event
         **/
        @Override
        public long getNewFavor() {
            return newFavorToApply;
        }

        /**
         * @return true if this change in favor also changes the favor level
         **/
        @Override
        public boolean isLevelChange() {
            return isLevelChangeToApply;
        }
    }


    public static class Post extends FavorChangedEvent {

        /**
         * @param playerIn  the player whose favor is changing
         * @param deityIn   the deity whose favor is changing
         * @param prevFavor the amount of favor before this event
         * @param curFavor  the amount of favor after this event
         * @param sourceIn  the source of the change in favor
         */
        public Post(Player playerIn, ResourceLocation deityIn, long prevFavor, long curFavor, Source sourceIn) {
            super(playerIn, deityIn, prevFavor, curFavor, sourceIn);
        }
    }


    /**
     * This is used to indicate why the favor is changing
     *
     * @see FavorChangedEvent
     */
    public static enum Source implements StringRepresentable {
        DECAY("decay"),
        OFFERING("offering"),
        SACRIFICE("sacrifice"),
        PERK("perk"),
        COMMAND("command"),
        OTHER("other");

        private final String name;

        private Source(final String sourceName) {
            name = sourceName;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
