package rpggods.event;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import net.minecraft.block.Block;
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
import net.minecraft.entity.IAngerable;
import net.minecraft.entity.IRangedAttackMob;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.entity.item.ExperienceOrbEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.merchant.IMerchant;
import net.minecraft.entity.merchant.villager.AbstractVillagerEntity;
import net.minecraft.entity.monster.DrownedEntity;
import net.minecraft.entity.monster.GuardianEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.WaterMobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.SpectralArrowEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MerchantOffer;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.IParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.pathfinding.SwimmerPathNavigator;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.server.MinecraftServer;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.concurrent.TickDelayedTask;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.event.entity.living.PotionEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.tuple.ImmutablePair;
import rpggods.RPGGods;
import rpggods.deity.Deity;
import rpggods.deity.DeityHelper;
import rpggods.deity.Offering;
import rpggods.deity.Sacrifice;
import rpggods.entity.AltarEntity;
import rpggods.entity.ai.AffinityGoal;
import rpggods.favor.FavorLevel;
import rpggods.favor.IFavor;
import rpggods.network.SUpdateSittingPacket;
import rpggods.perk.Affinity;
import rpggods.perk.Perk;
import rpggods.perk.PerkCondition;
import rpggods.perk.PerkAction;
import rpggods.tameable.ITameable;
import rpggods.deity.Cooldown;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class FavorEventHandler {

    public static final int COMBAT_TIMER = 40;

    /**
     * Called when the player attempts to give an offering
     * @param entity the AltarEntity associated with this offering, if any
     * @param deity the deity ID
     * @param player the player
     * @param favor the player's favor
     * @param item the item being offered
     * @return the ItemStack to replace the one provided, if any
     */
    public static Optional<ItemStack> onOffering(final Optional<AltarEntity> entity, final ResourceLocation deity, final PlayerEntity player, final IFavor favor, final ItemStack item) {
        boolean deityEnabled = favor.getFavor(deity).isEnabled();
        if(favor.isEnabled() && deityEnabled && !item.isEmpty()) {
            // find first matching offering for the given deity
            ResourceLocation offeringId = null;
            Offering offering = null;
            for(ResourceLocation id : RPGGods.DEITY_HELPER.get(deity).offeringMap.getOrDefault(item.getItem().getRegistryName(), ImmutableList.of())) {
                Offering o = RPGGods.OFFERING.get(id).orElse(null);
                if(o != null && o.matches(item)) {
                    offeringId = id;
                    offering = o;
                    break;
                }
            }
            // process the offering
            if(offering != null && offeringId != null) {
                // ensure offering can be accepted
                if(!favor.getOfferingCooldown(offeringId).canUse()) {
                    // send message to player informing them of maxed offering
                    ITextComponent message = new TranslationTextComponent("favor.offering.cooldown");
                    player.displayClientMessage(message, true);
                    return Optional.of(item);
                }
                // ensure player meets trade level, if any
                if(offering.getTrade().isPresent() && favor.getFavor(deity).getLevel() < offering.getTradeMinLevel()) {
                    // Send message to player informing them of trade level minimum
                    ITextComponent message = new TranslationTextComponent("favor.offering.trade.failure", offering.getTradeMinLevel());
                    player.displayClientMessage(message, true);
                    return Optional.of(item);
                }
                // add favor and run function, if any
                favor.getFavor(deity).addFavor(player, deity, offering.getFavor(), FavorChangedEvent.Source.OFFERING);
                offering.getFunction().ifPresent(f -> runFunction(player.level, player, f));
                // add cooldown
                favor.getOfferingCooldown(offeringId).addUse();
                // process trade, if any
                Optional<ItemStack> trade = offering.getTrade(item);
                if(trade.isPresent() && !trade.get().isEmpty()) {
                    ItemEntity itemEntity = new ItemEntity(player.level, player.getX(), player.getY(), player.getZ(), trade.get().copy());
                    itemEntity.setNoPickUpDelay();
                    player.level.addFreshEntity(itemEntity);
                }
                // shrink item stack
                if(!player.isCreative()) {
                    item.shrink(offering.getAccept().getCount());
                }
                // particles
                if(entity.isPresent() && player.level instanceof ServerWorld) {
                    Vector3d pos = Vector3d.atBottomCenterOf(entity.get().blockPosition().above());
                    IParticleData particle = offering.getFavor() >= 0 ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.ANGRY_VILLAGER;
                    ((ServerWorld)player.level).sendParticles(particle, pos.x, pos.y, pos.z, 8, 0.5D, 0.5D, 0.5D, 0);
                }
                // send player message
                favor.getFavor(deity).sendStatusMessage(player, deity);
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
            ResourceLocation deityId;
            Sacrifice sacrifice;
            Cooldown cooldown;
            for(Map.Entry<ResourceLocation, Optional<Sacrifice>> entry : RPGGods.SACRIFICE.getEntries()) {
                if(entry.getValue() != null && entry.getValue().isPresent()) {
                    sacrifice = entry.getValue().get();
                    // check sacrifice matches entity that was killed
                    if(entityId.equals(sacrifice.getEntity())) {
                        // check sacrifice cooldown
                        cooldown = favor.getSacrificeCooldown(entry.getKey());
                        deityId = Sacrifice.getDeity(entry.getKey());
                        boolean deityEnabled = favor.getFavor(deityId).isEnabled();
                        if(deityEnabled && cooldown.canUse()) {
                            // add sacrifice cooldown
                            cooldown.addUse();
                            // add favor and run function, if any
                            favor.getFavor(deityId).addFavor(player, deityId, sacrifice.getFavor(), FavorChangedEvent.Source.SACRIFICE);
                            sacrifice.getFunction().ifPresent(f -> runFunction(player.level, player, f));
                        }
                    }
                }
            }
        }
        return success;
    }

    /**
     * Loads all perks that match the given {@link PerkCondition.Type} and
     * attempts to run them.
     * @param type The Perk Condition Type to run
     * @param player The player
     * @param favor The player's favor
     * @param entity An entity associated with this condition, if any
     * @param data A ResourceLocation associated with this condition, if any
     * @param object An event associated with this condition, if any
     * @return true if at least one perk ran successfully
     */
    public static boolean triggerCondition(final PerkCondition.Type type, final PlayerEntity player, final IFavor favor,
                                           final Optional<Entity> entity, final Optional<ResourceLocation> data,
                                           final Optional<? extends Event> object) {
        boolean success = false;
        if(favor.isEnabled()) {
            // find matching perks (use set to ensure no duplicates)
            Set<ResourceLocation> perks = new HashSet<>();
            for(DeityHelper helper : RPGGods.DEITY_HELPER.values()) {
                boolean deityEnabled = favor.getFavor(helper.id).isEnabled();
                if(deityEnabled) {
                    perks.addAll(helper.perkByConditionMap.getOrDefault(type, ImmutableList.of()));
                }
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
     * Loads all perks with the given {@link PerkAction.Type} and attempts to run each one.
     * @param type the action type (eg, function, item, potion, summon, arrow, xp)
     * @param player the player
     * @param favor the player's favor
     * @param entity an entity to use when running the perk, if any
     * @return True if at least one perk ran successfully
     */
    public static boolean triggerPerks(final PerkAction.Type type, final PlayerEntity player, final IFavor favor, final Optional<Entity> entity) {
        return triggerPerks(type, player, favor, entity, Optional.empty(), Optional.empty());
    }

    /**
     * Loads all perks with the given {@link PerkAction.Type} and attempts to run each one.
     * @param type the action type (eg, function, item, potion, summon, arrow, xp)
     * @param player the player
     * @param favor the player's favor
     * @param entity an entity to use when running the perk, if any
     * @param data a ResourceLocation ID to use when running the perk, if any
     * @param object the Event to reference when running the perk, if any
     * @return True if at least one perk ran successfully
     */
    public static boolean triggerPerks(final PerkAction.Type type, final PlayerEntity player, final IFavor favor,
                                       final Optional<Entity> entity, final Optional<ResourceLocation> data,
                                       final Optional<? extends Event> object) {
        boolean success = false;
        if(favor.isEnabled()) {
            // find matching perks
            List<ResourceLocation> perks = new ArrayList<>();
            for(DeityHelper helper : RPGGods.DEITY_HELPER.values()) {
                boolean deityEnabled = favor.getFavor(helper.id).isEnabled();
                if(deityEnabled) {
                    perks.addAll(helper.perkByTypeMap.getOrDefault(type, ImmutableList.of()));
                }
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
                && favor.getFavor(perk.getDeity()).isEnabled()
                && favor.hasNoPerkCooldown(perk.getCategory())
                && Math.random() < perk.getAdjustedChance(favor.getFavor(perk.getDeity()))) {
            // load nbt data
            Optional<CompoundNBT> nbt = Optional.empty();
            if(entity.isPresent()) {
                nbt = Optional.ofNullable(entity.get().serializeNBT());
            }
            // check perk conditions
            for(final PerkCondition condition : perk.getConditions()) {
                if(!condition.match(perk.getDeity(), player, favor, data, nbt)) {
                    return false;
                }
            }
            boolean success = false;
            for(final PerkAction action : perk.getActions()) {
                success |= action.run(perk.getDeity(), player, favor, entity, data, object);
            }
            if(success) {
                // send feedback
                sendPerkFeedback(perk.getDeity(), player, favor);
                // apply cooldown
                long cooldown = (long) Math.floor(perk.getCooldown() * (1.0D + Math.random() * 0.25D));
                favor.setPerkCooldown(perk.getCategory(), cooldown);
                return true;
            }
        }

        return false;
    }

    public static void sendPerkFeedback(ResourceLocation deity, PlayerEntity player, IFavor favor) {
        if(RPGGods.CONFIG.canGiveFeedback()) {
            final ITextComponent deityName = DeityHelper.getName(deity);
            final boolean positive = favor.getFavor(deity).getLevel() >= 0;
            final ITextComponent message;
            if(positive) {
                message = new TranslationTextComponent("favor.perk.feedback.positive", deityName).withStyle(TextFormatting.GREEN);
            } else {
                message = new TranslationTextComponent("favor.perk.feedback.negative", deityName).withStyle(TextFormatting.RED);
            }
            player.displayClientMessage(message, !RPGGods.CONFIG.isFeedbackChat());
        }
    }

    /**
     * @param altar the altar entity
     * @param deityId the Deity ID of the deity for this altar
     * @return true if a ritual was detected and patron was changed
     */
    public static boolean performRitual(final AltarEntity altar, final ResourceLocation deityId) {
        Vector3i facing = altar.getDirection().getNormal();
        BlockPos pos = altar.blockPosition().offset(facing);
        AxisAlignedBB aabb = new AxisAlignedBB(pos).inflate(0.15D, 1.0D, 0.15D);
        List<ItemEntity> list = altar.level.getEntitiesOfClass(ItemEntity.class, aabb, e -> e.isOnFire());
        // detect first burning item in list
        if(list.isEmpty()) {
            return false;
        }
        ItemEntity item = list.get(0);
        // detect player who threw the item
        if(null == item.getThrower()) {
            return false;
        }
        PlayerEntity player = altar.level.getPlayerByUUID(item.getThrower());
        if(null == player) {
            return false;
        }
        // detect ritual perks for this deity
        ResourceLocation itemId = item.getItem().getItem().getRegistryName();
        DeityHelper deity = RPGGods.DEITY_HELPER.computeIfAbsent(deityId, DeityHelper::new);
        List<ResourceLocation> perkIds = deity.perkByConditionMap
                .getOrDefault(PerkCondition.Type.RITUAL, ImmutableList.of());
        // create list using perk IDs
        List<Perk> perks = new ArrayList<>();
        for(ResourceLocation perkId : perkIds) {
            perks.add(RPGGods.PERK.get(perkId).orElse(Perk.EMPTY));
        }
        // load favor
        boolean success = false;
        LazyOptional<IFavor> ifavor = player.getCapability(RPGGods.FAVOR);
        if(ifavor.isPresent()) {
            IFavor favor = ifavor.orElse(null);
            // attempt to run the perks
            if(favor.isEnabled()) {
                for(Perk perk : perks) {
                    success |= runPerk(perk, player, favor, Optional.of(altar), Optional.of(itemId), Optional.empty());
                }
            }
        }
        // send feedback
        if(success) {
            // summon visual lightning bolt
            LightningBoltEntity bolt = EntityType.LIGHTNING_BOLT.create(altar.level);
            bolt.setVisualOnly(true);
            Vector3d position = Vector3d.atBottomCenterOf(pos.below());
            bolt.setPos(position.x, position.y, position.z);
            altar.level.addFreshEntity(bolt);
            // send message
            ITextComponent message = new TranslationTextComponent("favor.perk.type.patron.description.add", DeityHelper.getName(deityId))
                    .withStyle(TextFormatting.LIGHT_PURPLE, TextFormatting.BOLD);
            player.displayClientMessage(message, true);
        }

        return false;
    }

    public static class ModEvents {

        @SubscribeEvent
        public static void onAddEntityAttributes(final EntityAttributeModificationEvent event) {
            RPGGods.LOGGER.debug("onAddEntityAttributes");
            for(final EntityType<? extends LivingEntity> type : event.getTypes()) {
                if(!event.has(type, Attributes.ATTACK_DAMAGE)) {
                    event.add(type, Attributes.ATTACK_DAMAGE, 0);
                }
            }
        }
    }

    public static class ForgeEvents {

        @SubscribeEvent
        public static void onLivingDeath(final LivingDeathEvent event) {
            if (!event.isCanceled() && event.getEntityLiving() != null && !event.getEntityLiving().level.isClientSide() && event.getEntityLiving().isEffectiveAi()) {
                if (event.getEntityLiving() instanceof PlayerEntity) {
                    final PlayerEntity player = (PlayerEntity) event.getEntityLiving();
                    final Entity source = event.getSource().getEntity();
                    // onEntityKillPlayer
                    if (source instanceof LivingEntity && !player.isSpectator() && !player.isCreative()) {
                        player.getCapability(RPGGods.FAVOR).ifPresent(f -> {
                            triggerCondition(PerkCondition.Type.ENTITY_KILLED_PLAYER, player, f, Optional.of(source),
                                    Optional.of(source.getType().getRegistryName()), Optional.empty());
                        });
                    }
                } else if (event.getSource().getEntity() instanceof PlayerEntity) {
                    final PlayerEntity player = (PlayerEntity) event.getSource().getEntity();
                    // onPlayerKillEntity
                    player.getCapability(RPGGods.FAVOR).ifPresent(f -> {
                        triggerCondition(PerkCondition.Type.PLAYER_KILLED_ENTITY, player, f, Optional.of(event.getEntityLiving()),
                                Optional.of(event.getEntityLiving().getType().getRegistryName()), Optional.empty());
                        onSacrifice(player, f, event.getEntityLiving());
                    });
                }
                // onTameDeath
                LazyOptional<ITameable> tameable = event.getEntity().getCapability(RPGGods.TAMEABLE);
                tameable.ifPresent(t -> {
                    Optional<LivingEntity> owner = t.getOwner(event.getEntityLiving().level);
                    // send death message to owner
                    if (owner.isPresent() && owner.get() instanceof PlayerEntity) {
                        ITextComponent message = event.getSource().getLocalizedDeathMessage(event.getEntityLiving());
                        ((PlayerEntity) owner.get()).displayClientMessage(message, false);
                    }
                });
            }
        }

        @SubscribeEvent
        public static void onLivingHurt(final LivingHurtEvent event) {
            if (!event.isCanceled() && !event.getEntityLiving().level.isClientSide() && event.getEntityLiving().isEffectiveAi() && event.getEntityLiving().isAlive()) {
                if (event.getSource().getDirectEntity() != null && event.getEntityLiving() instanceof PlayerEntity) {
                    PlayerEntity player = (PlayerEntity) event.getEntityLiving();
                    Entity source = event.getSource().getDirectEntity();
                    if (!player.isSpectator() && !player.isCreative()) {
                        // onEntityHurtPlayer
                        player.getCapability(RPGGods.FAVOR).ifPresent(f -> {
                            triggerCondition(PerkCondition.Type.ENTITY_HURT_PLAYER, player, f, Optional.of(source),
                                    Optional.of(source.getType().getRegistryName()), Optional.of(event));
                        });
                    }
                } else if (event.getSource().getDirectEntity() instanceof PlayerEntity) {
                    PlayerEntity player = (PlayerEntity) event.getSource().getDirectEntity();
                    LivingEntity target = event.getEntityLiving();
                    // onPlayerHurtEntity
                    player.getCapability(RPGGods.FAVOR).ifPresent(f -> {
                        triggerCondition(PerkCondition.Type.PLAYER_HURT_ENTITY, player, f, Optional.of(target),
                                Optional.of(target.getType().getRegistryName()), Optional.of(event));
                        // onEnterCombat
                        if (player.getCombatTracker().getCombatDuration() < COMBAT_TIMER) {
                            triggerCondition(PerkCondition.Type.ENTER_COMBAT, player, f,
                                    Optional.of(target), Optional.of(target.getType().getRegistryName()),
                                    Optional.empty());
                        }
                    });
                }
            }
        }

        @SubscribeEvent
        public static void onEntityInteract(final PlayerInteractEvent.EntityInteract event) {
            if (!event.getPlayer().level.isClientSide && event.getHand() == Hand.MAIN_HAND) {
                // onPlayerInteractEntity
                event.getPlayer().getCapability(RPGGods.FAVOR).ifPresent(f -> {
                    final ResourceLocation id = event.getTarget().getType().getRegistryName();
                    if (triggerCondition(PerkCondition.Type.PLAYER_INTERACT_ENTITY, event.getPlayer(), f, Optional.of(event.getTarget()), Optional.of(id), Optional.empty())) {
                        event.setCancellationResult(ActionResultType.SUCCESS);
                    }
                    if (event.getTarget() instanceof IMerchant && triggerPerks(PerkAction.Type.SPECIAL_PRICE, event.getPlayer(), f, Optional.of(event.getTarget()), Optional.of(id), Optional.of(event))) {
                        event.setCancellationResult(event.isCanceled() ? ActionResultType.FAIL : ActionResultType.SUCCESS);
                    }
                });
                // toggle sitting for tamed mobs
                if (null == event.getCancellationResult() || !event.getCancellationResult().consumesAction()) {
                    event.getTarget().getCapability(RPGGods.TAMEABLE).ifPresent(t -> {
                        if (t.isOwner(event.getPlayer())) {
                            t.setSittingWithUpdate(event.getTarget(), !t.isSitting());
                            // send packet to notify client of new sitting state
                            RPGGods.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> event.getPlayer()),
                                    new SUpdateSittingPacket(event.getTarget().getId(), t.isSitting()));
                            event.setCancellationResult(ActionResultType.SUCCESS);
                        }
                    });
                }
            }
        }

        @SubscribeEvent
        public static void onInteractBlock(final PlayerInteractEvent.RightClickBlock event) {
            if (!event.getPlayer().level.isClientSide) {
                BlockState state = event.getPlayer().level.getBlockState(event.getHitVec().getBlockPos());
                ResourceLocation blockId = state.getBlock().getRegistryName();
                if (blockId != null) {
                    // onPlayerInteractBlock
                    event.getPlayer().getCapability(RPGGods.FAVOR).ifPresent(f -> {
                        triggerCondition(PerkCondition.Type.PLAYER_INTERACT_BLOCK, event.getPlayer(), f, Optional.empty(),
                                Optional.of(blockId), Optional.empty());
                    });
                }
            }
        }

        @SubscribeEvent
        public static void onChangeFavor(FavorChangedEvent.Post event) {
            if (!event.getPlayer().level.isClientSide && event.isLevelChange()) {
                // onFavorChanged
                event.getPlayer().getCapability(RPGGods.FAVOR).ifPresent(f -> {
                    triggerPerks(PerkAction.Type.UNLOCK, event.getPlayer(), f, Optional.empty());
                });
            }
        }

        private static final AttributeModifier MOB_ATTACK = new AttributeModifier(
                UUID.fromString("2953b29d-7974-45e0-9a52-24b6ed738197"),
                "mob_attack", 1.0D, AttributeModifier.Operation.ADDITION);

        @SubscribeEvent
        public static void onEntityJoinWorld(final EntityJoinWorldEvent event) {
            if (!event.getEntity().level.isClientSide && (event.getEntity() instanceof ArrowEntity || event.getEntity() instanceof SpectralArrowEntity)) {
                final AbstractArrowEntity arrow = (AbstractArrowEntity) event.getEntity();
                final Entity thrower = arrow.getOwner();
                if (thrower instanceof PlayerEntity) {
                    // onArrowDamage, onArrowEffect, onArrowCount
                    thrower.getCapability(RPGGods.FAVOR).ifPresent(f -> {
                        triggerPerks(PerkAction.Type.ARROW_DAMAGE, (PlayerEntity) thrower, f, Optional.of(arrow));
                        triggerPerks(PerkAction.Type.ARROW_EFFECT, (PlayerEntity) thrower, f, Optional.of(arrow));
                        triggerPerks(PerkAction.Type.ARROW_COUNT, (PlayerEntity) thrower, f, Optional.of(arrow));
                    });
                }
            }
            if (!event.getEntity().level.isClientSide && event.getEntity() instanceof MobEntity) {
                MobEntity mob = (MobEntity) event.getEntity();
                boolean fleeEnabled = RPGGods.CONFIG.isFleeEnabled();
                boolean hostileEnabled= RPGGods.CONFIG.isHostileEnabled();
                boolean passiveEnabled = RPGGods.CONFIG.isPassiveEnabled();
                boolean tameableEnabled = RPGGods.CONFIG.isTameableEnabled();
                boolean checkAttackGoal = false;
                // add tameable goals
                if(tameableEnabled && event.getEntity().getCapability(RPGGods.TAMEABLE).isPresent()) {
                    mob.goalSelector.addGoal(0, new AffinityGoal.SittingGoal(mob));
                    mob.goalSelector.addGoal(0, new AffinityGoal.SittingResetGoal(mob));
                    mob.goalSelector.addGoal(1, new AffinityGoal.FollowOwnerGoal(mob, 1.0D, 10.0F, 5.0F, false));
                    mob.goalSelector.addGoal(1, new AffinityGoal.OwnerHurtByTargetGoal(mob));
                    mob.goalSelector.addGoal(1, new AffinityGoal.OwnerHurtTargetGoal(mob));
                    checkAttackGoal = true;
                }
                // add flee goal
                if(fleeEnabled && event.getEntity() instanceof CreatureEntity) {
                    mob.goalSelector.addGoal(1, new AffinityGoal.FleeGoal((CreatureEntity) mob));
                }
                // add hostile goal
                if(hostileEnabled) {
                    mob.goalSelector.addGoal(4, new AffinityGoal.NearestAttackableGoal(mob, 0.1F));
                    checkAttackGoal = true;
                }
                // add target reset goal
                if(hostileEnabled || passiveEnabled) {
                    mob.goalSelector.addGoal(2, new AffinityGoal.NearestAttackableResetGoal(mob));
                }
                // ensure target has attack goal
                if(checkAttackGoal && event.getEntity() instanceof CreatureEntity
                        && !(event.getEntity() instanceof IRangedAttackMob)
                        && !(event.getEntity() instanceof IAngerable)) {
                    // check for existing attack goal
                    boolean hasAttackGoal = false;
                    for(Goal g : mob.goalSelector.getRunningGoals().collect(Collectors.toList())) {
                        if(g instanceof MeleeAttackGoal) {
                            hasAttackGoal = true;
                            break;
                        }
                    }
                    // add attack goal if none was found
                    if(!hasAttackGoal) {
                        mob.goalSelector.addGoal(4, new MeleeAttackGoal((CreatureEntity) event.getEntity(), 1.2D, false));
                        // ensure mob has attack damage
                        ModifiableAttributeInstance attack = mob.getAttribute(Attributes.ATTACK_DAMAGE);
                        if(attack != null && attack.getBaseValue() < 0.5D && !attack.hasModifier(MOB_ATTACK)) {
                            attack.addPermanentModifier(MOB_ATTACK);
                        }
                    }
                }
            }
        }

        @SubscribeEvent
        public static void onAddPotion(final PotionEvent.PotionAddedEvent event) {
            if (!event.isCanceled() && event.getEntityLiving() instanceof PlayerEntity && !event.getEntityLiving().level.isClientSide
                    && event.getEntityLiving().isAlive()) {
                PlayerEntity player = (PlayerEntity) event.getEntityLiving();
                if (!player.isSpectator() && !player.isCreative()) {
                    // onEffectStart
                    player.getCapability(RPGGods.FAVOR).ifPresent(f -> {
                        triggerCondition(PerkCondition.Type.EFFECT_START, player, f, Optional.empty(),
                                Optional.of(event.getPotionEffect().getEffect().getRegistryName()), Optional.empty());
                    });
                }
            }
        }

        @SubscribeEvent
        public static void onLivingTarget(final LivingSetAttackTargetEvent event) {
            if (!event.getEntityLiving().level.isClientSide && event.getEntityLiving() instanceof MobEntity
                    && event.getTarget() instanceof PlayerEntity) {
                // Determine if entity is passive or hostile toward target
                ImmutablePair<Boolean, Boolean> passiveHostile = AffinityGoal.getPassiveAndHostile(event.getEntityLiving(), event.getTarget());
                if (passiveHostile.getLeft()) {
                    ((MobEntity) event.getEntityLiving()).setTarget(null);
                    return;
                }
            }
        }

        @SubscribeEvent
        public static void onBabySpawn(final BabyEntitySpawnEvent event) {
            if (!event.isCanceled() && event.getParentA().isEffectiveAi() && event.getCausedByPlayer() != null
                    && !event.getCausedByPlayer().isCreative() && !event.getCausedByPlayer().isSpectator()
                    && event.getParentA() instanceof AnimalEntity && event.getParentB() instanceof AnimalEntity) {
                event.getCausedByPlayer().getCapability(RPGGods.FAVOR).ifPresent(f -> {
                    triggerPerks(PerkAction.Type.OFFSPRING, event.getCausedByPlayer(), f, Optional.of(event.getParentA()), Optional.empty(), Optional.of(event));
                });
            }
        }

        @SubscribeEvent
        public static void onPlayerPickupXp(final PlayerXpEvent.PickupXp event) {
            if (event.getPlayer().isEffectiveAi() && !event.getPlayer().level.isClientSide()) {
                event.getPlayer().getCapability(RPGGods.FAVOR).ifPresent(f -> {
                    triggerPerks(PerkAction.Type.XP, event.getPlayer(), f, Optional.of(event.getOrb()));
                });
            }
        }

        @SubscribeEvent
        public static void onPlayerTick(final TickEvent.PlayerTickEvent event) {
            if (!event.isCanceled() && !event.player.level.isClientSide() && event.player.isEffectiveAi()
                    && event.player.isAlive() && canTickFavor(event.player)) {
                event.player.getCapability(RPGGods.FAVOR).ifPresent(f -> {
                    // trigger perks
                    if (Math.random() < RPGGods.CONFIG.getRandomPerkChance()) {
                        // onRandomTick
                        triggerCondition(PerkCondition.Type.RANDOM_TICK, event.player, f, Optional.empty(),
                                Optional.empty(), Optional.empty());
                    }
                    // reduce cooldown
                    f.tickCooldown(event.player.level.getGameTime());
                    // deplete favor
                    if (Math.random() < RPGGods.CONFIG.getFavorDecayRate()) {
                        f.depleteFavor(event.player);
                    }
                });
            }
        }

        public static boolean canTickFavor(final LivingEntity entity) {
            return RPGGods.CONFIG.isFavorEnabled() && (entity.tickCount + entity.getId()) % RPGGods.CONFIG.getFavorUpdateRate() == 0;
        }
    }

    public static class ClientEvents {

        @SubscribeEvent
        public static void onRenderLiving(final net.minecraftforge.client.event.RenderLivingEvent.Pre<?,?> event) {
            if(event.getEntity().isAlive() && event.getEntity() instanceof MobEntity && !(event.getEntity() instanceof TameableEntity)) {
                LazyOptional<ITameable> tameable = event.getEntity().getCapability(RPGGods.TAMEABLE);
                if(tameable.isPresent() && tameable.orElse(null).isSitting()) {
                    // shift down when sitting
                    ResourceLocation id = event.getEntity().getType().getRegistryName();
                    double dy = RPGGods.CONFIG.isSittingMob(id) ? -0.5D : -0.125D;
                    event.getMatrixStack().translate(0, dy, 0);
                }
            }
        }
    }
}
