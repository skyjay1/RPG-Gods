package rpggods.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import rpggods.RGRegistry;
import rpggods.RPGGods;
import rpggods.item.AltarItem;

@JeiPlugin
public class JEICompat implements IModPlugin {
    private static final ResourceLocation UID = new ResourceLocation(RPGGods.MODID, "jei_provider");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, RGRegistry.ALTAR_ITEM.get(), (ItemStack ingredient, UidContext context) -> {
            if(ingredient.hasTag() && ingredient.getTag().contains(AltarItem.KEY_ALTAR)) {
                return ingredient.getTag().getString(AltarItem.KEY_ALTAR);
            }
            return "empty";
        });
    }
}
