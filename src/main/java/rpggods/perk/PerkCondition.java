package rpggods.perk;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Effect;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.registries.ForgeRegistries;
import rpggods.deity.Altar;

import java.util.Optional;

public class PerkCondition {

    public static final Codec<PerkCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PerkCondition.Type.CODEC.fieldOf("type").forGetter(PerkCondition::getType),
            Codec.STRING.optionalFieldOf("data").forGetter(PerkCondition::getData)
    ).apply(instance, PerkCondition::new));

    private final PerkCondition.Type type;
    private final Optional<String> data;
    private final Optional<ResourceLocation> id;

    public PerkCondition(PerkCondition.Type type, Optional<String> data) {
        this.type = type;
        this.data = data;
        if(data.isPresent() && data.get().contains(":")) {
            id = Optional.ofNullable(ResourceLocation.tryParse(getData().get()));
        } else {
            id = Optional.empty();
        }
    }

    public PerkCondition.Type getType() {
        return type;
    }

    public Optional<String> getData() {
        return data;
    }

    public Optional<ResourceLocation> getId() {
        return id;
    }

    public boolean isInBiome(final World world, final BlockPos pos) {
        // if biome data is present for this condition, make sure the biome matches
        if(type == PerkCondition.Type.BIOME && data.isPresent()) {
            final Optional<RegistryKey<Biome>> biome = world.getBiomeName(pos);
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

    public boolean isInStructure(final ServerWorld world, final BlockPos pos) {
        if(type == PerkCondition.Type.STRUCTURE && id.isPresent()) {
            Structure<?> structure = ForgeRegistries.STRUCTURE_FEATURES.getValue(id.get());
            if(structure != null) {
                return world.structureFeatureManager().getStructureAt(pos, true, structure).isValid();
            }
        }
        return false;
    }

    public static DataResult<PerkCondition> fromType(PerkCondition.Type type) {
        return DataResult.success(new PerkCondition(type, Optional.empty()));
    }

    @Override
    public String toString() {
        return "PerkCondition: " + " type[" + type + "]" + " data[" + data + "]";
    }

    public ITextComponent getDisplayName() {
        return this.getType().getDisplayName(dataToDisplay(getData().orElse("")));
    }

    private ITextComponent dataToDisplay(final String d) {
        ResourceLocation rl = ResourceLocation.tryParse(d);
        switch (getType()) {
            case PATRON: case UNLOCKED:
                return new TranslationTextComponent(Altar.createTranslationKey(rl));
            case MAINHAND_ITEM: case RITUAL:
                Item item = ForgeRegistries.ITEMS.getValue(rl);
                if(item != null) {
                    return new ItemStack(item).getDisplayName();
                }
                return new StringTextComponent(d);
            case BIOME:
                // read data as either biome name or biome dictionary type
                return d.contains(":")
                    ? new TranslationTextComponent("biome." + rl.getNamespace() + "." + rl.getPath())
                    : new StringTextComponent(d);
            case PLAYER_INTERACT_BLOCK:
                if(!d.startsWith("#")) {
                    Block block = ForgeRegistries.BLOCKS.getValue(rl);
                    if(block != null) {
                        return block.getName();
                    }
                }
                return new StringTextComponent(d);
            case EFFECT_START:
                Effect effect = ForgeRegistries.POTIONS.getValue(rl);
                if(effect != null) {
                    return effect.getDisplayName();
                }
                return new StringTextComponent(d);
            case DIMENSION: return new TranslationTextComponent("dimension." + rl.getNamespace() + "." + rl.getPath());
            case STRUCTURE: return new TranslationTextComponent("structure." + rl.getNamespace() + "." + rl.getPath());
            case PLAYER_HURT_ENTITY: case PLAYER_KILLED_ENTITY: case ENTITY_HURT_PLAYER:
            case ENTITY_KILLED_PLAYER: case PLAYER_INTERACT_ENTITY: case PLAYER_RIDE_ENTITY:
                // read data as Entity ID
                Optional<EntityType<?>> entityType = EntityType.byString(d);
                return entityType.isPresent()
                        ? new TranslationTextComponent(entityType.get().getDescriptionId())
                        : new StringTextComponent("<ERR>");
            case DAY: case NIGHT: case RANDOM_TICK: case ENTER_COMBAT: case PLAYER_CROUCHING: default:
                return StringTextComponent.EMPTY;
        }
    }

    public static enum Type implements IStringSerializable {
        PATRON("patron"),
        BIOME("biome"),
        DAY("day"),
        NIGHT("night"),
        RANDOM_TICK("random_tick"),
        MAINHAND_ITEM("mainhand_item"),
        STRUCTURE("structure"),
        DIMENSION("dimension"),
        EFFECT_START("effect_start"),
        ENTITY_HURT_PLAYER("entity_hurt_player"),
        ENTITY_KILLED_PLAYER("entity_killed_player"),
        PLAYER_HURT_ENTITY("player_hurt_entity"),
        PLAYER_KILLED_ENTITY("player_killed_entity"),
        PLAYER_INTERACT_ENTITY("player_interact_entity"),
        PLAYER_INTERACT_BLOCK("player_interact_block"),
        PLAYER_RIDE_ENTITY("player_ride_entity"),
        PLAYER_CROUCHING("player_crouching"),
        RITUAL("ritual"),
        UNLOCKED("unlocked"),
        ENTER_COMBAT("enter_combat");

        private static final Codec<PerkCondition.Type> CODEC = Codec.STRING.comapFlatMap(PerkCondition.Type::fromString, PerkCondition.Type::getSerializedName).stable();
        private final String name;

        private Type(final String id) {
            name = id;
        }

        public static DataResult<Type> fromString(String id) {
            for(final PerkCondition.Type t : values()) {
                if(t.getSerializedName().equals(id)) {
                    return DataResult.success(t);
                }
            }
            return DataResult.error("Failed to parse perk condition '" + id + "'");
        }

        public ITextComponent getDisplayName(ITextComponent data) {
            return new TranslationTextComponent("favor.perk.condition." + getSerializedName(), data);
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
