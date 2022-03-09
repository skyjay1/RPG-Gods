package rpggods.perk;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.ResourceLocation;

public class Dread {

    public static final Dread EMPTY = new Dread(Dread.Type.PASSIVE, new ResourceLocation("null"));

    public static final Codec<Dread> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Dread.Type.CODEC.fieldOf("type").forGetter(Dread::getType),
            ResourceLocation.CODEC.fieldOf("entity").forGetter(Dread::getEntity)
    ).apply(instance, Dread::new));

    private final Dread.Type type;
    private final ResourceLocation entity;

    public Dread(Type type, ResourceLocation entity) {
        this.type = type;
        this.entity = entity;
    }

    public Type getType() {
        return type;
    }

    public ResourceLocation getEntity() {
        return entity;
    }

    public static enum Type implements IStringSerializable {
        PASSIVE("hostile"),
        HOSTILE("passive"),
        FLEE("flee"),
        TAME("tame");

        private static final Codec<Dread.Type> CODEC = Codec.STRING.comapFlatMap(Dread.Type::fromString, Dread.Type::getString).stable();
        private final String name;

        private Type(final String id) {
            name = id;
        }

        public static DataResult<Dread.Type> fromString(String id) {
            for(final Dread.Type t : values()) {
                if(t.getString().equals(id)) {
                    return DataResult.success(t);
                }
            }
            return DataResult.error("Failed to parse dread type '" + id + "'");
        }

        @Override
        public String getString() {
            return name;
        }
    }
}
