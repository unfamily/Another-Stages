package net.unfamily.anotherstages.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

/**
 * {@code /another_stages}: owns {@code call_action}; other stage CRUD subcommands mirror Library.
 */
public final class StageMirrorCommand {
    private StageMirrorCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
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
                builder.then(Commands.literal(name).redirect(child));
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
}
