package rpggods.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaRecipeCategoryUid;
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import rpggods.RGRegistry;
import rpggods.RPGGods;
import rpggods.item.AltarItem;
import rpggods.recipe.ShapedAltarRecipe;
import rpggods.recipe.ShapelessAltarRecipe;

import java.util.List;
import java.util.stream.Collectors;

@JeiPlugin
public class JEIProvider implements IModPlugin {

    private static final ResourceLocation PLUGIN_UID = new ResourceLocation(RPGGods.MODID, RPGGods.MODID + "_jei");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(RGRegistry.ItemReg.ALTAR, (ingredient, context) -> {
            if(ingredient.hasTag()) {
                return ingredient.getTag().getString(AltarItem.KEY_ALTAR);
            }
            return IIngredientSubtypeInterpreter.NONE;
        });
    }

    @Override
    public void registerRecipes(final IRecipeRegistration registry) {
        final List<IRecipe<?>> list = Minecraft.getInstance().level.getRecipeManager().getRecipes().stream()
                .filter(r -> r.getResultItem().getItem() instanceof AltarItem)
                .collect(Collectors.toList());
        registry.addRecipes(list, VanillaRecipeCategoryUid.CRAFTING);
    }

    @Override
    public void registerVanillaCategoryExtensions(final IVanillaCategoryExtensionRegistration registry) {
        registry.getCraftingCategory().addCategoryExtension(ShapedAltarRecipe.class, JEIShapedAltarRecipe.Wrapper::new);
        registry.getCraftingCategory().addCategoryExtension(ShapelessAltarRecipe.class, JEIShapelessAltarRecipe.Wrapper::new);
    }
}
