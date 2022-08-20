package rpggods.perk;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.util.StringRepresentable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.registries.ForgeRegistries;
import rpggods.RPGGods;
import rpggods.deity.Altar;
import rpggods.deity.Deity;
import rpggods.deity.DeityHelper;
import rpggods.event.FavorChangedEvent;
import rpggods.event.FavorEventHandler;
import rpggods.favor.FavorLevel;
import rpggods.favor.IFavor;
import rpggods.tameable.ITameable;

import java.util.Optional;
import java.util.Random;
import java.util.function.Supplier;

public final class PerkAction {

    public static final PerkAction EMPTY = new PerkAction(PerkAction.Type.FAVOR, Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), true);

    public static final Codec<PerkAction> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PerkAction.Type.CODEC.fieldOf("type").forGetter(PerkAction::getType),
            Codec.STRING.optionalFieldOf("data").forGetter(PerkAction::getString),
            ResourceLocation.CODEC.optionalFieldOf("id").forGetter(PerkAction::getId),
            CompoundTag.CODEC.optionalFieldOf("tag").forGetter(PerkAction::getTag),
            ItemStack.CODEC.optionalFieldOf("item").forGetter(PerkAction::getItem),
            Codec.LONG.optionalFieldOf("favor").forGetter(PerkAction::getFavor),
            Codec.FLOAT.optionalFieldOf("multiplier").forGetter(PerkAction::getMultiplier),
            Affinity.CODEC.optionalFieldOf("affinity").forGetter(PerkAction::getAffinity),
            Patron.CODEC.optionalFieldOf("patron").forGetter(PerkAction::getPatron),
            Codec.BOOL.optionalFieldOf("hidden", false).forGetter(PerkAction::isHidden)
    ).apply(instance, PerkAction::new));

    private final PerkAction.Type type;
    private final Optional<String> string;
    private final Optional<ResourceLocation> id;
    private final Optional<CompoundTag> tag;
    private final Optional<ItemStack> item;
    private final Optional<Long> favor;
    private final Optional<Float> multiplier;
    private final Optional<Affinity> affinity;
    private final Optional<Patron> patron;
    private final boolean hidden;

    public PerkAction(Type type, Optional<String> string, Optional<ResourceLocation> id, Optional<CompoundTag> tag,
                      Optional<ItemStack> item, Optional<Long> favor, Optional<Float> multiplier,
                      Optional<Affinity> affinity, Optional<Patron> patron, boolean hidden) {
        this.type = type;
        this.string = string;
        this.id = id;
        this.tag = tag;
        this.item = item;
        this.favor = favor;
        this.multiplier = multiplier;
        this.affinity = affinity;
        this.patron = patron;
        this.hidden = hidden;
    }

    /**
     * Runs a single Perk without any of the preliminary checks or cooldown.
     * If you want these, call {@link FavorEventHandler#runPerk(Perk, Player, IFavor)} or
     * {@link FavorEventHandler#runPerk(Perk, Player, IFavor, Optional, Optional, Optional)} instead.
     * @param deity the Deity that is associated with the perk
     * @param player the player
     * @param favor the player's favor
     * @param entity an entity to use when running the perk, if any
     * @param data a ResourceLocation ID to use when running the perk, if any
     * @param object the Event to reference when running the perk, if any
     * @return True if the action ran successfully
     */
    public boolean run(final ResourceLocation deity, final Player player, final IFavor favor,
                                        final Optional<Entity> entity, final Optional<ResourceLocation> data, final Optional<? extends Event> object) {
        switch (this.getType()) {
            case FUNCTION: return getId().isPresent() && FavorEventHandler.runFunction(player.level, player, getId().get());
            case POTION:
                if(getTag().isPresent()) {
                    Optional<MobEffectInstance> effect = readEffectInstance(getTag().get());
                    if(effect.isPresent()) {
                        return player.addEffect(effect.get());
                    }
                }
                return false;
            case SUMMON:
                float distance = getMultiplier().orElse(9F);
                return getTag().isPresent() && summonEntityNearPlayer(player.level, player, getTag(), distance).isPresent();
            case ITEM:
                if(getItem().isPresent()) {
                    ItemEntity itemEntity = new ItemEntity(player.level, player.getX(), player.getY(), player.getZ(), getItem().get().copy());
                    itemEntity.setNoPickUpDelay();
                    return player.level.addFreshEntity(itemEntity);
                }
                return false;
            case FAVOR:
                if(getFavor().isPresent() && getFavor().get() != 0 && getId().isPresent()) {
                    favor.getFavor(getId().get()).addFavor(player, getId().get(), getFavor().get(), FavorChangedEvent.Source.PERK);
                    return true;
                }
                return false;
            case AFFINITY:
                if(getAffinity().isPresent() && entity.isPresent() && data.isPresent() && getAffinity().get().getType() == Affinity.Type.TAME) {
                    LazyOptional<ITameable> tameable = entity.get().getCapability(RPGGods.TAMEABLE);
                    if(tameable.isPresent()) {
                        if(tameable.orElse(null).setTamedBy(player)) {
                            // set custom name
                            if(!entity.get().hasCustomName()) {
                                entity.get().setCustomName(entity.get().getDisplayName());
                            }
                            // send particle packet
                            if(entity.get().level instanceof ServerLevel) {
                                Vec3 pos = entity.get().getEyePosition(1.0F);
                                ((ServerLevel)entity.get().level).sendParticles(ParticleTypes.HEART, pos.x, pos.y, pos.z, 10, 0.5D, 0.5D, 0.5D, 0);
                            }
                            return true;
                        }
                    }
                }
                return false;
            case ARROW_DAMAGE:
                if(entity.isPresent() && getMultiplier().isPresent() && entity.get() instanceof Arrow) {
                    Arrow arrow = (Arrow) entity.get();
                    arrow.setBaseDamage(arrow.getBaseDamage() * getMultiplier().get());
                    return true;
                }
                return false;
            case ARROW_EFFECT:
                if(entity.isPresent() && getTag().isPresent() && entity.get() instanceof Arrow) {
                    Arrow arrow = (Arrow) entity.get();
                    readEffectInstance(getTag().get()).ifPresent(e -> arrow.addEffect(e));
                    return true;
                }
                return false;
            case ARROW_COUNT:
                if(entity.isPresent() && getMultiplier().isPresent() && entity.get() instanceof AbstractArrow) {
                    AbstractArrow arrow = (AbstractArrow) entity.get();
                    int arrowCount = Math.round(getMultiplier().get());
                    double motionScale = 0.8;
                    for(int i = 0; i < arrowCount; i++) {
                        AbstractArrow arrow2 = (AbstractArrow) arrow.getType().create(arrow.level);
                        arrow2.copyPosition(arrow);
                        arrow2.setDeltaMovement(arrow.getDeltaMovement().multiply(
                                (Math.random() * 2.0D - 1.0D) * motionScale,
                                (Math.random() * 2.0D - 1.0D) * motionScale,
                                (Math.random() * 2.0D - 1.0D) * motionScale));
                        arrow2.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                        arrow.level.addFreshEntity(arrow2);
                    }
                    return true;
                }
                return false;
            case OFFSPRING:
                if(getMultiplier().isPresent() && entity.isPresent() && entity.get() instanceof AgeableMob
                        && object.isPresent() && object.get() instanceof BabyEntitySpawnEvent) {
                    int childCount = Math.round(getMultiplier().get());
                    if(childCount < 1) {
                        // number of babies is zero, so cancel the event
                        object.get().setCanceled(true);
                        if(entity.get().level instanceof ServerLevel) {
                            Vec3 pos = entity.get().getEyePosition(1.0F);
                            ((ServerLevel)entity.get().level).sendParticles(ParticleTypes.ANGRY_VILLAGER, pos.x, pos.y, pos.z, 6, 0.5D, 0.5D, 0.5D, 0);
                        }
                    } else if(childCount > 1) {
                        // number of babies is more than one, so spawn additional mobs
                        AgeableMob parent = (AgeableMob) entity.get();
                        for(int i = 1; i < childCount; i++) {
                            AgeableMob bonusChild = (AgeableMob) parent.getType().create(parent.level);
                            if(bonusChild != null) {
                                bonusChild.copyPosition(parent);
                                bonusChild.setBaby(true);
                                parent.level.addFreshEntity(bonusChild);
                                if(parent.level instanceof ServerLevel) {
                                    Vec3 pos = bonusChild.getEyePosition(1.0F);
                                    ((ServerLevel)parent.level).sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.x, pos.y, pos.z, 8, 0.5D, 0.5D, 0.5D, 0);
                                }
                            }
                        }
                    }
                    return true;
                }
                return false;
            case CROP_GROWTH:
                if(getMultiplier().isPresent()) {
                    return growCropsNearPlayer(player, favor, Math.round(getMultiplier().get()));
                }
                return false;
            case CROP_HARVEST:
                // This is handled using loot table modifiers
                return getMultiplier().isPresent();
            case DAMAGE:
                if(getMultiplier().isPresent() && object.isPresent() && object.get() instanceof LivingHurtEvent) {
                    LivingHurtEvent event = (LivingHurtEvent) object.get();
                    float amount = event.getAmount();
                    event.setAmount(amount * (getMultiplier().get()));
                }
                return false;
            case DURABILITY:
                if(getMultiplier().isPresent() && getString().isPresent()) {
                    EquipmentSlot slot = EquipmentSlot.byName(getString().get());
                    ItemStack item = player.getItemBySlot(slot);
                    // add or remove durability
                    if(!item.isEmpty() && item.isDamageableItem()) {
                        float multiplier = Math.max(-1.0F, Math.min(1.0F, getMultiplier().get()));
                        int delta = Math.round(multiplier * item.getMaxDamage());
                        int damage = Math.max(0, item.getDamageValue() - delta);
                        item.setDamageValue(damage);
                        return true;
                    }
                }
                return false;
            case AUTOSMELT:
            case UNSMELT:
                // These are handled using loot table modifiers
                return true;
            case SPECIAL_PRICE:
                if(getMultiplier().isPresent() && entity.isPresent() && entity.get() instanceof Merchant) {
                    final int diff = Math.round(getMultiplier().get());
                    final Merchant merchant = (Merchant) entity.get();
                    // cancel event if the diff is ridiculously high
                    if(diff >= 100 && object.isPresent()) {
                        object.get().setCanceled(true);
                        // cause villager to shake head and play unhappy sound
                        if(entity.get() instanceof AbstractVillager) {
                            ((AbstractVillager)entity.get()).setUnhappyCounter(40);
                            entity.get().playSound(SoundEvents.VILLAGER_NO, 0.5F, 1.0F);
                        }
                        // spawn angry particles
                        if(entity.get().level instanceof ServerLevel) {
                            Vec3 pos = entity.get().getEyePosition(1.0F);
                            ((ServerLevel)entity.get().level).sendParticles(ParticleTypes.ANGRY_VILLAGER, pos.x, pos.y, pos.z, 4, 0.5D, 0.5D, 0.5D, 0);
                        }
                        return true;
                    }
                    // add or reduce special price for all offers
                    final boolean add = diff > 0;
                    int special;
                    for(MerchantOffer offer : merchant.getOffers()) {
                        special = offer.getSpecialPriceDiff();
                        if((add && special < diff) || (!add && special > diff)) {
                            offer.setSpecialPriceDiff(diff);
                        }
                    }
                    return !merchant.getOffers().isEmpty();
                }
                return false;
            case PATRON:
                if(getPatron().isPresent()) {
                    return favor.setPatron(player, getPatron().get());
                }
                return false;
            case ADD_DECAY:
                if(getId().isPresent() && getMultiplier().isPresent()) {
                    FavorLevel level = favor.getFavor(getId().get());
                    if(!level.isEnabled() && RPGGods.DEITY.get(getId().get()).orElse(Deity.EMPTY).isEnabled()) {
                        level.setDecayRate(level.getDecayRate() + getMultiplier().get());
                        return true;
                    }
                }
                return false;
            case UNLOCK:
                if(getId().isPresent()) {
                    FavorLevel level = favor.getFavor(getId().get());
                    if(!level.isEnabled() && RPGGods.DEITY.get(getId().get()).orElse(Deity.EMPTY).isEnabled()) {
                        favor.getFavor(getId().get()).setEnabled(true);
                        // send player feedback
                        Component message = getDisplayDescription();
                        player.displayClientMessage(message.copy().withStyle(ChatFormatting.BOLD, ChatFormatting.LIGHT_PURPLE), true);
                        // play sound
                        player.level.playSound(player, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
                        return false; // hacky solution to prevent feedback, since we don't need cooldown anyway
                    }
                }
                return false;
            case XP:
                if(entity.isPresent() && getMultiplier().isPresent() && entity.get() instanceof ExperienceOrb) {
                    ((ExperienceOrb)entity.get()).value *= getMultiplier().get();
                    return true;
                }
                return false;
        }
        return false;
    }

    public static Optional<MobEffectInstance> readEffectInstance(final CompoundTag tag) {
        if(tag.contains("Potion", 8)) {
            final CompoundTag nbt = tag.copy();
            // "show particles" will default to false if not specified
            if(!nbt.contains("ShowParticles")) {
                nbt.putBoolean("ShowParticles", false);
            }
            MobEffect potion = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(nbt.getString("Potion")));
            if(potion != null) {
                nbt.putByte("Id", (byte) MobEffect.getId(potion));
                return Optional.of(MobEffectInstance.load(nbt));
            }
        }
        return Optional.empty();
    }



    /**
     * Attempts to summon an entity near the player
     * @param worldIn the world
     * @param playerIn the player
     * @param entityTag the CompoundNBT of the entity
     * @param distance the maximum distance from the player to summon. Using 0 will skip the usual canSpawn checks.
     * @return the entity if it was summoned, or an empty optional
     **/
    public static Optional<Entity> summonEntityNearPlayer(final Level worldIn, final Player playerIn,
                                                          final Optional<CompoundTag> entityTag, final float distance) {
        if(entityTag.isPresent() && worldIn instanceof ServerLevelAccessor) {
            final Optional<EntityType<?>> entityType = EntityType.by(entityTag.get());
            if(entityType.isPresent()) {
                Entity entity = entityType.get().create(worldIn);
                final boolean waterMob = entity instanceof WaterAnimal || entity instanceof Drowned || entity instanceof Guardian
                        || (entity instanceof Mob && ((Mob)entity).getNavigation() instanceof WaterBoundPathNavigation);
                // find a place to spawn the entity
                Random rand = playerIn.getRandom();
                BlockPos spawnPos;
                for(int range = 1 + Math.round(distance), attempts = Math.min(32, range * 3); attempts > 0; attempts--) {
                    if(range > 1) {
                        spawnPos = playerIn.blockPosition().offset(rand.nextInt(range) - rand.nextInt(range), rand.nextInt(2) - rand.nextInt(2), rand.nextInt(range) - rand.nextInt(range));
                    } else {
                        spawnPos = playerIn.blockPosition().above();
                    }
                    // check if this is a valid position
                    boolean canSpawnHere = (range == 1)
                            || SpawnPlacements.checkSpawnRules(entityType.get(), (ServerLevelAccessor)worldIn, MobSpawnType.SPAWN_EGG, spawnPos, rand)
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
                entity.discard();
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
    public static boolean growCropsNearPlayer(final Player player, final IFavor favor, final int amount) {
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
            final int y1 = rand.nextInt(variationY * 2) - variationY;
            final int z1 = rand.nextInt(radius * 2) - radius;
            final BlockPos blockpos = player.blockPosition().offset(x1, y1, z1);
            final BlockState state = player.level.getBlockState(blockpos);
            // if the block can be grown, grow it and return
            if (state.getBlock() instanceof BonemealableBlock) {
                // determine which age property applies to this state
                for(final IntegerProperty AGE : AGES) {
                    if(state.hasProperty(AGE)) {
                        // attempt to update the age (add or subtract)
                        int oldAge = state.getValue(AGE);
                        int newAge = Math.max(0, oldAge + amount);
                        if(AGE.getPossibleValues().contains(newAge)) {
                            // update the block age
                            player.level.setBlock(blockpos, state.setValue(AGE, newAge), 2);
                            // spawn particles
                            if(player.level instanceof ServerLevel) {
                                ParticleOptions particle = (amount > 0) ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.ANGRY_VILLAGER;
                                ((ServerLevel)player.level).sendParticles(particle, blockpos.getX() + 0.5D, blockpos.getY() + 0.25D, blockpos.getZ() + 0.5D, 10, 0.5D, 0.5D, 0.5D, 0);
                            }
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public Type getType() {
        return type;
    }

    public Optional<ResourceLocation> getId() {
        return id;
    }

    public Optional<String> getString() {
        return string;
    }

    public Optional<CompoundTag> getTag() {
        return tag;
    }

    public Optional<ItemStack> getItem() {
        return item;
    }

    public Optional<Long> getFavor() {
        return favor;
    }

    public Optional<Float> getMultiplier() {
        return multiplier;
    }

    public Optional<Affinity> getAffinity() {
        return affinity;
    }

    public Optional<Patron> getPatron() {
        return patron;
    }

    public boolean isHidden() {
        return hidden;
    }

    @Override
    public String toString() {
        return "PerkData{" +
                "type=" + type +
                ", string=" + string +
                ", id=" + id +
                ", tag=" + tag +
                ", item=" + item +
                ", favor=" + favor +
                ", multiplier=" + multiplier +
                '}';
    }

    public Component getDisplayName() {
        return this.getType().getDisplayName();
    }

    public Component getDisplayDescription() {
        return getType().getDisplayDescription(dataToDisplay());
    }

    private Component dataToDisplay() {
        switch (getType()) {
            case POTION:
            case ARROW_EFFECT:
                if(tag.isPresent()) {
                    // format potion ID as effect name (with amplifier)
                    Optional<MobEffectInstance> effect = readEffectInstance(tag.get());
                    if(effect.isPresent()) {
                        String potencyKey = "potion.potency." + effect.get().getAmplifier();
                        return new TranslatableComponent(effect.get().getDescriptionId())
                                .append(" ")
                                .append(new TranslatableComponent(potencyKey));
                    }
                }
                return TextComponent.EMPTY;
            case SUMMON:
                if(tag.isPresent()) {
                    // format entity ID as name
                    String entity = tag.get().getString("id");
                    Optional<EntityType<?>> type = EntityType.byString(entity);
                    return type.isPresent() ? type.get().getDescription() : new TextComponent(entity);
                }
                return TextComponent.EMPTY;
            case ITEM:
                return getItem().orElse(ItemStack.EMPTY).getHoverName();
            case FAVOR:
                if(favor.isPresent()) {
                    // format favor as discrete amount
                    // EX: multiplier of -1.1 becomes -1, 0.6 becomes +1, 1.2 becomes +1, etc.
                    String prefix = (favor.get() > 0) ? "+" : "";
                    return new TextComponent(prefix + Math.round(getFavor().get()));
                }
                return TextComponent.EMPTY;
            case AFFINITY:
                if(getAffinity().isPresent()) {
                    return getAffinity().get().getDisplayDescription();
                }
                return TextComponent.EMPTY;
            case ARROW_COUNT:
            case SPECIAL_PRICE:
            case CROP_GROWTH:
                if(getMultiplier().isPresent()) {
                    // format multiplier as discrete bonus
                    // EX: multiplier of 0.0 becomes +0, 0.6 becomes +1, 1.2 becomes +1, etc.
                    String prefix = (getMultiplier().get() > 0) ? "+" : "";
                    return new TextComponent(prefix + Math.round(getMultiplier().get()));
                }
                return TextComponent.EMPTY;
            case ADD_DECAY:
                if(getMultiplier().isPresent() && getId().isPresent()) {
                    // format multiplier as signed bonus
                    String prefix = (getMultiplier().get() > 0) ? "+" : "";
                    return new TranslatableComponent("favor.perk.type.add_decay.description.full", prefix + getMultiplier().get(), DeityHelper.getName(getId().get()));
                }
                return TextComponent.EMPTY;
            case DURABILITY:
                if(getMultiplier().isPresent() && getString().isPresent()) {
                    // format multiplier as percentage
                    // EX: multiplier of -0.9 becomes -90%, 0.0 becomes +0%, 0.5 becomes +50%, 1.2 becomes +120%, etc.
                    String prefix = getMultiplier().get() >= 0.0F ? "+" : "";
                    Component durability = new TextComponent(prefix + Math.round((getMultiplier().get()) * 100.0F) + "%");
                    Component slot = new TranslatableComponent("equipment.type." + getString().get());
                    return new TranslatableComponent("favor.perk.type.durability.description.full", durability, slot);
                }
                return TextComponent.EMPTY;
            case DAMAGE:
            case ARROW_DAMAGE:
            case CROP_HARVEST:
            case OFFSPRING:
            case XP:
                if(getMultiplier().isPresent()) {
                    // format multiplier as adjusted percentage
                    // EX: multiplier of 0.0 becomes -100%, 0.5 becomes -50%, 1.2 becomes +120%, etc.
                    String prefix = getMultiplier().get() >= 1.0F ? "+" : "";
                    return new TextComponent(prefix + Math.round((getMultiplier().get() - 1.0F) * 100.0F) + "%");
                }
                return TextComponent.EMPTY;
            case PATRON:
                if(getPatron().isPresent()) {
                    if (getPatron().get().getDeity().isPresent()) {
                        Component deityName = DeityHelper.getName(getPatron().get().getDeity().get());
                        return new TranslatableComponent("favor.perk.type.patron.description.add", deityName);
                    }
                    return new TranslatableComponent("favor.perk.type.patron.description.remove");
                }
                return TextComponent.EMPTY;
            case UNLOCK:
                if(getId().isPresent()) {
                    ResourceLocation deityId = getId().get();
                    Component deityName = DeityHelper.getName(deityId);
                    Altar altar = RPGGods.ALTAR.get(deityId).orElse(Altar.EMPTY);
                    String suffix = altar.isFemale() ? "female" : "male";
                    return new TranslatableComponent("favor.perk.type.unlock.description." + suffix, deityName);
                }
                return TextComponent.EMPTY;
            case FUNCTION:
                if(getString().isPresent()) {
                    return new TranslatableComponent(getString().get());
                }
                return new TranslatableComponent("favor.perk.type.function.description.default");
            case AUTOSMELT: case UNSMELT: default:
                return TextComponent.EMPTY;
        }
    }

    public static enum Type implements StringRepresentable {
        FUNCTION("function"),
        POTION("potion"),
        SUMMON("summon"),
        ITEM("item"),
        FAVOR("favor"),
        AFFINITY("affinity"),
        ARROW_DAMAGE("arrow_damage"),
        ARROW_EFFECT("arrow_effect"),
        ARROW_COUNT("arrow_count"),
        OFFSPRING("offspring"),
        CROP_GROWTH("crop_growth"),
        CROP_HARVEST("crop_harvest"),
        AUTOSMELT("autosmelt"),
        UNSMELT("unsmelt"),
        SPECIAL_PRICE("special_price"),
        DURABILITY("durability"),
        DAMAGE("damage"),
        PATRON("patron"),
        UNLOCK("unlock"),
        ADD_DECAY("add_decay"),
        XP("xp");

        private static final Codec<PerkAction.Type> CODEC = Codec.STRING.comapFlatMap(PerkAction.Type::fromString, PerkAction.Type::getSerializedName).stable();

        private final String name;

        private Type(final String id) {
            this.name = id;
        }

        public static DataResult<PerkAction.Type> fromString(String id) {
            for(final PerkAction.Type t : values()) {
                if(t.getSerializedName().equals(id)) {
                    return DataResult.success(t);
                }
            }
            return DataResult.error("Failed to parse perk data type '" + id + "'");
        }

        /**
         * @param data the data to pass to the translation key
         * @return Translation key for the description of this perk type, using the provided data
         */
        public Component getDisplayDescription(final Component data) {
            return new TranslatableComponent("favor.perk.type." + getSerializedName() + ".description", data);
        }

        /**
         * @return Translation key for the name of this perk type
         */
        public MutableComponent getDisplayName() {
            return new TranslatableComponent("favor.perk.type." + getSerializedName());
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
