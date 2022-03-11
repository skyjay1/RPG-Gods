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

    public static final Perk EMPTY = new Perk(PerkIcon.EMPTY, Optional.of(PerkTrigger.EMPTY), Lists.newArrayList(), FavorRange.EMPTY,
            0.0F, "null", 1000L, Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty());

    public static final Codec<Perk> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PerkIcon.CODEC.fieldOf("icon").forGetter(Perk::getIcon),
            PerkTrigger.CODEC.optionalFieldOf("trigger").forGetter(Perk::getTrigger),
            Codec.either(PerkCondition.CODEC, PerkCondition.CODEC.listOf())
                    .xmap(either -> either.map(ImmutableList::of, Function.identity()),
                            list -> list.size() == 1 ? Either.left(list.get(0)) : Either.right(list))
                    .optionalFieldOf("condition", Lists.newArrayList()).forGetter(Perk::getConditions),
            FavorRange.CODEC.fieldOf("range").forGetter(Perk::getRange),
            Codec.FLOAT.optionalFieldOf("chance", 1.0F).forGetter(Perk::getChance),
            Codec.STRING.optionalFieldOf("cooldown_category", "").forGetter(Perk::getCategory),
            Codec.LONG.optionalFieldOf("cooldown", 600L).forGetter(Perk::getCooldown),
            ResourceLocation.CODEC.optionalFieldOf("function").forGetter(Perk::getFunction),
            CompoundNBT.CODEC.optionalFieldOf("potion").forGetter(Perk::getPotion),
            CompoundNBT.CODEC.optionalFieldOf("summon").forGetter(Perk::getSummon),
            ItemStack.CODEC.optionalFieldOf("item").forGetter(Perk::getItem),
            Codec.LONG.optionalFieldOf("favor").forGetter(Perk::getFavor)
    ).apply(instance, Perk::new));

    private final PerkIcon icon;
    private final Optional<PerkTrigger> trigger;
    private final List<PerkCondition> conditions;
    private final FavorRange range;
    private final float chance;
    private final String category;
    private final long cooldown;

    private final Optional<ResourceLocation> function;
    private final Optional<CompoundNBT> potion;
    private final Optional<CompoundNBT> summon;
    private final Optional<ItemStack> item;
    private final Optional<Long> favor;

    public Perk(PerkIcon icon, Optional<PerkTrigger> trigger, List<PerkCondition> conditions, FavorRange range, float chance,
                String category, long cooldown, Optional<ResourceLocation> function, Optional<CompoundNBT> potion,
                Optional<CompoundNBT> summon, Optional<ItemStack> item, Optional<Long> favor) {
        this.icon = icon;
        this.trigger = trigger;
        this.conditions = conditions;
        this.range = range;
        this.chance = chance;
        this.cooldown = cooldown;
        this.function = function;
        this.potion = potion;
        this.summon = summon;
        this.item = item;
        this.favor = favor;
        // determine category if not provided
        if(null == category || category.isEmpty()) {
            if(function.isPresent()) category = "function";
            else if(potion.isPresent()) category = "potion";
            else if(summon.isPresent()) category = "summon";
            else if(item.isPresent()) category = "item";
            else category = "perk";
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

    public Optional<ResourceLocation> getFunction() {
        return function;
    }

    public Optional<CompoundNBT> getPotion() {
        return potion;
    }

    public Optional<CompoundNBT> getSummon() {
        return summon;
    }

    public Optional<ItemStack> getItem() {
        return item;
    }

    public Optional<Long> getFavor() {
        return favor;
    }

    public ResourceLocation getDeity() {
        return getRange().getDeity();
    }
}
