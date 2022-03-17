package rpggods.tameable;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
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
        public INBT writeNBT(Capability<ITameable> capability, ITameable instance, Direction side) {
            return instance.serializeNBT();
        }

        @Override
        public void readNBT(Capability<ITameable> capability, ITameable instance, Direction side, INBT nbt) {
            if (nbt instanceof CompoundNBT) {
                instance.deserializeNBT((CompoundNBT) nbt);
            } else {
                RPGGods.LOGGER.error("Failed to read Tameable capability from NBT of type " + (nbt != null ? nbt.getType().getName() : "null"));
            }
        }
    }

    public static class Provider implements ICapabilitySerializable<CompoundNBT> {
        public ITameable instance = RPGGods.TAMEABLE.getDefaultInstance();

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
            return cap == RPGGods.TAMEABLE ? RPGGods.TAMEABLE.orEmpty(cap, LazyOptional.of(() -> instance)) : LazyOptional.empty();
        }

        @Override
        public CompoundNBT serializeNBT() {
            return (CompoundNBT) RPGGods.TAMEABLE.getStorage().writeNBT(RPGGods.TAMEABLE, this.instance, null);
        }

        @Override
        public void deserializeNBT(CompoundNBT nbt) {
            RPGGods.TAMEABLE.getStorage().readNBT(RPGGods.TAMEABLE, this.instance, null, nbt);
        }
    }
}
