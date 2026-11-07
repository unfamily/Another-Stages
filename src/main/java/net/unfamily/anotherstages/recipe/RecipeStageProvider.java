package net.unfamily.anotherstages.recipe;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.unfamily.iskalib.crafting.RecipeBlockBridge;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** Registers Another Stages as the Library {@link RecipeBlockBridge} provider. */
public final class RecipeStageProvider implements RecipeBlockBridge.Provider {
    private RecipeStageProvider() {}

    public static final RecipeStageProvider INSTANCE = new RecipeStageProvider();

    public static void register() {
        RecipeBlockBridge.registerProvider(INSTANCE);
    }

    @Override
    public boolean isBlocked(
            Identifier recipeId,
            @Nullable UUID ownerUuid,
            @Nullable Level level,
            @Nullable ServerPlayer onlinePlayer) {
        return StageRecipeHandler.isBlocked(recipeId, ownerUuid, level, onlinePlayer);
    }
}
