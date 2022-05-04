package rpggods.perk;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.util.StringRepresentable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

import java.util.Optional;

public class Affinity {

    public static final Affinity EMPTY = new Affinity(Affinity.Type.PASSIVE, new ResourceLocation("null"));

    public static final Codec<Affinity> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Affinity.Type.CODEC.fieldOf("type").forGetter(Affinity::getType),
            ResourceLocation.CODEC.fieldOf("entity").forGetter(Affinity::getEntity)
    ).apply(instance, Affinity::new));

    private final Affinity.Type type;
    private final ResourceLocation entity;

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

    public Component getDisplayName() {
        return getType().getDisplayName();
    }

    public Component getDisplayDescription() {
        Optional<EntityType<?>> entityType = EntityType.byString(getEntity().toString());
        Component entityName = entityType.isPresent() ? entityType.get().getDescription() : new TextComponent(getEntity().toString());
        return getType().getDisplayDescription(entityName);
    }

    public static enum Type implements StringRepresentable {
        PASSIVE("passive"),
        HOSTILE("hostile"),
        FLEE("flee"),
        TAME("tame");

        private static final Codec<Affinity.Type> CODEC = Codec.STRING.comapFlatMap(Affinity.Type::fromString, Affinity.Type::getSerializedName).stable();
        private final String name;

        private Type(final String id) {
            name = id;
        }

        public static DataResult<Affinity.Type> fromString(String id) {
            for(final Affinity.Type t : values()) {
                if(t.getSerializedName().equals(id)) {
                    return DataResult.success(t);
                }
            }
            return DataResult.error("Failed to parse affinity type '" + id + "'");
        }

        public Component getDisplayName() {
            return new TranslatableComponent("favor.affinity." + getSerializedName());
        }

        public Component getDisplayDescription(Component entityName) {
            return new TranslatableComponent("favor.affinity." + getSerializedName() + ".description", entityName);
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
