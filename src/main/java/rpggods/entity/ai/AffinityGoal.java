package rpggods.entity.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.block.BlockState;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.EntityPredicate;
import net.minecraft.entity.IAngerable;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.NearestAttackableTargetGoal;
import net.minecraft.entity.ai.goal.TargetGoal;
import net.minecraft.entity.monster.CreeperEntity;
import net.minecraft.entity.monster.GhastEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.pathfinding.WalkNodeProcessor;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.LazyOptional;
import rpggods.RPGGods;
import rpggods.favor.FavorRange;
import rpggods.favor.IFavor;
import rpggods.perk.Affinity;
import rpggods.perk.Perk;
import rpggods.tameable.ITameable;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class AffinityGoal {

    private static boolean shouldAttackEntity(LivingEntity target, LivingEntity owner) {
        if (!(target instanceof CreeperEntity) && !(target instanceof GhastEntity)) {
            if (target instanceof MobEntity) {
                ITameable t = target.getCapability(RPGGods.TAMEABLE).orElse(RPGGods.TAMEABLE.getDefaultInstance());
                return !t.isTamed() || !t.getOwnerId().isPresent() || !t.getOwnerId().get().equals(owner.getUUID());
            } else if (target instanceof PlayerEntity && owner instanceof PlayerEntity && !((PlayerEntity)owner).canHarmPlayer((PlayerEntity)target)) {
                return false;
            } else if (target instanceof AbstractHorseEntity && ((AbstractHorseEntity)target).isTamed()) {
                return false;
            } else {
                return !(target instanceof TameableEntity) || !((TameableEntity)target).isTame();
            }
        } else {
            return false;
        }
    }

    /**
     * Determines if the given target is within range to cause the entity to be passive or hostile.
     * If one of the return parameters is true, the entity should have that affinity enforced.
     * @param creature the entity
     * @param target the target (such as a player)
     * @return a Tuple where A=isPassive and B=isHostile
     */
    public static Tuple<Boolean, Boolean> getPassiveAndHostile(final LivingEntity creature, final LivingEntity target) {
        final ResourceLocation id = creature.getType().getRegistryName();
        // passive behavior based on tame status
        if(AffinityGoal.isOwnerOrTeam(creature, target)) {
            return new Tuple<>(false, false);
        }
        // passive behavior based on favor
        if(target != creature.getLastHurtByMob()) {
            LazyOptional<IFavor> favor = target.getCapability(RPGGods.FAVOR);
            if(favor.isPresent()) {
                IFavor f = favor.orElse(null);
                if(!f.isEnabled()) {
                    return new Tuple<>(false, false);
                }
                boolean isPassive = isPassive(creature, f);
                boolean isHostile = isHostile(creature, f);
                // passive entity should not attack unless another perk enables hostility
                if(isPassive && isHostile) {
                    final Map<Affinity.Type, List<ResourceLocation>> affinityMap = RPGGods.AFFINITY.getOrDefault(id, ImmutableMap.of());
                    final List<FavorRange> passivePerks = affinityMap.getOrDefault(Affinity.Type.PASSIVE, ImmutableList.of())
                            .stream().map(r -> RPGGods.PERK.get(id).orElse(Perk.EMPTY)).map(Perk::getRange).collect(Collectors.toList());
                    final List<FavorRange> hostilePerks = affinityMap.getOrDefault(Affinity.Type.HOSTILE, ImmutableList.of())
                            .stream().map(r -> RPGGods.PERK.get(id).orElse(Perk.EMPTY)).map(Perk::getRange).collect(Collectors.toList());;
                    RPGGods.LOGGER.error("Conflicting affinity perks for " + id + " ; Hostile is " + hostilePerks + " and Passive is " + passivePerks);
                    return new Tuple<>(false, false);
                }
                return new Tuple<>(isPassive, isHostile);
            }
        }
        return new Tuple<>(false, false);
    }

    public static boolean isPassive(final LivingEntity creature, final IFavor playerFavor) {
        final ResourceLocation id = creature.getType().getRegistryName();
        Perk p;
        for(ResourceLocation r : RPGGods.AFFINITY.getOrDefault(id, ImmutableMap.of()).getOrDefault(Affinity.Type.PASSIVE, ImmutableList.of())) {
            p = RPGGods.PERK.get(r).orElse(null);
            if(p != null && p.getRange().isInRange(playerFavor)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isHostile(final LivingEntity creature, final IFavor playerFavor) {
        final ResourceLocation id = creature.getType().getRegistryName();
        Perk p;
        for(ResourceLocation r : RPGGods.AFFINITY.getOrDefault(id, ImmutableMap.of()).getOrDefault(Affinity.Type.HOSTILE, ImmutableList.of())) {
            p = RPGGods.PERK.get(r).orElse(null);
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

    public static class NearestAttackableGoal extends NearestAttackableTargetGoal<PlayerEntity> {

        public NearestAttackableGoal(final MobEntity entity, float chance) {
            super(entity, PlayerEntity.class, Math.round(chance * 100), true, false, e -> getPassiveAndHostile(entity, e).getB());
        }
    }

    public static class NearestAttackableResetGoal extends Goal {
        protected MobEntity entity;
        protected int interval;

        protected final Predicate<LivingEntity> passivePredicate;

        public NearestAttackableResetGoal(final MobEntity entityIn) { this(entityIn, 10, e -> getPassiveAndHostile(entityIn, e).getA()); }

        public NearestAttackableResetGoal(final MobEntity entityIn, int intervalIn, Predicate<LivingEntity> passivePredicate) {
            entity = entityIn;
            interval = intervalIn;
            this.passivePredicate = passivePredicate;
            this.setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            final LivingEntity target = entity.getTarget();
            if(entity.tickCount % interval == 0 && entity.isAlive() && target instanceof PlayerEntity
                    && target != entity.getLastHurtByMob()
                    && !(entity instanceof IAngerable && target.getUUID().equals(((IAngerable)entity).getPersistentAngerTarget()))) {
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

    public static class FleeGoal extends AvoidEntityGoal<PlayerEntity> {

        public FleeGoal(final CreatureEntity entityIn) {
            this(entityIn, 8.0F);
        }

        public FleeGoal(final CreatureEntity owner, float distanceIn) {
            super(owner, PlayerEntity.class, distanceIn, 1.30D, 1.20D, createAvoidPredicate(owner));
        }

        private static Predicate<LivingEntity> createAvoidPredicate(final CreatureEntity creature) {
            final ResourceLocation id = creature.getType().getRegistryName();
            return e -> {
                if(e instanceof PlayerEntity && e != creature.getLastHurtByMob() && !isOwnerOrTeam(creature, e)) {
                    List<ResourceLocation> perks = RPGGods.AFFINITY.getOrDefault(id, ImmutableMap.of()).getOrDefault(Affinity.Type.FLEE, ImmutableList.of());
                    if(perks.size() > 0) {
                        IFavor favor = e.getCapability(RPGGods.FAVOR).orElse(RPGGods.FAVOR.getDefaultInstance());
                        if(favor.isEnabled()) {
                            Perk p;
                            for(ResourceLocation r : perks) {
                                p = RPGGods.PERK.get(r).orElse(null);
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

    public static class SittingGoal extends Goal {
        private final MobEntity entity;

        public SittingGoal(MobEntity entity) {
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

    public static class SittingResetGoal extends Goal {
        private final MobEntity entity;

        public SittingResetGoal(MobEntity entity) {
            this.entity = entity;
        }

        @Override
        public boolean canUse() {
            ITameable t = entity.getCapability(RPGGods.TAMEABLE).orElse(RPGGods.TAMEABLE.getDefaultInstance());
            return t.isTamed() && t.isSitting() && entity.hurtTime > 0;
        }

        @Override
        public void start() {
            entity.getCapability(RPGGods.TAMEABLE).ifPresent(t -> t.setSitting(false));
        }
    }

    public static class FollowOwnerGoal extends Goal {
        private final MobEntity entity;
        private LivingEntity owner;
        private final double followSpeed;
        private final PathNavigator navigator;
        private int timeToRecalcPath;
        private final float closeDist;
        private final float farDist;
        private float oldWaterCost;
        private final boolean teleportToLeaves;

        public FollowOwnerGoal(MobEntity entityIn, double followSpeedIn, float farDistance, float closeDistance, boolean teleportToLeavesIn) {
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
            ITameable tameable = entity.getCapability(RPGGods.TAMEABLE).orElse(RPGGods.TAMEABLE.getDefaultInstance());
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
            this.oldWaterCost = this.entity.getPathfindingMalus(PathNodeType.WATER);
            this.entity.setPathfindingMalus(PathNodeType.WATER, 0.0F);
        }

        @Override
        public void stop() {
            this.owner = null;
            this.navigator.stop();
            this.entity.setPathfindingMalus(PathNodeType.WATER, this.oldWaterCost);
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
            this.entity.moveTo(x + 0.5D, y, z + 0.5D, this.entity.yRot,
                    this.entity.xRot);
            this.navigator.stop();
            return true;
        }

        private boolean isTeleportFriendlyBlock(BlockPos pos) {
            PathNodeType pathType = WalkNodeProcessor.getBlockPathTypeStatic(this.entity.level, pos.mutable());

            if (pathType != PathNodeType.WALKABLE) {
                return false;
            }

            BlockState posDown = this.entity.level.getBlockState(pos.below());
            if (!this.teleportToLeaves && posDown.getBlock() instanceof net.minecraft.block.LeavesBlock) {
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

    public static class OwnerHurtByTargetGoal extends TargetGoal {
        private LivingEntity attacker;
        private LivingEntity owner;
        private int timestamp;

        public OwnerHurtByTargetGoal(MobEntity entity) {
            super(entity, false);
            this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        /**
         * Returns whether execution should begin. You can also read and cache any state necessary for execution in this
         * method as well.
         */
        public boolean canUse() {
            ITameable tameable = mob.getCapability(RPGGods.TAMEABLE).orElse(RPGGods.TAMEABLE.getDefaultInstance());
            Optional<LivingEntity> owner = tameable.getOwner(mob.level);
            if (!owner.isPresent() || !tameable.isTamed() || tameable.isSitting()) {
                return false;
            }
            this.owner = owner.get();
            this.attacker = this.owner.getLastHurtByMob();
            int i = this.owner.getLastHurtByMobTimestamp();
            return i != this.timestamp && this.canAttack(this.attacker, EntityPredicate.DEFAULT) && AffinityGoal.shouldAttackEntity(this.attacker, this.owner);
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

    public static class OwnerHurtTargetGoal extends TargetGoal {
        private final MobEntity entity;
        private LivingEntity owner;
        private LivingEntity attacker;
        private int timestamp;

        public OwnerHurtTargetGoal(MobEntity entity) {
            super(entity, false);
            this.entity = entity;
            this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        /**
         * Returns whether execution should begin. You can also read and cache any state necessary for execution in this
         * method as well.
         */
        public boolean canUse() {
            ITameable tameable = mob.getCapability(RPGGods.TAMEABLE).orElse(RPGGods.TAMEABLE.getDefaultInstance());
            Optional<LivingEntity> owner = tameable.getOwner(mob.level);
            if (!owner.isPresent() || !tameable.isTamed() || tameable.isSitting()) {
                return false;
            }
            this.owner = owner.get();
            this.attacker = this.owner.getLastHurtMob();
            int i = this.owner.getLastHurtMobTimestamp();
            return i != this.timestamp && this.canAttack(this.attacker, EntityPredicate.DEFAULT) && AffinityGoal.shouldAttackEntity(this.attacker, this.owner);
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
}
