package net.unfamily.anotherstages.compat.refinedstorage;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

/**
 * Thread-local craft subject for RS matrix / encode / autocraft gates.
 * Prefer interacting player UUID; fall back to node {@code placedByPlayerId}.
 */
public final class RsCraftSubjects {
    private static final ThreadLocal<Deque<Frame>> STACK = ThreadLocal.withInitial(ArrayDeque::new);

    private RsCraftSubjects() {}

    public static void push(@Nullable UUID uuid, @Nullable Player player, @Nullable Level level) {
        STACK.get().push(new Frame(uuid, player, level));
    }

    public static void pushPlayer(Player player) {
        push(player.getUUID(), player, player.level());
    }

    public static void pushUuid(@Nullable UUID uuid, @Nullable Level level) {
        push(uuid, null, level);
    }

    public static void pop() {
        Deque<Frame> stack = STACK.get();
        if (!stack.isEmpty()) {
            stack.pop();
        }
        if (stack.isEmpty()) {
            STACK.remove();
        }
    }

    @Nullable
    public static UUID currentUuid() {
        Frame frame = peek();
        return frame == null ? null : frame.uuid;
    }

    @Nullable
    public static Player currentPlayer() {
        Frame frame = peek();
        return frame == null ? null : frame.player;
    }

    @Nullable
    public static ServerPlayer currentServerPlayer() {
        Player player = currentPlayer();
        return player instanceof ServerPlayer serverPlayer ? serverPlayer : null;
    }

    @Nullable
    public static Level currentLevel() {
        Frame frame = peek();
        return frame == null ? null : frame.level;
    }

    @Nullable
    private static Frame peek() {
        Deque<Frame> stack = STACK.get();
        return stack.isEmpty() ? null : stack.peek();
    }

    private record Frame(@Nullable UUID uuid, @Nullable Player player, @Nullable Level level) {}
}
