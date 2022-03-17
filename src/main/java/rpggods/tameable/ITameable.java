package rpggods.tameable;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.util.INBTSerializable;
import rpggods.RPGGods;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public interface ITameable extends INBTSerializable<CompoundNBT> {

    public static final ResourceLocation REGISTRY_NAME = new ResourceLocation(RPGGods.MODID, "tameable");

    static final String OWNER = "Owner";
    static final String SITTING = "Sitting";

    Optional<UUID> getOwnerId();

    boolean isSitting();

    boolean isTamed();

    void setTamed(boolean tamed);

    void setSitting(boolean sitting);

    void setOwnerId(@Nullable UUID ownerId);

    default boolean setTamedBy(PlayerEntity player) {
        if(!getOwnerId().isPresent()) {
            this.setTamed(true);
            this.setOwnerId(player.getUUID());
            return true;
        }
        return false;
    }

    default Optional<LivingEntity> getOwner(World world) {
        Optional<UUID> uuid = this.getOwnerId();
        return uuid.isPresent() ? Optional.ofNullable(world.getPlayerByUUID(uuid.get())) : Optional.empty();
    }

    default boolean isOwner(LivingEntity entity) {
        return isTamed() && getOwnerId().isPresent() && getOwnerId().get().equals(entity.getUUID());
    }

    @Override
    default CompoundNBT serializeNBT() {
        final CompoundNBT nbt = new CompoundNBT();
        if(getOwnerId().isPresent()) {
            nbt.putUUID(OWNER, getOwnerId().get());
        }
        nbt.putBoolean(SITTING, isSitting());
        return nbt;
    }

    @Override
    default void deserializeNBT(final CompoundNBT nbt) {
        UUID uuid = null;
        if (nbt.hasUUID(OWNER)) {
            uuid = nbt.getUUID(OWNER);
        }

        if (uuid != null) {
            try {
                this.setOwnerId(uuid);
                this.setTamed(true);
            } catch (Throwable throwable) {
                this.setTamed(false);
            }
        }
        setSitting(nbt.getBoolean(SITTING));
    }
}
