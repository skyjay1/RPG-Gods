package rpggods;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import net.minecraft.world.entity.EntityType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.stream.Collectors;

public class RGConfig {

    // Favor
    private final ForgeConfigSpec.BooleanValue FAVOR_ENABLED;
    private final ForgeConfigSpec.DoubleValue RANDOM_PERK_CHANCE;
    private final ForgeConfigSpec.DoubleValue FAVOR_DECAY_RATE;
    private final ForgeConfigSpec.IntValue FAVOR_DECAY_AMOUNT;
    private final ForgeConfigSpec.IntValue FAVOR_UPDATE_RATE;
    private final ForgeConfigSpec.BooleanValue PERK_FEEDBACK;
    private final ForgeConfigSpec.BooleanValue PERK_FEEDBACK_CHAT;

    // Affinity
    private final ForgeConfigSpec.BooleanValue FLEE_ENABLED;
    private final ForgeConfigSpec.BooleanValue HOSTILE_ENABLED;
    private final ForgeConfigSpec.BooleanValue PASSIVE_ENABLED;
    private final ForgeConfigSpec.BooleanValue TAMEABLE_ENABLED;

    private final ForgeConfigSpec.ConfigValue<List<? extends String>> SITTING_MOBS;

    private boolean favorEnabled;
    private double randomPerkChance;
    private double favorDecayRate;
    private int favorDecayAmount;
    private int favorUpdateRate;
    private boolean perkFeedback;
    private boolean perkFeedbackChat;

    private boolean fleeEnabled;
    private boolean hostileEnabled;
    private boolean passiveEnabled;
    private boolean tameableEnabled;

    private ImmutableList<ResourceLocation> sittingMobs;

    public RGConfig(final ForgeConfigSpec.Builder builder) {
        builder.push("favor");
        FAVOR_ENABLED = builder
                .comment("Set to false to disable favor, offerings, sacrifices, and perks")
                .define("favor_enabled", true);
        FAVOR_UPDATE_RATE = builder
                .comment("Number of ticks between favor calculations.",
                        "Increase to reduce the frequency of favor updates.",
                        "THIS AFFECTS PERKS, FAVOR DECAY, AND COOLDOWNS.")
                .defineInRange("favor_update_rate", 20, 1, 24000);
        RANDOM_PERK_CHANCE = builder
                .comment("Percent chance that perks with [random tick] conditions run (every [favor_update_rate] ticks)")
                .defineInRange("random_perk_chance", 0.34D, 0.0D, 1.0D);
        FAVOR_DECAY_RATE = builder
                .comment("Percent chance that favor will deplete (every [favor_update_rate] ticks)",
                        "Applies to all players independent of their favor decay rate")
                .defineInRange("favor_decay_rate", 0.006D, 0.0D, 1.0D);
        FAVOR_DECAY_AMOUNT = builder
                .comment("Amount of favor to deplete when applicable")
                .defineInRange("favor_decay_amount", 1, 0, 1024);
        PERK_FEEDBACK = builder
                .comment("True to notify player when a perk is given")
                .define("perk_feedback", true);
        PERK_FEEDBACK_CHAT = builder
                .comment("True to send feedback through chat instead of status bar", "Only if perk_feedback is True")
                .define("perk_feedback_chat", false);
        builder.pop();
        builder.push("affinity");
        FLEE_ENABLED = builder
                .comment("True if mobs can be given AI to avoid the player")
                .worldRestart()
                .define("flee_enabled", true);
        HOSTILE_ENABLED = builder
                .comment("True if mobs can be given AI to be hostile")
                .worldRestart()
                .define("hostile_enabled", true);
        PASSIVE_ENABLED = builder
                .comment("True if mobs can be given AI to be passive")
                .worldRestart()
                .define("passive_enabled", true);
        TAMEABLE_ENABLED = builder
                .comment("True if mobs can be given AI to become tamed")
                .worldRestart()
                .define("tameable_enabled", true);
        builder.pop();
        builder.push("sitting");
        SITTING_MOBS = builder
                .comment("List of mobs that appear to sit (used client-side)")
                .defineList("sitting_mobs", Lists.newArrayList(
                        ForgeRegistries.ENTITY_TYPES.getKey(EntityType.ZOMBIE).toString(),
                        ForgeRegistries.ENTITY_TYPES.getKey(EntityType.HUSK).toString(),
                        ForgeRegistries.ENTITY_TYPES.getKey(EntityType.DROWNED).toString(),
                        ForgeRegistries.ENTITY_TYPES.getKey(EntityType.SKELETON).toString(),
                        ForgeRegistries.ENTITY_TYPES.getKey(EntityType.STRAY).toString(),
                        ForgeRegistries.ENTITY_TYPES.getKey(EntityType.WITHER_SKELETON).toString(),
                        ForgeRegistries.ENTITY_TYPES.getKey(EntityType.VINDICATOR).toString(),
                        ForgeRegistries.ENTITY_TYPES.getKey(EntityType.PILLAGER).toString(),
                        ForgeRegistries.ENTITY_TYPES.getKey(EntityType.EVOKER).toString(),
                        ForgeRegistries.ENTITY_TYPES.getKey(EntityType.ILLUSIONER).toString(),
                        ForgeRegistries.ENTITY_TYPES.getKey(EntityType.PIGLIN).toString(),
                        ForgeRegistries.ENTITY_TYPES.getKey(EntityType.ZOMBIFIED_PIGLIN).toString(),
                        ForgeRegistries.ENTITY_TYPES.getKey(EntityType.ENDERMAN).toString()
                ), o -> o instanceof String && ((String)o).contains(":"));
        builder.pop();
    }


    public void bake() {
        favorEnabled = FAVOR_ENABLED.get();
        randomPerkChance = RANDOM_PERK_CHANCE.get();
        favorDecayRate = FAVOR_DECAY_RATE.get();
        favorDecayAmount = FAVOR_DECAY_AMOUNT.get();
        favorUpdateRate = FAVOR_UPDATE_RATE.get();
        perkFeedback = PERK_FEEDBACK.get();
        perkFeedbackChat = PERK_FEEDBACK_CHAT.get();

        fleeEnabled = FLEE_ENABLED.get();
        hostileEnabled = HOSTILE_ENABLED.get();
        passiveEnabled = PASSIVE_ENABLED.get();
        tameableEnabled = TAMEABLE_ENABLED.get();

        ImmutableList.Builder<ResourceLocation> builder = ImmutableList.builder();
        for(String s : SITTING_MOBS.get()) {
            builder.add(ResourceLocation.tryParse(s));
        }
        sittingMobs = builder.build();
    }

    public boolean isFavorEnabled() { return favorEnabled; }
    public double getRandomPerkChance() { return randomPerkChance; }
    public double getFavorDecayRate() { return favorDecayRate; }
    public int getFavorDecayAmount() { return favorDecayAmount; }
    public int getFavorUpdateRate() { return favorUpdateRate; }
    public boolean canGiveFeedback() { return perkFeedback; }
    public boolean isFeedbackChat() { return perkFeedbackChat; }

    public boolean isFleeEnabled() { return fleeEnabled; }
    public boolean isHostileEnabled() { return hostileEnabled; }
    public boolean isPassiveEnabled() { return passiveEnabled; }
    public boolean isTameableEnabled() { return tameableEnabled; }

    public boolean isSittingMob(final ResourceLocation id) { return sittingMobs.contains(id); }
}
