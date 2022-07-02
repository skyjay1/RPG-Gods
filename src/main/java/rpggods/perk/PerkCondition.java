package rpggods.perk;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.potion.Effect;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ItemTags;
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
import rpggods.RPGGods;
import rpggods.deity.Altar;
import rpggods.event.FavorEventHandler;
import rpggods.favor.IFavor;

import java.util.Optional;

public class PerkCondition {

    public static final Codec<PerkCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PerkCondition.Type.CODEC.fieldOf("type").forGetter(PerkCondition::getType),
            Codec.STRING.optionalFieldOf("data").forGetter(PerkCondition::getData),
            Codec.STRING.optionalFieldOf("tag").forGetter(PerkCondition::getTagString)
    ).apply(instance, PerkCondition::new));

    private final PerkCondition.Type type;
    private final Optional<String> data;
    private final Optional<String> tagString;
    private final Optional<CompoundNBT> tag;
    private final Optional<ResourceLocation> id;

    public PerkCondition(PerkCondition.Type type, Optional<String> data, Optional<String> tag) {
        this.type = type;
        this.data = data;
        this.tagString = tag;
        // parse ResourceLocation ID from data string
        if(data.isPresent() && data.get().contains(":")) {
            this.id = Optional.ofNullable(ResourceLocation.tryParse(getData().get()));
        } else {
            this.id = Optional.empty();
        }
        // parse NBT from tag string
        Optional<CompoundNBT> temp = Optional.empty();
        if(tagString.isPresent()) {
            try {
                temp = Optional.of(JsonToNBT.parseTag(tagString.get()));
            } catch (CommandSyntaxException e) {
                RPGGods.LOGGER.error("Failed to parse NBT in PerkCondition\n" + e.getMessage());
            }
        }
        this.tag = temp;
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

    public Optional<String> getTagString() {
        return tagString;
    }

    public Optional<CompoundNBT> getTag() {
        return tag;
    }

    /**
     * Checks if the player is in a specific biome
     * @param world the world
     * @param pos the player position
     * @return True if this condition has a biome and the position is in that biome
     */
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

    /**
     * Checks if the player is in a specific structure, according to the Chunk data
     * @param world the world
     * @param pos the player location
     * @return True if this condition has a structure and the position is inside the structure
     */
    public boolean isInStructure(final ServerWorld world, final BlockPos pos) {
        if(type == PerkCondition.Type.STRUCTURE && id.isPresent()) {
            Structure<?> structure = ForgeRegistries.STRUCTURE_FEATURES.getValue(id.get());
            if(structure != null) {
                return world.structureFeatureManager().getStructureAt(pos, true, structure).isValid();
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "PerkCondition: " + " type[" + type + "]" + " data[" + data + "]";
    }

    public ITextComponent getDisplayName() {
        return this.getType().getDisplayName(dataToDisplay(getData().orElse("")));
    }

    /**
     * Determines if the given perk condition is true.
     * @param deity the deity for which the perk is running
     * @param player the player
     * @param favor the player's favor
     * @param data a ResourceLocation associated with the perk calling this condition, if any
     * @param entityTag an Entity CompoundNBT associated with the perk calling this condition, if any
     * @return True if the PerkCondition passed
     */
    public boolean match(final ResourceLocation deity, final PlayerEntity player, final IFavor favor, 
                         final Optional<ResourceLocation> data, final Optional<CompoundNBT> entityTag) {
        boolean idMatch;
        boolean tagMatch;
        switch (this.getType()) {
            case PATRON: return favor.getPatron().isPresent() && deity.equals(favor.getPatron().get());
            case BIOME: return isInBiome(player.level, player.blockPosition());
            case DAY: return player.level.isDay();
            case NIGHT: return player.level.isNight();
            case RANDOM_TICK: return true;
            case ENTER_COMBAT: return player.getCombatTracker().getCombatDuration() < FavorEventHandler.COMBAT_TIMER;
            case PLAYER_CROUCHING: return player.isCrouching();
            case UNLOCKED: return getId().isPresent() && favor.getFavor(getId().get()).isEnabled();
            case MAINHAND_ITEM:
                // match item registry name
                ItemStack heldItem = player.getMainHandItem();
                idMatch = getId().isPresent() && getId().get().equals(heldItem.getItem().getRegistryName());
                // match item tag
                if(getData().isPresent() && getData().get().startsWith("#")) {
                    // match item tag
                    ResourceLocation tagId = ResourceLocation.tryParse(getData().get().substring(1));
                    ITag<Item> tag = ItemTags.getAllTags().getTagOrEmpty(tagId);
                    idMatch = tag.contains(heldItem.getItem());
                }
                // match nbt tag
                tagMatch = true;
                if(idMatch && tag.isPresent()) {
                    tagMatch = NBTUtil.compareNbt(tag.get(), heldItem.getTag(), true);
                    if(!tagMatch) {
                        RPGGods.LOGGER.debug("PerkCondition: Item NBT tags do not match: main=" + tag.get().getAsString() + " and item=" + heldItem.getTag().getAsString());
                    }
                }
                return idMatch && tagMatch;
            case PLAYER_INTERACT_BLOCK:
                if(data.isPresent()) {
                    // locate block from registry
                    Block block = ForgeRegistries.BLOCKS.getValue(data.get());
                    if(null == block) {
                        return false;
                    }
                    // match block registry name
                    idMatch = getId().isPresent() && getId().get().equals(data.get());
                    // match block tag
                    if(getData().isPresent() && getData().get().startsWith("#")) {
                        // match block tag
                        ResourceLocation tagId = ResourceLocation.tryParse(getData().get().substring(1));
                        ITag<Block> tag = BlockTags.getAllTags().getTagOrEmpty(tagId);
                        idMatch = tag.contains(block);
                    }
                    return idMatch;
                }
                return false;
            case PLAYER_RIDE_ENTITY:
                return player.isPassenger() && player.getVehicle() != null && getId().isPresent()
                        && getId().get().equals(player.getVehicle().getType().getRegistryName())
                        && (!tag.isPresent() || NBTUtil.compareNbt(tag.get(), entityTag.get(), true));
            case DIMENSION: return getId().isPresent() && getId().get().equals(player.level.dimension().location());
            case STRUCTURE: return player.level instanceof ServerWorld && isInStructure((ServerWorld) player.level, player.blockPosition());
            // match data to perk condition data
            case RITUAL:
            case EFFECT_START:
                return getId().isPresent() && data.isPresent() && getId().get().equals(data.get());
            // match data and NBT tag
            case ENTITY_HURT_PLAYER:
            case ENTITY_KILLED_PLAYER:
            case PLAYER_HURT_ENTITY:
            case PLAYER_KILLED_ENTITY:
            case PLAYER_INTERACT_ENTITY:
                idMatch = getId().isPresent() && data.isPresent() && getId().get().equals(data.get());
                tagMatch = true;
                if(idMatch && tag.isPresent()) {
                    tagMatch = entityTag.isPresent() && NBTUtil.compareNbt(tag.get(), entityTag.get(), true);
                    if(!tagMatch) {
                        RPGGods.LOGGER.debug("PerkCondition: Entity NBT tags do not match: main=" + tag.get().getAsString() + " and entity=" + entityTag.get().getAsString());
                    }
                }
                return idMatch && tagMatch;
        }
        return false;
    }

    private ITextComponent dataToDisplay(final String d) {
        ResourceLocation rl = ResourceLocation.tryParse(d);
        switch (getType()) {
            case PATRON: case UNLOCKED:
                return new TranslationTextComponent(Altar.createTranslationKey(rl));
            case MAINHAND_ITEM: case RITUAL:
                // display name of item tag
                if(d.startsWith("#")) {
                    return new TranslationTextComponent("favor.perk.condition.mainhand_item.tag", d);
                }
                // display name of item
                Item item = ForgeRegistries.ITEMS.getValue(rl);
                if(item != null) {
                    ItemStack itemStack = new ItemStack(item);
                    tag.ifPresent(nbt -> itemStack.setTag(nbt));
                    return itemStack.getItem().getName(itemStack);
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
