package rpggods.event;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.IGrowable;
import net.minecraft.block.material.Material;
import net.minecraft.command.CommandSource;
import net.minecraft.command.FunctionObject;
import net.minecraft.entity.AgeableEntity;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.entity.item.ExperienceOrbEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.monster.DrownedEntity;
import net.minecraft.entity.monster.GuardianEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.WaterMobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.SpectralArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.IParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.pathfinding.SwimmerPathNavigator;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.server.MinecraftServer;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.util.concurrent.TickDelayedTask;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import rpggods.RPGGods;
import rpggods.deity.Deity;
import rpggods.deity.Offering;
import rpggods.deity.Sacrifice;
import rpggods.entity.AltarEntity;
import rpggods.entity.ai.AffinityGoal;
import rpggods.favor.IFavor;
import rpggods.perk.Affinity;
import rpggods.perk.Perk;
import rpggods.perk.PerkCondition;
import rpggods.perk.PerkData;
import rpggods.tameable.ITameable;
import rpggods.util.Cooldown;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

public class FavorEventHandler {

    private static final int COMBAT_TIMER = 40;

    /**
     * Called when the player attempts to give an offering
     * @param deity the deity ID
     * @param player the player
     * @param favor the player's favor
     * @param item the item being offered
     * @return the ItemStack to replace the one provided, if any
     */
    public static Optional<ItemStack> onOffering(final Optional<AltarEntity> entity, final ResourceLocation deity, final PlayerEntity player, final IFavor favor, final ItemStack item) {
        if(favor.isEnabled() && !item.isEmpty()) {
            // find first matching offering for the given deity
            ResourceLocation offeringId = null;
            Offering offering = null;
            for(ResourceLocation id : RPGGods.DEITY.get(deity).offeringMap.getOrDefault(item.getItem().getRegistryName(), ImmutableList.of())) {
                Offering o = RPGGods.OFFERING.get(id).orElse(null);
                if(o != null && item.getCount() >= o.getAccept().getCount()
                        && ItemStack.isSame(item, o.getAccept())
                        && ItemStack.tagMatches(item, o.getAccept())) {
                    offeringId = id;
                    offering = o;
                    break;
                }
            }
            // process the offering
            if(offering != null && offeringId != null && favor.getOfferingCooldown(offeringId).canUse()) {
                favor.getFavor(deity).addFavor(player, deity, offering.getFavor(), FavorChangedEvent.Source.OFFERING);
                favor.getOfferingCooldown(offeringId).addUse();
                offering.getFunction().ifPresent(f -> runFunction(player.level, player, f));
                // shrink item stack
                if(!player.isCreative()) {
                    item.shrink(offering.getAccept().getCount());
                }
                // process trade, if any
                if(offering.getTrade().isPresent() && favor.getFavor(deity).getLevel() >= offering.getTradeMinLevel()) {
                    ItemEntity itemEntity = new ItemEntity(player.level, player.getX(), player.getY(), player.getZ(), offering.getTrade().get().copy());
                    itemEntity.setNoPickUpDelay();
                    player.level.addFreshEntity(itemEntity);
                }
                // particles
                if(entity.isPresent() && player.level instanceof ServerWorld) {
                    Vector3d pos = Vector3d.atBottomCenterOf(entity.get().blockPosition().above());
                    IParticleData particle = offering.getFavor() >= 0 ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.ANGRY_VILLAGER;
                    ((ServerWorld)player.level).sendParticles(particle, pos.x, pos.y, pos.z, 8, 0.5D, 0.5D, 0.5D, 0);
                }
                return Optional.of(item);
            }
        }


        return Optional.empty();
    }

    /**
     * Called when the player kills a living entity
     * @param player the player
     * @param favor the player's favor
     * @param entity the entity that was killed
     * @return true if the player's favor was modified
     */
    public static boolean onSacrifice(final PlayerEntity player, final IFavor favor, final LivingEntity entity) {
        boolean success = false;
        if(favor.isEnabled()) {
            // find and process all matching sacrifices
            ResourceLocation entityId = entity.getType().getRegistryName();
            Sacrifice sacrifice;
            Cooldown cooldown;
            for(Map.Entry<ResourceLocation, Optional<Sacrifice>> entry : RPGGods.SACRIFICE.getEntries()) {
                if(entry.getValue() != null && entry.getValue().isPresent()) {
                    sacrifice = entry.getValue().get();
                    if(sacrifice != null && entityId.equals(sacrifice.getEntity())) {
                        cooldown = favor.getSacrificeCooldown(entry.getKey());
                        if(cooldown.canUse()) {
                            cooldown.addUse();
                            favor.getFavor(sacrifice.getDeity()).addFavor(player, sacrifice.getDeity(), sacrifice.getFavor(), FavorChangedEvent.Source.SACRIFICE);
                            sacrifice.getFunction().ifPresent(f -> runFunction(player.level, player, f));
                        }
                    }
                }
            }
        }
        return success;
    }

    public static boolean triggerCondition(final PerkCondition.Type type, final PlayerEntity player, final IFavor favor,
                                           final Optional<Entity> entity, final Optional<ResourceLocation> data,
                                           final Optional<? extends Event> object) {
        boolean success = false;
        if(favor.isEnabled()) {
            // find matching perks (use set to ensure no duplicates)
            Set<ResourceLocation> perks = new HashSet<>();
            for(Deity deity : RPGGods.DEITY.values()) {
                perks.addAll(deity.perkByConditionMap.getOrDefault(type, ImmutableList.of()));
            }
            // shuffle perks
            List<ResourceLocation> perkList = Lists.newArrayList(perks);
            Collections.shuffle(perkList);
            // run each perk
            Perk perk;
            for(ResourceLocation id : perkList) {
                perk = RPGGods.PERK.get(id).orElse(null);
                success |= runPerk(perk, player, favor, entity, data, object);
            }
        }
        return success;
    }

    /**
     * Loads all perks with the given {@link PerkData.Type} and attempts to run each one.
     * @param type the action type (eg, function, item, potion, summon, arrow, xp)
     * @param player the player
     * @param favor the player's favor
     * @param entity an entity to use when running the perk, if any
     * @return True if at least one perk ran
     */
    public static boolean triggerPerks(final PerkData.Type type, final PlayerEntity player, final IFavor favor, final Optional<Entity> entity) {
        return triggerPerks(type, player, favor, entity, Optional.empty(), Optional.empty());
    }

    /**
     * Loads all perks with the given {@link PerkData.Type} and attempts to run each one.
     * @param type the action type (eg, function, item, potion, summon, arrow, xp)
     * @param player the player
     * @param favor the player's favor
     * @param entity an entity to use when running the perk, if any
     * @param data a ResourceLocation ID to use when running the perk, if any
     * @param object the Event to reference when running the perk, if any
     * @return True if at least one perk ran
     */
    public static boolean triggerPerks(final PerkData.Type type, final PlayerEntity player, final IFavor favor,
                                       final Optional<Entity> entity, final Optional<ResourceLocation> data,
                                       final Optional<? extends Event> object) {
        boolean success = false;
        if(favor.isEnabled()) {
            // find matching perks
            List<ResourceLocation> perks = new ArrayList<>();
            for(Deity deity : RPGGods.DEITY.values()) {
                perks.addAll(deity.perkByTypeMap.getOrDefault(type, ImmutableList.of()));
            }
            // shuffle perks
            Collections.shuffle(perks);
            // run each perk
            Perk perk;
            for(ResourceLocation id : perks) {
                perk = RPGGods.PERK.get(id).orElse(null);
                success |= runPerk(perk, player, favor, entity, data, object);
            }
        }
        return success;
    }

    /**
     * Loads and runs a single function at the entity position
     * @param worldIn the world
     * @param entity the entity (for example, a player)
     * @param functionId the function ID of a function to run
     * @return true if the function ran successfully
     */
    public static boolean runFunction(final World worldIn, final LivingEntity entity, ResourceLocation functionId) {
        final MinecraftServer server = worldIn.getServer();
        if(server != null) {
            final net.minecraft.advancements.FunctionManager manager = server.getFunctions();
            final Optional<FunctionObject> function = manager.get(functionId);
            if(function.isPresent()) {
                final CommandSource commandSource = manager.getGameLoopSender()
                        .withEntity(entity)
                        .withPosition(entity.position())
                        .withPermission(4)
                        .withSuppressedOutput();
                manager.execute(function.get(), commandSource);
                return true;
            }
        }
        return false;
    }

    /**
     * Attempts to run a single perk and sets a cooldown if successful.
     * Checks favor range, cooldown, random chance, and conditions before running the perk.
     * @param perk the Perk to run
     * @param player the player to affect
     * @param favor the player's favor
     * @return True if the perk was run and cooldown was added.
     * @see #runPerk(Perk, PlayerEntity, IFavor, Optional, Optional, Optional)
     */
    public static boolean runPerk(final Perk perk, final PlayerEntity player, final IFavor favor) {
        return runPerk(perk, player, favor, Optional.empty(), Optional.empty(), Optional.empty());
    }

    /**
     * Attempts to run a single perk and sets a cooldown if successful.
     * Checks favor range, cooldown, random chance, and conditions before running the perk.
     * @param perk the Perk to run
     * @param player the player to affect
     * @param favor the player's favor
     * @param entity an entity to use when running the perk, if any
     * @param data a ResourceLocation ID to use when running the perk, if any
     * @param object the Event to reference when running the perk, if any
     * @return True if the perk was run and cooldown was added.
     */
    public static boolean runPerk(final Perk perk, final PlayerEntity player, final IFavor favor, final Optional<Entity> entity,
                                  final Optional<ResourceLocation> data, final Optional<? extends Event> object) {
        // check favor range, perk cooldown, and random chance
        if(perk != null && !player.level.isClientSide && perk.getRange().isInRange(favor)
                && favor.hasNoPerkCooldown(perk.getCategory()) && Math.random() < perk.getChance()) {
            // check perk conditions
            for(final PerkCondition condition : perk.getConditions()) {
                if(!matchCondition(perk.getDeity(), condition, player, favor, data)) {
                    return false;
                }
            }
            // run the perk
            // DEBUG
            RPGGods.LOGGER.debug("Running perk " + perk);
            boolean success = false;
            for(final PerkData action : perk.getActions()) {
                success |= runPerkAction(perk.getDeity(), action, player, favor, entity, data, object);
            }
            if(success) {
                // apply cooldown
                long cooldown = (long) Math.floor(perk.getCooldown() * (1.0D + Math.random() * 0.25D));
                favor.setPerkCooldown(perk.getCategory(), cooldown);
                return true;
            }
        }

        return false;
    }

    /**
     * Runs a single Perk without any of the preliminary checks or cooldown.
     * If you want these, call {@link #runPerk(Perk, PlayerEntity, IFavor)} or
     * {@link #runPerk(Perk, PlayerEntity, IFavor, Optional, Optional, Optional)} instead.
     * @param deity the Deity that is associated with the perk
     * @param action information about the action to perform
     * @param player the player
     * @param favor the player's favor
     * @param entity an entity to use when running the perk, if any
     * @param data a ResourceLocation ID to use when running the perk, if any
     * @param object the Event to reference when running the perk, if any
     * @return True if the action ran successfully
     */
    public static boolean runPerkAction(final ResourceLocation deity, final PerkData action, final PlayerEntity player, final IFavor favor,
                                        final Optional<Entity> entity, final Optional<ResourceLocation> data, final Optional<? extends Event> object) {
        switch (action.getType()) {
            case FUNCTION: return action.getId().isPresent() && runFunction(player.level, player, action.getId().get());
            case POTION:
                if(action.getTag().isPresent()) {
                    Optional<EffectInstance> effect = readEffectInstance(action.getTag().get());
                    if(effect.isPresent()) {
                        return player.addEffect(effect.get());
                    }
                }
                return false;
            case SUMMON:
                return action.getTag().isPresent() && summonEntityNearPlayer(player.level, player, action.getTag()).isPresent();
            case ITEM: if(action.getItem().isPresent()) {
                    ItemEntity itemEntity = new ItemEntity(player.level, player.getX(), player.getY(), player.getZ(), action.getItem().get().copy());
                    itemEntity.setNoPickUpDelay();
                    return player.level.addFreshEntity(itemEntity);
                }
                return false;
            case FAVOR: return action.getFavor().isPresent() && action.getId().isPresent()
                    && favor.getFavor(action.getId().get()).addFavor(player, action.getId().get(), action.getFavor().get(), FavorChangedEvent.Source.PERK)
                        != favor.getFavor(action.getId().get()).getFavor();
            case AFFINITY:
                if(action.getAffinity().isPresent() && entity.isPresent() && data.isPresent() && action.getAffinity().get().getType() == Affinity.Type.TAME) {
                    LazyOptional<ITameable> tameable = entity.get().getCapability(RPGGods.TAMEABLE);
                    if(tameable.isPresent()) {
                        if(tameable.orElse(null).setTamedBy(player)) {
                            entity.get().setCustomName(entity.get().getDisplayName());
                            if(entity.get().level instanceof ServerWorld) {
                                Vector3d pos = entity.get().getEyePosition(1.0F);
                                ((ServerWorld)entity.get().level).sendParticles(ParticleTypes.HEART, pos.x, pos.y, pos.z, 10, 0.5D, 0.5D, 0.5D, 0);
                            }
                            return true;
                        }
                    }
                }
                return false;
            case ARROW_DAMAGE:
                if(entity.isPresent() && action.getMultiplier().isPresent() && entity.get() instanceof ArrowEntity) {
                    ArrowEntity arrow = (ArrowEntity) entity.get();
                    arrow.setBaseDamage(arrow.getBaseDamage() * action.getMultiplier().get());
                    return true;
                }
                return false;
            case ARROW_EFFECT:
                if(entity.isPresent() && action.getTag().isPresent() && entity.get() instanceof ArrowEntity) {
                    ArrowEntity arrow = (ArrowEntity) entity.get();
                    readEffectInstance(action.getTag().get()).ifPresent(e -> arrow.addEffect(e));
                    return true;
                }
                return false;
            case ARROW_COUNT:
                if(entity.isPresent() && action.getMultiplier().isPresent() && entity.get() instanceof AbstractArrowEntity) {
                    AbstractArrowEntity arrow = (AbstractArrowEntity) entity.get();
                    int arrowCount = (int)Math.round(action.getMultiplier().get());
                    double motionScale = 0.8;
                    for(int i = 0; i < arrowCount; i++) {
                        AbstractArrowEntity arrow2 = (AbstractArrowEntity) arrow.getType().create(arrow.level);
                        arrow2.copyPosition(arrow);
                        arrow2.setDeltaMovement(arrow.getDeltaMovement().multiply(
                                (Math.random() * 2.0D - 1.0D) * motionScale,
                                (Math.random() * 2.0D - 1.0D) * motionScale,
                                (Math.random() * 2.0D - 1.0D) * motionScale));
                        arrow2.pickup = AbstractArrowEntity.PickupStatus.CREATIVE_ONLY;
                        arrow.level.addFreshEntity(arrow2);
                    }
                    return true;
                }
                return false;
            case OFFSPRING:
                if(action.getMultiplier().isPresent() && entity.isPresent() && entity.get() instanceof AgeableEntity
                        && object.isPresent() && object.get() instanceof BabyEntitySpawnEvent) {
                    int childCount = Math.round(action.getMultiplier().get());
                    if(childCount < 1) {
                        // number of babies is zero, so cancel the event
                        ((BabyEntitySpawnEvent)object.get()).setCanceled(true);
                        if(entity.get().level instanceof ServerWorld) {
                            Vector3d pos = entity.get().getEyePosition(1.0F);
                            ((ServerWorld)entity.get().level).sendParticles(ParticleTypes.ANGRY_VILLAGER, pos.x, pos.y, pos.z, 6, 0.5D, 0.5D, 0.5D, 0);
                        }
                    } else if(childCount > 1) {
                        // number of babies is more than one, so spawn additional mobs
                        AgeableEntity parent = (AgeableEntity) entity.get();
                        for(int i = 1; i < childCount; i++) {
                            AgeableEntity bonusChild = (AgeableEntity) parent.getType().create(parent.level);
                            if(bonusChild != null) {
                                bonusChild.copyPosition(parent);
                                bonusChild.setBaby(true);
                                parent.level.addFreshEntity(bonusChild);
                                if(parent.level instanceof ServerWorld) {
                                    Vector3d pos = bonusChild.getEyePosition(1.0F);
                                    ((ServerWorld)parent.level).sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.x, pos.y, pos.z, 8, 0.5D, 0.5D, 0.5D, 0);
                                }
                            }
                        }
                    }
                    return true;
                }
                return false;
            case CROP_GROWTH:
                if(action.getMultiplier().isPresent()) {
                    return growCropsNearPlayer(player, favor, Math.round(action.getMultiplier().get()));
                }
                return false;
            case CROP_HARVEST:
                // This is handled using loot table modifiers
                return action.getMultiplier().isPresent();
            case AUTOSMELT:
            case UNSMELT:
                // These are handled using loot table modifiers
                return true;
            case SPECIAL_PRICE:
                break;
            case XP:
                if(entity.isPresent() && action.getMultiplier().isPresent() && entity.get() instanceof ExperienceOrbEntity) {
                    ((ExperienceOrbEntity)entity.get()).value *= action.getMultiplier().get();
                    return true;
                }
                return false;
        }
        return false;
    }

    /**
     * Determines if the given
     * @param deity the deity for which the perk is running
     * @param condition the PerkCondition
     * @param player the player
     * @param favor the player's favor
     * @return True if the PerkCondition passed
     */
    public static boolean matchCondition(final ResourceLocation deity, final PerkCondition condition,
                                         final PlayerEntity player, final IFavor favor, final Optional<ResourceLocation> data) {
        switch (condition.getType()) {
            case PATRON: return favor.getPatron().isPresent() && deity.equals(favor.getPatron().get());
            case BIOME: return condition.isInBiome(player.level, player.blockPosition());
            case DAY: return player.level.isDay();
            case NIGHT: return player.level.isNight();
            case RANDOM_TICK: return true;
            case ENTER_COMBAT: return player.getCombatTracker().getCombatDuration() < COMBAT_TIMER;
            case MAINHAND_ITEM: return data.isPresent() && data.get().equals(player.getMainHandItem().getItem().getRegistryName());
            case PLAYER_RIDE_ENTITY: return player.isPassenger() && player.getVehicle() != null
                    && data.isPresent() && data.equals(player.getVehicle().getType().getRegistryName());
            // match data to perk condition data
            case ENTITY_HURT_PLAYER:
            case ENTITY_KILLED_PLAYER:
            case PLAYER_HURT_ENTITY:
            case PLAYER_KILLED_ENTITY:
            case PLAYER_INTERACT_ENTITY:
                Optional<ResourceLocation> id = condition.getId();
                return id.isPresent() && data.isPresent() && id.get().equals(data.get());
        }
        return false;
    }

    public static Optional<EffectInstance> readEffectInstance(final CompoundNBT tag) {
        if(tag.contains("Potion", 8)) {
            final CompoundNBT nbt = tag.copy();
            nbt.putByte("Id", (byte) Effect.getId(ForgeRegistries.POTIONS.getValue(new ResourceLocation(nbt.getString("Potion")))));
            return Optional.of(EffectInstance.load(nbt));
        }
        return Optional.empty();
    }

    public static Optional<Entity> summonEntityNearPlayer(final World worldIn, final PlayerEntity playerIn, final Optional<CompoundNBT> entityTag) {
        if(entityTag.isPresent() && worldIn instanceof IServerWorld) {
            final Optional<EntityType<?>> entityType = EntityType.by(entityTag.get());
            if(entityType.isPresent()) {
                Entity entity = entityType.get().create(worldIn);
                final boolean waterMob = entity instanceof WaterMobEntity || entity instanceof DrownedEntity || entity instanceof GuardianEntity
                        || (entity instanceof MobEntity && ((MobEntity)entity).getNavigation() instanceof SwimmerPathNavigator);
                // find a place to spawn the entity
                Random rand = playerIn.getRandom();
                BlockPos spawnPos;
                for(int attempts = 24, range = 9; attempts > 0; attempts--) {
                    spawnPos = playerIn.blockPosition().offset(rand.nextInt(range) - rand.nextInt(range), rand.nextInt(2) - rand.nextInt(2), rand.nextInt(range) - rand.nextInt(range));
                    // check if this is a valid position
                    boolean canSpawnHere = EntitySpawnPlacementRegistry.checkSpawnRules(entityType.get(), (IServerWorld)worldIn, SpawnReason.SPAWN_EGG, spawnPos, rand)
                            || (waterMob && worldIn.getBlockState(spawnPos).is(Blocks.WATER))
                            || (!waterMob && worldIn.getBlockState(spawnPos.below()).canOcclude()
                            && worldIn.getBlockState(spawnPos).getMaterial() == Material.AIR
                            && worldIn.getBlockState(spawnPos.above()).getMaterial() == Material.AIR);
                    if(canSpawnHere) {
                        // spawn the entity at this position and finish
                        entity.load(entityTag.get());
                        entity.setPos(spawnPos.getX() + 0.5D, spawnPos.getY() + 0.01D, spawnPos.getZ() + 0.5D);
                        worldIn.addFreshEntity(entity);
                        return Optional.of(entity);
                    }
                }
                entity.remove();
            }
        }
        return Optional.empty();
    }

    /**
     * Checks random blocks in a radius until either a growable crop has been found
     * and changed, or no crops were found in a limited number of attempts.
     * @param player the player
     * @param favor the player's favor
     * @param amount the amount of growth to add (can be negative to remove growth)
     * @return whether a crop was found and its age was changed
     **/
    public static boolean growCropsNearPlayer(final PlayerEntity player, final IFavor favor, final int amount) {
        if(amount == 0) {
            return false;
        }
        final IntegerProperty[] AGES = new IntegerProperty[] {
                BlockStateProperties.AGE_1, BlockStateProperties.AGE_15, BlockStateProperties.AGE_2,
                BlockStateProperties.AGE_3, BlockStateProperties.AGE_5, BlockStateProperties.AGE_7
        };
        final Random rand = player.level.getRandom();
        final int maxAttempts = 10;
        final int variationY = 1;
        final int radius = 5;
        int attempts = 0;
        // if there are effects that should change growth states, find a crop to affect
        while (attempts++ <= maxAttempts) {
            // get random block in radius
            final int x1 = rand.nextInt(radius * 2) - radius;
            final int y1 = rand.nextInt(variationY * 2) - variationY + 1;
            final int z1 = rand.nextInt(radius * 2) - radius;
            final BlockPos blockpos = player.blockPosition().offset(x1, y1, z1);
            final BlockState state = player.level.getBlockState(blockpos);
            // if the block can be grown, grow it and return
            if (state.getBlock() instanceof IGrowable) {
                // determine which age property applies to this state
                for(final IntegerProperty AGE : AGES) {
                    if(state.hasProperty(AGE)) {
                        // attempt to update the age (add or subtract)
                        int oldAge = state.getValue(AGE);
                        int newAge = Math.max(0, oldAge + amount);
                        if(AGE.getPossibleValues().contains(Integer.valueOf(newAge))) {
                            // update the blockstate age
                            player.level.setBlock(blockpos, state.setValue(AGE, newAge), 2);
                            // spawn particles
                            if(player.level instanceof ServerWorld) {
                                IParticleData particle = (amount > 0) ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.ANGRY_VILLAGER;
                                ((ServerWorld)player.level).sendParticles(particle, blockpos.getX() + 0.5D, blockpos.getY() + 0.25D, blockpos.getZ() + 0.5D, 10, 0.5D, 0.5D, 0.5D, 0);
                            }
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static class ModEvents {
        // TODO: not yet registered
    }

    public static class ForgeEvents {

        @SubscribeEvent()
        public static void onLivingDeath(final LivingDeathEvent event) {
            if(!event.isCanceled() && event.getEntityLiving() != null && !event.getEntityLiving().level.isClientSide() && event.getEntityLiving().isEffectiveAi()) {
                if(event.getEntityLiving() instanceof PlayerEntity) {
                    final PlayerEntity player = (PlayerEntity) event.getEntityLiving();
                    final Entity source = event.getSource().getEntity();
                    // onEntityKillPlayer
                    if(source instanceof LivingEntity && !player.isSpectator() && !player.isCreative()) {
                        player.getCapability(RPGGods.FAVOR).ifPresent(f -> {
                            triggerCondition(PerkCondition.Type.ENTITY_KILLED_PLAYER, player, f, Optional.of(source),
                                    Optional.of(source.getType().getRegistryName()), Optional.empty());
                        });
                    }
                } else if(event.getSource().getEntity() instanceof PlayerEntity) {
                    final PlayerEntity player = (PlayerEntity)event.getSource().getEntity();
                    // onPlayerKillEntity
                    player.getCapability(RPGGods.FAVOR).ifPresent(f -> {
                        triggerCondition(PerkCondition.Type.PLAYER_KILLED_ENTITY, player, f, Optional.of(event.getEntityLiving()),
                                Optional.of(event.getEntityLiving().getType().getRegistryName()), Optional.empty());
                        onSacrifice(player, f, event.getEntityLiving());
                    });
                }
            }
        }

        @SubscribeEvent
        public static void onPlayerAttack(final AttackEntityEvent event) {
            if(!event.isCanceled() && !event.getPlayer().level.isClientSide() && event.getEntityLiving().isEffectiveAi()
                    && event.getPlayer().isAlive() && !event.getPlayer().isSpectator() && !event.getPlayer().isCreative()) {
                // onPlayerHurtEntity
                event.getPlayer().getCapability(RPGGods.FAVOR).ifPresent(f -> {
                    triggerCondition(PerkCondition.Type.PLAYER_HURT_ENTITY, event.getPlayer(), f, Optional.of(event.getEntityLiving()),
                            Optional.of(event.getEntityLiving().getType().getRegistryName()), Optional.empty());
                    // onEnterCombat
                    if(event.getPlayer().getCombatTracker().getCombatDuration() < COMBAT_TIMER) {
                        triggerCondition(PerkCondition.Type.ENTER_COMBAT, event.getPlayer(), f,
                                Optional.of(event.getEntityLiving()), Optional.of(event.getEntityLiving().getType().getRegistryName()),
                                Optional.empty());
                    }
                });
            }
        }

        @SubscribeEvent
        public static void onLivingHurt(final LivingHurtEvent event) {
            if(!event.isCanceled() && !event.getEntityLiving().level.isClientSide() && event.getEntityLiving().isEffectiveAi() && event.getEntityLiving().isAlive()
                    && event.getSource().getEntity() instanceof LivingEntity && event.getEntityLiving() instanceof PlayerEntity) {
                PlayerEntity player = (PlayerEntity)event.getEntityLiving();
                Entity source = (Entity) event.getSource().getDirectEntity();
                if(!player.isSpectator() && !player.isCreative()) {
                    // onEntityHurtPlayer
                    player.getCapability(RPGGods.FAVOR).ifPresent(f -> {
                        triggerCondition(PerkCondition.Type.ENTITY_HURT_PLAYER, player, f, Optional.of(source),
                                Optional.of(source.getType().getRegistryName()), Optional.empty());
                    });
                }
            }
        }

        @SubscribeEvent
        public static void onEntityInteract(final PlayerInteractEvent.EntityInteract event) {
            if(!event.getPlayer().level.isClientSide && event.getHand() == Hand.MAIN_HAND) {
                // onPlayerInteractEntity
                event.getPlayer().getCapability(RPGGods.FAVOR).ifPresent(f -> {
                    final ResourceLocation id = event.getTarget().getType().getRegistryName();
                    if(triggerCondition(PerkCondition.Type.PLAYER_INTERACT_ENTITY, event.getPlayer(), f, Optional.of(event.getTarget()), Optional.of(id), Optional.empty())) {
                        event.setCancellationResult(ActionResultType.SUCCESS);
                    }
                });
                // toggle sitting for tamed mobs
                if(null == event.getCancellationResult() || !event.getCancellationResult().consumesAction()) {
                    event.getTarget().getCapability(RPGGods.TAMEABLE).ifPresent(t -> {
                        if(t.isOwner(event.getPlayer())) {
                            t.setSitting(!t.isSitting());
                            event.setCancellationResult(ActionResultType.SUCCESS);
                        }
                    });
                }
            }
        }

        @SubscribeEvent
        public static void onEntityJoinWorld(final EntityJoinWorldEvent event) {
            if(!event.getEntity().level.isClientSide && (event.getEntity() instanceof ArrowEntity || event.getEntity() instanceof SpectralArrowEntity)) {
                final AbstractArrowEntity arrow = (AbstractArrowEntity) event.getEntity();
                final Entity thrower = arrow.getOwner();
                if(thrower instanceof PlayerEntity) {
                    // onArrowDamage, onArrowEffect, onArrowCount
                    thrower.getCapability(RPGGods.FAVOR).ifPresent(f -> {
                        triggerPerks(PerkData.Type.ARROW_DAMAGE, (PlayerEntity) thrower, f, Optional.of(arrow));
                        triggerPerks(PerkData.Type.ARROW_EFFECT, (PlayerEntity) thrower, f, Optional.of(arrow));
                        triggerPerks(PerkData.Type.ARROW_COUNT, (PlayerEntity) thrower, f, Optional.of(arrow));
                    });
                }
            }
            if(!event.getEntity().level.isClientSide && event.getEntity() instanceof MobEntity) {
                MobEntity mob = (MobEntity) event.getEntity();
                //addAffinityGoals(mob);
                // add tameable goals
                if(event.getEntity().getCapability(RPGGods.TAMEABLE).isPresent()) {
                    mob.goalSelector.addGoal(0, new AffinityGoal.SittingGoal(mob));
                    mob.goalSelector.addGoal(0, new AffinityGoal.SittingResetGoal(mob));
                    mob.goalSelector.addGoal(1, new AffinityGoal.FollowOwnerGoal(mob, 1.0D, 10.0F, 5.0F, false));
                    mob.goalSelector.addGoal(1, new AffinityGoal.OwnerHurtByTargetGoal(mob));
                    mob.goalSelector.addGoal(1, new AffinityGoal.OwnerHurtTargetGoal(mob));
                }
                // add flee goal
                if(event.getEntity() instanceof CreatureEntity) {
                    mob.goalSelector.addGoal(1, new AffinityGoal.FleeGoal((CreatureEntity) mob));
                }
                // add hostile goal
                mob.goalSelector.addGoal(4, new AffinityGoal.NearestAttackableGoal(mob, 0.1F));
                // add target reset goal
                mob.goalSelector.addGoal(2, new AffinityGoal.NearestAttackableResetGoal(mob));
            }
        }

        @SubscribeEvent
        public static void onLivingTarget(final LivingSetAttackTargetEvent event) {
            if(!event.getEntityLiving().level.isClientSide && event.getEntityLiving() instanceof MobEntity
                    && event.getTarget() instanceof PlayerEntity) {
                // Determine if entity is passive or hostile toward target
                Tuple<Boolean, Boolean> passiveHostile = AffinityGoal.getPassiveAndHostile(event.getEntityLiving(), event.getTarget());
                if (passiveHostile.getA()) {
                    ((MobEntity) event.getEntityLiving()).setTarget(null);
                    return;
                }
            }
        }

        @SubscribeEvent
        public static void onBabySpawn(final BabyEntitySpawnEvent event) {
            if(!event.isCanceled() && event.getParentA().isEffectiveAi() && event.getCausedByPlayer() != null
                    && !event.getCausedByPlayer().isCreative() && !event.getCausedByPlayer().isSpectator()
                    && event.getParentA() instanceof AnimalEntity && event.getParentB() instanceof AnimalEntity) {
                event.getCausedByPlayer().getCapability(RPGGods.FAVOR).ifPresent(f -> {
                    triggerPerks(PerkData.Type.OFFSPRING, event.getCausedByPlayer(), f, Optional.of(event.getParentA()), Optional.empty(), Optional.of(event));
                });
            }
        }

        @SubscribeEvent
        public static void onPlayerPickupXp(final PlayerXpEvent.PickupXp event) {
            if(event.getPlayer().isEffectiveAi() && !event.getPlayer().level.isClientSide()) {
                event.getPlayer().getCapability(RPGGods.FAVOR).ifPresent(f -> {
                    triggerPerks(PerkData.Type.XP, event.getPlayer(), f, Optional.of(event.getOrb()));
                });
            }
        }

        @SubscribeEvent
        public static void onPlayerTick(final TickEvent.PlayerTickEvent event) {
            if(!event.isCanceled() && !event.player.level.isClientSide() && event.player.isEffectiveAi()
                    && event.player.isAlive() && canTickFavor(event.player)) {
                event.player.getCapability(RPGGods.FAVOR).ifPresent(f -> {
                    // trigger perks
                    if(Math.random() < RANDOM_TICK_CHANCE) {
                        // onRandomTick
                        triggerCondition(PerkCondition.Type.RANDOM_TICK, event.player, f, Optional.empty(),
                                Optional.empty(), Optional.empty());
                    }
                    // reduce cooldowns
                    f.tickCooldown(event.player.level.getGameTime());
                });
            }
        }

        // TODO: add to config
        public static final int TICK_RATE = 20;
        public static final float RANDOM_TICK_CHANCE = 0.6F;

        public static boolean canTickFavor(final LivingEntity entity) {
            return (entity.tickCount + entity.getId()) % TICK_RATE == 0;
        }

        // TODO: use this instead of adding to all entities
        public static void addAffinityGoals(final MobEntity mob) {
            final ResourceLocation id = mob.getType().getRegistryName();
            // create task
            Runnable task = () -> {
                // ensure entity is still available
                if(null == mob || !mob.isAlive()) {
                    return;
                }
                Set<Affinity.Type> types = RPGGods.AFFINITY.getOrDefault(id, ImmutableMap.of()).keySet();
                RPGGods.LOGGER.debug(id + " has affinity types " + types);
                // add tameable goals
                if(types.contains(Affinity.Type.TAME)) {
                    mob.goalSelector.addGoal(0, new AffinityGoal.SittingGoal(mob));
                    mob.goalSelector.addGoal(0, new AffinityGoal.SittingResetGoal(mob));
                    mob.goalSelector.addGoal(1, new AffinityGoal.FollowOwnerGoal(mob, 1.0D, 10.0F, 5.0F, false));
                    mob.goalSelector.addGoal(1, new AffinityGoal.OwnerHurtByTargetGoal(mob));
                    mob.goalSelector.addGoal(1, new AffinityGoal.OwnerHurtTargetGoal(mob));
                }
                // add flee goal
                if(types.contains(Affinity.Type.FLEE) && mob instanceof CreatureEntity) {
                    mob.goalSelector.addGoal(1, new AffinityGoal.FleeGoal((CreatureEntity) mob));
                }
                // add hostile goal
                if(types.contains(Affinity.Type.HOSTILE)) {
                    mob.goalSelector.addGoal(4, new AffinityGoal.NearestAttackableGoal(mob, 0.1F));
                }
                // add target reset goal
                if(types.contains(Affinity.Type.HOSTILE) || types.contains(Affinity.Type.PASSIVE)) {
                    mob.goalSelector.addGoal(2, new AffinityGoal.NearestAttackableResetGoal(mob));
                }
            };
            // schedule task
            MinecraftServer server = mob.getServer();
            server.tell(new TickDelayedTask(10, task));
        }
    }
}
