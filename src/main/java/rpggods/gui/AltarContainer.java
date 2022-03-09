package rpggods.gui;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import rpggods.RGRegistry;
import rpggods.deity.Altar;
import rpggods.entity.AltarEntity;

import java.util.ArrayList;
import java.util.List;

public class AltarContainer extends Container {

    public static final int PLAYER_INV_X = 32;
    public static final int PLAYER_INV_Y = 120;

    private List<Slot> handItems;
    private List<Slot> armorItems;
    private Slot blockSlot;

    private AltarEntity entity;
    private Altar altar;

    public AltarContainer(int id, final PlayerInventory inventory) {
        this(id, inventory, new Inventory(3), null);
    }

    public AltarContainer(final int id, final PlayerInventory inventory, final IInventory iinventory, final AltarEntity entity) {
        super(RGRegistry.ContainerReg.ALTAR_CONTAINER, id);
        this.entity = entity;
        this.altar = entity != null ? entity.createAltarProperties() : Altar.EMPTY;
        // add container inventory
        handItems = new ArrayList<>();
        armorItems = new ArrayList<>();
        if(iinventory != null) {
            handItems.add(this.addSlot(new Slot(iinventory, 0, 8, 90)));
            handItems.add(this.addSlot(new Slot(iinventory, 1, 24, 90)));
            blockSlot = this.addSlot(new Slot(iinventory, 2, 44, 90));
        }
        // add player inventory
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(inventory, j + i * 9 + 9, PLAYER_INV_X + j * 18, PLAYER_INV_Y + i * 18));
            }
        }
        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(inventory, k, PLAYER_INV_X + k * 18, 178));
        }
    }

    @Override
    public boolean canInteractWith(final PlayerEntity playerIn) {
        return true; // TODO
    }

    public Altar getAltar() { return altar; }

    public AltarEntity getEntity() { return entity; }

    public ItemStack getItemStackFromSlot(EquipmentSlotType slotIn) {
        Slot slot;
        switch(slotIn.getSlotType()) {
            case HAND:
                slot = handItems.get(slotIn.getIndex());
                return slot != null ? slot.getStack() : ItemStack.EMPTY;
            case ARMOR:
                slot = armorItems.get(slotIn.getIndex());
                return slot != null ? slot.getStack() : ItemStack.EMPTY;
        }
        return ItemStack.EMPTY;
    }

    /**
     * Handle when the stack in slot {@code index} is shift-clicked. Normally this moves the stack between the player
     * inventory and the other inventory(s).
     */
    @Override
    public ItemStack transferStackInSlot(PlayerEntity playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);
        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();
            if (index < 2) {
                if (!this.mergeItemStack(itemstack1, 2, this.inventorySlots.size(), false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.mergeItemStack(itemstack1, 0, 2, true)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }
        }

        return itemstack;
    }
}
