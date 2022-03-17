package rpggods.perk;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.EntityType;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.Optional;

public class Affinity {

    public static final Affinity EMPTY = new Affinity(Affinity.Type.PASSIVE, new ResourceLocation("null"));

    public static final Codec<Affinity> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Affinity.Type.CODEC.fieldOf("type").forGetter(Affinity::getType),
            ResourceLocation.CODEC.fieldOf("entity").forGetter(Affinity::getEntity)
    ).apply(instance, Affinity::new));

    private final Affinity.Type type;
    private final ResourceLocation entity;
    private ITextComponent translationKey;

    public Affinity(Type type, ResourceLocation entity) {
        this.type = type;
        this.entity = entity;
    }

    public Type getType() {
        return type;
    }

    public ResourceLocation getEntity() {
        return entity;
    }

    public IFormattableTextComponent getDisplayName() {
        Optional<EntityType<?>> entityType = EntityType.byKey(getEntity().toString());
        ITextComponent entityName = entityType.isPresent() ? entityType.get().getName() : new StringTextComponent(getEntity().toString());
        return new TranslationTextComponent("favor.affinity",
                entityName, getType().getDisplayName());
    }

    public static enum Type implements IStringSerializable {
        PASSIVE("passive"),
        HOSTILE("hostile"),
        FLEE("flee"),
        TAME("tame");

        private static final Codec<Affinity.Type> CODEC = Codec.STRING.comapFlatMap(Affinity.Type::fromString, Affinity.Type::getString).stable();
        private final String name;

        private Type(final String id) {
            name = id;
        }

        public static DataResult<Affinity.Type> fromString(String id) {
            for(final Affinity.Type t : values()) {
                if(t.getString().equals(id)) {
                    return DataResult.success(t);
                }
            }
            return DataResult.error("Failed to parse affinity type '" + id + "'");
        }

        public ITextComponent getDisplayName() {
            return new TranslationTextComponent("favor.affinity." + getString());
        }

        @Override
        public String getString() {
            return name;
        }
    }
}
