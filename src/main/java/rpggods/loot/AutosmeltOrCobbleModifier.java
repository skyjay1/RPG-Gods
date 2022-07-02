package rpggods.loot;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.Tag;
import net.minecraft.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.loot.GlobalLootModifierSerializer;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.ForgeRegistries;
import rpggods.RPGGods;
import rpggods.deity.DeityHelper;
import rpggods.event.FavorEventHandler;
import rpggods.favor.IFavor;
import rpggods.perk.Perk;
import rpggods.perk.PerkAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AutosmeltOrCobbleModifier extends LootModifier {

    private final Block stone;
    private final ResourceLocation oresTag;
    private final TagKey<Block> ores;

    protected AutosmeltOrCobbleModifier(final LootItemCondition[] conditionsIn, final Block stoneIn, final ResourceLocation oresTagIn) {
        super(conditionsIn);
        stone = stoneIn;
        oresTag = oresTagIn;
        // TODO make sure this works
        // TODO figure out why the loot modifier "fails to parse"
        ores = BlockTags.create(oresTagIn);
    }

    @Override
    public List<ItemStack> doApply(List<ItemStack> generatedLoot, LootContext context) {
        Entity entity = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        ItemStack itemStack = context.getParamOrNull(LootContextParams.TOOL);
        BlockState block = context.getParamOrNull(LootContextParams.BLOCK_STATE);
        // do not apply when missing entity or item or breaking non-ore block
        if(entity == null || itemStack == null || block == null || !block.is(ores)) {
            return generatedLoot;
        }
        // do not apply when using silk touch tool
        if(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, itemStack) > 0) {
            return generatedLoot;
        }
        // determine which of the mining effects can activate
        List<ResourceLocation> autosmelt = Lists.newArrayList();
        List<ResourceLocation> unsmelt = Lists.newArrayList();
        for (DeityHelper deity : RPGGods.DEITY_HELPER.values()) {
            autosmelt.addAll(deity.perkByTypeMap.getOrDefault(PerkAction.Type.AUTOSMELT, ImmutableList.of()));
            unsmelt.addAll(deity.perkByTypeMap.getOrDefault(PerkAction.Type.UNSMELT, ImmutableList.of()));
        }
        // make sure this is an ore mined by a non-creative player
        if (entity instanceof Player && !entity.isSpectator() && !((Player) entity).isCreative()
                && (!autosmelt.isEmpty() || !unsmelt.isEmpty())) {
            final Player player = (Player) entity;
            final LazyOptional<IFavor> favor = player.getCapability(RPGGods.FAVOR);
            // determine results using player favor
            if(favor.isPresent() && favor.orElse(null).isEnabled()) {
                IFavor f = favor.orElse(null);
                ArrayList<ItemStack> replacement = new ArrayList<>();
                // attempt to autosmelt
                Collections.shuffle(autosmelt);
                Perk perk;
                for(ResourceLocation id : autosmelt) {
                    perk = RPGGods.PERK.get(id).orElse(null);
                    if(FavorEventHandler.runPerk(perk, player, f)) {
                        generatedLoot.forEach((stack) -> replacement.add(smelt(stack, context)));
                        return replacement;
                    }
                }
                // if no autosmelt occurred, attempt to unsmelt
                Collections.shuffle(unsmelt);
                for(ResourceLocation id : unsmelt) {
                    perk = RPGGods.PERK.get(id).orElse(null);
                    if(FavorEventHandler.runPerk(perk, player, f)) {
                        replacement.add(new ItemStack(stone.asItem()));
                        return replacement;
                    }
                }

            }
        }
        return generatedLoot;
    }


    /**
     * @param stack   the item to smelt
     * @param context the loot context
     * @return the item that would normally result from smelting the given item
     */
    private static ItemStack smelt(ItemStack stack, LootContext context) {
        return context.getLevel().getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SimpleContainer(stack), context.getLevel())
                .map(SmeltingRecipe::getResultItem)
                .filter(itemStack -> !itemStack.isEmpty())
                .map(itemStack -> ItemHandlerHelper.copyStackWithSize(itemStack, stack.getCount() * itemStack.getCount()))
                .orElse(stack);
    }

    private static ItemStack cobble(ItemStack stack, Block stone) {
        return new ItemStack(stone.asItem());
    }

    public static class Serializer extends GlobalLootModifierSerializer<AutosmeltOrCobbleModifier> {

        private static final String STONE = "stone";
        private static final String ORES = "ores";

        @Override
        public AutosmeltOrCobbleModifier read(ResourceLocation name, JsonObject object, LootItemCondition[] conditionsIn) {
            Block stone = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(GsonHelper.getAsString(object, STONE)));
            ResourceLocation oresTag = new ResourceLocation(GsonHelper.getAsString(object, ORES));
            return new AutosmeltOrCobbleModifier(conditionsIn, stone, oresTag);
        }

        @Override
        public JsonObject write(AutosmeltOrCobbleModifier instance) {
            JsonObject json = makeConditions(instance.conditions);
            json.addProperty(STONE, instance.stone.getRegistryName().toString());
            json.addProperty(ORES, instance.oresTag.toString());
            return json;
        }
    }
}
