package rpggods.util;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Registry;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.ForgeRegistries;
import rpggods.RPGGods;
import rpggods.data.deity.DeityWrapper;
import rpggods.RGEvents;
import rpggods.data.favor.IFavor;
import rpggods.data.perk.Perk;
import rpggods.data.perk.PerkAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class AutosmeltOrCobbleModifier extends LootModifier {

    public static final Supplier<Codec<AutosmeltOrCobbleModifier>> CODEC_SUPPLIER = Suppliers.memoize(() -> RecordCodecBuilder.create(inst ->
            codecStart(inst)
                    .and(ForgeRegistries.BLOCKS.getCodec().fieldOf("stone").forGetter(AutosmeltOrCobbleModifier::getStone))
                    .and(TagKey.codec(ForgeRegistries.Keys.BLOCKS).fieldOf("ores").forGetter(AutosmeltOrCobbleModifier::getOres))
                    .apply(inst, AutosmeltOrCobbleModifier::new)));;

    private final Block stone;
    private final TagKey<Block> ores;

    protected AutosmeltOrCobbleModifier(final LootItemCondition[] conditions, final Block stone, final TagKey<Block> ores) {
        super(conditions);
        this.stone = stone;
        this.ores = ores;
    }

    public Block getStone() {
        return stone;
    }

    public TagKey<Block> getOres() {
        return ores;
    }

    @Override
    public ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        Entity entity = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        ItemStack itemStack = context.getParamOrNull(LootContextParams.TOOL);
        BlockState block = context.getParamOrNull(LootContextParams.BLOCK_STATE);
        // do not apply when missing entity or item or breaking non-ore block
        if(entity == null || itemStack == null || block == null || !block.is(ores)) {
            return generatedLoot;
        }
        // determine which of the mining effects can activate
        List<ResourceLocation> autosmelt = new ArrayList<>();
        List<ResourceLocation> unsmelt = new ArrayList<>();
        for (DeityWrapper deity : RPGGods.DEITY_HELPER.values()) {
            autosmelt.addAll(deity.perkByTypeMap.getOrDefault(PerkAction.Type.AUTOSMELT, ImmutableList.of()));
            unsmelt.addAll(deity.perkByTypeMap.getOrDefault(PerkAction.Type.UNSMELT, ImmutableList.of()));
        }
        // make sure this is an ore mined by a non-creative player
        if (entity instanceof Player && !entity.isSpectator() && !((Player) entity).isCreative()
                && (!autosmelt.isEmpty() || !unsmelt.isEmpty())) {
            final Player player = (Player) entity;
            final LazyOptional<IFavor> favor = RPGGods.getFavor(player);
            // determine results using player favor
            if(favor.isPresent() && favor.orElse(null).isEnabled()) {
                IFavor f = favor.orElse(null);
                ObjectArrayList<ItemStack> replacement = new ObjectArrayList<>();
                // attempt to autosmelt
                Collections.shuffle(autosmelt);
                Perk perk;
                for(ResourceLocation id : autosmelt) {
                    perk = RPGGods.PERK_MAP.get(id);
                    if(RGEvents.runPerk(perk, player, f)) {
                        generatedLoot.forEach((stack) -> replacement.add(smelt(stack, context)));
                        return replacement;
                    }
                }
                // if no autosmelt occurred, attempt to unsmelt
                Collections.shuffle(unsmelt);
                for(ResourceLocation id : unsmelt) {
                    perk = RPGGods.PERK_MAP.get(id);
                    if(RGEvents.runPerk(perk, player, f)) {
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

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC_SUPPLIER.get();
    }
}
