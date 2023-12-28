package rpggods.data.perk;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import rpggods.data.favor.FavorLevel;
import rpggods.data.favor.FavorRange;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class Perk {

    public static final Perk EMPTY = new Perk(PerkIcon.EMPTY, List.of(),
            FavorRange.EMPTY, List.of(), 0.0F, "null", 1000L, Optional.empty());

    public static final Codec<Perk> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PerkIcon.CODEC.optionalFieldOf("icon", PerkIcon.EMPTY).forGetter(Perk::getIcon),
            Codec.either(PerkCondition.CODEC, PerkCondition.CODEC.listOf())
                    .xmap(either -> either.map(ImmutableList::of, Function.identity()),
                            list -> list.size() == 1 ? Either.left(list.get(0)) : Either.right(list))
                    .optionalFieldOf("condition", List.of()).forGetter(Perk::getConditions),
            FavorRange.CODEC.optionalFieldOf("range", FavorRange.EMPTY).forGetter(Perk::getRange),
            Codec.either(PerkAction.CODEC, PerkAction.CODEC.listOf())
                    .xmap(either -> either.map(ImmutableList::of, Function.identity()),
                            list -> list.size() == 1 ? Either.left(list.get(0)) : Either.right(list))
                    .optionalFieldOf("action", List.of()).forGetter(Perk::getActions),
            Codec.FLOAT.optionalFieldOf("chance", 1.0F).forGetter(Perk::getChance),
            Codec.STRING.optionalFieldOf("cooldown_category", "").forGetter(Perk::getCategory),
            Codec.LONG.optionalFieldOf("cooldown", 600L).forGetter(Perk::getCooldown),
            Codec.BOOL.optionalFieldOf("positive").forGetter(Perk::getPositiveFlag)
    ).apply(instance, Perk::new));

    private final PerkIcon icon;
    private final List<PerkCondition> conditions;
    private final FavorRange range;
    private final List<PerkAction> actions;
    private final float chance;
    private final String category;
    private final long cooldown;
    private Optional<Boolean> positiveFlag;
    private boolean isPositive;

    public Perk(PerkIcon icon, List<PerkCondition> conditions, FavorRange range,
                List<PerkAction> actions, float chance, String category, long cooldown,
                Optional<Boolean> positiveFlag) {
        this.icon = icon;
        this.conditions = conditions;
        this.range = range;
        this.actions = actions;
        this.chance = chance;
        this.cooldown = cooldown;
        this.positiveFlag = positiveFlag;
        this.isPositive = positiveFlag.orElse(range.getMinLevel() >= 0);
        // determine category if not provided
        if(null == category || category.isEmpty()) {
            category = actions.isEmpty() ? "wtf u broke it" : actions.get(0).getType().getSerializedName();
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

    /**
     * @param level the favor level to check
     * @return the percent chance to run this perk with added bonus, if any
     */
    public float getAdjustedChance(FavorLevel level) {
        return getChance() + level.getPerkBonus();
    }

    public String getCategory() {
        return category;
    }

    public long getCooldown() {
        return cooldown;
    }

    public Optional<Boolean> getPositiveFlag() {
        return positiveFlag;
    }

    public boolean isPositive() {
        return isPositive;
    }

    public List<PerkAction> getActions() {
        return actions;
    }

    public ResourceLocation getDeity() {
        return getRange().getDeity();
    }

    @Override
    public String toString() {
        return "Perk{" +
                "icon=" + icon.getItem().getItem().toString() +
                ", conditions=" + conditions +
                ", range=" + range +
                ", actions=" + actions +
                ", chance=" + chance +
                ", cooldown_category='" + category + '\'' +
                ", cooldown=" + cooldown +
                '}';
    }
}
