package rpggods.gui;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.resources.ResourceLocation;
import rpggods.RGRegistry;
import rpggods.RPGGods;
import rpggods.deity.Altar;
import rpggods.favor.Favor;
import rpggods.favor.IFavor;

import java.util.Optional;

public class FavorContainer extends AbstractContainerMenu {

    private IFavor favor;
    private Optional<ResourceLocation> deity;

    public FavorContainer(int id, final Inventory inventory) {
        this(id, inventory, Favor.EMPTY, null);
    }

    public FavorContainer(final int id, final Inventory inventory, final IFavor favorIn, final ResourceLocation deityIn) {
        super(RGRegistry.ContainerReg.FAVOR_CONTAINER, id);
        favor = favorIn;
        deity = Optional.ofNullable(deityIn);
    }

    @Override
    public boolean stillValid(final Player playerIn) {
        return true;
    }

    public IFavor getFavor() {
        return favor;
    }

    public Optional<ResourceLocation> getDeity() {
        return deity;
    }
}
