package rpggods.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraftforge.common.util.LazyOptional;
import org.apache.commons.lang3.tuple.ImmutablePair;
import rpggods.RPGGods;
import rpggods.favor.Favor;
import rpggods.favor.FavorRange;
import rpggods.favor.IFavor;
import rpggods.perk.Affinity;
import rpggods.perk.Perk;
import rpggods.tameable.ITameable;
import rpggods.tameable.Tameable;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class AffinityGoal {

    private static boolean shouldAttackEntity(LivingEntity target, LivingEntity owner) {
        if (target instanceof Ghast) {
            return false;
        } else if (target instanceof Player && owner instanceof Player && !((Player)owner).canHarmPlayer((Player)target)) {
            return false;
        } else if (target instanceof AbstractHorse && ((AbstractHorse)target).isTamed()) {
            return false;
        } else if(target instanceof TamableAnimal && ((TamableAnimal)target).isTame()) {
            return false;
        } else if (target instanceof Mob) {
            ITameable t = target.getCapability(RPGGods.TAMEABLE).orElse(Tameable.EMPTY);
            return !t.isTamed() || !t.getOwnerId().isPresent() || !t.getOwnerId().get().equals(owner.getUUID());
        }

        return false;
    }

    /**
     * Determines if the given target is within range to cause the entity to be passive or hostile.
     * If one of the return parameters is true, the entity should have that affinity enforced.
     * @param creature the entity
     * @param target the target (such as a player)
     * @return an Immutable Pair where Left=isPassive and Right=isHostile
     */
    public static ImmutablePair<Boolean, Boolean> getPassiveAndHostile(final LivingEntity creature, final LivingEntity target) {
        final ResourceLocation id = creature.getType().getRegistryName();
        // passive behavior based on tame status
        if(AffinityGoal.isOwnerOrTeam(creature, target)) {
            return ImmutablePair.of(true, false);
        }
        // passive behavior based on favor
        if(target != creature.getLastHurtByMob()) {
            LazyOptional<IFavor> favor = RPGGods.getFavor(target);
            if(favor.isPresent()) {
                IFavor f = favor.orElse(null);
                if(!f.isEnabled()) {
                    return ImmutablePair.of(false, false);
                }
                boolean isPassive = isPassive(creature, f);
                boolean isHostile = isHostile(creature, f);
                // log error if there are conflicts
                if(isPassive && isHostile) {
                    final Map<Affinity.Type, List<ResourceLocation>> affinityMap = RPGGods.AFFINITY.getOrDefault(id, ImmutableMap.of());
                    final List<FavorRange> passivePerks = affinityMap.getOrDefault(Affinity.Type.PASSIVE, ImmutableList.of())
                            .stream().map(r -> RPGGods.PERK_MAP.getOrDefault(id, Perk.EMPTY)).map(Perk::getRange).collect(Collectors.toList());
                    final List<FavorRange> hostilePerks = affinityMap.getOrDefault(Affinity.Type.HOSTILE, ImmutableList.of())
                            .stream().map(r -> RPGGods.PERK_MAP.getOrDefault(id, Perk.EMPTY)).map(Perk::getRange).collect(Collectors.toList());;
                    RPGGods.LOGGER.error("Conflicting affinity perks for " + id + " ; Hostile is " + hostilePerks + " and Passive is " + passivePerks);
                    return ImmutablePair.of(false, false);
                }
                return ImmutablePair.of(isPassive, isHostile);
            }
        }
        return ImmutablePair.of(false, false);
    }

    /**
     * @param creature the creature that wants to know if it is passive
     * @param playerFavor the player favor capability
     * @return true if the creature is passive toward players with the given favor
     */
    public static boolean isPassive(final LivingEntity creature, final IFavor playerFavor) {
        final ResourceLocation id = creature.getType().getRegistryName();
        Perk p;
        for(ResourceLocation r : RPGGods.AFFINITY.getOrDefault(id, ImmutableMap.of()).getOrDefault(Affinity.Type.PASSIVE, ImmutableList.of())) {
            p = RPGGods.PERK_MAP.get(r);
            if(p != null && p.getRange().isInRange(playerFavor)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param creature the creature that wants to know if it is hostile
     * @param playerFavor the player favor capability
     * @return true if the creature is hostile toward players with the given favor
     */
    public static boolean isHostile(final LivingEntity creature, final IFavor playerFavor) {
        final ResourceLocation id = creature.getType().getRegistryName();
        Perk p;
        for(ResourceLocation r : RPGGods.AFFINITY.getOrDefault(id, ImmutableMap.of()).getOrDefault(Affinity.Type.HOSTILE, ImmutableList.of())) {
            p = RPGGods.PERK_MAP.get(r);
            if(p != null && p.getRange().isInRange(playerFavor)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isOwnerOrTeam(final LivingEntity mob, final LivingEntity target) {
        LazyOptional<ITameable> tameable = mob.getCapability(RPGGods.TAMEABLE);
        if(tameable.isPresent()) {
            ITameable t = tameable.orElse(null);
            Optional<LivingEntity> owner = t.getOwner(mob.level);
            // tamed entity should not attack owner or owner team
            if(t.isOwner(target) || (owner.isPresent() && owner.get().isAlliedTo(target))) {
                return true;
            }
        }
        return false;
    }

    public static class AffinityNearestAttackableGoal extends NearestAttackableTargetGoal<Player> {

        public AffinityNearestAttackableGoal(final Mob entity, float chance) {
            super(entity, Player.class, Math.round(chance * 100), true, false, e -> getPassiveAndHostile(entity, e).getRight());
        }
    }

    public static class AffinityNearestAttackableResetGoal extends Goal {
        protected Mob entity;
        protected int interval;

        protected final Predicate<LivingEntity> passivePredicate;

        public AffinityNearestAttackableResetGoal(final Mob entityIn) { this(entityIn, 10, e -> getPassiveAndHostile(entityIn, e).getLeft()); }

        public AffinityNearestAttackableResetGoal(final Mob entityIn, int intervalIn, Predicate<LivingEntity> passivePredicate) {
            entity = entityIn;
            interval = intervalIn;
            this.passivePredicate = passivePredicate;
            this.setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            final LivingEntity target = entity.getTarget();
            if(entity.tickCount % interval == 0 && entity.isAlive() && target instanceof Player
                    && target != entity.getLastHurtByMob()
                    && !(entity instanceof NeutralMob && target.getUUID().equals(((NeutralMob)entity).getPersistentAngerTarget()))) {
                return passivePredicate.test(target);
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() { return false; }

        @Override
        public void start() {
            entity.setTarget(null);
        }
    }

    public static class AffinityFleeGoal extends AvoidEntityGoal<Player> {

        public AffinityFleeGoal(final PathfinderMob entityIn) {
            this(entityIn, 8.0F);
        }

        public AffinityFleeGoal(final PathfinderMob owner, float distanceIn) {
            super(owner, Player.class, distanceIn, 1.30D, 1.20D, createAvoidPredicate(owner));
        }

        private static Predicate<LivingEntity> createAvoidPredicate(final PathfinderMob creature) {
            final ResourceLocation id = creature.getType().getRegistryName();
            return e -> {
                if(e instanceof Player && e != creature.getLastHurtByMob() && !isOwnerOrTeam(creature, e)) {
                    List<ResourceLocation> perks = RPGGods.AFFINITY.getOrDefault(id, ImmutableMap.of()).getOrDefault(Affinity.Type.FLEE, ImmutableList.of());
                    if(perks.size() > 0) {
                        IFavor favor = RPGGods.getFavor(e).orElse(Favor.EMPTY);
                        if(favor.isEnabled()) {
                            Perk p;
                            for(ResourceLocation r : perks) {
                                p = RPGGods.PERK_MAP.get(r);
                                if(p != null && p.getRange().isInRange(favor)) {
                                    return true;
                                }
                            }
                        }
                    }
                }
                return false;
            };
        }
    }

    public static class AffinitySittingGoal extends Goal {
        private final Mob entity;

        public AffinitySittingGoal(Mob entity) {
            this.entity = entity;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            LazyOptional<ITameable> tameable = entity.getCapability(RPGGods.TAMEABLE);
            if(tameable.isPresent()) {
                ITameable t = tameable.orElse(null);
                return t.isTamed() && t.isSitting();
            }
            return false;
        }

        @Override
        public void tick() {
            entity.getNavigation().stop();
        }
    }

    public static class AffinitySittingResetGoal extends Goal {
        private final Mob entity;

        public AffinitySittingResetGoal(Mob entity) {
            this.entity = entity;
        }

        @Override
        public boolean canUse() {
            ITameable t = entity.getCapability(RPGGods.TAMEABLE).orElse(Tameable.EMPTY);
            return t != Tameable.EMPTY && t.isTamed() && t.isSitting() && entity.hurtTime > 0;
        }

        @Override
        public void start() {
            entity.getCapability(RPGGods.TAMEABLE).ifPresent(t -> {
                t.setSittingWithUpdate(entity, false);
            });
        }
    }

    public static class AffinityFollowOwnerGoal extends Goal {
        private final Mob entity;
        private LivingEntity owner;
        private final double followSpeed;
        private final PathNavigation navigator;
        private int timeToRecalcPath;
        private final float closeDist;
        private final float farDist;
        private float oldWaterCost;
        private final boolean teleportToLeaves;

        public AffinityFollowOwnerGoal(Mob entityIn, double followSpeedIn, float farDistance, float closeDistance, boolean teleportToLeavesIn) {
            this.entity = entityIn;
            this.followSpeed = followSpeedIn;
            this.navigator = entityIn.getNavigation();
            this.farDist = farDistance;
            this.closeDist = closeDistance;
            this.teleportToLeaves = teleportToLeavesIn;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            ITameable tameable = entity.getCapability(RPGGods.TAMEABLE).orElse(Tameable.EMPTY);
            Optional<LivingEntity> owner = tameable.getOwner(entity.level);
            if (!owner.isPresent()) {
                return false;
            }
            this.owner = owner.get();
            if (!tameable.isTamed() || tameable.isSitting() || this.owner.isSpectator()
                    || this.entity.distanceToSqr(this.owner) < (this.farDist * this.farDist)) {
                return false;
            }
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            if (this.navigator.isDone()) {
                return false;
            }
            if (this.entity.distanceToSqr(this.owner) <= (this.closeDist * this.closeDist)) {
                return false;
            }
            return true;
        }

        @Override
        public void start() {
            this.timeToRecalcPath = 0;
            this.oldWaterCost = this.entity.getPathfindingMalus(BlockPathTypes.WATER);
            this.entity.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
        }

        @Override
        public void stop() {
            this.owner = null;
            this.navigator.stop();
            this.entity.setPathfindingMalus(BlockPathTypes.WATER, this.oldWaterCost);
        }

        @Override
        public void tick() {
            this.entity.getLookControl().setLookAt(this.owner, 10.0F, this.entity.getMaxHeadXRot());

            if (--this.timeToRecalcPath > 0) {
                return;
            }
            this.timeToRecalcPath = 10;

            if (this.entity.isLeashed() || this.entity.isPassenger()) {
                return;
            }

            if (this.entity.distanceToSqr(this.owner) >= 4 * (this.farDist * this.farDist)) {
                tryToTeleportNearEntity();
            } else {
                this.navigator.moveTo(this.owner, this.followSpeed);
            }
        }

        private void tryToTeleportNearEntity() {
            BlockPos ownerPos = this.owner.blockPosition();

            for (int attempts = 0; attempts < 10; attempts++) {
                int x = getRandomNumber(-3, 3);
                int y = getRandomNumber(-1, 1);
                int z = getRandomNumber(-3, 3);
                boolean teleportSuccess = tryToTeleportToLocation(ownerPos.getX() + x, ownerPos.getY() + y,
                        ownerPos.getZ() + z);
                if (teleportSuccess) {
                    return;
                }
            }
        }

        private boolean tryToTeleportToLocation(int x, int y, int z) {
            if (Math.abs(x - this.owner.getX()) < 2.0D && Math.abs(z - this.owner.getZ()) < 2.0D) {
                return false;
            }
            if (!isTeleportFriendlyBlock(new BlockPos(x, y, z))) {
                return false;
            }
            this.entity.moveTo(x + 0.5D, y, z + 0.5D, this.entity.getYRot(), this.entity.getXRot());
            this.navigator.stop();
            return true;
        }

        private boolean isTeleportFriendlyBlock(BlockPos pos) {
            BlockPathTypes pathType = WalkNodeEvaluator.getBlockPathTypeStatic(this.entity.level, pos.mutable());

            if (pathType != BlockPathTypes.WALKABLE) {
                return false;
            }

            BlockState posDown = this.entity.level.getBlockState(pos.below());
            if (!this.teleportToLeaves && posDown.getBlock() instanceof net.minecraft.world.level.block.LeavesBlock) {
                return false;
            }

            BlockPos distance = pos.subtract(this.entity.blockPosition());
            if (!this.entity.level.noCollision(this.entity, this.entity.getBoundingBox().move(distance))) {
                return false;
            }

            return true;
        }

        private int getRandomNumber(int min, int max) {
            return this.entity.getRandom().nextInt(max - min + 1) + min;
        }
    }

    public static class AffinityOwnerHurtByTargetGoal extends TargetGoal {
        private LivingEntity attacker;
        private LivingEntity owner;
        private int timestamp;

        public AffinityOwnerHurtByTargetGoal(Mob entity) {
            super(entity, false);
            this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        /**
         * Returns whether execution should begin. You can also read and cache any state necessary for execution in this
         * method as well.
         */
        public boolean canUse() {
            ITameable tameable = mob.getCapability(RPGGods.TAMEABLE).orElse(Tameable.EMPTY);
            Optional<LivingEntity> owner = tameable.getOwner(mob.level);
            if (tameable == Tameable.EMPTY || !owner.isPresent() || !tameable.isTamed() || tameable.isSitting()) {
                return false;
            }
            this.owner = owner.get();
            this.attacker = this.owner.getLastHurtByMob();
            int i = this.owner.getLastHurtByMobTimestamp();
            return i != this.timestamp && this.canAttack(this.attacker, TargetingConditions.DEFAULT) && AffinityGoal.shouldAttackEntity(this.attacker, this.owner);
        }

        /**
         * Execute a one shot task or start executing a continuous task
         */
        public void start() {
            this.mob.setTarget(this.attacker);
            if (this.owner != null) {
                this.timestamp = this.owner.getLastHurtByMobTimestamp();
            }
            super.start();
        }
    }

    public static class AffinityOwnerHurtTargetGoal extends TargetGoal {
        private final Mob entity;
        private LivingEntity owner;
        private LivingEntity attacker;
        private int timestamp;

        public AffinityOwnerHurtTargetGoal(Mob entity) {
            super(entity, false);
            this.entity = entity;
            this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        /**
         * Returns whether execution should begin. You can also read and cache any state necessary for execution in this
         * method as well.
         */
        public boolean canUse() {
            ITameable tameable = mob.getCapability(RPGGods.TAMEABLE).orElse(Tameable.EMPTY);
            Optional<LivingEntity> owner = tameable.getOwner(mob.level);
            if (tameable == Tameable.EMPTY || !owner.isPresent() || !tameable.isTamed() || tameable.isSitting()) {
                return false;
            }
            this.owner = owner.get();
            this.attacker = this.owner.getLastHurtMob();
            int i = this.owner.getLastHurtMobTimestamp();
            return i != this.timestamp && this.canAttack(this.attacker, TargetingConditions.DEFAULT) && AffinityGoal.shouldAttackEntity(this.attacker, this.owner);
        }

        /**
         * Execute a one shot task or start executing a continuous task
         */
        public void start() {
            this.mob.setTarget(this.attacker);
            if (this.owner != null) {
                this.timestamp = this.owner.getLastHurtMobTimestamp();
            }

            super.start();
        }
    }

    public static class AffinityMeleeAttackGoal extends MeleeAttackGoal {

        public AffinityMeleeAttackGoal(PathfinderMob mob, double speedModifier, boolean followEvenIfNotSeen) {
            super(mob, speedModifier, followEvenIfNotSeen);
        }

        @Override
        public boolean canUse() {
            if(this.mob.getTarget() instanceof Player player) {
                IFavor favor = RPGGods.getFavor(player).orElse(Favor.EMPTY);
                return super.canUse() && isHostile(mob, favor);
            }
            return false;
        }
    }
}
