package rpggods.favor;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.INBTSerializable;
import rpggods.RPGGods;
import rpggods.deity.Altar;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public interface IFavor extends INBTSerializable<CompoundNBT> {

    public static final ResourceLocation REGISTRY_NAME = new ResourceLocation(RPGGods.MODID, "favor");

    static final String ENABLED = "Enabled";
    static final String FAVOR_LEVELS = "FavorLevels";
    static final String PATRON = "Patron";
    static final String NAME = "Name";
    static final String FAVOR = "Favor";
    static final String COOLDOWN = "Cooldown";
    static final String TIMESTAMP = "Timestamp";

    public static final long MIN_FAVOR = 10;

    /**
     * Gets the FavorLevel for the given Deity
     *
     * @param deity the IDeity
     * @return the FavorLevel associated with the given IDeity
     */
    FavorLevel getFavor(final ResourceLocation deity);

    /**
     * Updates the favor info for the given diety
     *
     * @param deity      the IDeity
     * @param favorLevel the new FavorLevel
     */
    void setFavor(final ResourceLocation deity, final FavorLevel favorLevel);

    /**
     * @return a map of all Deity and favor info objects
     **/
    Map<ResourceLocation, FavorLevel> getAllFavor();

    /**
     * @return a map of all cooldown categories and amounts
     */
    Map<String, Long> getPerkCooldownMap();

    /**
     * @return true if favor is enabled for this player
     **/
    boolean isEnabled();

    /**
     * @param enabledIn true if favor should be enabled
     **/
    void setEnabled(boolean enabledIn);

    /**
     * @return the time (in ticks) of the last cooldown calculation
     */
    long getCooldownTimestamp();

    /**
     * @param currentTime the current time
     */
    void setCooldownTimestamp(long currentTime);

    /**
     * @return the Patron deity of this favor holder
     */
    Optional<ResourceLocation> getPatron();

    /**
     * @param patron the updated Patron deity of this favor holder, or an empty optional
     */
    void setPatron(Optional<ResourceLocation> patron);

    /**
     * @return the time until the next perk
     **/
    default long getPerkCooldown(final String key) {
        return getPerkCooldownMap().getOrDefault(key, 0L);
    }

    /**
     * @param cooldown the amount of time until the next favor effect
     **/
    default void setPerkCooldown(final String key, long cooldown) {
        getPerkCooldownMap().put(key, cooldown);
    }

    /**
     * @param key the perk category
     * @return true if the time is greater than the timestamp+cooldown
     */
    default boolean hasNoPerkCooldown(final String key) {
        return getPerkCooldownMap().getOrDefault(key, 0L) <= 0;
    }

    /**
     * Reduces all perk cooldown categories by the given amount
     * @param currentTime the current time in ticks
     */
    default void tickPerkCooldown(final long currentTime) {
        long timeSinceLastTick = currentTime - getCooldownTimestamp();
        setCooldownTimestamp(currentTime);
        for(Entry<String, Long> entry : getPerkCooldownMap().entrySet()) {
            if(entry.getValue() > 0) {
                entry.setValue(entry.getValue() - timeSinceLastTick);
            }
        }
    }

    @Override
    default CompoundNBT serializeNBT() {
        final CompoundNBT nbt = new CompoundNBT();
        final ListNBT deities = new ListNBT();
        // write favor levels
        for (final Entry<ResourceLocation, FavorLevel> entry : getAllFavor().entrySet()) {
            final CompoundNBT deityTag = new CompoundNBT();
            deityTag.putString(NAME, entry.getKey().toString());
            deityTag.put(FAVOR, entry.getValue().serializeNBT());
            deities.add(deityTag);
        }
        nbt.put(FAVOR_LEVELS, deities);
        // write patron
        getPatron().ifPresent(p -> nbt.putString(PATRON, p.toString()));
        // write enabled
        nbt.putBoolean(ENABLED, isEnabled());
        // write cooldowns
        CompoundNBT cooldown = new CompoundNBT();
        for(Entry<String, Long> entry : getPerkCooldownMap().entrySet()) {
            cooldown.putLong(entry.getKey(), entry.getValue());
        }
        nbt.put(COOLDOWN, cooldown);
        // write cooldown timestamp
        nbt.putLong(TIMESTAMP, getCooldownTimestamp());
        return nbt;
    }

    @Override
    default void deserializeNBT(final CompoundNBT nbt) {
        final ListNBT deities = nbt.getList(FAVOR_LEVELS, 10);
        for (int i = 0, l = deities.size(); i < l; i++) {
            final CompoundNBT deity = deities.getCompound(i);
            final String name = deity.getString(NAME);
            final FavorLevel level = deity.contains(FAVOR, 10)
                    ? new FavorLevel(deity.getCompound(FAVOR))
                    : new FavorLevel(deity.getLong(FAVOR));
            setFavor(new ResourceLocation(name), level);
        }
        setEnabled((!nbt.contains(ENABLED)) || nbt.getBoolean(ENABLED));
        if(nbt.contains(PATRON)) {
            setPatron(Optional.ofNullable(ResourceLocation.tryParse(nbt.getString(PATRON))));
        }
        CompoundNBT cooldown = nbt.getCompound(COOLDOWN);
        for(String key : cooldown.getAllKeys()) {
            setPerkCooldown(key, cooldown.getLong(key));
        }
        setCooldownTimestamp(nbt.getLong(TIMESTAMP));
    }
}
