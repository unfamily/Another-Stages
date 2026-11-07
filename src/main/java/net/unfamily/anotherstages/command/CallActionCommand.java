package net.unfamily.anotherstages.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.unfamily.iskalib.stage.StageActionHooks;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {@code /another_stages call_action} — manual stage-action runs (migrated off Library).
 */
public final class CallActionCommand {
    private CallActionCommand() {}

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_PLAYERS = (context, builder) -> {
        var server = context.getSource().getServer();
        if (server == null) {
            return builder.buildFuture();
        }
        List<String> names = server.getPlayerList().getPlayers().stream()
                .map(p -> p.getGameProfile().name())
                .collect(Collectors.toList());
        names.add("@p");
        names.add("@a");
        names.add("@s");
        names.add("@r");
        return SharedSuggestionProvider.suggest(names, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_ACTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(StageActionHooks.getListener().listActionIds(), builder);

    public static LiteralArgumentBuilder<CommandSourceStack> subtree() {
        return Commands.literal("call_action")
                .then(Commands.argument("target", EntityArgument.entities()).suggests(SUGGEST_PLAYERS)
                        .then(Commands.argument("action_id", StringArgumentType.word()).suggests(SUGGEST_ACTIONS)
                                .executes(CallActionCommand::run)
                                .then(Commands.argument("force", BoolArgumentType.bool())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(new String[]{"false", "true"}, b))
                                        .executes(CallActionCommand::run)
                                        .then(Commands.argument("silent", BoolArgumentType.bool())
                                                .executes(CallActionCommand::run)
                                                .then(Commands.argument("hide", BoolArgumentType.bool())
                                                        .executes(CallActionCommand::run))))));
    }

    private static int run(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String actionId = StringArgumentType.getString(context, "action_id");
        boolean force = optionalBool(context, "force", false);
        boolean silent = optionalBool(context, "silent", false);
        boolean hide = optionalBool(context, "hide", false);

        List<ServerPlayer> players;
        try {
            Collection<? extends Entity> entities = EntityArgument.getEntities(context, "target");
            players = entities.stream()
                    .filter(ServerPlayer.class::isInstance)
                    .map(ServerPlayer.class::cast)
                    .collect(Collectors.toList());
        } catch (CommandSyntaxException e) {
            if (!silent) {
                source.sendFailure(Component.literal("§cInvalid target: " + e.getMessage()));
            }
            return 0;
        }

        if (players.isEmpty()) {
            if (!silent) {
                source.sendFailure(Component.literal("§cNo valid players found"));
            }
            return 0;
        }

        int executed = StageActionHooks.getListener().executeActionById(actionId, players, force);
        if (executed == -1) {
            if (!silent) {
                source.sendFailure(Component.literal("§cUnknown action id: " + actionId));
            }
            return 0;
        }
        if (executed == 0 && !force) {
            if (!silent) {
                source.sendFailure(Component.literal(
                        "§cAction '" + actionId + "' has onCall=false. Use force=true to bypass."));
            }
            return 0;
        }
        if (!silent && !hide) {
            source.sendSuccess(
                    () -> Component.literal("§aExecuted action §e" + actionId + " §afor §f" + executed + " §aplayer(s)"),
                    false);
        }
        return executed;
    }

    private static boolean optionalBool(CommandContext<CommandSourceStack> context, String name, boolean defaultValue) {
        try {
            return context.getArgument(name, Boolean.class);
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }
}
