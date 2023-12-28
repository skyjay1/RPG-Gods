package rpggods.data.tameable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import rpggods.RPGGods;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public class Tameable implements ITameable {

    public static final Tameable EMPTY = new Tameable();

    private Optional<UUID> owner = Optional.empty();
    private boolean sitting;
    private boolean tamed;

    public Tameable() {}

    @Override
    public boolean isTamed() {
        return this != EMPTY && tamed;
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

    public static class Provider implements ICapabilitySerializable<CompoundTag> {
        private final ITameable instance;
        private final LazyOptional<ITameable> storage;

        public Provider(final Entity entity) {
            instance = new Tameable();
            storage = LazyOptional.of(() -> instance);
        }

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
            if(cap == RPGGods.TAMEABLE) {
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
