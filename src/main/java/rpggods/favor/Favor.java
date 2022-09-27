package rpggods.favor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import rpggods.RPGGods;
import rpggods.deity.Cooldown;
import rpggods.deity.Deity;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Favor implements IFavor {

    public static final Favor EMPTY = new Favor(false);

    protected final Map<ResourceLocation, FavorLevel> favorMap = new HashMap<>();
    protected final Map<String, Long> perkCooldownMap = new HashMap<>();
    protected final Map<ResourceLocation, Cooldown> offeringCooldownMap = new HashMap<>();
    protected final Map<ResourceLocation, Cooldown> sacrificeCooldownMap = new HashMap<>();
    private Optional<ResourceLocation> patron = Optional.empty();
    private boolean enabled = true;
    private long timestamp = 0L;

    public Favor() {
    }

    private Favor(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public FavorLevel getFavor(final ResourceLocation deityId) {
        return favorMap.computeIfAbsent(deityId, id -> {
            final FavorLevel level = new FavorLevel(0);
            Optional<Deity> deity = Optional.ofNullable(RPGGods.DEITY_MAP.get(deityId));
            deity.ifPresent(d -> {
                level.setEnabled(d.isUnlocked() && d.isEnabled());
                level.setLevelBounds(d.getMinLevel(), d.getMaxLevel());
            });
            return level;
        });
    }

    @Override
    public void setFavor(ResourceLocation deity, FavorLevel favorLevel) {
        favorMap.put(deity, favorLevel);
    }

    @Override
    public Map<ResourceLocation, FavorLevel> getAllFavor() {
        return favorMap;
    }

    @Override
    public Map<String, Long> getPerkCooldownMap() {
        return perkCooldownMap;
    }

    @Override
    public Map<ResourceLocation, Cooldown> getOfferingCooldownMap() {
        return offeringCooldownMap;
    }

    @Override
    public Map<ResourceLocation, Cooldown> getSacrificeCooldownMap() {
        return sacrificeCooldownMap;
    }

    @Override
    public boolean isEnabled() {
        return RPGGods.CONFIG.isFavorEnabled() && enabled;
    }

    @Override
    public void setEnabled(boolean enabledIn) {
        enabled = enabledIn;
    }

    @Override
    public long getCooldownTimestamp() {
        return timestamp;
    }

    @Override
    public void setCooldownTimestamp(long currentTime) {
        timestamp = currentTime;
    }

    @Override
    public Optional<ResourceLocation> getPatron() {
        return this.patron;
    }

    @Override
    public void setPatron(Optional<ResourceLocation> patron) {
        this.patron = patron;
    }

    @Override
    public String toString() {
        final StringBuilder b = new StringBuilder("Favor:");
        b.append(" enabled[").append(enabled).append("]");
        b.append("\nfavorMap[").append(getAllFavor().toString()).append("]");
        b.append("\ncooldownMap[").append(getPerkCooldownMap().toString()).append("]");
        return b.toString();
    }

    public static class Provider implements ICapabilitySerializable<CompoundTag> {
        private final IFavor instance;
        private final LazyOptional<IFavor> storage;

        public Provider(Player player) {
            instance = new Favor();
            storage = LazyOptional.of(() -> instance);
        }

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
            if(cap == RPGGods.FAVOR) {
                return storage.cast();
            }
            return LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            return instance.serializeNBT();
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            instance.deserializeNBT(nbt);
        }
    }
}
