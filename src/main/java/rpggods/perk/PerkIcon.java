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

import java.util.function.Function;

public class PerkIcon {

    public static final PerkIcon EMPTY = new PerkIcon(ItemStack.EMPTY, 0x000);

    public static final Codec<PerkIcon> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.CODEC.fieldOf("item").forGetter(PerkIcon::getItem),
            Codec.INT.optionalFieldOf("color", 0x000).forGetter(PerkIcon::getColor)
    ).apply(instance, PerkIcon::new));

    private final ItemStack item;
    private final int color;

    public PerkIcon(ItemStack item, int color) {
        this.item = item;
        this.color = color;
    }

    public ItemStack getItem() {
        return item;
    }

    public int getColor() {
        return color;
    }
}
