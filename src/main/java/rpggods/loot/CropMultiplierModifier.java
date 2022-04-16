package rpggods.loot;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootParameters;
import net.minecraft.loot.conditions.ILootCondition;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ITag;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.loot.GlobalLootModifierSerializer;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.common.util.LazyOptional;
import rpggods.RPGGods;
import rpggods.deity.DeityHelper;
import rpggods.event.FavorEventHandler;
import rpggods.favor.IFavor;
import rpggods.perk.Perk;
import rpggods.perk.PerkAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CropMultiplierModifier extends LootModifier {

    private final ResourceLocation cropsTag;
    private final ITag<Block> crops;

    protected CropMultiplierModifier(final ILootCondition[] conditionsIn, final ResourceLocation cropsTagIn) {
        super(conditionsIn);
        cropsTag = cropsTagIn;
        crops = BlockTags.bind(cropsTagIn.toString());
    }

    @Override
    public List<ItemStack> doApply(List<ItemStack> generatedLoot, LootContext context) {
        Entity entity = context.getParamOrNull(LootParameters.THIS_ENTITY);
        BlockState block = context.getParamOrNull(LootParameters.BLOCK_STATE);
        // do not apply when entity is null or breaking non-crops
        if(entity == null || block == null || !block.is(crops)) {
            return generatedLoot;
        }
        // determine which of the mining effects can activate
        List<ResourceLocation> cropHarvest = Lists.newArrayList();
        for (DeityHelper deity : RPGGods.DEITY_HELPER.values()) {
            cropHarvest.addAll(deity.perkByTypeMap.getOrDefault(PerkAction.Type.CROP_HARVEST, ImmutableList.of()));
        }
        // make sure this is an ore mined by a non-creative player
        if (entity instanceof PlayerEntity && !entity.isSpectator() && !((PlayerEntity) entity).isCreative()
                && !cropHarvest.isEmpty()) {
            final PlayerEntity player = (PlayerEntity) entity;
            final LazyOptional<IFavor> favor = player.getCapability(RPGGods.FAVOR);
            // determine results using player favor
            if (favor.isPresent() && favor.orElse(null).isEnabled()) {
                IFavor f = favor.orElse(null);
                ArrayList<ItemStack> replacement = new ArrayList<>();
                // attempt to add or remove crops
                Collections.shuffle(cropHarvest);
                Perk perk;
                for (ResourceLocation id : cropHarvest) {
                    perk = RPGGods.PERK.get(id).orElse(null);
                    if (FavorEventHandler.runPerk(perk, player, f)) {
                        float multiplier = 0;
                        for(PerkAction action : perk.getActions()) {
                            if(action.getType() == PerkAction.Type.CROP_HARVEST && action.getMultiplier().isPresent()) {
                                multiplier += action.getMultiplier().get();
                            }
                        }
                        int amount = Math.round(multiplier);
                        if(amount != 0) {
                            generatedLoot.forEach(i -> i.grow(amount));
                        }
                    }
                }
            }
        }
        return generatedLoot;
    }

    public static class Serializer extends GlobalLootModifierSerializer<CropMultiplierModifier> {

        private static final String CROPS = "crops";

        @Override
        public CropMultiplierModifier read(ResourceLocation name, JsonObject object, ILootCondition[] conditionsIn) {
            ResourceLocation cropsTag = new ResourceLocation(JSONUtils.getAsString(object, CROPS));
            return new CropMultiplierModifier(conditionsIn, cropsTag);
        }

        @Override
        public JsonObject write(CropMultiplierModifier instance) {
            JsonObject json = makeConditions(instance.conditions);
            json.addProperty(CROPS, instance.cropsTag.toString());
            return json;
        }
    }
}
