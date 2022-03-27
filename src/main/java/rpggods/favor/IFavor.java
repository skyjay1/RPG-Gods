package rpggods.favor;

import com.google.common.collect.Iterables;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.INBTSerializable;
import rpggods.RPGGods;
import rpggods.deity.Offering;
import rpggods.deity.Sacrifice;
import rpggods.event.FavorChangedEvent;
import rpggods.deity.Cooldown;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

public interface IFavor extends INBTSerializable<CompoundNBT> {

    ResourceLocation REGISTRY_NAME = new ResourceLocation(RPGGods.MODID, "favor");

    String ENABLED = "Enabled";
    String FAVOR_LEVELS = "FavorLevels";
    String PATRON = "Patron";
    String NAME = "Name";
    String FAVOR = "Favor";
    String PERK_COOLDOWN = "PerkCooldown";
    String OFFERING_COOLDOWN = "OfferingCooldown";
    String SACRIFICE_COOLDOWN = "SacrificeCooldown";
    String TIMESTAMP = "Timestamp";

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
     * @return a map of all offering IDs and cooldowns
     */
    Map<ResourceLocation, Cooldown> getOfferingCooldownMap();

    /**
     * @return a map of all sacrifice IDs and cooldowns
     */
    Map<ResourceLocation, Cooldown> getSacrificeCooldownMap();

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
     * Updates the player's patron deity by removing the previous patron, if any, and adjusting favor decay
     * @param player the player
     * @param patron the new patron deity, or empty to remove the current patron
     * @param decayBonus the favor decay rate to add to the given patron
     * @param favorPunish the amount of favor to add to the old patron (typically negative)
     * @return true if the patron deity was changed
     */
    default boolean setPatron(final PlayerEntity player, final Optional<ResourceLocation> patron,
                                    final float decayBonus, final long favorPunish) {
        Optional<ResourceLocation> old = getPatron();
        // attempt to change patron (if no existing patron OR given patron is empty OR existing and given are different)
        if(!old.isPresent() || !patron.isPresent() || !patron.get().equals(old.get())) {
            // remove old patron
            if (old.isPresent()) {
                // reset favor decay
                FavorLevel level = getFavor(old.get());
                level.setDecayRate((float) RPGGods.CONFIG.getFavorDecayRate());
                // add favor to old patron
                level.addFavor(player, old.get(), favorPunish, FavorChangedEvent.Source.OTHER);
            }
            // set new patron
            setPatron(patron);
            // add multiplier to favor decay
            if(patron.isPresent()) {
                FavorLevel level = getFavor(patron.get());
                level.setDecayRate(level.getDecayRate() + decayBonus);
            }
            return true;
        }
        return false;
    }

    /**
     * @param key the cooldown category
     * @return the amount of cooldown left for the given category, in ticks
     **/
    default long getPerkCooldown(final String key) {
        return getPerkCooldownMap().getOrDefault(key, 0L);
    }

    /**
     * @param key the cooldown category
     * @param cooldown the amount of cooldown for the given category, in ticks
     **/
    default void setPerkCooldown(final String key, long cooldown) {
        getPerkCooldownMap().put(key, cooldown);
    }

    default void reset() {
        getAllFavor().clear();
        setPatron(Optional.empty());
        resetCooldowns();
    }

    default void resetCooldowns() {
        getPerkCooldownMap().clear();
        getOfferingCooldownMap().clear();
        getSacrificeCooldownMap().clear();
    }

    /**
     * @param key the perk category
     * @return true if the time is greater than the timestamp+cooldown
     */
    default boolean hasNoPerkCooldown(final String key) {
        return getPerkCooldownMap().getOrDefault(key, 0L) <= 0;
    }

    /**
     * Sets the cooldown for a given offering
     * @param id the offering id
     * @param cooldown the cooldown to apply
     */
    default void setOfferingCooldown(final ResourceLocation id, final Cooldown cooldown) {
        getOfferingCooldownMap().put(id, cooldown);
    }

    /**
     * @param id the offering id
     * @return the cooldown tracker for the given offering
     **/
    default Cooldown getOfferingCooldown(final ResourceLocation id) {
        return getOfferingCooldownMap().computeIfAbsent(id, r -> RPGGods.OFFERING.get(r).orElse(Offering.EMPTY).createCooldown());
    }

    /**
     * Sets the cooldown for a given sacrifice
     * @param id the sacrifice id
     * @param cooldown the cooldown to apply
     */
    default void setSacrificeCooldown(final ResourceLocation id, final Cooldown cooldown) {
        getSacrificeCooldownMap().put(id, cooldown);
    }

    /**
     * @param id the sacrifice id
     * @return the cooldown tracker for the given offering
     **/
    default Cooldown getSacrificeCooldown(final ResourceLocation id) {
        return getSacrificeCooldownMap().computeIfAbsent(id, r -> RPGGods.SACRIFICE.get(r).orElse(Sacrifice.EMPTY).createCooldown());
    }

    /**
     * Reduces all perk, offering, and sacrifice cooldown categories by the given amount
     * @param currentTime the current time in ticks
     */
    default void tickCooldown(final long currentTime) {
        long timeSinceLastTick = currentTime - getCooldownTimestamp();
        setCooldownTimestamp(currentTime);
        // iterate over perk cooldown map
        for(Entry<String, Long> entry : getPerkCooldownMap().entrySet()) {
            if(entry.getValue() > 0) {
                entry.setValue(entry.getValue() - timeSinceLastTick);
            }
        }
        // iterate over offering and sacrifice cooldown maps
        Iterable<Cooldown> cooldowns = Iterables.concat(getOfferingCooldownMap().values(), getSacrificeCooldownMap().values());
        for(Cooldown cooldown : cooldowns) {
            if(cooldown.getCooldown() > 0) {
                cooldown.addCooldown(-timeSinceLastTick);
            }
        }
    }

    default void depleteFavor(final PlayerEntity player) {
        final int amount = RPGGods.CONFIG.getFavorDecayAmount();
        for(Entry<ResourceLocation, FavorLevel> entry : getAllFavor().entrySet()) {
            entry.getValue().depleteFavor(player, entry.getKey(), amount, FavorChangedEvent.Source.DECAY, false);
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
        // write perk cooldowns
        CompoundNBT perkCooldowns = new CompoundNBT();
        for(Entry<String, Long> entry : getPerkCooldownMap().entrySet()) {
            perkCooldowns.putLong(entry.getKey(), entry.getValue());
        }
        nbt.put(PERK_COOLDOWN, perkCooldowns);
        // write offering cooldowns
        CompoundNBT offeringCooldowns = new CompoundNBT();
        for(Entry<ResourceLocation, Cooldown> entry : getOfferingCooldownMap().entrySet()) {
            offeringCooldowns.put(entry.getKey().toString(), entry.getValue().serializeNBT());
        }
        nbt.put(OFFERING_COOLDOWN, offeringCooldowns);
        // write sacrifice cooldowns
        CompoundNBT sacrificeCooldowns = new CompoundNBT();
        for(Entry<ResourceLocation, Cooldown> entry : getOfferingCooldownMap().entrySet()) {
            sacrificeCooldowns.put(entry.getKey().toString(), entry.getValue().serializeNBT());
        }
        nbt.put(SACRIFICE_COOLDOWN, sacrificeCooldowns);
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
        // read perk cooldowns
        CompoundNBT perks = nbt.getCompound(PERK_COOLDOWN);
        for(String key : perks.getAllKeys()) {
            setPerkCooldown(key, perks.getLong(key));
        }
        // read offering cooldowns
        CompoundNBT offerings = nbt.getCompound(OFFERING_COOLDOWN);
        for(String key : offerings.getAllKeys()) {
            setOfferingCooldown(ResourceLocation.tryParse(key), new Cooldown(offerings.getCompound(key)));
        }
        // read sacrifice cooldowns
        CompoundNBT sacrifices = nbt.getCompound(SACRIFICE_COOLDOWN);
        for(String key : sacrifices.getAllKeys()) {
            setSacrificeCooldown(ResourceLocation.tryParse(key), new Cooldown(sacrifices.getCompound(key)));
        }
        // read timestamp
        setCooldownTimestamp(nbt.getLong(TIMESTAMP));
    }
}
