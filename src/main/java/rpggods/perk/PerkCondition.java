package rpggods.perk;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.nbt.TagParser;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.StringRepresentable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.registries.ForgeRegistries;
import rpggods.RPGGods;
import rpggods.deity.Altar;
import rpggods.entity.AltarEntity;
import rpggods.RGEvents;
import rpggods.favor.IFavor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public final class PerkCondition {

    public static final Codec<PerkCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PerkCondition.Type.CODEC.fieldOf("type").forGetter(PerkCondition::getType),
            Codec.STRING.optionalFieldOf("data").forGetter(PerkCondition::getData),
            Codec.STRING.optionalFieldOf("tag").forGetter(PerkCondition::getTagString)
    ).apply(instance, PerkCondition::new));

    private final PerkCondition.Type type;
    private final Optional<String> data;
    private final Optional<String> tagString;
    private final Optional<CompoundTag> tag;
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
        Optional<CompoundTag> temp = Optional.empty();
        if(tagString.isPresent()) {
            try {
                temp = Optional.of(TagParser.parseTag(tagString.get()));
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

    public Optional<CompoundTag> getTag() {
        return tag;
    }

    public Optional<String> getTagString() {
        return tagString;
    }

    /**
     * Checks if the player is in a specific biome
     * @param world the world
     * @param pos the player position
     * @return True if this condition has a biome and the position is in that biome
     */
    public boolean isInBiome(final Level world, final BlockPos pos) {
        // if biome data is present for this condition, make sure the biome matches
        if(type == PerkCondition.Type.BIOME && data.isPresent()) {
            final Holder<Biome> biome = world.getBiome(pos);
            if(id.isPresent()) {
                // interpret as a ResourceLocation
                // if the biome name does not match, the condition is false
                if(!biome.is(id.get())) {
                    return false;
                }
            } else {
                // interpret as a BiomeDictionary.TYPE
                final BiomeDictionary.Type type = BiomeDictionary.Type.getType(data.get());
                final Optional<ResourceKey<Biome>> resourceKey = biome.unwrapKey();
                // if the biome does not match the given type, the condition is false
                if(resourceKey.isPresent() && !BiomeDictionary.hasType(resourceKey.get(), type)) {
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
    public boolean isInStructure(final ServerLevel world, final BlockPos pos) {
        if(type == PerkCondition.Type.STRUCTURE && id.isPresent()) {
            StructureFeature<?> structure = ForgeRegistries.STRUCTURE_FEATURES.getValue(id.get());
            if(structure != null) {
                world.structureFeatureManager().getAllStructuresAt(pos);
                for(ConfiguredStructureFeature<?, ?> f : world.structureFeatureManager().getAllStructuresAt(pos).keySet()) {
                    if(id.get().equals(f.feature.getRegistryName())) {
                        return true;
                    }
                }
                return false;
            }
        }
        return false;
    }

    /**
     * @param level the level
     * @param origin the position of the event
     * @param distance the maximum distance to the altar
     * @return true if the origin is within the given distance to an altar to the given deity
     */
    public boolean isNearAltar(final Level level, final Vec3 origin, final double distance) {
        if(type == PerkCondition.Type.NEAR_ALTAR && id.isPresent()) {
            AABB aabb = new AABB(new BlockPos(origin)).inflate(distance, distance / 2.0D, distance);
            List<AltarEntity> altars = level.getEntities(EntityTypeTest.forClass(AltarEntity.class), aabb, a -> a.getDeity().isPresent() && id.get().equals(a.getDeity().get()));
            return !altars.isEmpty();
        }
        return false;
    }

    @Override
    public String toString() {
        return "PerkCondition: " + " type[" + type + "]" + " data[" + data + "]";
    }

    public Component getDisplayName() {
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
    public boolean match(final ResourceLocation deity, final Player player, final IFavor favor,
                         final Optional<ResourceLocation> data, final Optional<CompoundTag> entityTag) {
        boolean idMatch;
        boolean tagMatch;
        switch (this.getType()) {
            case PATRON: return favor.getPatron().isPresent() && deity.equals(favor.getPatron().get());
            case BIOME: return isInBiome(player.level, player.blockPosition());
            case DAY: return player.level.isDay();
            case NIGHT: return player.level.isNight();
            case RANDOM_TICK: return true;
            case ENTER_COMBAT: return player.getCombatTracker().getCombatDuration() < RGEvents.COMBAT_TIMER;
            case PLAYER_CROUCHING: return player.isCrouching();
            case UNLOCKED: return getId().isPresent() && favor.getFavor(getId().get()).isEnabled();
            case LEVEL_UP: case LEVEL_DOWN:
                return getId().isPresent() && deity.equals(getId().get());
            case MAINHAND_ITEM:
                // match item registry name
                ItemStack heldItem = player.getMainHandItem();
                idMatch = getId().isPresent() && getId().get().equals(heldItem.getItem().getRegistryName());
                // match item tag
                if(getData().isPresent() && getData().get().startsWith("#")) {
                    // match item tag
                    ResourceLocation tagId = ResourceLocation.tryParse(getData().get().substring(1));
                    TagKey<Item> tag = ItemTags.create(tagId);
                    idMatch = heldItem.is(tag);
                }
                // match nbt tag
                tagMatch = true;
                if(idMatch && tag.isPresent()) {
                    tagMatch = NbtUtils.compareNbt(tag.get(), heldItem.getTag(), true);
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
                        TagKey<Block> tagKey = BlockTags.create(tagId);
                        idMatch = block.defaultBlockState().is(tagKey);
                    }
                    return idMatch;
                }
                return false;
            case PLAYER_RIDE_ENTITY:
                return player.isPassenger() && player.getVehicle() != null && getId().isPresent()
                        && getId().get().equals(player.getVehicle().getType().getRegistryName())
                        && (!tag.isPresent() || NbtUtils.compareNbt(tag.get(), entityTag.get(), true));
            case DIMENSION: return getId().isPresent() && getId().get().equals(player.level.dimension().location());
            case STRUCTURE: return player.level instanceof ServerLevel && isInStructure((ServerLevel) player.level, player.blockPosition());
            // match data to perk condition data
            case RITUAL:
            case EFFECT_START:
                return getId().isPresent() && data.isPresent() && getId().get().equals(data.get());
            // match data and altar in range (hardcoded to 8 blocks)
            case NEAR_ALTAR: return getId().isPresent() && isNearAltar(player.level, player.position(), 8.0D);
            // match data and NBT tag
            case ENTITY_HURT_PLAYER:
            case ENTITY_KILLED_PLAYER:
            case PLAYER_HURT_ENTITY:
            case PLAYER_KILLED_ENTITY:
            case PLAYER_INTERACT_ENTITY:
                idMatch = getId().isPresent() && data.isPresent() && getId().get().equals(data.get());
                tagMatch = true;
                if(idMatch && tag.isPresent()) {
                    tagMatch = entityTag.isPresent() && NbtUtils.compareNbt(tag.get(), entityTag.get(), true);
                }
                return idMatch && tagMatch;
        }
        return false;
    }

    private Component dataToDisplay(final String d) {
        ResourceLocation rl = ResourceLocation.tryParse(d);
        switch (getType()) {
            case PATRON: case UNLOCKED: case NEAR_ALTAR: case LEVEL_UP: case LEVEL_DOWN:
                return new TranslatableComponent(Altar.createTranslationKey(rl));
            case MAINHAND_ITEM: case RITUAL:
                // display name of item tag
                if(d.startsWith("#")) {
                    return new TranslatableComponent("favor.perk.condition.mainhand_item.tag", d);
                }
                // display name of item
                Item item = ForgeRegistries.ITEMS.getValue(rl);
                if(item != null) {
                    ItemStack itemStack = new ItemStack(item);
                    tag.ifPresent(nbt -> itemStack.setTag(nbt));
                    return itemStack.getItem().getName(itemStack);
                }
                return new TextComponent(d);
            case BIOME:
                // read data as either biome name or biome dictionary type
                return d.contains(":")
                    ? new TranslatableComponent("biome." + rl.getNamespace() + "." + rl.getPath())
                    : new TextComponent(d);
            case PLAYER_INTERACT_BLOCK:
                if(!d.startsWith("#")) {
                    Block block = ForgeRegistries.BLOCKS.getValue(rl);
                    if(block != null) {
                        return block.getName();
                    }
                }
                return new TextComponent(d);
            case EFFECT_START:
                MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(rl);
                if(effect != null) {
                    return effect.getDisplayName();
                }
                return new TextComponent(d);
            case DIMENSION: return new TranslatableComponent("dimension." + rl.getNamespace() + "." + rl.getPath());
            case STRUCTURE: return new TranslatableComponent("structure." + rl.getNamespace() + "." + rl.getPath());
            case PLAYER_HURT_ENTITY: case PLAYER_KILLED_ENTITY: case ENTITY_HURT_PLAYER:
            case ENTITY_KILLED_PLAYER: case PLAYER_INTERACT_ENTITY: case PLAYER_RIDE_ENTITY:
                // read data as Entity ID
                Optional<EntityType<?>> entityType = EntityType.byString(d);
                return entityType.isPresent()
                        ? new TranslatableComponent(entityType.get().getDescriptionId())
                        : new TextComponent("<ERR>");
            case DAY: case NIGHT: case RANDOM_TICK: case ENTER_COMBAT: case PLAYER_CROUCHING: default:
                return TextComponent.EMPTY;
        }
    }

    /**
     * @param list the component list
     * @param color the text component formatting
     * @param blacklist perk condition types to not include in the list
     * @return a list of Components, one for each perk condition, with plurality and formatting
     */
    public static List<Component> formatDescriptions(List<PerkCondition> list, ChatFormatting color, Collection<PerkCondition.Type> blacklist) {
        List<Component> perkConditions = new ArrayList<>();
        // add perk condition texts
        for(PerkCondition condition : list) {
            // do not show ommitted conditions
            if(!blacklist.contains(condition.getType())) {
                perkConditions.add(condition.getDisplayName().copy().withStyle(color));
            }
        }
        // add prefix to each condition based on plurality
        if (perkConditions.size() > 0) {
            // add prefix to first condition
            Component t2 = new TranslatableComponent("favor.perk.condition.single", perkConditions.get(0))
                    .withStyle(color);
            perkConditions.set(0, t2);
            // add prefix to following conditions
            for (int i = 1, l = perkConditions.size(); i < l; i++) {
                t2 = new TranslatableComponent("favor.perk.condition.multiple", perkConditions.get(i))
                        .withStyle(color);
                perkConditions.set(i, t2);
            }
        }
        return perkConditions;
    }

    public static enum Type implements StringRepresentable {
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
        ENTER_COMBAT("enter_combat"),
        NEAR_ALTAR("near_altar"),
        LEVEL_UP("level_up"),
        LEVEL_DOWN("level_down");

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

        public Component getDisplayName(Component data) {
            return new TranslatableComponent("favor.perk.condition." + getSerializedName(), data);
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
