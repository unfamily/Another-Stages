package net.unfamily.anotherstages.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.unfamily.anotherstages.AnotherStages;

import java.net.URI;

/**
 * {@code /another_stages}: owns {@code call_action} and {@code wiki}; other stage CRUD subcommands mirror Library.
 * Registered at {@link EventPriority#LOWEST} so {@code /iska_lib_stage} already exists.
 */
@EventBusSubscriber(modid = AnotherStages.MOD_ID)
public final class StageMirrorCommand {
    private static final String WIKI_URL = "https://www.anothermoddevwiki.eu/another-stages";

    private StageMirrorCommand() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal("another_stages")
                .requires(src -> src.permissions().hasPermission(
                        new Permission.HasCommandLevel(PermissionLevel.byId(2))));

        // Mirror of Library call_action (JSON-only Stage Actions).
        builder.then(CallActionCommand.subtree());
        builder.then(Commands.literal("wiki").executes(StageMirrorCommand::executeWiki));

        CommandNode<CommandSourceStack> libraryRoot = dispatcher.getRoot().getChild("iska_lib_stage");
        boolean mirroredEdit = false;
        if (libraryRoot != null) {
            for (CommandNode<CommandSourceStack> child : libraryRoot.getChildren()) {
                String name = child.getName();
                if ("call_action".equals(name)) {
                    continue;
                }
                if ("edit".equals(name)) {
                    mirroredEdit = true;
                }
                builder.then(mirrorLiteral(name, child));
            }
        } else {
            builder.executes(ctx -> {
                ctx.getSource().sendFailure(Component.literal(
                        "Library stage command /iska_lib_stage is not registered; install Iskandert's Library."));
                return 0;
            });
        }

        // Fallback when Library has no edit (hook missing / older Library).
        if (!mirroredEdit) {
            builder.then(Commands.literal("edit").executes(StageMirrorCommand::executeEdit));
        }

        dispatcher.register(builder);
    }

    private static int executeEdit(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof net.minecraft.server.level.ServerPlayer player)) {
            context.getSource().sendFailure(Component.translatable("commands.iska_utils.stage.error.need_player"));
            return 0;
        }
        net.unfamily.anotherstages.network.AsNetworking.openStageGenerator(player);
        return 1;
    }

    private static int executeWiki(CommandContext<CommandSourceStack> context) {
        MutableComponent link = Component.literal(WIKI_URL)
                .withStyle(Style.EMPTY
                        .withColor(ChatFormatting.AQUA)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent.OpenUrl(URI.create(WIKI_URL)))
                        .withHoverEvent(new HoverEvent.ShowText(
                                Component.translatable("commands.another_stages.wiki.hover"))));
        context.getSource().sendSuccess(
                () -> Component.translatable("commands.another_stages.wiki", link),
                false);
        return 1;
    }

    /**
     * Brigadier {@code redirect} alone does not run leaf {@code executes} (e.g. {@code reload}).
     * Copy the target command when present, and redirect so nested args still parse under Library.
     */
    private static LiteralArgumentBuilder<CommandSourceStack> mirrorLiteral(
            String name, CommandNode<CommandSourceStack> target) {
        LiteralArgumentBuilder<CommandSourceStack> lit = Commands.literal(name);
        Command<CommandSourceStack> command = target.getCommand();
        if (command != null) {
            // Leaf commands (e.g. reload): redirect alone does not run the target executes.
            lit.executes(command);
        }
        if (!target.getChildren().isEmpty()) {
            // Nested trees (list/set/add/…): continue parsing under the Library node.
            lit.redirect(target);
        }
        return lit;
    }
}
