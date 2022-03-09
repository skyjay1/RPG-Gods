package rpggods.deity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ResourceLocation;

import java.util.Optional;

public class Sacrifice {

    public static final Sacrifice EMPTY = new Sacrifice(new ResourceLocation("null"),
            new ResourceLocation("null"), 0, Optional.empty());

    public static final Codec<Sacrifice> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("deity").forGetter(Sacrifice::getDeity),
            ResourceLocation.CODEC.fieldOf("entity").forGetter(Sacrifice::getEntity),
            Codec.INT.fieldOf("favor").forGetter(Sacrifice::getFavor),
            ResourceLocation.CODEC.optionalFieldOf("function").forGetter(Sacrifice::getFunction)
    ).apply(instance, Sacrifice::new));

    private final ResourceLocation deity;
    private final ResourceLocation entity;
    private final int favor;
    private final Optional<ResourceLocation> function;

    public Sacrifice(ResourceLocation deity, ResourceLocation entity, int favor, Optional<ResourceLocation> function) {
        this.deity = deity;
        this.entity = entity;
        this.favor = favor;
        this.function = function;
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

    public Optional<ResourceLocation> getFunction() {
        return function;
    }
}
