package rpggods.perk;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.ResourceLocation;

public class PerkTrigger {

    public static final PerkTrigger EMPTY = new PerkTrigger(Type.ENTITY_KILLED_PLAYER, new ResourceLocation("null"));

    public static final Codec<PerkTrigger> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PerkTrigger.Type.CODEC.fieldOf("type").forGetter(PerkTrigger::getType),
            ResourceLocation.CODEC.fieldOf("data").forGetter(PerkTrigger::getData)
    ).apply(instance, PerkTrigger::new));

    private final PerkTrigger.Type type;
    private final ResourceLocation data;

    public PerkTrigger(PerkTrigger.Type typeIn, ResourceLocation dataIn) {
        super();
        this.type = typeIn;
        this.data = dataIn;
    }

    /** @return the trigger type **/
    public PerkTrigger.Type getType() { return type; }

    /**
     * @return the data associated with this trigger.
     * Depending on the FavorEffectTrigger.Type, this could be
     * a potion id or an entity id
     */
    public ResourceLocation getData() { return data; }

    public static enum Type implements IStringSerializable {
        ENTITY_HURT_PLAYER("entity_hurt_player"),
        ENTITY_KILLED_PLAYER("entity_killed_player"),
        PLAYER_HURT_ENTITY("player_hurt_entity"),
        PLAYER_KILLED_ENTITY("player_killed_entity");

        private static final Codec<PerkTrigger.Type> CODEC = Codec.STRING.comapFlatMap(PerkTrigger.Type::fromString, PerkTrigger.Type::getString).stable();

        private final String name;

        private Type(final String id) {
            name = id;
        }

        public static DataResult<PerkTrigger.Type> fromString(String id) {
            for(final PerkTrigger.Type t : values()) {
                if(t.getString().equals(id)) {
                    return DataResult.success(t);
                }
            }
            return DataResult.error("Failed to parse perk trigger '" + id + "'");
        }

        @Override
        public String getString() {
            return name;
        }
    }
}
