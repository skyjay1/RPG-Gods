package rpggods.gui;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.util.ResourceLocation;
import rpggods.RGRegistry;
import rpggods.RPGGods;
import rpggods.deity.Altar;
import rpggods.favor.IFavor;

import java.util.Optional;

public class FavorContainer extends Container {
  
  private IFavor favor;
  private Optional<ResourceLocation> deity;

  public FavorContainer(int id, final PlayerInventory inventory) {
    this(id, inventory, RPGGods.FAVOR.getDefaultInstance(), null);
  }
  
  public FavorContainer(final int id, final PlayerInventory inventory, final IFavor favorIn, final ResourceLocation deityIn) {
    super(RGRegistry.ContainerReg.FAVOR_CONTAINER, id);
    favor = favorIn;
    deity = Optional.ofNullable(deityIn);
  }

  @Override
  public boolean canInteractWith(final PlayerEntity playerIn) {
    return true;
  }
  
  public IFavor getFavor() {
    return favor;
  }
  
  public Optional<ResourceLocation> getDeity() {
    return deity;
  }
}
