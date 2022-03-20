package rpggods;

import net.minecraftforge.common.ForgeConfigSpec;

public class RGConfig {

    // Favor
    private final ForgeConfigSpec.BooleanValue FAVOR_ENABLED;
    private final ForgeConfigSpec.DoubleValue RANDOM_PERK_CHANCE;
    private final ForgeConfigSpec.DoubleValue FAVOR_DECAY_RATE;
    private final ForgeConfigSpec.IntValue FAVOR_DECAY_AMOUNT;
    private final ForgeConfigSpec.IntValue FAVOR_UPDATE_RATE;

    // Affinity
    private final ForgeConfigSpec.BooleanValue FLEE_ENABLED;
    private final ForgeConfigSpec.BooleanValue HOSTILE_ENABLED;
    private final ForgeConfigSpec.BooleanValue PASSIVE_ENABLED;
    private final ForgeConfigSpec.BooleanValue TAMEABLE_ENABLED;

    private boolean favorEnabled;
    private double randomPerkChance;
    private double favorDecayRate;
    private int favorDecayAmount;
    private int favorUpdateRate;

    private boolean fleeEnabled;
    private boolean hostileEnabled;
    private boolean passiveEnabled;
    private boolean tameableEnabled;

    public RGConfig(final ForgeConfigSpec.Builder builder) {
        builder.push("favor");
        FAVOR_ENABLED = builder.define("favor_enabled", true);
        FAVOR_UPDATE_RATE = builder
                .comment("Number of ticks between favor calculations.")
                .comment("Increase to reduce the frequency of favor updates.")
                .comment("THIS AFFECTS PERKS, FAVOR DECAY, AND COOLDOWNS.")
                .defineInRange("favor_update_rate", 20, 1, 24000);
        RANDOM_PERK_CHANCE = builder
                .comment("Percent chance that perks with [random tick] conditions run (every [favor_update_rate] ticks)")
                .defineInRange("random_perk_chance", 0.2D, 0.0D, 1.0D);
        FAVOR_DECAY_RATE = builder
                .comment("Percent chance that favor will deplete (every [favor_update_rate] ticks)")
                .comment("Applies to all players independent of their favor decay rate")
                .defineInRange("favor_decay_rate", 0.006D, 0.0D, 1.0D);
        FAVOR_DECAY_AMOUNT = builder
                .comment("Amount of favor to deplete when applicable")
                .defineInRange("favor_decay_amount", 1, 0, 1024);
        builder.pop();
        builder.push("affinity");
        FLEE_ENABLED = builder
                .comment("True if mobs can be given AI to avoid the player")
                .define("flee_enabled", true);
        HOSTILE_ENABLED = builder
                .comment("True if mobs can be given AI to be hostile")
                .define("hostile_enabled", true);
        PASSIVE_ENABLED = builder
                .comment("True if mobs can be given AI to be passive")
                .define("passive_enabled", true);
        TAMEABLE_ENABLED = builder
                .comment("True if mobs can be given AI to become tamed")
                .define("tameable_enabled", true);
        builder.pop();
    }


    public void bake() {
        favorEnabled = FAVOR_ENABLED.get();
        randomPerkChance = RANDOM_PERK_CHANCE.get();
        favorDecayRate = FAVOR_DECAY_RATE.get();
        favorDecayAmount = FAVOR_DECAY_AMOUNT.get();
        favorUpdateRate = FAVOR_UPDATE_RATE.get();

        fleeEnabled = FLEE_ENABLED.get();
        hostileEnabled = HOSTILE_ENABLED.get();
        passiveEnabled = PASSIVE_ENABLED.get();
        tameableEnabled = TAMEABLE_ENABLED.get();
    }

    public boolean isFavorEnabled() { return favorEnabled; }
    public double getRandomPerkChance() { return randomPerkChance; }
    public double getFavorDecayRate() { return favorDecayRate; }
    public int getFavorDecayAmount() { return favorDecayAmount; }
    public int getFavorUpdateRate() { return favorUpdateRate; }

    public boolean isFleeEnabled() { return fleeEnabled; }
    public boolean isHostileEnabled() { return hostileEnabled; }
    public boolean isPassiveEnabled() { return passiveEnabled; }
    public boolean isTameableEnabled() { return tameableEnabled; }
}
