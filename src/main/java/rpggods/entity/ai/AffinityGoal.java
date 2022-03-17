package rpggods.entity.ai;

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
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.LazyOptional;
import rpggods.RPGGods;
import rpggods.favor.IFavor;
import rpggods.perk.Affinity;
import rpggods.perk.Perk;
import rpggods.tameable.ITameable;

import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public class AffinityGoal {

    private static boolean shouldAttackEntity(LivingEntity target, LivingEntity owner) {
        if (!(target instanceof CreeperEntity) && !(target instanceof GhastEntity)) {
            if (target instanceof MobEntity) {
                ITameable t = target.getCapability(RPGGods.TAMEABLE).orElse(RPGGods.TAMEABLE.getDefaultInstance());
                return !t.isTamed() || !t.getOwnerId().isPresent() || !t.getOwnerId().get().equals(owner.getUniqueID());
            } else if (target instanceof PlayerEntity && owner instanceof PlayerEntity && !((PlayerEntity)owner).canAttackPlayer((PlayerEntity)target)) {
                return false;
            } else if (target instanceof AbstractHorseEntity && ((AbstractHorseEntity)target).isTame()) {
                return false;
            } else {
                return !(target instanceof TameableEntity) || !((TameableEntity)target).isTamed();
            }
        } else {
            return false;
        }
    }

    public static Predicate<LivingEntity> createAttackPredicate(final MobEntity creature) {
        final ResourceLocation id = creature.getType().getRegistryName();
        return e -> {
            // passive behavior based on tame status
            LazyOptional<ITameable> tameable = creature.getCapability(RPGGods.TAMEABLE);
            if(tameable.isPresent()) {
                ITameable t = tameable.orElse(null);
                Optional<LivingEntity> owner = t.getOwner(creature.world);
                // tamed entity should not attack owner or owner team
                if(t.isOwner(e) || (owner.isPresent() && owner.get().isOnSameTeam(e))) {
                    return false;
                }
            }
            // passive behavior based on favor
            if(e != creature.getRevengeTarget()) {
                final Map<Affinity.Type, Perk> affinityMap = RPGGods.AFFINITY.getOrDefault(id, ImmutableMap.of());
                LazyOptional<IFavor> favor = e.getCapability(RPGGods.FAVOR);
                if(favor.isPresent()) {
                    IFavor f = favor.orElse(null);
                    Perk passivePerk = affinityMap.get(Affinity.Type.PASSIVE);
                    Perk hostilePerk = affinityMap.get(Affinity.Type.HOSTILE);
                    boolean isPassive = passivePerk != null && passivePerk.getRange().isInRange(f);
                    boolean isHostile = hostilePerk != null && hostilePerk.getRange().isInRange(f);
                    // passive entity should not attack unless another perk enables hostility
                    if(isPassive && isHostile) {
                        RPGGods.LOGGER.error("Conflicting affinity perks for " + id +
                                " ; Hostile is " + hostilePerk.getRange() + " and Passive is " + passivePerk.getRange());
                        return false;
                    }
                    return f.isEnabled() && isHostile;
                }
            }
            return false;
        };
    }

    public static Predicate<LivingEntity> createPassivePredicate(final MobEntity creature) {
        final ResourceLocation id = creature.getType().getRegistryName();
        return e -> {
            // passive behavior based on tame status
            LazyOptional<ITameable> tameable = creature.getCapability(RPGGods.TAMEABLE);
            if(tameable.isPresent()) {
                ITameable t = tameable.orElse(null);
                Optional<LivingEntity> owner = t.getOwner(creature.world);
                // tamed entity should not attack owner or owner team
                if(t.isOwner(e) || (owner.isPresent() && owner.get().isOnSameTeam(e))) {
                    return true;
                }
            }
            // passive behavior based on favor
            if(e != creature.getRevengeTarget()) {
                final Map<Affinity.Type, Perk> affinityMap = RPGGods.AFFINITY.getOrDefault(id, ImmutableMap.of());
                LazyOptional<IFavor> favor = e.getCapability(RPGGods.FAVOR);
                if(favor.isPresent()) {
                    IFavor f = favor.orElse(null);
                    Perk passivePerk = affinityMap.get(Affinity.Type.PASSIVE);
                    Perk hostilePerk = affinityMap.get(Affinity.Type.HOSTILE);
                    boolean isPassive = passivePerk != null && passivePerk.getRange().isInRange(f);
                    boolean isHostile = hostilePerk != null && hostilePerk.getRange().isInRange(f);
                    // passive entity should not attack unless another perk enables hostility
                    if(isPassive && isHostile) {
                        RPGGods.LOGGER.error("Conflicting affinity perks for " + id +
                                " ; Hostile is " + hostilePerk.getRange() + " and Passive is " + passivePerk.getRange());
                        return false;
                    }
                    return f.isEnabled() && isPassive;
                }
            }
            return false;
        };
    }

    public static class NearestAttackableGoal extends NearestAttackableTargetGoal<PlayerEntity> {

        public NearestAttackableGoal(final MobEntity entity, float chance) {
            super(entity, PlayerEntity.class, Math.round(chance * 100), true, false, createAttackPredicate(entity));
        }
    }

    public static class NearestAttackableResetGoal extends Goal {
        protected MobEntity entity;
        protected int interval;

        protected final Predicate<LivingEntity> passivePredicate;

        public NearestAttackableResetGoal(final MobEntity entityIn) { this(entityIn, 10, createPassivePredicate(entityIn)); }

        public NearestAttackableResetGoal(final MobEntity entityIn, int intervalIn, Predicate<LivingEntity> passivePredicate) {
            entity = entityIn;
            interval = intervalIn;
            this.passivePredicate = passivePredicate;
            this.setMutexFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean shouldExecute() {
            final LivingEntity target = entity.getAttackTarget();
            if(entity.ticksExisted % interval == 0 && entity.isAlive() && target instanceof PlayerEntity
                    && target != entity.getRevengeTarget()
                    && !(entity instanceof IAngerable && target.getUniqueID().equals(((IAngerable)entity).getAngerTarget()))) {
                return passivePredicate.test(target);
            }
            return false;
        }

        @Override
        public boolean shouldContinueExecuting() { return false; }

        @Override
        public void startExecuting() {
            entity.setAttackTarget(null);
        }
    }

    public static class FleeGoal extends AvoidEntityGoal<PlayerEntity> {

        public FleeGoal(final CreatureEntity entityIn) {
            this(entityIn, 8.0F);
        }

        public FleeGoal(final CreatureEntity owner, float distanceIn) {
            super(owner, PlayerEntity.class, distanceIn, 1.30D, 1.20D, createAvoidPredicate(owner));
        }

        @Override
        public void startExecuting() {
            super.startExecuting();
            // TODO: cooldown?
        }

        private static Predicate<LivingEntity> createAvoidPredicate(final CreatureEntity creature) {
            final ResourceLocation id = creature.getType().getRegistryName();
            return e -> {
                if(e instanceof PlayerEntity && e != creature.getRevengeTarget() /* TODO && !isOwner((PlayerEntity) e, creature)*/) {
                    Perk perk = RPGGods.AFFINITY.getOrDefault(id, ImmutableMap.of()).get(Affinity.Type.FLEE);
                    if(perk != null) {
                        IFavor favor = e.getCapability(RPGGods.FAVOR).orElse(RPGGods.FAVOR.getDefaultInstance());
                        return favor.isEnabled() && perk.getRange().isInRange(favor) && Math.random() < perk.getChance()
                                && favor.hasNoPerkCooldown(perk.getCategory());
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
            this.setMutexFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.TARGET));
        }

        @Override
        public boolean shouldExecute() {
            LazyOptional<ITameable> tameable = entity.getCapability(RPGGods.TAMEABLE);
            if(tameable.isPresent()) {
                ITameable t = tameable.orElse(null);
                return t.isTamed() && t.isSitting();
            }
            return false;
        }

        @Override
        public void tick() {
            entity.getNavigator().clearPath();
        }
    }

    public static class SittingResetGoal extends Goal {
        private final MobEntity entity;

        public SittingResetGoal(MobEntity entity) {
            this.entity = entity;
        }

        @Override
        public boolean shouldExecute() {
            ITameable t = entity.getCapability(RPGGods.TAMEABLE).orElse(RPGGods.TAMEABLE.getDefaultInstance());
            return t.isTamed() && t.isSitting() && entity.hurtTime > 0;
        }

        @Override
        public void startExecuting() {
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
            this.navigator = entityIn.getNavigator();
            this.farDist = farDistance;
            this.closeDist = closeDistance;
            this.teleportToLeaves = teleportToLeavesIn;
            setMutexFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean shouldExecute() {
            ITameable tameable = entity.getCapability(RPGGods.TAMEABLE).orElse(RPGGods.TAMEABLE.getDefaultInstance());
            Optional<LivingEntity> owner = tameable.getOwner(entity.world);
            if (!owner.isPresent()) {
                return false;
            }
            this.owner = owner.get();
            if (!tameable.isTamed() || tameable.isSitting() || this.owner.isSpectator()
                    || this.entity.getDistanceSq(this.owner) < (this.farDist * this.farDist)) {
                return false;
            }
            return true;
        }

        @Override
        public boolean shouldContinueExecuting() {
            if (this.navigator.noPath()) {
                return false;
            }
            if (this.entity.getDistanceSq(this.owner) <= (this.closeDist * this.closeDist)) {
                return false;
            }
            return true;
        }

        @Override
        public void startExecuting() {
            this.timeToRecalcPath = 0;
            this.oldWaterCost = this.entity.getPathPriority(PathNodeType.WATER);
            this.entity.setPathPriority(PathNodeType.WATER, 0.0F);
        }

        @Override
        public void resetTask() {
            this.owner = null;
            this.navigator.clearPath();
            this.entity.setPathPriority(PathNodeType.WATER, this.oldWaterCost);
        }

        @Override
        public void tick() {
            this.entity.getLookController().setLookPositionWithEntity(this.owner, 10.0F, this.entity.getVerticalFaceSpeed());

            if (--this.timeToRecalcPath > 0) {
                return;
            }
            this.timeToRecalcPath = 10;

            if (this.entity.getLeashed() || this.entity.isPassenger()) {
                return;
            }

            if (this.entity.getDistanceSq(this.owner) >= 4 * (this.farDist * this.farDist)) {
                tryToTeleportNearEntity();
            } else {
                this.navigator.tryMoveToEntityLiving(this.owner, this.followSpeed);
            }
        }

        private void tryToTeleportNearEntity() {
            BlockPos ownerPos = this.owner.getPosition();

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
            if (Math.abs(x - this.owner.getPosX()) < 2.0D && Math.abs(z - this.owner.getPosZ()) < 2.0D) {
                return false;
            }
            if (!isTeleportFriendlyBlock(new BlockPos(x, y, z))) {
                return false;
            }
            this.entity.setLocationAndAngles(x + 0.5D, y, z + 0.5D, this.entity.rotationYaw,
                    this.entity.rotationPitch);
            this.navigator.clearPath();
            return true;
        }

        private boolean isTeleportFriendlyBlock(BlockPos pos) {
            PathNodeType pathType = WalkNodeProcessor.getFloorNodeType(this.entity.world, pos.toMutable());

            if (pathType != PathNodeType.WALKABLE) {
                return false;
            }

            BlockState posDown = this.entity.world.getBlockState(pos.down());
            if (!this.teleportToLeaves && posDown.getBlock() instanceof net.minecraft.block.LeavesBlock) {
                return false;
            }

            BlockPos distance = pos.subtract(this.entity.getPosition());
            if (!this.entity.world.hasNoCollisions(this.entity, this.entity.getBoundingBox().offset(distance))) {
                return false;
            }

            return true;
        }

        private int getRandomNumber(int min, int max) {
            return this.entity.getRNG().nextInt(max - min + 1) + min;
        }
    }

    public static class OwnerHurtByTargetGoal extends TargetGoal {
        private LivingEntity attacker;
        private LivingEntity owner;
        private int timestamp;

        public OwnerHurtByTargetGoal(MobEntity entity) {
            super(entity, false);
            this.setMutexFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        /**
         * Returns whether execution should begin. You can also read and cache any state necessary for execution in this
         * method as well.
         */
        public boolean shouldExecute() {
            ITameable tameable = goalOwner.getCapability(RPGGods.TAMEABLE).orElse(RPGGods.TAMEABLE.getDefaultInstance());
            Optional<LivingEntity> owner = tameable.getOwner(goalOwner.world);
            if (!owner.isPresent() || !tameable.isTamed() || tameable.isSitting()) {
                return false;
            }
            this.owner = owner.get();
            this.attacker = this.owner.getRevengeTarget();
            int i = this.owner.getRevengeTimer();
            return i != this.timestamp && this.isSuitableTarget(this.attacker, EntityPredicate.DEFAULT) && AffinityGoal.shouldAttackEntity(this.attacker, this.owner);
        }

        /**
         * Execute a one shot task or start executing a continuous task
         */
        public void startExecuting() {
            this.goalOwner.setAttackTarget(this.attacker);
            if (this.owner != null) {
                this.timestamp = this.owner.getRevengeTimer();
            }
            super.startExecuting();
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
            this.setMutexFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        /**
         * Returns whether execution should begin. You can also read and cache any state necessary for execution in this
         * method as well.
         */
        public boolean shouldExecute() {
            ITameable tameable = goalOwner.getCapability(RPGGods.TAMEABLE).orElse(RPGGods.TAMEABLE.getDefaultInstance());
            Optional<LivingEntity> owner = tameable.getOwner(goalOwner.world);
            if (!owner.isPresent() || !tameable.isTamed() || tameable.isSitting()) {
                return false;
            }
            this.owner = owner.get();
            this.attacker = this.owner.getLastAttackedEntity();
            int i = this.owner.getLastAttackedEntityTime();
            return i != this.timestamp && this.isSuitableTarget(this.attacker, EntityPredicate.DEFAULT) && AffinityGoal.shouldAttackEntity(this.attacker, this.owner);
        }

        /**
         * Execute a one shot task or start executing a continuous task
         */
        public void startExecuting() {
            this.goalOwner.setAttackTarget(this.attacker);
            if (this.owner != null) {
                this.timestamp = this.owner.getLastAttackedEntityTime();
            }

            super.startExecuting();
        }
    }
}
