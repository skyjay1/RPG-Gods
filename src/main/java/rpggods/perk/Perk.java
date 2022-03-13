package rpggods.perk;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ResourceLocation;
import rpggods.favor.FavorRange;

import java.util.List;
import java.util.function.Function;

public class Perk {

    public static final Perk EMPTY = new Perk(PerkIcon.EMPTY, Lists.newArrayList(),
            FavorRange.EMPTY, Lists.newArrayList(), 0.0F, "null", 1000L);

    public static final Codec<Perk> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PerkIcon.CODEC.optionalFieldOf("icon", PerkIcon.EMPTY).forGetter(Perk::getIcon),
            Codec.either(PerkCondition.CODEC, PerkCondition.CODEC.listOf())
                    .xmap(either -> either.map(ImmutableList::of, Function.identity()),
                            list -> list.size() == 1 ? Either.left(list.get(0)) : Either.right(list))
                    .optionalFieldOf("condition", Lists.newArrayList()).forGetter(Perk::getConditions),
            FavorRange.CODEC.fieldOf("range").forGetter(Perk::getRange),
            Codec.either(PerkData.CODEC, PerkData.CODEC.listOf())
                    .xmap(either -> either.map(ImmutableList::of, Function.identity()),
                            list -> list.size() == 1 ? Either.left(list.get(0)) : Either.right(list))
                    .optionalFieldOf("action", Lists.newArrayList()).forGetter(Perk::getActions),
            Codec.FLOAT.optionalFieldOf("chance", 1.0F).forGetter(Perk::getChance),
            Codec.STRING.optionalFieldOf("cooldown_category", "").forGetter(Perk::getCategory),
            Codec.LONG.optionalFieldOf("cooldown", 600L).forGetter(Perk::getCooldown)
    ).apply(instance, Perk::new));

    private final PerkIcon icon;
    private final List<PerkCondition> conditions;
    private final FavorRange range;
    private final List<PerkData> actions;
    private final float chance;
    private final String category;
    private final long cooldown;

    public Perk(PerkIcon icon, List<PerkCondition> conditions, FavorRange range,
                List<PerkData> actions, float chance, String category, long cooldown) {
        this.icon = icon;
        this.conditions = conditions;
        this.range = range;
        this.actions = actions;
        this.chance = chance;
        this.cooldown = cooldown;
        // determine category if not provided
        if(null == category || category.isEmpty()) {
            category = actions.isEmpty() ? "wtf u broke it" : actions.get(0).getType().getString();
        }
        this.category = category;
    }

    public PerkIcon getIcon() {
        return icon;
    }

    public List<PerkCondition> getConditions() {
        return conditions;
    }

    public FavorRange getRange() {
        return range;
    }

    public float getChance() {
        return chance;
    }

    public String getCategory() {
        return category;
    }

    public long getCooldown() {
        return cooldown;
    }

    public List<PerkData> getActions() {
        return actions;
    }

    public ResourceLocation getDeity() {
        return getRange().getDeity();
    }

    @Override
    public String toString() {
        return "Perk{" +
                "icon=" + icon.getItem().getItem().getRegistryName() +
                ", conditions=" + conditions +
                ", range=" + range +
                ", actions=" + actions +
                ", chance=" + chance +
                ", cooldown_category='" + category + '\'' +
                ", cooldown=" + cooldown +
                '}';
    }
}
