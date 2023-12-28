package rpggods.util.altar;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Registry;
import net.minecraftforge.registries.ForgeRegistries;
import rpggods.data.deity.Offering;

public class AltarItems {

    public static final AltarItems EMPTY = new AltarItems(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
            ItemStack.EMPTY, ItemStack.EMPTY, Blocks.AIR, false, false, false);

    private static final Codec<ItemStack> ITEM_OR_STACK_CODEC = Offering.ITEM_OR_STACK_CODEC;

    public static final Codec<AltarItems> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ITEM_OR_STACK_CODEC.optionalFieldOf("head", ItemStack.EMPTY).forGetter(o -> o.getItemStackFromSlot(EquipmentSlot.HEAD)),
            ITEM_OR_STACK_CODEC.optionalFieldOf("chest", ItemStack.EMPTY).forGetter(o -> o.getItemStackFromSlot(EquipmentSlot.CHEST)),
            ITEM_OR_STACK_CODEC.optionalFieldOf("legs", ItemStack.EMPTY).forGetter(o -> o.getItemStackFromSlot(EquipmentSlot.LEGS)),
            ITEM_OR_STACK_CODEC.optionalFieldOf("feet", ItemStack.EMPTY).forGetter(o -> o.getItemStackFromSlot(EquipmentSlot.FEET)),
            ITEM_OR_STACK_CODEC.optionalFieldOf("mainhand", ItemStack.EMPTY).forGetter(o -> o.getItemStackFromSlot(EquipmentSlot.MAINHAND)),
            ITEM_OR_STACK_CODEC.optionalFieldOf("offhand", ItemStack.EMPTY).forGetter(o -> o.getItemStackFromSlot(EquipmentSlot.OFFHAND)),
            ForgeRegistries.BLOCKS.getCodec().optionalFieldOf("block", Blocks.AIR).forGetter(AltarItems::getBlock),
            Codec.BOOL.optionalFieldOf("armor_locked", false).forGetter(AltarItems::isArmorLocked),
            Codec.BOOL.optionalFieldOf("hands_locked", false).forGetter(AltarItems::isHandsLocked),
            Codec.BOOL.optionalFieldOf("block_locked", false).forGetter(AltarItems::isBlockLocked)
    ).apply(instance, AltarItems::new));

    private final ImmutableList<ItemStack> handItems;
    private final ImmutableList<ItemStack> armorItems;
    // TODO block state instead of block
    private final Block block;
    private final boolean armorLocked;
    private final boolean handsLocked;
    private final boolean blockLocked;

    public AltarItems(ItemStack head, ItemStack chest, ItemStack legs, ItemStack feet,
                      ItemStack mainhand, ItemStack offhand, Block block,
                      boolean armorLocked, boolean handsLocked, boolean blockLocked) {
        this.handItems = ImmutableList.of(mainhand, offhand);
        this.armorItems = ImmutableList.of(head, chest, legs, feet);
        this.block = block;
        this.armorLocked = armorLocked;
        this.handsLocked = handsLocked;
        this.blockLocked = blockLocked;
    }

    public ItemStack getItemStackFromSlot(EquipmentSlot slotIn) {
        switch(slotIn.getType()) {
            case HAND:
                return handItems.get(slotIn.getIndex());
            case ARMOR:
                return armorItems.get(slotIn.getIndex());
        }
        return ItemStack.EMPTY;
    }

    public Block getBlock() {
        return block;
    }

    public boolean isBlockLocked() {
        return blockLocked;
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
        b.append(" block[").append(block.toString()).append("]");
        b.append(" armor_locked[").append(armorLocked).append("]");
        b.append(" hands_locked[").append(handsLocked).append("]");
        b.append(" block_locked[").append(blockLocked).append("]");
        return b.toString();
    }
}
