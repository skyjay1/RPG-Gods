package rpggods.perk;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import rpggods.favor.FavorRange;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class Perk {

    public static final Perk EMPTY = new Perk(PerkIcon.EMPTY, Optional.of(PerkTrigger.EMPTY), Lists.newArrayList(),
            FavorRange.EMPTY, PerkData.EMPTY, 0.0F, "null", 1000L);

    public static final Codec<Perk> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PerkIcon.CODEC.fieldOf("icon").forGetter(Perk::getIcon),
            PerkTrigger.CODEC.optionalFieldOf("trigger").forGetter(Perk::getTrigger),
            Codec.either(PerkCondition.CODEC, PerkCondition.CODEC.listOf())
                    .xmap(either -> either.map(ImmutableList::of, Function.identity()),
                            list -> list.size() == 1 ? Either.left(list.get(0)) : Either.right(list))
                    .optionalFieldOf("condition", Lists.newArrayList()).forGetter(Perk::getConditions),
            FavorRange.CODEC.fieldOf("range").forGetter(Perk::getRange),
            PerkData.CODEC.fieldOf("action").forGetter(Perk::getData),
            Codec.FLOAT.optionalFieldOf("chance", 1.0F).forGetter(Perk::getChance),
            Codec.STRING.optionalFieldOf("cooldown_category", "").forGetter(Perk::getCategory),
            Codec.LONG.optionalFieldOf("cooldown", 600L).forGetter(Perk::getCooldown)
    ).apply(instance, Perk::new));

    private final PerkIcon icon;
    private final Optional<PerkTrigger> trigger;
    private final List<PerkCondition> conditions;
    private final FavorRange range;
    private final PerkData data;
    private final float chance;
    private final String category;
    private final long cooldown;

    public Perk(PerkIcon icon, Optional<PerkTrigger> trigger, List<PerkCondition> conditions, FavorRange range,
                PerkData data, float chance, String category, long cooldown) {
        this.icon = icon;
        this.trigger = trigger;
        this.conditions = conditions;
        this.range = range;
        this.data = data;
        this.chance = chance;
        this.cooldown = cooldown;
        // determine category if not provided
        if(null == category || category.isEmpty()) {
            category = data.getType().getString();
        }
        this.category = category;
    }

    public PerkIcon getIcon() {
        return icon;
    }

    public Optional<PerkTrigger> getTrigger() {
        return trigger;
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

    public PerkData getData() {
        return data;
    }

    public ResourceLocation getDeity() {
        return getRange().getDeity();
    }
}
