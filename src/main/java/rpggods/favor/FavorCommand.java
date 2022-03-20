package rpggods.favor;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.command.arguments.ResourceLocationArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;
import rpggods.RPGGods;
import rpggods.deity.Deity;
import rpggods.event.FavorChangedEvent;

import java.util.Collection;
import java.util.Optional;

public class FavorCommand {
    private static final DynamicCommandExceptionType FAVOR_DISABLED_EXCEPTION = new DynamicCommandExceptionType(o -> new TranslationTextComponent("commands.favor.enabled.disabled", o));

    public static void register(CommandDispatcher<CommandSource> commandSource) {
        LiteralCommandNode<CommandSource> commandNode = commandSource.register(
                Commands.literal("favor")
                        .requires(p -> p.hasPermission(2))
                        .then(Commands.literal("add")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("deity", ResourceLocationArgument.id())
                                                .then((Commands.argument("amount", IntegerArgumentType.integer())
                                                        .executes(command -> addFavor(command.getSource(), EntityArgument.getPlayers(command, "targets"), ResourceLocationArgument.getId(command, "deity"), IntegerArgumentType.getInteger(command, "amount"), Type.LEVELS))
                                                        .then(Commands.literal("points")
                                                                .executes(command -> addFavor(command.getSource(), EntityArgument.getPlayers(command, "targets"), ResourceLocationArgument.getId(command, "deity"), IntegerArgumentType.getInteger(command, "amount"), Type.POINTS)))
                                                        .then(Commands.literal("levels")
                                                                .executes(command -> addFavor(command.getSource(), EntityArgument.getPlayers(command, "targets"), ResourceLocationArgument.getId(command, "deity"), IntegerArgumentType.getInteger(command, "amount"), Type.LEVELS)))
                                                        .then(Commands.literal("decay")
                                                                .executes(command -> addDecay(command.getSource(), EntityArgument.getPlayers(command, "targets"), ResourceLocationArgument.getId(command, "deity"), IntegerArgumentType.getInteger(command, "amount")))))))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("deity", ResourceLocationArgument.id())
                                                .then((Commands.argument("amount", IntegerArgumentType.integer())
                                                        .executes(command -> setFavor(command.getSource(), EntityArgument.getPlayers(command, "targets"), ResourceLocationArgument.getId(command, "deity"), IntegerArgumentType.getInteger(command, "amount"), Type.LEVELS))
                                                        .then(Commands.literal("points")
                                                                .executes(command -> setFavor(command.getSource(), EntityArgument.getPlayers(command, "targets"), ResourceLocationArgument.getId(command, "deity"), IntegerArgumentType.getInteger(command, "amount"), Type.POINTS)))
                                                        .then(Commands.literal("levels")
                                                                .executes(command -> setFavor(command.getSource(), EntityArgument.getPlayers(command, "targets"), ResourceLocationArgument.getId(command, "deity"), IntegerArgumentType.getInteger(command, "amount"), Type.LEVELS)))
                                                        .then(Commands.literal("decay")
                                                                .executes(command -> setDecay(command.getSource(), EntityArgument.getPlayers(command, "targets"), ResourceLocationArgument.getId(command, "deity"), IntegerArgumentType.getInteger(command, "amount"))))))
                                                .then(Commands.literal("patron")
                                                        .executes(command -> setPatron(command.getSource(), EntityArgument.getPlayers(command, "target"), ResourceLocationArgument.getId(command, "deity")))))
                                        .then(Commands.literal("enabled")
                                                .then(Commands.argument("flag", BoolArgumentType.bool())
                                                        .executes(command -> setEnabled(command.getSource(), EntityArgument.getPlayers(command, "targets"), BoolArgumentType.getBool(command, "flag")))))))
                        .then(Commands.literal("query")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .then(Commands.argument("deity", ResourceLocationArgument.id())
                                                .executes(command -> queryFavor(command.getSource(), EntityArgument.getPlayer(command, "target"), ResourceLocationArgument.getId(command, "deity"), Type.LEVELS))
                                                .then(Commands.literal("points")
                                                        .executes(command -> queryFavor(command.getSource(), EntityArgument.getPlayer(command, "target"), ResourceLocationArgument.getId(command, "deity"), Type.POINTS)))
                                                .then(Commands.literal("levels")
                                                        .executes(command -> queryFavor(command.getSource(), EntityArgument.getPlayer(command, "target"), ResourceLocationArgument.getId(command, "deity"), Type.LEVELS)))
                                                .then(Commands.literal("decay")
                                                        .executes(command -> queryDecay(command.getSource(), EntityArgument.getPlayer(command, "target"), ResourceLocationArgument.getId(command, "deity")))))
                                        .then(Commands.literal("enabled")
                                                .executes(command -> queryEnabled(command.getSource(), EntityArgument.getPlayer(command, "target"))))
                                        .then(Commands.literal("patron")
                                                .executes(command -> queryPatron(command.getSource(), EntityArgument.getPlayer(command, "target"))))))
                        .then(Commands.literal("cap")
                                .then(Commands.argument("target", EntityArgument.players())
                                        .then(Commands.argument("deity", ResourceLocationArgument.id())
                                                .then(Commands.argument("min", IntegerArgumentType.integer())
                                                        .then(Commands.argument("max", IntegerArgumentType.integer())
                                                                .executes(command -> setCap(command.getSource(), EntityArgument.getPlayers(command, "target"), ResourceLocationArgument.getId(command, "deity"),
                                                                        IntegerArgumentType.getInteger(command, "min"), IntegerArgumentType.getInteger(command, "max"), Type.LEVELS))
                                                                .then(Commands.literal("points")
                                                                        .executes(command -> setCap(command.getSource(), EntityArgument.getPlayers(command, "target"), ResourceLocationArgument.getId(command, "deity"),
                                                                                IntegerArgumentType.getInteger(command, "min"), IntegerArgumentType.getInteger(command, "max"), Type.POINTS)))
                                                                .then(Commands.literal("levels")
                                                                        .executes(command -> setCap(command.getSource(), EntityArgument.getPlayers(command, "target"), ResourceLocationArgument.getId(command, "deity"),
                                                                                IntegerArgumentType.getInteger(command, "min"), IntegerArgumentType.getInteger(command, "max"), Type.LEVELS))))))))
                        .then(Commands.literal("reset")
                                .then(Commands.argument("target", EntityArgument.players())
                                        .executes(command -> resetFavor(command.getSource(), EntityArgument.getPlayers(command, "target")))
                                        .then(Commands.argument("deity", ResourceLocationArgument.id())
                                                .executes(command -> resetFavor(command.getSource(), EntityArgument.getPlayers(command, "target"), ResourceLocationArgument.getId(command, "deity"))))
                                        .then(Commands.literal("cooldown")
                                                .executes(command -> resetCooldown(command.getSource(), EntityArgument.getPlayers(command, "target"))))))
        );

        commandSource.register(Commands.literal("favor")
                .requires(p -> p.hasPermission(2))
                .redirect(commandNode));
    }

    private static int queryFavor(CommandSource source, ServerPlayerEntity player, ResourceLocation deity, Type type) throws CommandSyntaxException {
        final IFavor favor = player.getCapability(RPGGods.FAVOR).orElse(RPGGods.FAVOR.getDefaultInstance());
        if (!favor.isEnabled()) {
            throw FAVOR_DISABLED_EXCEPTION.create(player.getDisplayName());
        }
        int amount = type.favorGetter.accept(player, favor, deity, 0);
        source.sendSuccess(new TranslationTextComponent("commands.favor.query." + type.name, player.getDisplayName(), amount, Deity.getName(deity)), false);
        return amount;
    }

    private static int setFavor(CommandSource source, Collection<? extends ServerPlayerEntity> players, ResourceLocation deity, int amount, Type type) throws CommandSyntaxException {
        // set favor for each player in the collection
        IFavor favor;
        for (final ServerPlayerEntity player : players) {
            favor = player.getCapability(RPGGods.FAVOR).orElse(RPGGods.FAVOR.getDefaultInstance());
            if (!favor.isEnabled()) {
                throw FAVOR_DISABLED_EXCEPTION.create(player.getDisplayName());
            }
            type.favorSetter.accept(player, favor, deity, amount);
        }
        // send command feedback
        if (players.size() == 1) {
            source.sendSuccess(new TranslationTextComponent("commands.favor.set." + type.name + ".success.single", amount, Deity.getName(deity), players.iterator().next().getDisplayName()), true);
        } else {
            source.sendSuccess(new TranslationTextComponent("commands.favor.set." + type.name + ".success.multiple", amount, Deity.getName(deity), players.size()), true);
        }

        return players.size();
    }

    private static int addFavor(CommandSource source, Collection<? extends ServerPlayerEntity> players, ResourceLocation deity, int amount, Type type) throws CommandSyntaxException {
        // add favor to each player in the collection
        IFavor favor;
        for (final ServerPlayerEntity player : players) {
            favor = player.getCapability(RPGGods.FAVOR).orElse(RPGGods.FAVOR.getDefaultInstance());
            if (!favor.isEnabled()) {
                throw FAVOR_DISABLED_EXCEPTION.create(player.getDisplayName());
            }
            type.favorAdder.accept(player, favor, deity, amount);
        }
        // send command feedback
        if (players.size() == 1) {
            source.sendSuccess(new TranslationTextComponent("commands.favor.add." + type.name + ".success.single", amount, Deity.getName(deity), players.iterator().next().getDisplayName()), true);
        } else {
            source.sendSuccess(new TranslationTextComponent("commands.favor.add." + type.name + ".success.multiple", amount, Deity.getName(deity), players.size()), true);
        }

        return players.size();
    }

    private static int queryEnabled(CommandSource source, ServerPlayerEntity player) {
        final boolean enabled = player.getCapability(RPGGods.FAVOR).orElse(RPGGods.FAVOR.getDefaultInstance()).isEnabled();
        // send command feedback
        source.sendSuccess(new TranslationTextComponent("commands.favor.enabled." + (enabled ? "enabled" : "disabled"), player.getDisplayName()), true);
        return enabled ? 1 : 0;
    }

    private static int setEnabled(CommandSource source, Collection<? extends ServerPlayerEntity> players, boolean enabled) {
        for (final ServerPlayerEntity player : players) {
            player.getCapability(RPGGods.FAVOR).orElse(RPGGods.FAVOR.getDefaultInstance()).setEnabled(enabled);
        }
        // send command feedback
        final String sub = (enabled ? "enabled" : "disabled");
        if (players.size() == 1) {
            source.sendSuccess(new TranslationTextComponent("commands.favor." + sub + ".success.single", players.iterator().next().getDisplayName()), true);
        } else {
            source.sendSuccess(new TranslationTextComponent("commands.favor." + sub + ".success.multiple", players.size()), true);
        }
        return players.size();
    }

    private static int queryDecay(CommandSource source, ServerPlayerEntity target, ResourceLocation deity) throws CommandSyntaxException {
        IFavor favor = target.getCapability(RPGGods.FAVOR).orElse(RPGGods.FAVOR.getDefaultInstance());
        if (!favor.isEnabled()) {
            throw FAVOR_DISABLED_EXCEPTION.create(target.getDisplayName());
        }
        float fDecay = favor.getFavor(deity).getDecayRate();
        int decay = Math.round(fDecay * 100);
        // send command feedback
        source.sendSuccess(new TranslationTextComponent("commands.favor.query.decay", target.getDisplayName(), decay, Deity.getName(deity)), true);
        return decay;
    }

    private static int setDecay(CommandSource source, Collection<? extends ServerPlayerEntity> players, ResourceLocation deity, int decay) throws CommandSyntaxException {
        // set decay rate to each player in the collection
        IFavor favor;
        float fDecay = decay / 100.0F;
        for (final ServerPlayerEntity player : players) {
            favor = player.getCapability(RPGGods.FAVOR).orElse(RPGGods.FAVOR.getDefaultInstance());
            if (!favor.isEnabled()) {
                throw FAVOR_DISABLED_EXCEPTION.create(player.getDisplayName());
            }
            favor.getFavor(deity).setDecayRate(fDecay);
        }
        // send command feedback
        if (players.size() == 1) {
            source.sendSuccess(new TranslationTextComponent("commands.favor.set.decay.success.single", decay, Deity.getName(deity), players.iterator().next().getDisplayName()), true);
        } else {
            source.sendSuccess(new TranslationTextComponent("commands.favor.set.decay.success.multiple", decay, Deity.getName(deity), players.size()), true);
        }
        return players.size();
    }

    private static int addDecay(CommandSource source, Collection<? extends ServerPlayerEntity> players, ResourceLocation deity, int decay) throws CommandSyntaxException {
        // add decay rate to each player in the collection
        IFavor favor;
        float fDecay = decay / 100.0F;
        for (final ServerPlayerEntity player : players) {
            favor = player.getCapability(RPGGods.FAVOR).orElse(RPGGods.FAVOR.getDefaultInstance());
            if (!favor.isEnabled()) {
                throw FAVOR_DISABLED_EXCEPTION.create(player.getDisplayName());
            }
            float d = favor.getFavor(deity).getDecayRate();
            favor.getFavor(deity).setDecayRate(d + fDecay);
        }
        // send command feedback
        if (players.size() == 1) {
            source.sendSuccess(new TranslationTextComponent("commands.favor.add.decay.success.single", decay, Deity.getName(deity), players.iterator().next().getDisplayName()), true);
        } else {
            source.sendSuccess(new TranslationTextComponent("commands.favor.add.decay.success.multiple", decay, Deity.getName(deity), players.size()), true);
        }
        return players.size();
    }

    private static int queryPatron(CommandSource source, ServerPlayerEntity target) throws CommandSyntaxException {
        IFavor favor = target.getCapability(RPGGods.FAVOR).orElse(RPGGods.FAVOR.getDefaultInstance());
        if (!favor.isEnabled()) {
            throw FAVOR_DISABLED_EXCEPTION.create(target.getDisplayName());
        }
        Optional<ResourceLocation> deity = favor.getPatron();
        // send command feedback
        if(deity.isPresent()) {
            source.sendSuccess(new TranslationTextComponent("commands.favor.query.patron.success", target.getDisplayName(), Deity.getName(deity.get())), true);
            return 1;
        } else {
            source.sendSuccess(new TranslationTextComponent("commands.favor.query.patron.empty", target.getDisplayName()), true);
            return 0;
        }
    }

    private static int setPatron(CommandSource source, Collection<ServerPlayerEntity> players, ResourceLocation deity) throws CommandSyntaxException {
        // set decay rate to each player in the collection
        IFavor favor;
        for (final ServerPlayerEntity player : players) {
            favor = player.getCapability(RPGGods.FAVOR).orElse(RPGGods.FAVOR.getDefaultInstance());
            if (!favor.isEnabled()) {
                throw FAVOR_DISABLED_EXCEPTION.create(player.getDisplayName());
            }
            favor.setPatron(Optional.of(deity));
        }
        // send command feedback
        if (players.size() == 1) {
            source.sendSuccess(new TranslationTextComponent("commands.favor.set.patron.success.single", Deity.getName(deity), players.iterator().next().getDisplayName()), true);
        } else {
            source.sendSuccess(new TranslationTextComponent("commands.favor.set.patron.success.multiple", Deity.getName(deity), players.size()), true);
        }
        return players.size();
    }


    private static int resetFavor(CommandSource source, Collection<? extends ServerPlayerEntity> players) {
        // reset favor for each player in the collection
        IFavor favor;
        for (final ServerPlayerEntity player : players) {
            favor = player.getCapability(RPGGods.FAVOR).orElse(RPGGods.FAVOR.getDefaultInstance());
            favor.reset();
        }
        // send command feedback
        if (players.size() == 1) {
            source.sendSuccess(new TranslationTextComponent("commands.favor.reset.success.single", players.iterator().next().getDisplayName()), true);
        } else {
            source.sendSuccess(new TranslationTextComponent("commands.favor.reset.success.multiple", players.size()), true);
        }

        return players.size();
    }

    private static int resetFavor(CommandSource source, Collection<? extends ServerPlayerEntity> players, ResourceLocation deity) throws CommandSyntaxException {
        // reset favor for a single deity for each player in the collection
        IFavor favor;
        for (final ServerPlayerEntity player : players) {
            favor = player.getCapability(RPGGods.FAVOR).orElse(RPGGods.FAVOR.getDefaultInstance());
            if (!favor.isEnabled()) {
                throw FAVOR_DISABLED_EXCEPTION.create(player.getDisplayName());
            }
            favor.setFavor(deity, new FavorLevel(0));
        }
        // send command feedback
        if (players.size() == 1) {
            source.sendSuccess(new TranslationTextComponent("commands.favor.reset.deity.success.single", Deity.getName(deity), players.iterator().next().getDisplayName()), true);
        } else {
            source.sendSuccess(new TranslationTextComponent("commands.favor.reset.deity.success.multiple", Deity.getName(deity), players.size()), true);
        }

        return players.size();
    }

    private static int resetCooldown(CommandSource source, Collection<? extends ServerPlayerEntity> players) throws CommandSyntaxException {
        // add favor to each player in the collection
        IFavor favor;
        long time;
        for (final ServerPlayerEntity player : players) {
            favor = player.getCapability(RPGGods.FAVOR).orElse(RPGGods.FAVOR.getDefaultInstance());
            if (!favor.isEnabled()) {
                throw FAVOR_DISABLED_EXCEPTION.create(player.getDisplayName());
            }
            favor.resetCooldowns();
        }
        // send command feedback
        if (players.size() == 1) {
            source.sendSuccess(new TranslationTextComponent("commands.favor.reset.cooldown.success.single", players.iterator().next().getDisplayName()), true);
        } else {
            source.sendSuccess(new TranslationTextComponent("commands.favor.reset.cooldown.success.multiple", players.size()), true);
        }

        return players.size();
    }

    private static int setCap(CommandSource source, Collection<ServerPlayerEntity> players, ResourceLocation deity, int min, int max, Type type) throws CommandSyntaxException {
        // cap favor for each player in the collection
        int actualMin = Math.min(min, max);
        int actualMax = Math.max(min, max);
        IFavor favor;
        for (final ServerPlayerEntity player : players) {
            favor = player.getCapability(RPGGods.FAVOR).orElse(RPGGods.FAVOR.getDefaultInstance());
            if (!favor.isEnabled()) {
                throw FAVOR_DISABLED_EXCEPTION.create(player.getDisplayName());
            }
            type.favorCapper.accept(player, favor, deity, actualMin, actualMax);
        }
        // send command feedback
        if (players.size() == 1) {
            source.sendSuccess(new TranslationTextComponent("commands.favor.cap." + type.name + ".success.single", actualMin, actualMax, Deity.getName(deity), players.iterator().next().getDisplayName()), true);
        } else {
            source.sendSuccess(new TranslationTextComponent("commands.favor.cap." + type.name + ".success.multiple", actualMin, actualMax, Deity.getName(deity), players.size()), true);
        }

        return players.size();
    }


    static enum Type {
        POINTS("points",
                (p, f, d, a) -> (int) f.getFavor(d).getFavor(),
                (p, f, d, a) -> {
                    f.getFavor(d).setFavor(p, d, a, FavorChangedEvent.Source.COMMAND);
                    return a;
                },
                (p, f, d, a) -> {
                    f.getFavor(d).addFavor(p, d, a, FavorChangedEvent.Source.COMMAND);
                    return a;
                },
                (p, f, d, a1, a2) -> {
                    f.getFavor(d).setLevelBounds(FavorLevel.calculateLevel(a1), FavorLevel.calculateLevel(a2));
                    return a2 - a1;
                }
        ),
        LEVELS("levels",
                (p, f, d, a) -> f.getFavor(d).getLevel(),
                (p, f, d, a) -> {
                    f.getFavor(d).setFavor(p, d, FavorLevel.calculateFavor(a), FavorChangedEvent.Source.COMMAND);
                    return a;
                },
                (p, f, d, a) -> {
                    f.getFavor(d).addFavor(p, d, FavorLevel.calculateFavor(a) + (long) Math.signum(a), FavorChangedEvent.Source.COMMAND);
                    return a;
                },
                (p, f, d, a1, a2) -> {
                    f.getFavor(d).setLevelBounds(a1, a2);
                    return a2 - a1;
                }
        );

        public final String name;
        public final IFavorFunction favorGetter;
        public final IFavorFunction favorSetter;
        public final IFavorFunction favorAdder;
        public final IXFavorFunction favorCapper;

        Type(final String key, final IFavorFunction getter, final IFavorFunction setter, final IFavorFunction adder, final IXFavorFunction capper) {
            name = key;
            favorGetter = getter;
            favorSetter = setter;
            favorAdder = adder;
            favorCapper = capper;
        }
    }

    @FunctionalInterface
    private interface IFavorFunction {
        public int accept(final ServerPlayerEntity player, final IFavor favor, final ResourceLocation deity, final int amount);
    }

    @FunctionalInterface
    private interface IXFavorFunction {
        public int accept(final ServerPlayerEntity player, final IFavor favor, final ResourceLocation deity, final int amount1, final int amount2);
    }
}
