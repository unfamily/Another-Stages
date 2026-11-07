package net.unfamily.anotherstages.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

/**
 * Thin mirror of Library {@code /iska_lib_stage}: redirects when possible, otherwise points packdevs to it.
 */
public final class StageMirrorCommand {
    private StageMirrorCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        CommandNode<CommandSourceStack> libraryRoot = dispatcher.getRoot().getChild("iska_lib_stage");
        var builder = Commands.literal("another_stages")
                .requires(src -> src.permissions().hasPermission(
                        new Permission.HasCommandLevel(PermissionLevel.byId(2))));

        if (libraryRoot != null) {
            dispatcher.register(builder.redirect(libraryRoot));
        } else {
            dispatcher.register(builder.executes(ctx -> {
                ctx.getSource().sendFailure(Component.literal(
                        "Library stage command /iska_lib_stage is not registered; install Iskandert's Library."));
                return 0;
            }));
        }
    }
}
