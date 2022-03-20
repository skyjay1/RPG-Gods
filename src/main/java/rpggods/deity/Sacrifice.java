package rpggods.deity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ResourceLocation;
import rpggods.RPGGods;
import rpggods.util.Cooldown;

import java.util.Optional;

public class Sacrifice {

    public static final Sacrifice EMPTY = new Sacrifice(new ResourceLocation("null"), 0, 0, 0,
            Optional.empty(), Optional.empty());

    public static final Codec<Sacrifice> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.optionalFieldOf("entity", new ResourceLocation("null")).forGetter(Sacrifice::getEntity),
            Codec.INT.optionalFieldOf("favor", 0).forGetter(Sacrifice::getFavor),
            Codec.INT.optionalFieldOf("maxuses", 16).forGetter(Sacrifice::getMaxUses),
            Codec.INT.optionalFieldOf("cooldown", 12000).forGetter(Sacrifice::getCooldown),
            ResourceLocation.CODEC.optionalFieldOf("function").forGetter(Sacrifice::getFunction),
            Codec.STRING.optionalFieldOf("function_text").forGetter(Sacrifice::getFunctionText)
    ).apply(instance, Sacrifice::new));

    private final ResourceLocation entity;
    private final int favor;
    private final Optional<ResourceLocation> function;
    private final Optional<String> functionText;
    private final int maxUses;
    private final int cooldown;

    public Sacrifice(ResourceLocation entity, int favor, int maxUses, int cooldown,
                     Optional<ResourceLocation> function, Optional<String> functionText) {
        this.entity = entity;
        this.favor = favor;
        this.maxUses = maxUses;
        this.cooldown = cooldown;
        this.function = function;
        this.functionText = functionText;
    }

    /**
     * Attempts to parse the deity from the given sacrifice id
     * @param sacrificeId the offering id in the form {@code namespace:deity/sacrificename}
     * @return the resource location if found, otherwise {@link Deity#EMPTY}
     */
    public static ResourceLocation getDeity(final ResourceLocation sacrificeId) {
        String path = sacrificeId.getPath();
        int index = path.indexOf("/");
        if(index > -1) {
            return new ResourceLocation(sacrificeId.getNamespace(), path.substring(0, index));
        }
        return Deity.EMPTY.id;
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
