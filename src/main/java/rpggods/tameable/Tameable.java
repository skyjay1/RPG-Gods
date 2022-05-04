package rpggods.tameable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import rpggods.RPGGods;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public class Tameable implements ITameable {

    private Optional<UUID> owner = Optional.empty();
    private boolean sitting;
    private boolean tamed;

    public Tameable() {}

    @Override
    public boolean isTamed() {
        return tamed;
    }

    @Override
    public Optional<UUID> getOwnerId() {
        return owner;
    }

    @Override
    public boolean isSitting() {
        return sitting;
    }

    @Override
    public void setTamed(boolean tamed) {
        this.tamed = tamed;
    }

    @Override
    public void setSitting(boolean sitting) {
        this.sitting = sitting;
    }

    @Override
    public void setOwnerId(@Nullable UUID ownerId) {
        this.owner = Optional.ofNullable(ownerId);
    }

    public static class Storage implements Capability.IStorage<ITameable> {

        @Override
        public Tag writeNBT(Capability<ITameable> capability, ITameable instance, Direction side) {
            return instance.serializeNBT();
        }

        @Override
        public void readNBT(Capability<ITameable> capability, ITameable instance, Direction side, Tag nbt) {
            if (nbt instanceof CompoundTag) {
                instance.deserializeNBT((CompoundTag) nbt);
            } else {
                RPGGods.LOGGER.error("Failed to read Tameable capability from NBT of type " + (nbt != null ? nbt.getType().getName() : "null"));
            }
        }
    }

    public static class Provider implements ICapabilitySerializable<CompoundTag> {
        public ITameable instance = RPGGods.TAMEABLE.getDefaultInstance();

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
            return cap == RPGGods.TAMEABLE ? RPGGods.TAMEABLE.orEmpty(cap, LazyOptional.of(() -> instance)) : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            return (CompoundTag) RPGGods.TAMEABLE.getStorage().writeNBT(RPGGods.TAMEABLE, this.instance, null);
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            RPGGods.TAMEABLE.getStorage().readNBT(RPGGods.TAMEABLE, this.instance, null, nbt);
        }
    }
}
