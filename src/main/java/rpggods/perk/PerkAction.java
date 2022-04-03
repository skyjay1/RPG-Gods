package rpggods.perk;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.EntityType;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import rpggods.deity.Deity;
import rpggods.event.FavorEventHandler;

import java.util.Optional;

public class PerkAction {

    public static final PerkAction EMPTY = new PerkAction(PerkAction.Type.FAVOR, Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), true);

    public static final Codec<PerkAction> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PerkAction.Type.CODEC.fieldOf("type").forGetter(PerkAction::getType),
            Codec.STRING.optionalFieldOf("data").forGetter(PerkAction::getString),
            ResourceLocation.CODEC.optionalFieldOf("id").forGetter(PerkAction::getId),
            CompoundNBT.CODEC.optionalFieldOf("tag").forGetter(PerkAction::getTag),
            ItemStack.CODEC.optionalFieldOf("item").forGetter(PerkAction::getItem),
            Codec.LONG.optionalFieldOf("favor").forGetter(PerkAction::getFavor),
            Codec.FLOAT.optionalFieldOf("multiplier").forGetter(PerkAction::getMultiplier),
            Affinity.CODEC.optionalFieldOf("affinity").forGetter(PerkAction::getAffinity),
            Codec.BOOL.optionalFieldOf("hidden", false).forGetter(PerkAction::isHidden)
    ).apply(instance, PerkAction::new));

    private final PerkAction.Type type;
    private final Optional<String> string;
    private final Optional<ResourceLocation> id;
    private final Optional<CompoundNBT> tag;
    private final Optional<ItemStack> item;
    private final Optional<Long> favor;
    private final Optional<Float> multiplier;
    private final Optional<Affinity> affinity;
    private final boolean hidden;

    public PerkAction(Type type, Optional<String> string, Optional<ResourceLocation> id, Optional<CompoundNBT> tag,
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

    public ITextComponent getDisplayName() {
        return this.getType().getDisplayName();
    }

    public ITextComponent getDisplayDescription() {
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
                        return new TranslationTextComponent(effect.get().getDescriptionId())
                                .append(" ")
                                .append(new TranslationTextComponent(potencyKey));
                    }
                }
                return StringTextComponent.EMPTY;
            case SUMMON:
                if(tag.isPresent()) {
                    // format entity ID as name
                    String entity = tag.get().getString("id");
                    Optional<EntityType<?>> type = EntityType.byString(entity);
                    return type.isPresent() ? type.get().getDescription() : new StringTextComponent(entity);
                }
                return StringTextComponent.EMPTY;
            case ITEM:
                return getItem().orElse(ItemStack.EMPTY).getHoverName();
            case FAVOR:
                if(favor.isPresent()) {
                    // format favor as discrete amount
                    // EX: multiplier of -1.1 becomes -1, 0.6 becomes +1, 1.2 becomes +1, etc.
                    String prefix = (favor.get() > 0) ? "+" : "";
                    return new StringTextComponent(prefix + Math.round(getFavor().get()));
                }
                return StringTextComponent.EMPTY;
            case AFFINITY:
                if(getAffinity().isPresent()) {
                    return getAffinity().get().getDisplayDescription();
                }
                return StringTextComponent.EMPTY;
            case ARROW_COUNT:
            case SPECIAL_PRICE:
            case CROP_GROWTH:
                if(getMultiplier().isPresent()) {
                    // format multiplier as discrete bonus
                    // EX: multiplier of 0.0 becomes +0, 0.6 becomes +1, 1.2 becomes +1, etc.
                    String prefix = (getMultiplier().get() > 0) ? "+" : "";
                    return new StringTextComponent(prefix + Math.round(getMultiplier().get()));
                }
                return StringTextComponent.EMPTY;
            case DURABILITY:
                if(getMultiplier().isPresent() && getString().isPresent()) {
                    // format multiplier as percentage
                    // EX: multiplier of -0.9 becomes -90%, 0.0 becomes +0%, 0.5 becomes +50%, 1.2 becomes +120%, etc.
                    String prefix = getMultiplier().get() >= 0.0F ? "+" : "";
                    ITextComponent durability = new StringTextComponent(prefix + Math.round((getMultiplier().get()) * 100.0F) + "%");
                    ITextComponent slot = new TranslationTextComponent("equipment.type." + getString().get());
                    return new TranslationTextComponent("favor.perk.type.durability.description.full", durability, slot);
                }
                return StringTextComponent.EMPTY;
            case DAMAGE:
            case ARROW_DAMAGE:
            case CROP_HARVEST:
            case OFFSPRING:
            case XP:
                if(getMultiplier().isPresent()) {
                    // format multiplier as adjusted percentage
                    // EX: multiplier of 0.0 becomes -100%, 0.5 becomes -50%, 1.2 becomes +120%, etc.
                    String prefix = getMultiplier().get() >= 1.0F ? "+" : "";
                    return new StringTextComponent(prefix + Math.round((getMultiplier().get() - 1.0F) * 100.0F) + "%");
                }
                return StringTextComponent.EMPTY;
            case PATRON:
                if(getId().isPresent()) {
                    return Deity.getName(getId().get());
                }
                return StringTextComponent.EMPTY;
            case FUNCTION:
                if(getString().isPresent()) {
                    return new TranslationTextComponent(getString().get());
                }
                return new TranslationTextComponent("favor.perk.type.function.description.default");
            case AUTOSMELT: case UNSMELT: default:
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
        DURABILITY("durability"),
        DAMAGE("damage"),
        PATRON("patron"),
        XP("xp");

        private static final Codec<PerkAction.Type> CODEC = Codec.STRING.comapFlatMap(PerkAction.Type::fromString, PerkAction.Type::getSerializedName).stable();

        private final String name;

        private Type(final String id) {
            name = id;
        }

        public static DataResult<PerkAction.Type> fromString(String id) {
            for(final PerkAction.Type t : values()) {
                if(t.getSerializedName().equals(id)) {
                    return DataResult.success(t);
                }
            }
            return DataResult.error("Failed to parse perk data type '" + id + "'");
        }

        /**
         * @param data the data to pass to the translation key
         * @return Translation key for the description of this perk type, using the provided data
         */
        public ITextComponent getDisplayDescription(final ITextComponent data) {
            return new TranslationTextComponent("favor.perk.type." + getSerializedName() + ".description", data);
        }

        /**
         * @return Translation key for the name of this perk type
         */
        public IFormattableTextComponent getDisplayName() {
            return new TranslationTextComponent("favor.perk.type." + getSerializedName());
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
