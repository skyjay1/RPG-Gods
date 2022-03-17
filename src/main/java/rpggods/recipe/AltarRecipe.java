package rpggods.recipe;

import com.google.gson.JsonObject;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapelessRecipe;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import rpggods.RGRegistry;
import rpggods.RPGGods;
import rpggods.item.AltarItem;

import java.util.Optional;

import net.minecraft.item.crafting.ShapelessRecipe.Serializer;

public class AltarRecipe extends ShapelessRecipe {

    public static final String CATEGORY = "altar";

    private final Optional<ResourceLocation> altarId;

    public AltarRecipe(ResourceLocation recipeId, final ItemStack outputItem, final Optional<ResourceLocation> altarId,
                       final NonNullList<Ingredient> recipeItemsIn) {
        super(recipeId, CATEGORY, outputItem, recipeItemsIn);
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
        return RGRegistry.RecipeReg.ALTAR_RECIPE_SERIALIZER;
    }

    public Optional<ResourceLocation> getAltarId() {
        return altarId;
    }

    public static class Factory extends Serializer {

        @Override
        public ShapelessRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            // read the recipe from shapeless recipe serializer
            final ShapelessRecipe recipe = super.fromJson(recipeId, json);
            final String sAltarId = JSONUtils.getAsString(json, AltarItem.KEY_ALTAR, "");
            if(sAltarId.isEmpty()) {
                return new AltarRecipe(recipeId, recipe.getResultItem(), Optional.empty(), recipe.getIngredients());
            }
            // attempt to read resource location
            ResourceLocation altarId = ResourceLocation.tryParse(sAltarId);
            if (null == altarId) {
                RPGGods.LOGGER.error("Failed to parse altar ID \"" + sAltarId + "\" in recipe with id " + recipeId);
            }
            return new AltarRecipe(recipeId, recipe.getResultItem(), Optional.ofNullable(altarId), recipe.getIngredients());
        }

        @Override
        public ShapelessRecipe fromNetwork(ResourceLocation recipeId, PacketBuffer buffer) {
            final ShapelessRecipe recipe = super.fromNetwork(recipeId, buffer);
            final boolean hasAltar = buffer.readBoolean();
            ResourceLocation altarId = null;
            if(hasAltar) {
                altarId = buffer.readResourceLocation();
            }
            return new AltarRecipe(recipeId, recipe.getResultItem(), Optional.ofNullable(altarId), recipe.getIngredients());
        }

        @Override
        public void toNetwork(PacketBuffer buffer, ShapelessRecipe recipeIn) {
            super.toNetwork(buffer, recipeIn);
            final AltarRecipe recipe = (AltarRecipe) recipeIn;
            final boolean hasAltar = recipe.getAltarId().isPresent();
            buffer.writeBoolean(hasAltar);
            if(hasAltar) {
                buffer.writeResourceLocation(recipe.getAltarId().get());
            }
        }
    }
}
