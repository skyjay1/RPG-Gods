package rpggods.perk;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.ResourceLocation;
import rpggods.favor.FavorRange;

import javax.swing.text.html.Option;
import java.util.Optional;
import java.util.function.Function;

public class PerkData {

    public static final PerkData EMPTY = new PerkData(PerkData.Type.FAVOR, Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());

    public static final Codec<PerkData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PerkData.Type.CODEC.fieldOf("type").forGetter(PerkData::getType),
            Codec.STRING.optionalFieldOf("data").forGetter(PerkData::getString),
            ResourceLocation.CODEC.optionalFieldOf("id").forGetter(PerkData::getId),
            CompoundNBT.CODEC.optionalFieldOf("tag").forGetter(PerkData::getTag),
            ItemStack.CODEC.optionalFieldOf("item").forGetter(PerkData::getItem),
            Codec.LONG.optionalFieldOf("favor").forGetter(PerkData::getFavor),
            Codec.FLOAT.optionalFieldOf("multiplier").forGetter(PerkData::getMultiplier)
    ).apply(instance, PerkData::new));

    private final PerkData.Type type;
    private final Optional<String> string;
    private final Optional<ResourceLocation> id;
    private final Optional<CompoundNBT> tag;
    private final Optional<ItemStack> item;
    private final Optional<Long> favor;
    private final Optional<Float> multiplier;

    public PerkData(Type type, Optional<String> string, Optional<ResourceLocation> id, Optional<CompoundNBT> tag,
                    Optional<ItemStack> item, Optional<Long> favor, Optional<Float> multiplier) {
        this.type = type;
        this.string = string;
        this.id = id;
        this.tag = tag;
        this.item = item;
        this.favor = favor;
        this.multiplier = multiplier;
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

    public static enum Type implements IStringSerializable {
        FUNCTION("function"),
        POTION("potion"),
        SUMMON("summon"),
        ITEM("item"),
        FAVOR("favor"),
        AFFINITY("affinity"),
        ARROW_DAMAGE("arrow_damage"),
        ARROW_COUNT("arrow_count"),
        OFFSPRING("offspring"),
        CROP_GROWTH("crop_growth"),
        CROP_HARVEST("crop_harvest"),
        AUTOSMELT("autosmelt"),
        UNSMELT("unsmelt"),
        EMBARGO("embargo"),
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

        @Override
        public String getString() {
            return name;
        }
    }
}
