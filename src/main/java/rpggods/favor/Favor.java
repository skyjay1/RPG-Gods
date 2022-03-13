package rpggods.favor;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.Capability.IStorage;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import rpggods.RPGGods;

import javax.swing.text.html.Option;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Favor implements IFavor {

    protected final Map<ResourceLocation, FavorLevel> favorMap = new HashMap<>();
    protected final Map<String, Long> cooldownMap = new HashMap<>();
    private Optional<ResourceLocation> patron = Optional.empty();
    private boolean enabled = true;
    private long timestamp = 0L;

    public Favor() {
    }

    @Override
    public FavorLevel getFavor(final ResourceLocation deity) {
        return favorMap.computeIfAbsent(deity, id -> new FavorLevel(0));
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
        return cooldownMap;
    }

    @Override
    public boolean isEnabled() {
        return /* TODO: GreekFantasy.CONFIG.isFavorEnabled() && */ enabled;
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
        public INBT writeNBT(Capability<IFavor> capability, IFavor instance, Direction side) {
            return instance.serializeNBT();
        }

        @Override
        public void readNBT(Capability<IFavor> capability, IFavor instance, Direction side, INBT nbt) {
            if (nbt instanceof CompoundNBT) {
                instance.deserializeNBT((CompoundNBT) nbt);
            } else {
                RPGGods.LOGGER.error("Failed to read Favor capability from NBT of type " + (nbt != null ? nbt.getType().getName() : "null"));
            }
        }
    }

    public static class Provider implements ICapabilitySerializable<CompoundNBT> {
        public IFavor instance = RPGGods.FAVOR.getDefaultInstance();

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
            return cap == RPGGods.FAVOR ? RPGGods.FAVOR.orEmpty(cap, LazyOptional.of(() -> instance)) : LazyOptional.empty();
        }

        @Override
        public CompoundNBT serializeNBT() {
            return (CompoundNBT) RPGGods.FAVOR.getStorage().writeNBT(RPGGods.FAVOR, this.instance, null);
        }

        @Override
        public void deserializeNBT(CompoundNBT nbt) {
            RPGGods.FAVOR.getStorage().readNBT(RPGGods.FAVOR, this.instance, null, nbt);
        }
    }
}
