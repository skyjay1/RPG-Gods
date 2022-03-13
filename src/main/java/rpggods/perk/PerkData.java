package rpggods.perk;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import rpggods.event.FavorEventHandler;

import java.util.Optional;

public class PerkData {

    public static final PerkData EMPTY = new PerkData(PerkData.Type.FAVOR, Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), true);

    public static final Codec<PerkData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PerkData.Type.CODEC.fieldOf("type").forGetter(PerkData::getType),
            Codec.STRING.optionalFieldOf("data").forGetter(PerkData::getString),
            ResourceLocation.CODEC.optionalFieldOf("id").forGetter(PerkData::getId),
            CompoundNBT.CODEC.optionalFieldOf("tag").forGetter(PerkData::getTag),
            ItemStack.CODEC.optionalFieldOf("item").forGetter(PerkData::getItem),
            Codec.LONG.optionalFieldOf("favor").forGetter(PerkData::getFavor),
            Codec.FLOAT.optionalFieldOf("multiplier").forGetter(PerkData::getMultiplier),
            Affinity.CODEC.optionalFieldOf("affinity").forGetter(PerkData::getAffinity),
            Codec.BOOL.optionalFieldOf("hidden", false).forGetter(PerkData::isHidden)
    ).apply(instance, PerkData::new));

    private final PerkData.Type type;
    private final Optional<String> string;
    private final Optional<ResourceLocation> id;
    private final Optional<CompoundNBT> tag;
    private final Optional<ItemStack> item;
    private final Optional<Long> favor;
    private final Optional<Float> multiplier;
    private final Optional<Affinity> affinity;
    private final boolean hidden;
    private IFormattableTextComponent descriptionTranslationKey;

    public PerkData(Type type, Optional<String> string, Optional<ResourceLocation> id, Optional<CompoundNBT> tag,
                    Optional<ItemStack> item, Optional<Long> favor, Optional<Float> multiplier,
                    Optional<Affinity> affinity, boolean hidden) {
        this.type = type;
        this.string = string;
        this.id = id;
        this.tag = tag;
        this.item = item;
        this.favor = favor;
        this.multiplier = multiplier;
        this.affinity = affinity;
        this.hidden = hidden;
    }

    public Type getType() {
        return type;
    }

    public Optional<ResourceLocation> getId() {
        return id;
    }

    public Optional<String> getString() {
        return string;
    }

    public Optional<CompoundNBT> getTag() {
        return tag;
    }

    public Optional<ItemStack> getItem() {
        return item;
    }

    public Optional<Long> getFavor() {
        return favor;
    }

    public Optional<Float> getMultiplier() {
        return multiplier;
    }

    public Optional<Affinity> getAffinity() {
        return affinity;
    }

    public boolean isHidden() {
        return hidden;
    }

    @Override
    public String toString() {
        return "PerkData{" +
                "type=" + type +
                ", string=" + string +
                ", id=" + id +
                ", tag=" + tag +
                ", item=" + item +
                ", favor=" + favor +
                ", multiplier=" + multiplier +
                '}';
    }

    public IFormattableTextComponent getDisplayName() {
        return this.getType().getDisplayName();
    }

    public IFormattableTextComponent getDisplayDescription() {
        return getType().getDisplayDescription(dataToDisplay());
    }

    private ITextComponent dataToDisplay() {
        switch (getType()) {
            case POTION:
            case ARROW_EFFECT:
                if(tag.isPresent()) {
                    // format potion ID as effect name (with amplifier)
                    Optional<EffectInstance> effect = FavorEventHandler.readEffectInstance(tag.get());
                    if(effect.isPresent()) {
                        String potencyKey = "potion.potency." + effect.get().getAmplifier();
                        return new TranslationTextComponent(effect.get().getEffectName())
                                .appendString(" ")
                                .appendSibling(new TranslationTextComponent(potencyKey));
                    }
                }
                return StringTextComponent.EMPTY;
            case SUMMON:
                if(tag.isPresent()) {
                    // format entity ID as name
                    String entity = tag.get().getString("id");
                    Optional<EntityType<?>> type = EntityType.byKey(entity);
                    return type.isPresent() ? type.get().getName() : new StringTextComponent(entity);
                }
                return StringTextComponent.EMPTY;
            case ITEM:
                return getItem().orElse(ItemStack.EMPTY).getDisplayName();
            case FAVOR:
                if(favor.isPresent()) {
                    // format favor as discrete amount
                    // EX: multiplier of -1.1 becomes -1, 0.6 becomes +1, 1.2 becomes +1, etc.
                    return new StringTextComponent((favor.get() >= 0 ? "+" : "") + Math.round(getFavor().get()));
                }
                return StringTextComponent.EMPTY;
            case AFFINITY:
                return getAffinity().isPresent() ? getAffinity().get().getDisplayName() : StringTextComponent.EMPTY;
            case ARROW_COUNT:
                if(getMultiplier().isPresent()) {
                    // format multiplier as discrete bonus
                    // EX: multiplier of 0.0 becomes +0, 0.6 becomes +1, 1.2 becomes +1, etc.
                    return new StringTextComponent("+" + Math.round(getMultiplier().get()));
                }
                return StringTextComponent.EMPTY;
            case CROP_GROWTH:
            case ARROW_DAMAGE:
            case CROP_HARVEST:
            case OFFSPRING:
            case SPECIAL_PRICE:
            case XP:
                if(getMultiplier().isPresent()) {
                    // format multiplier as percentage
                    // EX: multiplier of 0.0 becomes -100%, 0.5 becomes -50%, 1.2 becomes +120%, etc.
                    String prefix = getMultiplier().get() >= 0 ? "+" : "";
                    return new StringTextComponent(prefix + Math.round((getMultiplier().get() - 1.0F) * 100.0F) + "%");
                }
                return StringTextComponent.EMPTY;
            case FUNCTION: case AUTOSMELT: case UNSMELT: default:
                return StringTextComponent.EMPTY;
        }
    }

    public static enum Type implements IStringSerializable {
        FUNCTION("function"),
        POTION("potion"),
        SUMMON("summon"),
        ITEM("item"),
        FAVOR("favor"),
        AFFINITY("affinity"),
        ARROW_DAMAGE("arrow_damage"),
        ARROW_EFFECT("arrow_effect"),
        ARROW_COUNT("arrow_count"),
        OFFSPRING("offspring"),
        CROP_GROWTH("crop_growth"),
        CROP_HARVEST("crop_harvest"),
        AUTOSMELT("autosmelt"),
        UNSMELT("unsmelt"),
        SPECIAL_PRICE("special_price"),
        XP("xp");

        private static final Codec<PerkData.Type> CODEC = Codec.STRING.comapFlatMap(PerkData.Type::fromString, PerkData.Type::getString).stable();

        private final String name;

        private Type(final String id) {
            name = id;
        }

        public static DataResult<PerkData.Type> fromString(String id) {
            for(final PerkData.Type t : values()) {
                if(t.getString().equals(id)) {
                    return DataResult.success(t);
                }
            }
            return DataResult.error("Failed to parse perk data type '" + id + "'");
        }

        /**
         * @param data the data to pass to the translation key
         * @return Translation key for the description of this perk type, using the provided data
         */
        public IFormattableTextComponent getDisplayDescription(final ITextComponent data) {
            return new TranslationTextComponent("favor.perk.type." + getString() + ".description", data);
        }

        /**
         * @return Translation key for the name of this perk type
         */
        public IFormattableTextComponent getDisplayName() {
            return new TranslationTextComponent("favor.perk.type." + getString());
        }

        @Override
        public String getString() {
            return name;
        }
    }
}
