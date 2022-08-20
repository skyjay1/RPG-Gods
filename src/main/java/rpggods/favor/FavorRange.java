package rpggods.favor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import rpggods.RPGGods;

import java.util.Objects;

public class FavorRange {

    public static final FavorRange EMPTY = new FavorRange(new ResourceLocation(RPGGods.MODID, "null"), 0, 0);

    public static final Codec<FavorRange> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("deity").forGetter(FavorRange::getDeity),
            Codec.INT.fieldOf("minlevel").forGetter(FavorRange::getMinLevel),
            Codec.INT.fieldOf("maxlevel").forGetter(FavorRange::getMaxLevel)
    ).apply(instance, FavorRange::new));

    private final ResourceLocation deity;
    private final int minLevel;
    private final int maxLevel;

    public FavorRange(ResourceLocation deityIn, int minLevelIn, int maxLevelIn) {
        super();
        this.deity = deityIn;
        this.minLevel = Math.min(minLevelIn, maxLevelIn);
        this.maxLevel = Math.max(minLevelIn, maxLevelIn);
    }

    /**
     * @return the deity for this favor range
     **/
    public ResourceLocation getDeity() {
        return deity;
    }

    /**
     * @return the minimum favor level
     **/
    public int getMinLevel() {
        return minLevel;
    }

    /**
     * @return the maximum favor level
     **/
    public int getMaxLevel() {
        return maxLevel;
    }

    /**
     * @param player the player
     * @return true if this is a server world and the player matches this favor range
     */
    public boolean isInRange(final Player player) {
        if (!player.level.isClientSide && player.isEffectiveAi() && this != FavorRange.EMPTY) {
            return isInRange(RPGGods.getFavor(player).orElse(Favor.EMPTY));
        }
        return false;
    }

    /**
     * @param f the player's favor
     * @return true if the player's favor is within this favor range
     */
    public boolean isInRange(final IFavor f) {
        return f.isEnabled() && isInRange(f.getFavor(getDeity()).getLevel());
    }

    /**
     * @param favorLevel the player's favor level
     * @return true if the favor is within this favor range
     */
    public boolean isInRange(final int favorLevel) {
        if (this == EMPTY) {
            return false;
        }
        if (maxLevel > minLevel) {
            return favorLevel <= maxLevel && favorLevel >= minLevel;
        } else {
            return favorLevel <= minLevel && favorLevel >= maxLevel;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FavorRange that = (FavorRange) o;
        return minLevel == that.minLevel && maxLevel == that.maxLevel && deity.equals(that.deity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deity, minLevel, maxLevel);
    }

    @Override
    public String toString() {
        return "Favor Range: " + deity.toString() + " [" + minLevel + "," + maxLevel + "]";
    }
}
