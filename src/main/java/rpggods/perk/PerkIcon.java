package rpggods.perk;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3f;
import rpggods.favor.FavorRange;

import java.util.function.Function;

public class PerkIcon {

    public static final PerkIcon EMPTY = new PerkIcon(ItemStack.EMPTY, 0x000, true);

    public static final Codec<PerkIcon> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.CODEC.optionalFieldOf("item", ItemStack.EMPTY).forGetter(PerkIcon::getItem),
            Codec.INT.optionalFieldOf("color", 0x000).forGetter(PerkIcon::getColor),
            Codec.BOOL.optionalFieldOf("hidden", false).forGetter(PerkIcon::isHidden)
    ).apply(instance, PerkIcon::new));

    private final ItemStack item;
    private final int color;
    private final boolean hidden;
    private final float colorRed;
    private final float colorGreen;
    private final float colorBlue;

    public PerkIcon(ItemStack item, int color, boolean hidden) {
        this.item = item;
        this.color = color;
        this.hidden = hidden;
        // unpack color from int
        final Vector3f colorVec = unpackColor(color);
        this.colorRed = colorVec.x();
        this.colorGreen = colorVec.y();
        this.colorBlue = colorVec.z();
    }

    public ItemStack getItem() {
        return item;
    }

    public int getColor() {
        return color;
    }

    public boolean isHidden() {
        return hidden;
    }

    public float getColorRed() {
        return colorRed;
    }

    public float getColorGreen() {
        return colorGreen;
    }

    public float getColorBlue() {
        return colorBlue;
    }

    /**
     * Separates a hex color into RGB components.
     * @param color a packed int RGB color
     * @return the red, green, and blue components as a Vector3f
     **/
    public static Vector3f unpackColor(final int color) {
        long tmpColor = color;
        if ((tmpColor & -67108864) == 0) {
            tmpColor |= -16777216;
        }
        float colorRed = (float) (tmpColor >> 16 & 255) / 255.0F;
        float colorGreen = (float) (tmpColor >> 8 & 255) / 255.0F;
        float colorBlue = (float) (tmpColor & 255) / 255.0F;
        return new Vector3f(colorRed, colorGreen, colorBlue);
    }
}
