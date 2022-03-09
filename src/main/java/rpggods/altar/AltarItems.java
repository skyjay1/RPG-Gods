package rpggods.altar;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

import java.util.List;


public class AltarItems {

    public static final AltarItems EMPTY = new AltarItems(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
            ItemStack.EMPTY, ItemStack.EMPTY, false, false);

    public static final Codec<AltarItems> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.CODEC.optionalFieldOf("head", ItemStack.EMPTY).forGetter(o -> o.getItemStackFromSlot(EquipmentSlotType.HEAD)),
            ItemStack.CODEC.optionalFieldOf("chest", ItemStack.EMPTY).forGetter(o -> o.getItemStackFromSlot(EquipmentSlotType.CHEST)),
            ItemStack.CODEC.optionalFieldOf("legs", ItemStack.EMPTY).forGetter(o -> o.getItemStackFromSlot(EquipmentSlotType.LEGS)),
            ItemStack.CODEC.optionalFieldOf("feet", ItemStack.EMPTY).forGetter(o -> o.getItemStackFromSlot(EquipmentSlotType.FEET)),
            ItemStack.CODEC.optionalFieldOf("mainhand", ItemStack.EMPTY).forGetter(o -> o.getItemStackFromSlot(EquipmentSlotType.MAINHAND)),
            ItemStack.CODEC.optionalFieldOf("offhand", ItemStack.EMPTY).forGetter(o -> o.getItemStackFromSlot(EquipmentSlotType.OFFHAND)),
            Codec.BOOL.optionalFieldOf("armor_locked", true).forGetter(AltarItems::isArmorLocked),
            Codec.BOOL.optionalFieldOf("hands_locked", true).forGetter(AltarItems::isHandsLocked)
    ).apply(instance, AltarItems::new));

    private final ImmutableList<ItemStack> handItems;
    private final ImmutableList<ItemStack> armorItems;
    private final boolean armorLocked;
    private final boolean handsLocked;

    public AltarItems(ItemStack head, ItemStack chest, ItemStack legs, ItemStack feet,
                      ItemStack mainhand, ItemStack offhand,
                      boolean armorLocked, boolean handsLocked) {
        this.handItems = ImmutableList.of(mainhand, offhand);
        this.armorItems = ImmutableList.of(head, chest, legs, feet);
        this.armorLocked = armorLocked;
        this.handsLocked = handsLocked;
    }

    public ItemStack getItemStackFromSlot(EquipmentSlotType slotIn) {
        switch(slotIn.getSlotType()) {
            case HAND:
                return handItems.get(slotIn.getIndex());
            case ARMOR:
                return armorItems.get(slotIn.getIndex());
        }
        return ItemStack.EMPTY;
    }

    public List<ItemStack> getHandItems() {
        return handItems;
    }

    public List<ItemStack> getArmorItems() {
        return armorItems;
    }

    public boolean isArmorLocked() {
        return armorLocked;
    }

    public boolean isHandsLocked() {
        return handsLocked;
    }

    @Override
    public String toString() {
        final StringBuilder b = new StringBuilder("AltarItems:");
        b.append(" hands[").append(handItems.toString()).append("]");
        b.append(" armor[").append(armorItems.toString()).append("]");
        b.append(" armor_locked[").append(armorLocked).append("]");
        b.append(" hands_locked[").append(handsLocked).append("]");
        return b.toString();
    }
}
