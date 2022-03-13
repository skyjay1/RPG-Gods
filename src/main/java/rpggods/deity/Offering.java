package rpggods.deity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.Optional;

public class Offering {
    public static final Offering EMPTY = new Offering(new ResourceLocation("null"), ItemStack.EMPTY, 0,
            Optional.empty(), 0, Optional.empty());

    public static final Codec<Offering> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("deity").forGetter(Offering::getDeity),
            //Codec.either(ResourceLocation.CODEC, ItemStack.CODEC)
                    //.xmap(either -> either.map(rl -> ForgeRegistries.ITEMS.getValue(rl), stack -> stack.getItem().getRegistryName()))
            ItemStack.CODEC
                    .fieldOf("item").forGetter(Offering::getAccept),
            Codec.INT.optionalFieldOf("favor", 0).forGetter(Offering::getFavor),
            ItemStack.CODEC.optionalFieldOf("trade").forGetter(Offering::getTrade),
            Codec.INT.optionalFieldOf("minlevel", 0).forGetter(Offering::getTradeMinLevel),
            ResourceLocation.CODEC.optionalFieldOf("function").forGetter(Offering::getFunction)
    ).apply(instance, Offering::new));

    private final ResourceLocation deity;
    private final ItemStack accept;
    private final int favor;
    private final Optional<ItemStack> trade;
    private final int tradeMinLevel;
    private final Optional<ResourceLocation> function;

    public Offering(ResourceLocation deity, ItemStack accept, int favor, Optional<ItemStack> trade,
                    int tradeMinLevel, Optional<ResourceLocation> function) {
        this.deity = deity;
        this.accept = accept;
        this.favor = favor;
        this.trade = trade;
        this.tradeMinLevel = tradeMinLevel;
        this.function = function;
    }

    public ResourceLocation getDeity() {
        return deity;
    }

    public ItemStack getAccept() {
        return accept;
    }

    public int getFavor() {
        return favor;
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
}
