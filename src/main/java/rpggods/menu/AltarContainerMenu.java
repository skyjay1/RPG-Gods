package rpggods.menu;

import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeMod;
import rpggods.RGRegistry;
import rpggods.entity.AltarEntity;

import java.util.ArrayList;
import java.util.List;

public class AltarContainerMenu extends AbstractContainerMenu {

    public static final int PLAYER_INV_X = 32;
    public static final int PLAYER_INV_Y = 120;

    public static final int ALTAR_INV_SIZE = 7;

    private static final int ARMOR_X = 61;
    private static final int ARMOR_Y = 7;

    private List<Slot> altarSlots;

    private AltarEntity entity;
    private rpggods.deity.Altar altar;
    private Container altarInv;

    public AltarContainerMenu(int id, final Inventory inventory) {
        this(id, inventory, new SimpleContainer(3), null);
    }

    public AltarContainerMenu(final int id, final Inventory playerInv, final Container altarInv, final AltarEntity entity) {
        super(RGRegistry.ALTAR_CONTAINER.get(), id);
        this.entity = entity;
        this.altar = entity != null ? entity.createAltarProperties() : rpggods.deity.Altar.EMPTY;
        this.altarInv = altarInv;
        checkContainerSize(altarInv, ALTAR_INV_SIZE);
        altarInv.startOpen(playerInv.player);
        // add container inventory
        altarSlots = new ArrayList<>(ALTAR_INV_SIZE);
        int index = 0;
        boolean handsLocked = entity.isHandsLocked();
        boolean armorLocked = entity.isArmorLocked();
        boolean blockLocked = entity.isBlockLocked();
        // mainhand slot
        altarSlots.add(this.addSlot(new AltarSlot(altarInv, index++, ARMOR_X + 18 + 2, ARMOR_Y, handsLocked)));
        // armor slots
        altarSlots.add(this.addSlot(new EquipmentOnlySlot(altarInv, index++, ARMOR_X, ARMOR_Y + 3 * 18, armorLocked, EquipmentSlot.FEET)));
        altarSlots.add(this.addSlot(new EquipmentOnlySlot(altarInv, index++, ARMOR_X, ARMOR_Y + 2 * 18, armorLocked, EquipmentSlot.LEGS)));
        altarSlots.add(this.addSlot(new EquipmentOnlySlot(altarInv, index++, ARMOR_X, ARMOR_Y + 1 * 18, armorLocked, EquipmentSlot.CHEST)));
        altarSlots.add(this.addSlot(new EquipmentOnlySlot(altarInv, index++, ARMOR_X, ARMOR_Y + 0 * 18, armorLocked, EquipmentSlot.HEAD)));
        // offhand slot
        altarSlots.add(this.addSlot(new EquipmentOnlySlot(altarInv, index++, ARMOR_X + 18 + 2, ARMOR_Y + 18, handsLocked, EquipmentSlot.OFFHAND)));
        // block slot
        altarSlots.add(this.addSlot(new AltarSlot(altarInv, index++, ARMOR_X + 18 + 2, ARMOR_Y + 18 * 3, blockLocked)));
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
    public boolean stillValid(final Player playerIn) {
        final double maxDistanceSq = Math.pow(playerIn.getAttributeValue(ForgeMod.REACH_DISTANCE.get()) + 1.0D, 2);
        return getEntity() != null && getEntity().isAlive() && playerIn.distanceToSqr(getEntity()) < maxDistanceSq;
    }

    public rpggods.deity.Altar getAltar() { return altar; }

    public void setAltar(final rpggods.deity.Altar altar) { this.altar = altar; }

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
    public ItemStack quickMoveStack(Player playerIn, int index) {
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
    public void removed(final Player player) {
        super.removed(player);
        this.altarInv.stopOpen(player);
    }

    public static class AltarSlot extends Slot {

        private boolean locked;
        private boolean hidden;

        public AltarSlot(Container inventoryIn, int index, int xPosition, int yPosition, boolean locked) {
            super(inventoryIn, index, xPosition, yPosition);
            this.locked = locked;
        }

        @Override
        public boolean isActive() {
            return !isHidden();
        }

        @Override
        public boolean mayPickup(Player playerIn) {
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

    public static class EquipmentOnlySlot extends AltarSlot {

        private static final ResourceLocation[] ARMOR_SLOT_TEXTURES = new ResourceLocation[]{
                InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS, InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS,
                InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE, InventoryMenu.EMPTY_ARMOR_SLOT_HELMET,
                InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD
        };

        private EquipmentSlot type;

        public EquipmentOnlySlot(Container inventoryIn, int index, int xPosition, int yPosition, final boolean locked, EquipmentSlot slotType) {
            super(inventoryIn, index, xPosition, yPosition, locked);
            this.type = slotType;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return super.mayPlace(stack) && (getType().getType() != EquipmentSlot.Type.ARMOR || Mob.getEquipmentSlotForItem(stack) == getType());
        }

        public EquipmentSlot getType() {
            return type;
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
            return Pair.of(InventoryMenu.BLOCK_ATLAS, ARMOR_SLOT_TEXTURES[getType().getFilterFlag() - 1]);
        }
    }
}
