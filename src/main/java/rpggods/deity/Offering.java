package rpggods.deity;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraftforge.registries.ForgeRegistries;
import rpggods.RPGGods;
import rpggods.util.Cooldown;

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
