package rpggods.perk;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;

import java.util.Optional;

public class PerkCondition {

    public static final Codec<PerkCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PerkCondition.Type.CODEC.fieldOf("type").forGetter(PerkCondition::getType),
            Codec.STRING.optionalFieldOf("data").forGetter(PerkCondition::getData)
    ).apply(instance, PerkCondition::new));

    private PerkCondition.Type type;
    private Optional<String> data;

    public PerkCondition(PerkCondition.Type type, Optional<String> data) {
        this.type = type;
        this.data = data;
    }

    public PerkCondition.Type getType() {
        return type;
    }

    public Optional<String> getData() {
        return data;
    }

    public boolean isInBiome(final World world, final BlockPos pos) {
        // if biome data is present for this condition, make sure the biome matches
        if(type == PerkCondition.Type.BIOME && data.isPresent()) {
            final Optional<RegistryKey<Biome>> biome = world.func_242406_i(pos);
            if(data.get().contains(":")) {
                // interpret as a ResourceLocation
                // if the biome name does not match, the condition is false
                if(biome.isPresent() && !biome.get().getRegistryName().toString().equals(data.get())) {
                    return false;
                }
            } else {
                // interpret as a BiomeDictionary.TYPE
                final BiomeDictionary.Type type = BiomeDictionary.Type.getType(data.get());
                // if the biome does not match the given type, the condition is false
                if(biome.isPresent() && !BiomeDictionary.hasType(biome.get(), type)) {
                    return false;
                }
            }
        }
        // if the condition is not biome type OR it passed the tests, the condition is true
        return true;
    }

    public static enum Type implements IStringSerializable {
        PATRON("patron"),
        BIOME("biome"),
        DAY("day"),
        NIGHT("night");

        private static final Codec<PerkCondition.Type> CODEC = Codec.STRING.comapFlatMap(PerkCondition.Type::fromString, PerkCondition.Type::getString).stable();
        private final String name;

        private Type(final String id) {
            name = id;
        }

        public static DataResult<Type> fromString(String id) {
            for(final PerkCondition.Type t : values()) {
                if(t.getString().equals(id)) {
                    return DataResult.success(t);
                }
            }
            return DataResult.error("Failed to parse perk condition '" + id + "'");
        }

        @Override
        public String getString() {
            return name;
        }
    }
}
