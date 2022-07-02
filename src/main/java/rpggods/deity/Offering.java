package rpggods.deity;

import com.google.common.collect.ImmutableMap;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Registry;
import rpggods.RPGGods;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class Offering {
    public static final Offering EMPTY = new Offering(ItemStack.EMPTY, Optional.empty(), 0, 0, 0,
            Optional.empty(), Optional.empty(), 0, Optional.empty(), Optional.empty());

    // Codec that accepts Item or ItemStack
    public static final Codec<ItemStack> ITEM_OR_STACK_CODEC = Codec.either(Registry.ITEM.byNameCodec(), ItemStack.CODEC)
            .xmap(either -> either.map(ItemStack::new, Function.identity()),
                    stack -> stack.getCount() == 1 && !stack.hasTag()
                            ? Either.left(stack.getItem())
                            : Either.right(stack));

    public static final Codec<Offering> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ITEM_OR_STACK_CODEC.optionalFieldOf("item", ItemStack.EMPTY).forGetter(Offering::getAccept),
            Codec.STRING.optionalFieldOf("item_tag").forGetter(Offering::getAcceptTagString),
            Codec.INT.optionalFieldOf("favor", 0).forGetter(Offering::getFavor),
            Codec.INT.optionalFieldOf("maxuses", 16).forGetter(Offering::getMaxUses),
            Codec.INT.optionalFieldOf("cooldown", 12000).forGetter(Offering::getCooldown),
            ITEM_OR_STACK_CODEC.optionalFieldOf("trade").forGetter(Offering::getTrade),
            Codec.STRING.optionalFieldOf("trade_tag").forGetter(Offering::getTradeTagString),
            Codec.INT.optionalFieldOf("minlevel", 0).forGetter(Offering::getTradeMinLevel),
            ResourceLocation.CODEC.optionalFieldOf("function").forGetter(Offering::getFunction),
            Codec.STRING.optionalFieldOf("function_text").forGetter(Offering::getFunctionText)
    ).apply(instance, Offering::new));

    private final ItemStack accept;
    private final Optional<String> acceptTagString;
    private final Optional<CompoundTag> acceptTag;
    private final int favor;
    private final Optional<ItemStack> trade;
    private final Optional<String> tradeTagString;
    private final Optional<CompoundTag> tradeTag;
    private final int tradeMinLevel;
    private final Optional<ResourceLocation> function;
    private final Optional<String> functionText;
    private final int maxUses;
    private final int cooldown;

    public Offering(ItemStack accept, Optional<String> acceptTagString, int favor, int maxUses, int cooldown,
                    Optional<ItemStack> trade, Optional<String> tradeTagString, int tradeMinLevel,
                    Optional<ResourceLocation> function, Optional<String> functionText) {
        this.accept = accept;
        this.acceptTagString = acceptTagString;
        this.favor = favor;
        this.maxUses = maxUses;
        this.cooldown = cooldown;
        this.trade = trade;
        this.tradeTagString = tradeTagString;
        this.tradeMinLevel = tradeMinLevel;
        this.function = function;
        this.functionText = functionText;
        // parse accept tag
        Optional<CompoundTag> temp = Optional.empty();
        if(acceptTagString.isPresent()) {
            try {
                temp = Optional.of(TagParser.parseTag(acceptTagString.get()));
                this.accept.setTag(temp.get());
            } catch (CommandSyntaxException e) {
                RPGGods.LOGGER.error("Failed to parse item NBT in Offering\n" + e.getMessage());
            }
        }
        this.acceptTag = temp;
        // parse trade tag
        temp = Optional.empty();
        if(this.trade.isPresent() && tradeTagString.isPresent()) {
            try {
                temp = Optional.of(TagParser.parseTag(tradeTagString.get()));
                this.trade.get().setTag(temp.get());
            } catch (CommandSyntaxException e) {
                RPGGods.LOGGER.error("Failed to parse trade NBT in Offering\n" + e.getMessage());
            }
        }
        this.tradeTag = temp;
    }

    /**
     * Attempts to parse the deity from the given offering id
     * @param offeringId the offering id in the form {@code namespace:deity/offering}
     * @return the resource location if found, otherwise {@link DeityHelper#EMPTY}
     */
    public static ResourceLocation getDeity(final ResourceLocation offeringId) {
        String path = offeringId.getPath();
        int index = path.indexOf("/");
        if(index > -1) {
            return new ResourceLocation(offeringId.getNamespace(), path.substring(0, index));
        }
        return DeityHelper.EMPTY.id;
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
        // check tag
        if(this.acceptTag.isPresent() && !NbtUtils.compareNbt(this.acceptTag.get(), offering.getTag(), true)) {
            return false;
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
        if(trade.isPresent() && offering.sameItem(trade.get()) && tradeTag.isPresent()) {
            // create copy of offering item with correct count
            ItemStack tradeItem = offering.copy();
            tradeItem.setCount(trade.get().getCount());
            // merge tags
            CompoundTag tradeItemTag = merge(tradeTag.get(), tradeItem.getOrCreateTag(), 0);
            tradeItem.setTag(tradeItemTag);
            // merge damage
            if(offering.isDamageableItem() && offering.isDamaged()) {
                tradeItem.setDamageValue(offering.getDamageValue());
            }
            return Optional.of(tradeItem);
        }
        // copy trade item
        if(getTrade().isPresent()) {
            return Optional.of(getTrade().get().copy());
        }
        // no trade item
        return Optional.empty();
    }

    /**
     * Checks all of the values in main and merges them with the values in rec
     * @param main the CompoundNBT that serves as a template
     * @param rec the CompoundNBT that will be modified
     * @param depth start at 0
     * @return the modified CompoundNBT
     */
    public static CompoundTag merge(CompoundTag main, CompoundTag rec, int depth) {
        // prevent tags that are too deep
        if(depth >= 512) {
            return rec;
        }
        // iterate over all values in main tag
        for(String key : main.getAllKeys()) {
            Tag mnbt = main.get(key);
            // merge the tag value with the value in the other list
            if(mnbt instanceof CompoundTag) {
                CompoundTag merged = merge((CompoundTag) mnbt, rec.getCompound(key), depth + 1);
                rec.put(key, merged);
            } else if (mnbt instanceof ListTag) {
                ListTag mList = (ListTag) mnbt;
                ListTag rList = rec.getList(key, mList.getElementType());
                ListTag merged = merge(mList, rList);
                rec.put(key, merged);
            } else {
                rec.put(key, mnbt.copy());
            }
        }
        return rec;
    }

    public static ListTag merge(ListTag main, ListTag rec) {
        // iterate through all items in list
        for(int m = 0, ml = main.size(); m < ml; m++) {
            Tag mItem = main.get(m);
            boolean hasItem = false;
            // attempt to locate the same item in the other list
            for(int r = 0, rl = rec.size(); r < rl; r++) {
                Tag rItem = rec.get(r);
                if(NbtUtils.compareNbt(mItem, rItem, true)) {
                    hasItem = true;
                    break;
                }
            }
            // add the item to the other list
            if(!hasItem) {
                rec.add(mItem);
            }
        }
        return rec;
    }

    public ItemStack getAccept() {
        return accept;
    }

    public Optional<String> getAcceptTagString() {
        return acceptTagString;
    }

    public Optional<CompoundTag> getAcceptTag() {
        return acceptTag;
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

    public Optional<String> getTradeTagString() {
        return tradeTagString;
    }

    public Optional<CompoundTag> getTradeTag() {
        return tradeTag;
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
