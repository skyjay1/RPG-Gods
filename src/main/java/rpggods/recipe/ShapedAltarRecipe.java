package rpggods.recipe;

import com.google.gson.JsonObject;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipe;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import rpggods.RGRegistry;
import rpggods.RPGGods;
import rpggods.item.AltarItem;

import java.util.Optional;

public class ShapedAltarRecipe extends ShapedRecipe {

    public static final String CATEGORY = "altar";
    public static final String NAME = CATEGORY + "_shaped";

    private final Optional<ResourceLocation> altarId;

    public ShapedAltarRecipe(ResourceLocation recipeId, final ItemStack outputItem, final Optional<ResourceLocation> altarId,
                             final int width, final int height, final NonNullList<Ingredient> recipeItemsIn) {
        super(recipeId, CATEGORY, width, height, recipeItemsIn, outputItem);
        this.altarId = altarId;
    }

    /**
     * Returns an Item that is the result of this recipe
     */
    @Override
    public ItemStack assemble(CraftingInventory inv) {
        final ItemStack result = super.assemble(inv);
        if(getAltarId().isPresent()) {
            result.getOrCreateTag().putString(AltarItem.KEY_ALTAR, getAltarId().get().toString());
        }
        return result;
    }

    @Override
    public IRecipeSerializer<?> getSerializer() {
        return RGRegistry.RecipeReg.SHAPED_ALTAR_RECIPE_SERIALIZER;
    }

    public Optional<ResourceLocation> getAltarId() {
        return altarId;
    }

    public static class Factory extends ShapedRecipe.Serializer {

        @Override
        public ShapedRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            // read the recipe from shapeless recipe serializer
            final ShapedRecipe recipe = super.fromJson(recipeId, json);
            final String sAltarId = JSONUtils.getAsString(json, AltarItem.KEY_ALTAR, "");
            if(sAltarId.isEmpty()) {
                return new ShapedAltarRecipe(recipeId, recipe.getResultItem(), Optional.empty(),
                        recipe.getWidth(), recipe.getHeight(), recipe.getIngredients());
            }
            // attempt to read resource location
            ResourceLocation altarId = ResourceLocation.tryParse(sAltarId);
            if (null == altarId) {
                RPGGods.LOGGER.error("Failed to parse altar ID \"" + sAltarId + "\" in recipe with id " + recipeId);
            }
            return new ShapedAltarRecipe(recipeId, recipe.getResultItem(), Optional.ofNullable(altarId),
                    recipe.getWidth(), recipe.getHeight(), recipe.getIngredients());
        }

        @Override
        public ShapedRecipe fromNetwork(ResourceLocation recipeId, PacketBuffer buffer) {
            final ShapedRecipe recipe = super.fromNetwork(recipeId, buffer);
            final boolean hasAltar = buffer.readBoolean();
            ResourceLocation altarId = null;
            if(hasAltar) {
                altarId = buffer.readResourceLocation();
            }
            return new ShapedAltarRecipe(recipeId, recipe.getResultItem(), Optional.ofNullable(altarId),
                    recipe.getWidth(), recipe.getHeight(), recipe.getIngredients());
        }

        @Override
        public void toNetwork(PacketBuffer buffer, ShapedRecipe recipeIn) {
            super.toNetwork(buffer, recipeIn);
            final ShapedAltarRecipe recipe = (ShapedAltarRecipe) recipeIn;
            final boolean hasAltar = recipe.getAltarId().isPresent();
            buffer.writeBoolean(hasAltar);
            if(hasAltar) {
                buffer.writeResourceLocation(recipe.getAltarId().get());
            }
        }
    }
}
