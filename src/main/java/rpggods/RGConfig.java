package rpggods;

import com.electronwill.nightconfig.core.EnumGetMethod;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EntityType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public class RGConfig {

    // Favor
    private final ForgeConfigSpec.BooleanValue FAVOR_ENABLED;
    private final ForgeConfigSpec.EnumValue<FavorMode> FAVOR_MODE;
    private final ForgeConfigSpec.DoubleValue RANDOM_PERK_CHANCE;
    private final ForgeConfigSpec.DoubleValue FAVOR_DECAY_RATE;
    private final ForgeConfigSpec.IntValue FAVOR_DECAY_AMOUNT;
    private final ForgeConfigSpec.IntValue FAVOR_UPDATE_RATE;
    private final ForgeConfigSpec.BooleanValue PERK_FEEDBACK;
    private final ForgeConfigSpec.BooleanValue PERK_FEEDBACK_CHAT;

    // Brazier
    private final ForgeConfigSpec.BooleanValue BRAZIER_ENABLED;
    private final ForgeConfigSpec.IntValue BRAZIER_COOLDOWN;
    private final ForgeConfigSpec.IntValue BRAZIER_RANGE;

    // Affinity
    private final ForgeConfigSpec.BooleanValue FLEE_ENABLED;
    private final ForgeConfigSpec.BooleanValue HOSTILE_ENABLED;
    private final ForgeConfigSpec.BooleanValue PASSIVE_ENABLED;
    private final ForgeConfigSpec.BooleanValue TAMEABLE_ENABLED;

    private final ForgeConfigSpec.ConfigValue<List<? extends String>> SITTING_MOBS;

    private boolean favorEnabled;
    private FavorMode favorMode;
    private double randomPerkChance;
    private double favorDecayRate;
    private int favorDecayAmount;
    private int favorUpdateRate;
    private boolean perkFeedback;
    private boolean perkFeedbackChat;

    private boolean brazierEnabled;
    private int brazierCooldown;
    private int brazierRange;

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
        FAVOR_MODE = builder
                .comment("Determines how favor and cooldown is handled")
                .defineEnum("favor_mode", FavorMode.PLAYER, EnumGetMethod.NAME_IGNORECASE);
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
        builder.push("brazier");
        BRAZIER_ENABLED = builder
                .comment("True to enable automatic offerings using the brazier")
                .define("brazier_enabled", true);
        BRAZIER_COOLDOWN = builder
                .comment("The number of ticks required to burn an offering")
                .defineInRange("brazier_cooldown", 8, 0, 12000);
        BRAZIER_RANGE = builder
                .comment("The number of blocks to search for an altar")
                .defineInRange("brazier_range", 2, 1, 8);
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
                .comment("Mobs that have a sitting pose, used for tameable mobs.",
                        "Used client-side only.")
                .defineList("sitting_mobs", Lists.newArrayList(
                        EntityType.ZOMBIE.getRegistryName().toString(),
                        EntityType.HUSK.getRegistryName().toString(),
                        EntityType.DROWNED.getRegistryName().toString(),
                        EntityType.SKELETON.getRegistryName().toString(),
                        EntityType.STRAY.getRegistryName().toString(),
                        EntityType.WITHER_SKELETON.getRegistryName().toString(),
                        EntityType.VINDICATOR.getRegistryName().toString(),
                        EntityType.PILLAGER.getRegistryName().toString(),
                        EntityType.EVOKER.getRegistryName().toString(),
                        EntityType.ILLUSIONER.getRegistryName().toString(),
                        EntityType.PIGLIN.getRegistryName().toString(),
                        EntityType.ZOMBIFIED_PIGLIN.getRegistryName().toString(),
                        EntityType.ENDERMAN.getRegistryName().toString()
                ), o -> o instanceof String && ((String)o).contains(":"));
        builder.pop();
    }


    public void bake() {
        favorEnabled = FAVOR_ENABLED.get();
        favorMode = FAVOR_MODE.get();
        randomPerkChance = RANDOM_PERK_CHANCE.get();
        favorDecayRate = FAVOR_DECAY_RATE.get();
        favorDecayAmount = FAVOR_DECAY_AMOUNT.get();
        favorUpdateRate = FAVOR_UPDATE_RATE.get();
        perkFeedback = PERK_FEEDBACK.get();
        perkFeedbackChat = PERK_FEEDBACK_CHAT.get();

        brazierEnabled = BRAZIER_ENABLED.get();
        brazierCooldown = BRAZIER_COOLDOWN.get();
        brazierRange = BRAZIER_RANGE.get();

        fleeEnabled = FLEE_ENABLED.get();
        hostileEnabled = HOSTILE_ENABLED.get();
        passiveEnabled = PASSIVE_ENABLED.get();
        tameableEnabled = TAMEABLE_ENABLED.get();

        ImmutableList.Builder<ResourceLocation> builder = ImmutableList.builder();
        for(String s : SITTING_MOBS.get()) {
            ResourceLocation r = ResourceLocation.tryParse(s);
            if(r != null) {
                builder.add(r);
            }
        }
        sittingMobs = builder.build();
    }

    public boolean isFavorEnabled() { return favorEnabled; }
    public boolean useGlobalFavor() { return favorMode == FavorMode.GLOBAL; }
    public boolean useTeamFavor() { return favorMode == FavorMode.TEAM; }
    public boolean usePlayerFavor() { return favorMode == FavorMode.PLAYER; }
    public double getRandomPerkChance() { return randomPerkChance; }
    public double getFavorDecayRate() { return favorDecayRate; }
    public int getFavorDecayAmount() { return favorDecayAmount; }
    public int getFavorUpdateRate() { return favorUpdateRate; }
    public boolean canGiveFeedback() { return perkFeedback; }
    public boolean isFeedbackChat() { return perkFeedbackChat; }

    public boolean isBrazierEnabled() { return brazierEnabled; }
    public int getBrazierCooldown() { return brazierCooldown; }
    public int getBrazierRange() { return brazierRange; }

    public boolean isFleeEnabled() { return fleeEnabled; }
    public boolean isHostileEnabled() { return hostileEnabled; }
    public boolean isPassiveEnabled() { return passiveEnabled; }
    public boolean isTameableEnabled() { return tameableEnabled; }

    public boolean isSittingMob(final ResourceLocation id) { return sittingMobs.contains(id); }

    public static enum FavorMode implements StringRepresentable {
        PLAYER("player"),
        TEAM("team"),
        GLOBAL("global");

        private String name;

        private FavorMode(final String name) {
            this.name = name;
        }

        public static FavorMode getByName(final String name) {
            for(FavorMode mode : values()) {
                if(mode.getSerializedName().equals(name)) {
                    return mode;
                }
            }
            return PLAYER;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
