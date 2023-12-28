package rpggods.data.deity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;

public class Deity {

    public static final Deity EMPTY = new Deity(new ResourceLocation("null"), ItemStack.EMPTY, false, false, 0, 0);

    private static final Codec<ItemStack> ITEM_OR_STACK_CODEC = Offering.ITEM_OR_STACK_CODEC;

    public static final Codec<Deity> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("name").forGetter(Deity::getId),
            ITEM_OR_STACK_CODEC.optionalFieldOf("icon", ItemStack.EMPTY).forGetter(Deity::getIcon),
            Codec.BOOL.optionalFieldOf("unlocked", true).forGetter(Deity::isUnlocked),
            Codec.BOOL.optionalFieldOf("enabled", true).forGetter(Deity::isEnabled),
            Codec.INT.optionalFieldOf("minlevel", -10).forGetter(Deity::getMinLevel),
            Codec.INT.optionalFieldOf("maxlevel", 10).forGetter(Deity::getMaxLevel)
    ).apply(instance, Deity::new));

    /** The deity ID, must be unique **/
    private final ResourceLocation id;
    /** The ItemStack icon in the favor GUI **/
    private final ItemStack icon;
    /** True if the deity is unlocked **/
    private final boolean unlocked;
    /** True if the deity is enabled **/
    private final boolean enabled;
    /** The minimum favor level **/
    private final int minLevel;
    /** The maximum favor level **/
    private final int maxLevel;

    public Deity(ResourceLocation id, ItemStack icon, boolean unlocked, boolean enabled, int minLevel, int maxLevel) {
        this.id = id;
        this.icon = icon;
        this.unlocked = unlocked;
        this.enabled = enabled;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
    }

    /** @return The deity ID, must be unique **/
    public ResourceLocation getId() {
        return id;
    }

    /** @return The ItemStack icon of the deity in the favor GUI **/
    public ItemStack getIcon() {
        return icon;
    }

    /** @return True if the deity is unlocked **/
    public boolean isUnlocked() {
        return unlocked;
    }

    /** @return True if the deity is enabled **/
    public boolean isEnabled() {
        return enabled;
    }

    /** @return The minimum favor level for this deity **/
    public int getMinLevel() {
        return minLevel;
    }

    /** @return The maximum favor level for this deity **/
    public int getMaxLevel() {
        return maxLevel;
    }

    @Override
    public String toString() {
        return "Deity{" +
                "id=" + id +
                ", unlocked=" + unlocked +
                ", enabled=" + enabled +
                ", minLevel=" + minLevel +
                ", maxLevel=" + maxLevel +
                '}';
    }
}
