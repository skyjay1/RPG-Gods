package rpggods.deity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;
import rpggods.altar.AltarItems;
import rpggods.altar.AltarPose;

import java.util.Optional;

/**
 * This class contains information about a deity.
 *
 * @author skyjay1
 **/
public class Altar {

    public static final ResourceLocation MATERIAL = new ResourceLocation("stone");

    public static final Altar EMPTY = new Altar(false, Optional.empty(), false, false,
            AltarItems.EMPTY, Blocks.STONE_SLAB.getDefaultState(), true, MATERIAL, new AltarPose(), true);

    public static final Codec<Altar> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("enabled", true).forGetter(Altar::isEnabled),
            Codec.STRING.optionalFieldOf("name").forGetter(Altar::getName),
            Codec.BOOL.optionalFieldOf("female", false).forGetter(Altar::isFemale),
            Codec.BOOL.optionalFieldOf("slim", false).forGetter(Altar::isSlim),
            AltarItems.CODEC.optionalFieldOf("items", AltarItems.EMPTY).forGetter(Altar::getItems),
            BlockState.CODEC.optionalFieldOf("block", Blocks.STONE_SLAB.getDefaultState()).forGetter(Altar::getBlock),
            Codec.BOOL.optionalFieldOf("block_locked", true).forGetter(Altar::isBlockLocked),
            ResourceLocation.CODEC.optionalFieldOf("material", MATERIAL).forGetter(Altar::getMaterial),
            AltarPose.CODEC.optionalFieldOf("pose", new AltarPose()).forGetter(Altar::getPose),
            Codec.BOOL.optionalFieldOf("pose_locked", true).forGetter(Altar::isPoseLocked)
    ).apply(instance, Altar::new));

    private final boolean enabled;
    private final Optional<String> name;
    private final boolean female;
    private final boolean slim;
    private final AltarItems items;
    private final BlockState block;
    private final boolean blockLocked;
    private final ResourceLocation material;
    private final AltarPose pose;
    private final boolean poseLocked;

    private final Optional<ResourceLocation> deity;

    public Altar(boolean enabled, Optional<String> name, boolean female, boolean slim, AltarItems items,
                 BlockState block, boolean blockLocked, ResourceLocation material,
                 AltarPose pose, boolean poseLocked) {
        this.enabled = enabled;
        this.name = name;
        this.female = female;
        this.slim = slim;
        this.items = items;
        this.block = block;
        this.blockLocked = blockLocked;
        this.material = material;
        this.pose = pose;
        this.poseLocked = poseLocked;

        this.deity = Optional.ofNullable(ResourceLocation.tryCreate(name.orElse("")));
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Optional<ResourceLocation> getDeity() {
        return deity;
    }

    public Optional<String> getName() { return name; }

    public boolean isFemale() {
        return female;
    }

    public boolean isSlim() {
        return slim;
    }

    public AltarItems getItems() {
        return items;
    }



    public BlockState getBlock() {
        return block;
    }

    public boolean isBlockLocked() {
        return blockLocked;
    }

    public ResourceLocation getMaterial() {
        return material;
    }

    public AltarPose getPose() {
        return pose;
    }

    public boolean isPoseLocked() {
        return poseLocked;
    }

    @Override
    public String toString() {
        final StringBuilder b = new StringBuilder("Deity:");
        b.append(" enabled[").append(enabled).append("]");
        b.append(" items[").append(items.toString()).append("]");
        b.append(" block[").append(block.toString()).append("]");
        b.append(" block_locked[").append(blockLocked).append("]");
        b.append(" female[").append(female).append("]");
        b.append(" slim[").append(slim).append("]");
        b.append(" pose_locked[").append(poseLocked).append("]");
        return b.toString();
    }

    public static String createTranslationKey(final ResourceLocation deity) {
        return "deity." + deity.getNamespace() + "." + deity.getPath();
    }
}
