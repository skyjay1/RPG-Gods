package rpggods.integration;

import com.google.common.collect.Lists;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.IIngredients;

import mezz.jei.plugins.vanilla.crafting.CraftingCategoryExtension;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import rpggods.item.AltarItem;
import rpggods.recipe.ShapedAltarRecipe;

import java.util.List;

public class JEIShapedAltarRecipe {

    public static final class Wrapper extends CraftingCategoryExtension<ShapedAltarRecipe> {

        public Wrapper(ShapedAltarRecipe recipe) {
            super(recipe);
        }

        @Override
        public void setIngredients(IIngredients ingredients) {
            // ensure any altar ingredients have tag for "Statue"
            final List<List<ItemStack>> inputList = Lists.newArrayList();
            // go through each ingredient, adding it to the list
            for (final Ingredient ingredient : recipe.getIngredients()) {
                List<ItemStack> matchingStacks = Lists.newArrayList();
                // before adding each ingredient to the list, check if it is an altar
                for(final ItemStack stack : ingredient.getItems()) {
                    // if this ingredient is an altar, we need to set NBT data and add subtypes
                    if(stack != null && !stack.isEmpty() && stack.getItem() instanceof AltarItem) {
                        // correct NBT values for input altar
                        AltarItem.addStatueItemsOnly(matchingStacks);
                    } else {
                        // if not altar, add directly to matching list
                        matchingStacks.add(stack);
                    }
                }
                // actually add the ingredient to the list
                inputList.add(matchingStacks);
            }
            // add all the INPUT ingredients
            ingredients.setInputLists(VanillaTypes.ITEM, inputList);
            // ensure output item uses NBT data
            ingredients.setOutput(VanillaTypes.ITEM, this.recipe.getResultItem());
        }
    }
}
