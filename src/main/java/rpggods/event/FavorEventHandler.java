package rpggods.event;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.command.CommandSource;
import net.minecraft.command.FunctionObject;
import net.minecraft.entity.AgeableEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.SpawnReason;
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
import net.minecraft.pathfinding.SwimmerPathNavigator;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.World;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import rpggods.RPGGods;
import rpggods.deity.Deity;
import rpggods.deity.Offering;
import rpggods.deity.Sacrifice;
import rpggods.favor.IFavor;
import rpggods.perk.Perk;
import rpggods.perk.PerkCondition;
import rpggods.perk.PerkData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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
     * @return the ItemStack after applying the offering, may be same as original
     */
    public static ItemStack onOffering(final ResourceLocation deity, final PlayerEntity player, final IFavor favor, final ItemStack item) {
        if(favor.isEnabled() && !item.isEmpty()) {
            // find first matching offering for the given deity
            Offering offering = null;
            for(Offering o : RPGGods.DEITY.get(deity).offeringMap.getOrDefault(item.getItem().getRegistryName(), ImmutableList.of())) {
                if(ItemStack.areItemStacksEqual(offering.getAccept(), item)) {
                    offering = o;
                    break;
                }
            }
            // process the offering
            if(offering != null) {
                favor.getFavor(deity).addFavor(player, deity, offering.getFavor(), FavorChangedEvent.Source.OFFERING);
                offering.getFunction().ifPresent(f -> runFunction(player.world, player, f));
                // shrink item stack
                if(!player.isCreative()) {
                    item.shrink(1);
                }
                // process trade, if any
                if(offering.getTrade().isPresent() && favor.getFavor(deity).getLevel() >= offering.getTradeMinLevel()) {
                    player.addItemStackToInventory(offering.getTrade().get().copy());
                }
            }
        }


        return item;
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
            // find all matching sacrifices
            List<Sacrifice> sacrificeList = new ArrayList<>();
            ResourceLocation entityId = entity.getType().getRegistryName();
            for(Optional<Sacrifice> sacrifice : RPGGods.SACRIFICE.getValues()) {
                if(sacrifice.isPresent() && entityId.equals(sacrifice.get().getEntity())) {
                    sacrificeList.add(sacrifice.get());
                }
            }
            // process all matching sacrifices
            for(Sacrifice sacrifice : sacrificeList) {
                favor.getFavor(sacrifice.getDeity()).addFavor(player, sacrifice.getDeity(), sacrifice.getFavor(), FavorChangedEvent.Source.SACRIFICE);
                sacrifice.getFunction().ifPresent(f -> runFunction(player.world, player, f));
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
            Set<Perk> perks = new HashSet<>();
            for(Deity deity : RPGGods.DEITY.values()) {
                perks.addAll(deity.perkByConditionMap.getOrDefault(type, ImmutableList.of()));
            }
            // shuffle perks
            List<Perk> perkList = Lists.newArrayList(perks);
            Collections.shuffle(perkList);
            // run each perk
            for(Perk perk : perkList) {
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
            List<Perk> perks = new ArrayList<>();
            for(Deity deity : RPGGods.DEITY.values()) {
                perks.addAll(deity.perkByTypeMap.getOrDefault(type, ImmutableList.of()));
            }
            // shuffle perks
            Collections.shuffle(perks);
            // run each perk
            for(Perk perk : perks) {
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
            final net.minecraft.advancements.FunctionManager manager = server.getFunctionManager();
            final Optional<FunctionObject> function = manager.get(functionId);
            if(function.isPresent()) {
                final CommandSource commandSource = manager.getCommandSource()
                        .withEntity(entity)
                        .withPos(entity.getPositionVec())
                        .withPermissionLevel(4)
                        .withFeedbackDisabled();
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
        if(!player.world.isRemote && perk.getRange().isInRange(favor)
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
            case FUNCTION: return action.getId().isPresent() && runFunction(player.world, player, action.getId().get());
            case POTION:
                if(action.getTag().isPresent()) {
                    Optional<EffectInstance> effect = readEffectInstance(action.getTag().get());
                    if(effect.isPresent()) {
                        return player.addPotionEffect(effect.get());
                    }
                }
                return false;
            case SUMMON:
                return action.getTag().isPresent() && summonEntityNearPlayer(player.world, player, action.getTag()).isPresent();
            case ITEM: if(action.getItem().isPresent()) {
                    ItemEntity itemEntity = new ItemEntity(player.world, player.getPosX(), player.getPosY(), player.getPosZ(), action.getItem().get().copy());
                    itemEntity.setNoPickupDelay();
                    return player.world.addEntity(itemEntity);
                }
                return false;
            case FAVOR: return action.getFavor().isPresent() && action.getId().isPresent()
                    && favor.getFavor(action.getId().get()).addFavor(player, action.getId().get(), action.getFavor().get(), FavorChangedEvent.Source.PERK)
                        != favor.getFavor(action.getId().get()).getFavor();
            case AFFINITY:
                // TODO
                break;
            case ARROW_DAMAGE:
                if(entity.isPresent() && action.getMultiplier().isPresent() && entity.get() instanceof ArrowEntity) {
                    ArrowEntity arrow = (ArrowEntity) entity.get();
                    arrow.setDamage(arrow.getDamage() * action.getMultiplier().get());
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
                        AbstractArrowEntity arrow2 = (AbstractArrowEntity) arrow.getType().create(arrow.world);
                        arrow2.copyLocationAndAnglesFrom(arrow);
                        arrow2.setMotion(arrow.getMotion().mul(
                                (Math.random() * 2.0D - 1.0D) * motionScale,
                                (Math.random() * 2.0D - 1.0D) * motionScale,
                                (Math.random() * 2.0D - 1.0D) * motionScale));
                        arrow2.pickupStatus = AbstractArrowEntity.PickupStatus.CREATIVE_ONLY;
                        arrow.world.addEntity(arrow2);
                    }
                }
                break;
            case OFFSPRING:
                if(action.getMultiplier().isPresent() && entity.isPresent() && entity.get() instanceof AgeableEntity
                        && object.isPresent() && object.get() instanceof BabyEntitySpawnEvent) {
                    int childCount = Math.round(action.getMultiplier().get());
                    if(childCount < 1) {
                        // number of babies is zero, so cancel the event
                        ((BabyEntitySpawnEvent)object.get()).setCanceled(true);
                    } else if(childCount > 1) {
                        // number of babies is more than one, so spawn additional mobs
                        AgeableEntity parent = (AgeableEntity) entity.get();
                        for(int i = 1; i < childCount; i++) {
                            AgeableEntity bonusChild = (AgeableEntity) parent.getType().create(parent.world);
                            if(bonusChild != null) {
                                bonusChild.copyLocationAndAnglesFrom(parent);
                                bonusChild.setChild(true);
                                parent.world.addEntity(bonusChild);
                            }
                        }
                    }
                }
                break;
            case CROP_GROWTH:
                break;
            case CROP_HARVEST:
                break;
            case AUTOSMELT:
                break;
            case UNSMELT:
                break;
            case SPECIAL_PRICE:
                break;
            case XP:
                if(entity.isPresent() && action.getMultiplier().isPresent() && entity.get() instanceof ExperienceOrbEntity) {
                    ((ExperienceOrbEntity)entity.get()).xpValue *= action.getMultiplier().get();
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
            case BIOME: return condition.isInBiome(player.world, player.getPosition());
            case DAY: return player.world.isDaytime();
            case NIGHT: return !player.world.isDaytime();
            case RANDOM_TICK: return true;
            case ENTER_COMBAT: return player.getCombatTracker().getCombatDuration() < COMBAT_TIMER;
            case ENTITY_HURT_PLAYER:
            case ENTITY_KILLED_PLAYER:
            case PLAYER_HURT_ENTITY:
            case PLAYER_KILLED_ENTITY:
                Optional<ResourceLocation> id = condition.getId();
                return id.isPresent() && data.isPresent() && id.get().equals(data.get());
        }
        return false;
    }

    public static Optional<EffectInstance> readEffectInstance(final CompoundNBT tag) {
        if(tag.contains("Potion", 8)) {
            final CompoundNBT nbt = tag.copy();
            nbt.putByte("Id", (byte) Effect.getId(ForgeRegistries.POTIONS.getValue(new ResourceLocation(nbt.getString("Potion")))));
            return Optional.of(EffectInstance.read(nbt));
        }
        return Optional.empty();
    }

    public static Optional<Entity> summonEntityNearPlayer(final World worldIn, final PlayerEntity playerIn, final Optional<CompoundNBT> entityTag) {
        if(entityTag.isPresent() && worldIn instanceof IServerWorld) {
            final Optional<EntityType<?>> entityType = EntityType.readEntityType(entityTag.get());
            if(entityType.isPresent()) {
                Entity entity = entityType.get().create(worldIn);
                final boolean waterMob = entity instanceof WaterMobEntity || entity instanceof DrownedEntity || entity instanceof GuardianEntity
                        || (entity instanceof MobEntity && ((MobEntity)entity).getNavigator() instanceof SwimmerPathNavigator);
                // find a place to spawn the entity
                Random rand = playerIn.getRNG();
                BlockPos spawnPos;
                for(int attempts = 24, range = 9; attempts > 0; attempts--) {
                    spawnPos = playerIn.getPosition().add(rand.nextInt(range) - rand.nextInt(range), rand.nextInt(2) - rand.nextInt(2), rand.nextInt(range) - rand.nextInt(range));
                    // check if this is a valid position
                    boolean canSpawnHere = EntitySpawnPlacementRegistry.canSpawnEntity(entityType.get(), (IServerWorld)worldIn, SpawnReason.SPAWN_EGG, spawnPos, rand)
                            || (waterMob && worldIn.getBlockState(spawnPos).matchesBlock(Blocks.WATER))
                            || (!waterMob && worldIn.getBlockState(spawnPos.down()).isSolid()
                            && worldIn.getBlockState(spawnPos).getMaterial() == Material.AIR
                            && worldIn.getBlockState(spawnPos.up()).getMaterial() == Material.AIR);
                    if(canSpawnHere) {
                        // spawn the entity at this position and finish
                        entity.read(entityTag.get());
                        entity.setPosition(spawnPos.getX() + 0.5D, spawnPos.getY() + 0.01D, spawnPos.getZ() + 0.5D);
                        worldIn.addEntity(entity);
                        return Optional.of(entity);
                    }
                }
                entity.remove();
            }
        }
        return Optional.empty();
    }

    public static class ModEvents {
        // TODO: not yet registered
    }

    public static class ForgeEvents {

        @SubscribeEvent()
        public static void onLivingDeath(final LivingDeathEvent event) {
            if(!event.isCanceled() && event.getEntityLiving() != null && !event.getEntityLiving().world.isRemote() && event.getEntityLiving().isServerWorld()) {
                if(event.getEntityLiving() instanceof PlayerEntity) {
                    final PlayerEntity player = (PlayerEntity) event.getEntityLiving();
                    final Entity source = event.getSource().getTrueSource();
                    // onEntityKillPlayer
                    if(source instanceof LivingEntity && !player.isSpectator() && !player.isCreative()) {
                        player.getCapability(RPGGods.FAVOR).ifPresent(f -> {
                            triggerCondition(PerkCondition.Type.ENTITY_KILLED_PLAYER, player, f, Optional.of(source),
                                    Optional.of(source.getType().getRegistryName()), Optional.empty());
                        });
                    }
                } else if(event.getSource().getTrueSource() instanceof PlayerEntity) {
                    final PlayerEntity player = (PlayerEntity)event.getSource().getTrueSource();
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
            if(!event.isCanceled() && !event.getPlayer().world.isRemote() && event.getEntityLiving().isServerWorld()
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
            if(!event.isCanceled() && !event.getEntityLiving().world.isRemote() && event.getEntityLiving().isServerWorld() && event.getEntityLiving().isAlive()
                    && event.getSource().getTrueSource() instanceof LivingEntity && event.getEntityLiving() instanceof PlayerEntity) {
                PlayerEntity player = (PlayerEntity)event.getEntityLiving();
                Entity source = (Entity) event.getSource().getImmediateSource();
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
        public static void onEntityJoinWorld(final EntityJoinWorldEvent event) {
            if((event.getEntity() instanceof ArrowEntity || event.getEntity() instanceof SpectralArrowEntity)
                    && !event.getEntity().getEntityWorld().isRemote()) {
                final AbstractArrowEntity arrow = (AbstractArrowEntity) event.getEntity();
                final Entity thrower = arrow.getShooter();
                if(thrower instanceof PlayerEntity) {
                    // onArrowDamage, onArrowEffect, onArrowCount
                    thrower.getCapability(RPGGods.FAVOR).ifPresent(f -> {
                        triggerPerks(PerkData.Type.ARROW_DAMAGE, (PlayerEntity) thrower, f, Optional.of(arrow));
                        triggerPerks(PerkData.Type.ARROW_EFFECT, (PlayerEntity) thrower, f, Optional.of(arrow));
                        triggerPerks(PerkData.Type.ARROW_COUNT, (PlayerEntity) thrower, f, Optional.of(arrow));
                    });
                }
            }
        }

        @SubscribeEvent
        public static void onBabySpawn(final BabyEntitySpawnEvent event) {
            if(!event.isCanceled() && event.getParentA().isServerWorld() && event.getCausedByPlayer() != null
                    && !event.getCausedByPlayer().isCreative() && !event.getCausedByPlayer().isSpectator()
                    && event.getParentA() instanceof AnimalEntity && event.getParentB() instanceof AnimalEntity) {
                event.getCausedByPlayer().getCapability(RPGGods.FAVOR).ifPresent(f -> {
                    triggerPerks(PerkData.Type.OFFSPRING, event.getCausedByPlayer(), f, Optional.of(event.getParentA()), Optional.empty(), Optional.of(event));
                });
            }
        }

        @SubscribeEvent
        public static void onPlayerPickupXp(final PlayerXpEvent.PickupXp event) {
            if(event.getPlayer().isServerWorld() && !event.getPlayer().world.isRemote()) {
                event.getPlayer().getCapability(RPGGods.FAVOR).ifPresent(f -> {
                    triggerPerks(PerkData.Type.XP, event.getPlayer(), f, Optional.of(event.getOrb()));
                });
            }
        }

        @SubscribeEvent
        public static void onPlayerTick(final TickEvent.PlayerTickEvent event) {
            if(!event.isCanceled() && !event.player.world.isRemote() && event.player.isServerWorld() && event.player.isAlive() && canTickFavor(event.player)) {
                event.player.getCapability(RPGGods.FAVOR).ifPresent(f -> {
                    // trigger perks
                    triggerCondition(PerkCondition.Type.RANDOM_TICK, event.player, f, Optional.empty(),
                            Optional.empty(), Optional.of(event));
                    // reduce cooldowns
                    f.tickPerkCooldown(event.player.world.getGameTime());
                });
            }
        }

        public static final int TICK_RATE = 20;

        public static boolean canTickFavor(final LivingEntity entity) {
            return (entity.ticksExisted + entity.getEntityId()) % TICK_RATE == 0;
        }
    }
}
