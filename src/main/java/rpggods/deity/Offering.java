package rpggods.deity;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import rpggods.RPGGods;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class Offering {
    public static final Offering EMPTY = new Offering(ItemStack.EMPTY, 0, 0, 0,
            Optional.empty(), 0, Optional.empty(), Optional.empty());

    // Codec that accepts Item or ItemStack
    public static final Codec<ItemStack> ITEM_OR_STACK_CODEC = Codec.either(Registry.ITEM, ItemStack.CODEC)
            .xmap(either -> either.map(ItemStack::new, Function.identity()),
                    stack -> stack.getCount() == 1 && !stack.hasTag()
                            ? Either.left(stack.getItem())
                            : Either.right(stack));

    public static final Codec<Offering> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ITEM_OR_STACK_CODEC.optionalFieldOf("item", ItemStack.EMPTY).forGetter(Offering::getAccept),
            Codec.INT.optionalFieldOf("favor", 0).forGetter(Offering::getFavor),
            Codec.INT.optionalFieldOf("maxuses", 16).forGetter(Offering::getMaxUses),
            Codec.INT.optionalFieldOf("cooldown", 12000).forGetter(Offering::getCooldown),
            ITEM_OR_STACK_CODEC.optionalFieldOf("trade").forGetter(Offering::getTrade),
            Codec.INT.optionalFieldOf("minlevel", 0).forGetter(Offering::getTradeMinLevel),
            ResourceLocation.CODEC.optionalFieldOf("function").forGetter(Offering::getFunction),
            Codec.STRING.optionalFieldOf("function_text").forGetter(Offering::getFunctionText)
    ).apply(instance, Offering::new));

    private final ItemStack accept;
    private final Map<Enchantment, Integer> acceptEnchantments;
    private final int favor;
    private final Optional<ItemStack> trade;
    private final int tradeMinLevel;
    private final Optional<ResourceLocation> function;
    private final Optional<String> functionText;
    private final int maxUses;
    private final int cooldown;

    public Offering(ItemStack accept, int favor, int maxUses, int cooldown,
                    Optional<ItemStack> trade, int tradeMinLevel,
                    Optional<ResourceLocation> function, Optional<String> functionText) {
        this.accept = accept;
        this.favor = favor;
        this.maxUses = maxUses;
        this.cooldown = cooldown;
        this.trade = trade;
        this.tradeMinLevel = tradeMinLevel;
        this.function = function;
        this.functionText = functionText;
        // parse enchantments for accepted item stack
        this.acceptEnchantments = ImmutableMap.copyOf(EnchantmentHelper.getEnchantments(this.accept));
    }

    /**
     * Attempts to parse the deity from the given offering id
     * @param offeringId the offering id in the form {@code namespace:deity/offering}
     * @return the resource location if found, otherwise {@link Deity#EMPTY}
     */
    public static ResourceLocation getDeity(final ResourceLocation offeringId) {
        String path = offeringId.getPath();
        int index = path.indexOf("/");
        if(index > -1) {
            return new ResourceLocation(offeringId.getNamespace(), path.substring(0, index));
        }
        return Deity.EMPTY.id;
    }

    /**
     * Checks the given item stack to see if this offering can accept it.
     * Examines item, count, and enchantments
     * @param offering another item stack
     * @return true if the given ItemStack matches the one in this offering
     */
    public boolean matches(ItemStack offering) {
        // check item and stack size
        if(!this.accept.sameItemStackIgnoreDurability(offering) || offering.getCount() < this.accept.getCount()) {
            return false;
        }
        // check enchantments
        if(!acceptEnchantments.isEmpty()) {
            Map<Enchantment, Integer> offeringEnchantments = EnchantmentHelper.getEnchantments(offering);
            // check each enchantment in accept map to see if a corresponding enchantment exists in the offering
            for(Map.Entry<Enchantment, Integer> entry : acceptEnchantments.entrySet()) {
                if(offeringEnchantments.getOrDefault(entry.getKey(), 0).intValue() != entry.getValue().intValue()) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * ItemStack aware version of {@link #getTrade()} that allows
     * the trade item to copy NBT data and enchantments from the offering item.
     * @param offering the offering item
     * @return the trade item (or empty if there is no trade)
     */
    public Optional<ItemStack> getTrade(final ItemStack offering) {
        // special handling of trade when same item and NBT is present
        if(trade.isPresent() && offering.sameItem(trade.get()) && trade.get().hasTag() && offering.hasTag()) {
            RPGGods.LOGGER.debug("Copying NBT from offering: " + offering.getTag());
            // create itemstack to modify and return
            ItemStack tradeItem = offering.copy();
            tradeItem.setCount(trade.get().getCount());
            // determine enchantments
            Map<Enchantment, Integer> currentEnchantments = EnchantmentHelper.getEnchantments(offering);
            currentEnchantments.putAll(EnchantmentHelper.getEnchantments(trade.get()));
            // merge NBT data
            tradeItem.setTag(trade.get().getTag().merge(tradeItem.getTag()));
            // set enchantments (they may not have merged correctly)
            EnchantmentHelper.setEnchantments(currentEnchantments, tradeItem);
            RPGGods.LOGGER.debug("Trade item NBT is now " + tradeItem.getTag());
            return Optional.of(tradeItem);
        }
        return getTrade();
    }

    public ItemStack getAccept() {
        return accept;
    }

    public int getFavor() {
        return favor;
    }

    public int getMaxUses() {
        return maxUses;
    }

    public int getCooldown() {
        return cooldown;
    }

    public Optional<ItemStack> getTrade() {
        return trade;
    }

    public int getTradeMinLevel() {
        return tradeMinLevel;
    }

    public Optional<ResourceLocation> getFunction() {
        return function;
    }

    public Optional<String> getFunctionText() {
        return functionText;
    }

    public Cooldown createCooldown() {
        return new Cooldown(this.maxUses, this.cooldown);
    }
}
