package rpggods.favor;

import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.INBTSerializable;
import rpggods.deity.DeityHelper;
import rpggods.util.FavorChangedEvent;

import javax.annotation.Nullable;

public class FavorLevel implements INBTSerializable<CompoundTag> {

    public static final int MAX_FAVOR_LEVEL = 10;
    public static final long MAX_FAVOR_POINTS = calculateFavor(MAX_FAVOR_LEVEL + 1) - 1;

    public static final String FAVOR = "Favor";
    public static final String MIN_LEVEL = "MinLevel";
    public static final String MAX_LEVEL = "MaxLevel";
    public static final String DECAY_RATE = "DecayRate";
    public static final String PERK_BONUS = "PerkBonus";
    public static final String ENABLED = "Enabled";

    private long favor;
    private int level;
    private int minLevel = -MAX_FAVOR_LEVEL;
    private int maxLevel = MAX_FAVOR_LEVEL;
    private float decayRate;
    private float perkBonus;
    private boolean enabled;

    public FavorLevel(final long f) {
        setFavor(f);
        setDecayRate(0);
        setPerkBonus(0);
    }

    public FavorLevel(final CompoundTag nbt) {
        this.deserializeNBT(nbt);
    }

    /**
     * Directly modifies the favor, with some bounds-checking
     * to keep it within the min and max range. If possible,
     * use the context-aware method
     * {@link #setFavor(Player, ResourceLocation, long, FavorChangedEvent.Source)}
     *
     * @param favorIn the new favor value
     */
    private void setFavor(long favorIn) {
        // update favor and level
        this.favor = clamp(favorIn, calculateFavor(minLevel - 1) + 1, calculateFavor(maxLevel + 1) - 1);
        this.level = calculateLevel(favor);
    }

    /**
     * @return the current favor value
     **/
    public long getFavor() {
        return favor;
    }

    /**
     * @return the current favor level
     **/
    public int getLevel() {
        return level;
    }

    /**
     * @return the lower bound of this favor level
     **/
    public int getMin() {
        return minLevel;
    }

    /**
     * @return the upper bound of this favor level
     **/
    public int getMax() {
        return maxLevel;
    }

    /**
     * @return The percent chance for favor to deplete
     **/
    public float getDecayRate() { return decayRate; }

    /**
     * @return The bonus percent chance to apply to perks
     **/
    public float getPerkBonus() { return perkBonus; }

    public boolean isEnabled() { return enabled; }

    /**
     * Context-aware method to add favor that also posts an event for any listeners.
     * If you don't want this, call {@link #setFavor(long)} directly.
     *
     * @param playerIn the player whose favor is being modified, or null for global favor
     * @param deityIn  the deity for which the favor is being modified
     * @param newFavor the new favor amount
     * @param source   the cause for the change in favor
     * @return the updated favor value
     */
    public long setFavor(@Nullable final Player playerIn, final ResourceLocation deityIn, final long newFavor, final FavorChangedEvent.Source source) {
        // Post a context-aware event to allow other modifiers
        final FavorChangedEvent.Pre eventPre = new FavorChangedEvent.Pre(playerIn, deityIn, favor, newFavor, source);
        MinecraftForge.EVENT_BUS.post(eventPre);
        final long eventOldFavor = eventPre.getOldFavor();
        final long eventNewFavor = eventPre.getNewFavor();
        setFavor(eventNewFavor);
        final FavorChangedEvent.Post eventPost = new FavorChangedEvent.Post(playerIn, deityIn, eventOldFavor, eventNewFavor, source);
        MinecraftForge.EVENT_BUS.post(eventPost);
        return favor;
    }

    /**
     * Context-aware method to add favor that also posts an event for any listeners.
     * If you don't want this, call {@link #setFavor(long)} directly.
     *
     * @param playerIn the player whose favor is being modified, or null for global favor
     * @param deityIn  the deity for which the favor is being modified
     * @param toAdd    the amount of favor to add or subtract
     * @param source   the cause for the change in favor
     * @return the updated favor value
     */
    public long addFavor(@Nullable final Player playerIn, final ResourceLocation deityIn, final long toAdd, final FavorChangedEvent.Source source) {
        // Post a context-aware event to allow other modifiers
        if(toAdd != 0) {
            return setFavor(playerIn, deityIn, favor + toAdd, source);
        }
        return favor;
    }

    /**
     * Uses the decay rate to determine whether to deplete favor
     *
     * @param playerIn the player whose favor is being modified
     * @param deityIn  the deity for which the favor is being modified
     * @param toRemove the amount of favor to deplete (must be positive)
     * @param source   the cause for the favor depletion (usually PASSIVE)
     * @param forced true to ignore decay rate and force the favor to change
     * @return the updated favor value
     */
    public long depleteFavor(final Player playerIn, final ResourceLocation deityIn, final long toRemove,
                                final FavorChangedEvent.Source source, final boolean forced) {
        if(enabled && (forced || Math.random() < (decayRate + 1.0F))) {
            return addFavor(playerIn, deityIn, Math.min(Math.abs(favor), Math.abs(toRemove)) * -1 * (long) Math.signum(favor), source);
        }
        return favor;
    }

    public void setLevelBounds(final int min, final int max) {
        minLevel = min;
        maxLevel = max;
        setFavor(favor);
    }

    /**
     * @param decayRate the decay rate modifier, from -1 to 1
     */
    public void setDecayRate(final float decayRate) {
        this.decayRate = decayRate;
    }

    /**
     * @param perkBonus the perk chance modifier, from -1 to 1
     */
    public void setPerkBonus(float perkBonus) {
        this.perkBonus = perkBonus;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @return the favor value required to advance to the next level
     **/
    public long getFavorToNextLevel() {
        return (level == minLevel || level == maxLevel) ? 0 : calculateFavor(level + (int) Math.signum(favor));
    }

    /**
     * @return the percent of favor that has been earned (always positive)
     **/
    public double getPercentFavor() {
        return Math.abs((double) favor / (double) (0.1D + calculateFavor(maxLevel) - calculateFavor(minLevel)));
    }

    public int compareToAbs(FavorLevel other) {
        return (int) (Math.abs(this.getFavor()) - Math.abs(other.getFavor()));
    }

    /**
     * Sends a chat message to the player informing them of their current favor level
     *
     * @param playerIn the player
     * @param deity    the deity associated with this favor level
     */
    public void sendStatusMessage(final Player playerIn, final ResourceLocation deity) {
        long favorToNext = Math.min(calculateFavor(maxLevel), getFavorToNextLevel());
        String sFavorToNext = (favorToNext == 0 ? "--" : String.valueOf(favorToNext));
        playerIn.displayClientMessage(new TranslatableComponent("favor.current_favor",
                DeityHelper.getName(deity), getFavor(), sFavorToNext, getLevel())
                .withStyle(ChatFormatting.LIGHT_PURPLE), false);
    }

    @Override
    public CompoundTag serializeNBT() {
        final CompoundTag nbt = new CompoundTag();
        nbt.putLong(FAVOR, favor);
        nbt.putInt(MIN_LEVEL, minLevel);
        nbt.putInt(MAX_LEVEL, maxLevel);
        nbt.putFloat(DECAY_RATE, decayRate);
        nbt.putFloat(PERK_BONUS, perkBonus);
        nbt.putBoolean(ENABLED, enabled);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        minLevel = nbt.getInt(MIN_LEVEL);
        maxLevel = nbt.getInt(MAX_LEVEL);
        setFavor(nbt.getLong(FAVOR));
        decayRate = nbt.getFloat(DECAY_RATE);
        perkBonus = nbt.getFloat(PERK_BONUS);
        enabled = nbt.getBoolean(ENABLED);
    }

    @Override
    public String toString() {
        return favor + " (" + level + ") range[" + minLevel + "," + maxLevel + "] enabled[" + enabled + "] decay[" + decayRate + "]";
    }

    /**
     * @param favorIn the favor amount
     * @return the favor level based on amount of favor
     **/
    public static int calculateLevel(final long favorIn) {
        // calculate the current level based on favor
        final long f = Math.abs(favorIn);
        final int sig = (int) Math.signum(favorIn + 1);
        return sig * Math.floorDiv(-100 + (int) Math.sqrt(10000 + 40 * f), 20);
    }

    /**
     * @param lv the favor level
     * @return the maximum amount of favor for a given level
     **/
    public static long calculateFavor(final int lv) {
        final int l = Math.abs(lv);
        final int sig = (int) Math.signum(lv);
        return sig * (10L * l * (l + 10L));
    }

    /**
     * Replacement for the "MathHelper" version which is client-only
     *
     * @param num the number to clamp
     * @param min the minimum value
     * @param max the maximum value
     * @return the number, or the min or max if num is out of range
     */
    private static long clamp(final long num, final long min, final long max) {
        if (num <= min) return min;
        else if (num >= max) return max;
        else return num;
    }
}
