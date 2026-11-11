package net.unfamily.anotherstages.kubejs;

import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * Payload for {@code AnotherStagesEvents.stageChanged}.
 * Fired on the server when a Library stage is added or removed (player / world / team).
 */
public final class StageChangedKubeEvent implements KubeEvent {
    /** {@code player}, {@code world}, or {@code team}. */
    public final String stageType;
    /** Stage id that changed. */
    public final String stage;
    /** {@code true} if the stage was added, {@code false} if removed. */
    public final boolean added;
    /** Set for player-stage changes; {@code null} for world (and often team). */
    public final @Nullable ServerPlayer player;
    /** Team key/name when {@link #stageType} is {@code team}; otherwise {@code null}. */
    public final @Nullable String team;
    public final MinecraftServer server;

    public StageChangedKubeEvent(
            String stageType,
            String stage,
            boolean added,
            @Nullable ServerPlayer player,
            @Nullable String team,
            MinecraftServer server
    ) {
        this.stageType = stageType;
        this.stage = stage;
        this.added = added;
        this.player = player;
        this.team = team;
        this.server = server;
    }
}
