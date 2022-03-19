package rpggods.deity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ResourceLocation;
import rpggods.util.Cooldown;

import java.util.Optional;

public class Sacrifice {

    public static final Sacrifice EMPTY = new Sacrifice(new ResourceLocation("null"),
            new ResourceLocation("null"), 0, 0, 0,
            Optional.empty(), Optional.empty());

    public static final Codec<Sacrifice> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.optionalFieldOf("deity", new ResourceLocation("null")).forGetter(Sacrifice::getDeity),
            ResourceLocation.CODEC.optionalFieldOf("entity", new ResourceLocation("null")).forGetter(Sacrifice::getEntity),
            Codec.INT.optionalFieldOf("favor", 0).forGetter(Sacrifice::getFavor),
            Codec.INT.optionalFieldOf("maxuses", 16).forGetter(Sacrifice::getFavor),
            Codec.INT.optionalFieldOf("cooldown", 12000).forGetter(Sacrifice::getFavor),
            ResourceLocation.CODEC.optionalFieldOf("function").forGetter(Sacrifice::getFunction),
            Codec.STRING.optionalFieldOf("function_text").forGetter(Sacrifice::getFunctionText)
    ).apply(instance, Sacrifice::new));

    private final ResourceLocation deity;
    private final ResourceLocation entity;
    private final int favor;
    private final Optional<ResourceLocation> function;
    private final Optional<String> functionText;
    private final int maxUses;
    private final int cooldown;

    public Sacrifice(ResourceLocation deity, ResourceLocation entity, int favor, int maxUses, int cooldown,
                     Optional<ResourceLocation> function, Optional<String> functionText) {
        this.deity = deity;
        this.entity = entity;
        this.favor = favor;
        this.maxUses = maxUses;
        this.cooldown = cooldown;
        this.function = function;
        this.functionText = functionText;
    }

    public ResourceLocation getDeity() {
        return deity;
    }

    public ResourceLocation getEntity() {
        return entity;
    }

    public int getFavor() {
        return favor;
    }

    public int getMaxUses() {
        return maxUses;
    }

    public int getCooldown() {
        return cooldown;
    }

    public Optional<ResourceLocation> getFunction() {
        return function;
    }

    public Optional<String> getFunctionText() {
        return functionText;
    }

    public Cooldown createCooldown() {
        return new Cooldown(this.maxUses, this.cooldown);
    }
}
