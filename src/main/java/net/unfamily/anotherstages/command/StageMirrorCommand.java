package net.unfamily.anotherstages.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.unfamily.anotherstages.AnotherStages;

/**
 * {@code /another_stages}: owns {@code call_action}; other stage CRUD subcommands mirror Library.
 * Registered at {@link EventPriority#LOWEST} so {@code /iska_lib_stage} already exists.
 */
@EventBusSubscriber(modid = AnotherStages.MOD_ID)
public final class StageMirrorCommand {
    private StageMirrorCommand() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(com.mojang.brigadier.CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal("another_stages")
                .requires(src -> src.permissions().hasPermission(
                        new Permission.HasCommandLevel(PermissionLevel.byId(2))));

        // Migrated off Library: only Another Stages registers call_action.
        builder.then(CallActionCommand.subtree());

        CommandNode<CommandSourceStack> libraryRoot = dispatcher.getRoot().getChild("iska_lib_stage");
        if (libraryRoot != null) {
            for (CommandNode<CommandSourceStack> child : libraryRoot.getChildren()) {
                String name = child.getName();
                if ("call_action".equals(name)) {
                    continue;
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

        dispatcher.register(builder);
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
