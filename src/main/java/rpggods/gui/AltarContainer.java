package rpggods.gui;

import com.mojang.datafixers.util.Pair;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeMod;
import rpggods.RGRegistry;
import rpggods.deity.Altar;
import rpggods.entity.AltarEntity;

import java.util.ArrayList;
import java.util.List;

public class AltarContainer extends Container {

    public static final int PLAYER_INV_X = 32;
    public static final int PLAYER_INV_Y = 120;

    public static final int ALTAR_INV_SIZE = 7;

    private static final int ARMOR_X = 61;
    private static final int ARMOR_Y = 7;

    private List<Slot> altarSlots;

    private AltarEntity entity;
    private Altar altar;
    private IInventory altarInv;

    public AltarContainer(int id, final PlayerInventory inventory) {
        this(id, inventory, new Inventory(3), null);
    }

    public AltarContainer(final int id, final PlayerInventory playerInv, final IInventory altarInv, final AltarEntity entity) {
        super(RGRegistry.ContainerReg.ALTAR_CONTAINER, id);
        this.entity = entity;
        this.altar = entity != null ? entity.createAltarProperties() : Altar.EMPTY;
        this.altarInv = altarInv;
        checkContainerSize(altarInv, ALTAR_INV_SIZE);
        altarInv.startOpen(playerInv.player);
        // add container inventory
        altarSlots = new ArrayList<>(ALTAR_INV_SIZE);
        int index = 0;
        if(altarInv != null) {
            boolean handsLocked = entity.isHandsLocked();
            boolean armorLocked = entity.isArmorLocked();
            boolean blockLocked = entity.isBlockLocked();
            // mainhand slot
            altarSlots.add(this.addSlot(new AltarSlot(altarInv, index++, ARMOR_X + 18 + 2, ARMOR_Y, handsLocked)));
            // armor slots
            altarSlots.add(this.addSlot(new EquipmentSlot(altarInv, index++, ARMOR_X, ARMOR_Y + 3 * 18, armorLocked, EquipmentSlotType.FEET)));
            altarSlots.add(this.addSlot(new EquipmentSlot(altarInv, index++, ARMOR_X, ARMOR_Y + 2 * 18, armorLocked, EquipmentSlotType.LEGS)));
            altarSlots.add(this.addSlot(new EquipmentSlot(altarInv, index++, ARMOR_X, ARMOR_Y + 1 * 18, armorLocked, EquipmentSlotType.CHEST)));
            altarSlots.add(this.addSlot(new EquipmentSlot(altarInv, index++, ARMOR_X, ARMOR_Y + 0 * 18, armorLocked, EquipmentSlotType.HEAD)));
            // offhand slot
            altarSlots.add(this.addSlot(new EquipmentSlot(altarInv, index++, ARMOR_X + 18 + 2, ARMOR_Y + 18, handsLocked, EquipmentSlotType.OFFHAND)));
            // block slot
            altarSlots.add(this.addSlot(new AltarSlot(altarInv, index++, ARMOR_X + 18 + 2, ARMOR_Y + 18 * 3, blockLocked)));
        }
        // add player inventory
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInv, j + i * 9 + 9, PLAYER_INV_X + j * 18, PLAYER_INV_Y + i * 18));
            }
        }
        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInv, k, PLAYER_INV_X + k * 18, 178));
        }
    }

    @Override
    public boolean stillValid(final PlayerEntity playerIn) {
        final double maxDistanceSq = Math.pow(playerIn.getAttributeValue(ForgeMod.REACH_DISTANCE.get()) + 1.0D, 2);
        return getEntity() != null && playerIn.distanceToSqr(getEntity()) < maxDistanceSq;
    }

    public Altar getAltar() { return altar; }

    public void setAltar(final Altar altar) { this.altar = altar; }

    public AltarEntity getEntity() { return entity; }

    public List<Slot> getAltarSlots() { return altarSlots; }

    public void setChanged() {
        this.altarInv.setChanged();
    }

    /**
     * Handle when the stack in slot {@code index} is shift-clicked. Normally this moves the stack between the player
     * inventory and the other inventory(s).
     */
    @Override
    public ItemStack quickMoveStack(PlayerEntity playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index < ALTAR_INV_SIZE) {
                if (!this.moveItemStackTo(itemstack1, ALTAR_INV_SIZE, this.slots.size(), false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 0, ALTAR_INV_SIZE, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }

    @Override
    public void removed(final PlayerEntity player) {
        super.removed(player);
        this.altarInv.stopOpen(player);
    }

    public static class AltarSlot extends Slot {

        private boolean locked;
        private boolean hidden;

        public AltarSlot(IInventory inventoryIn, int index, int xPosition, int yPosition, boolean locked) {
            super(inventoryIn, index, xPosition, yPosition);
            this.locked = locked;
        }

        @Override
        public boolean isActive() {
            return !isHidden();
        }

        @Override
        public boolean mayPickup(PlayerEntity playerIn) {
            return !isLocked();
        }

        @Override
        public boolean mayPlace(ItemStack p_75214_1_) {
            return !isLocked();
        }

        public boolean isHidden() {
            return hidden;
        }

        public boolean isLocked() {
            return locked;
        }

        public void setHidden(final boolean hidden) {
            this.hidden = hidden;
        }

        public void setLocked(final boolean locked) {
            this.locked = locked;
        }
    }

    public static class EquipmentSlot extends AltarSlot {

        private static final ResourceLocation[] ARMOR_SLOT_TEXTURES = new ResourceLocation[]{
                PlayerContainer.EMPTY_ARMOR_SLOT_BOOTS, PlayerContainer.EMPTY_ARMOR_SLOT_LEGGINGS,
                PlayerContainer.EMPTY_ARMOR_SLOT_CHESTPLATE, PlayerContainer.EMPTY_ARMOR_SLOT_HELMET,
                PlayerContainer.EMPTY_ARMOR_SLOT_SHIELD
        };

        private EquipmentSlotType type;

        public EquipmentSlot(IInventory inventoryIn, int index, int xPosition, int yPosition, final boolean locked, EquipmentSlotType slotType) {
            super(inventoryIn, index, xPosition, yPosition, locked);
            this.type = slotType;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return super.mayPlace(stack) && (getType().getType() != EquipmentSlotType.Group.ARMOR || MobEntity.getEquipmentSlotForItem(stack) == getType());
        }

        public EquipmentSlotType getType() {
            return type;
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
            return Pair.of(PlayerContainer.BLOCK_ATLAS, ARMOR_SLOT_TEXTURES[getType().getFilterFlag() - 1]);
        }
    }
}
