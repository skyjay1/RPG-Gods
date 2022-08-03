package rpggods.deity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import rpggods.perk.PerkCondition;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class Sacrifice {

    public static final Sacrifice EMPTY = new Sacrifice(new ResourceLocation("null"), 0, 0, 0, 0,
            List.of(), Optional.empty(), Optional.empty());

    public static final Codec<Sacrifice> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.optionalFieldOf("entity", new ResourceLocation("null")).forGetter(Sacrifice::getEntity),
            Codec.INT.optionalFieldOf("favor", 0).forGetter(Sacrifice::getFavor),
            Codec.INT.optionalFieldOf("maxuses", 16).forGetter(Sacrifice::getMaxUses),
            Codec.INT.optionalFieldOf("restocks", -1).forGetter(Sacrifice::getRestocks),
            Codec.INT.optionalFieldOf("cooldown", 12000).forGetter(Sacrifice::getCooldown),
            Codec.either(PerkCondition.CODEC, PerkCondition.CODEC.listOf())
                    .xmap(either -> either.map(ImmutableList::of, Function.identity()),
                            list -> list.size() == 1 ? Either.left(list.get(0)) : Either.right(list))
                    .optionalFieldOf("condition", Lists.newArrayList()).forGetter(Sacrifice::getConditions),
            ResourceLocation.CODEC.optionalFieldOf("function").forGetter(Sacrifice::getFunction),
            Codec.STRING.optionalFieldOf("function_text").forGetter(Sacrifice::getFunctionText)
    ).apply(instance, Sacrifice::new));

    private final ResourceLocation entity;
    private final int favor;
    private final int maxUses;
    private final int restocks;
    private final int cooldown;
    private final List<PerkCondition> conditions;
    private final Optional<ResourceLocation> function;
    private final Optional<String> functionText;

    public Sacrifice(ResourceLocation entity, int favor, int maxUses, int restocks, int cooldown,
                     List<PerkCondition> conditions,
                     Optional<ResourceLocation> function, Optional<String> functionText) {
        this.entity = entity;
        this.favor = favor;
        this.maxUses = maxUses;
        this.restocks = restocks;
        this.cooldown = cooldown;
        this.conditions = conditions;
        this.function = function;
        this.functionText = functionText;
    }

    /**
     * Attempts to parse the deity from the given sacrifice id
     * @param sacrificeId the offering id in the form {@code namespace:deity/sacrificename}
     * @return the resource location if found, otherwise {@link DeityHelper#EMPTY}
     */
    public static ResourceLocation getDeity(final ResourceLocation sacrificeId) {
        String path = sacrificeId.getPath();
        int index = path.indexOf("/");
        if(index > -1) {
            return new ResourceLocation(sacrificeId.getNamespace(), path.substring(0, index));
        }
        return DeityHelper.EMPTY.id;
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

    public int getRestocks() {
        return restocks;
    }

    public int getCooldown() {
        return cooldown;
    }

    public List<PerkCondition> getConditions() {
        return conditions;
    }

    public Optional<ResourceLocation> getFunction() {
        return function;
    }

    public Optional<String> getFunctionText() {
        return functionText;
    }

    public Cooldown createCooldown() {
        return new Cooldown(this.maxUses, this.cooldown, this.restocks);
    }
}
