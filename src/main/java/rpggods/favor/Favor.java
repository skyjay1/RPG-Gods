package rpggods.favor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.Capability.IStorage;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import rpggods.RPGGods;
import rpggods.deity.Cooldown;
import rpggods.deity.Deity;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Favor implements IFavor {

    protected final Map<ResourceLocation, FavorLevel> favorMap = new HashMap<>();
    protected final Map<String, Long> perkCooldownMap = new HashMap<>();
    protected final Map<ResourceLocation, Cooldown> offeringCooldownMap = new HashMap<>();
    protected final Map<ResourceLocation, Cooldown> sacrificeCooldownMap = new HashMap<>();
    private Optional<ResourceLocation> patron = Optional.empty();
    private boolean enabled = true;
    private long timestamp = 0L;

    public Favor() {
    }

    @Override
    public FavorLevel getFavor(final ResourceLocation deityId) {
        return favorMap.computeIfAbsent(deityId, id -> {
            final FavorLevel level = new FavorLevel(0);
            Optional<Deity> deity = RPGGods.DEITY.get(deityId);
            deity.ifPresent(d -> {
                level.setEnabled(d.isUnlocked());
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

    public static class Storage implements IStorage<IFavor> {

        @Override
        public Tag writeNBT(Capability<IFavor> capability, IFavor instance, Direction side) {
            return instance.serializeNBT();
        }

        @Override
        public void readNBT(Capability<IFavor> capability, IFavor instance, Direction side, Tag nbt) {
            if (nbt instanceof CompoundTag) {
                instance.deserializeNBT((CompoundTag) nbt);
            } else {
                RPGGods.LOGGER.error("Failed to read Favor capability from NBT of type " + (nbt != null ? nbt.getType().getName() : "null"));
            }
        }
    }

    public static class Provider implements ICapabilitySerializable<CompoundTag> {
        public IFavor instance = RPGGods.FAVOR.getDefaultInstance();

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
            return cap == RPGGods.FAVOR ? RPGGods.FAVOR.orEmpty(cap, LazyOptional.of(() -> instance)) : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            return (CompoundTag) RPGGods.FAVOR.getStorage().writeNBT(RPGGods.FAVOR, this.instance, null);
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            RPGGods.FAVOR.getStorage().readNBT(RPGGods.FAVOR, this.instance, null, nbt);
        }
    }
}
