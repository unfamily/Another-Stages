package net.unfamily.anotherstages.stagegen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.unfamily.anotherstages.AnotherStages;
import net.unfamily.anotherstages.client.gui.stagegen.RestrictionDraft;
import net.unfamily.anotherstages.client.gui.stagegen.export.ExportUtil;
import net.unfamily.anotherstages.client.gui.stagegen.export.JsonExporter;
import net.unfamily.anotherstages.client.gui.stagegen.export.KubeExporter;
import net.unfamily.anotherstages.config.AsConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Server-side fixed-path write/merge for Stage Generator saves. */
public final class StageGenFileWriter {
    private StageGenFileWriter() {}

    public record Result(boolean ok, String pathDisplay, Component message) {}

    public static Result save(ServerPlayer player, RestrictionDraft draft) {
        draft.ensureId();
        boolean kube = AsConfig.preferKubejs();
        try {
            if (kube) {
                return saveKube(draft);
            }
            return saveJson(draft);
        } catch (Exception ex) {
            AnotherStages.LOGGER.error("Stage Generator save failed", ex);
            return new Result(false, "", Component.translatable(
                    "screen.another_stages.stage_generator.save.failed",
                    ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()));
        }
    }

    /**
     * Removes a restriction from the category file.
     * JSON: by silent {@code id}. Kube: by {@code kubeEntryIndex} then rewrite Entry N file.
     */
    public static Result delete(ServerPlayer player, RestrictionDraft.Category category, String id, int kubeEntryIndex) {
        if (AsConfig.preferKubejs()) {
            return deleteKube(category, kubeEntryIndex);
        }
        return deleteJson(category, id);
    }

    private static Result deleteJson(RestrictionDraft.Category category, String id) {
        if (id == null || id.isBlank()) {
            return new Result(false, "", Component.translatable(
                    "screen.another_stages.stage_generator.save.failed", "missing id"));
        }
        try {
            Path file = StageGenPaths.resolveJsonFile(category);
            if (file == null || !Files.isRegularFile(file)) {
                return new Result(true, "", Component.translatable(
                        "screen.another_stages.stage_generator.delete.ok", id));
            }
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!root.has("restrictions") || !root.get("restrictions").isJsonArray()) {
                return new Result(true, "", Component.translatable(
                        "screen.another_stages.stage_generator.delete.ok", id));
            }
            JsonArray restrictions = root.getAsJsonArray("restrictions");
            JsonArray kept = new JsonArray();
            for (JsonElement el : restrictions) {
                if (el.isJsonObject() && el.getAsJsonObject().has("id")
                        && id.equals(el.getAsJsonObject().get("id").getAsString())) {
                    continue;
                }
                kept.add(el);
            }
            root.add("restrictions", kept);
            Files.writeString(file, ExportUtil.GSON.toJson(root) + "\n", StandardCharsets.UTF_8);
            return new Result(true, StageGenPaths.displayJsonPath(category), Component.translatable(
                    "screen.another_stages.stage_generator.delete.ok", id));
        } catch (Exception ex) {
            AnotherStages.LOGGER.error("Stage Generator delete failed", ex);
            return new Result(false, "", Component.translatable(
                    "screen.another_stages.stage_generator.save.failed",
                    ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()));
        }
    }

    private static Result deleteKube(RestrictionDraft.Category category, int kubeEntryIndex) {
        try {
            Path file = StageGenPaths.resolveKubeFile(category);
            List<RestrictionDraft> entries = new ArrayList<>();
            if (Files.isRegularFile(file)) {
                entries.addAll(StageGenKubeReader.loadCategory(category));
            }
            if (kubeEntryIndex >= 0 && kubeEntryIndex < entries.size()) {
                entries.remove(kubeEntryIndex);
            }
            for (int i = 0; i < entries.size(); i++) {
                entries.get(i).kubeEntryIndex = i;
                entries.get(i).category = category;
            }
            Files.createDirectories(file.getParent());
            Files.writeString(file, KubeExporter.exportFile(category, entries), StandardCharsets.UTF_8);
            String display = StageGenPaths.displayKubePath(category);
            return new Result(true, display, Component.translatable(
                    "screen.another_stages.stage_generator.delete.ok",
                    "Entry " + kubeEntryIndex));
        } catch (Exception ex) {
            AnotherStages.LOGGER.error("Stage Generator kube delete failed", ex);
            return new Result(false, "", Component.translatable(
                    "screen.another_stages.stage_generator.save.failed",
                    ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()));
        }
    }

    private static Result saveJson(RestrictionDraft draft) throws IOException {
        String rootCfg = StageGenPaths.firstDatapackPathConfig();
        if (rootCfg.isBlank()) {
            return new Result(false, "", Component.translatable(
                    "screen.another_stages.stage_generator.save.no_datapack_paths"));
        }
        Path file = StageGenPaths.resolveJsonFile(draft.category);
        if (file == null) {
            return new Result(false, "", Component.translatable(
                    "screen.another_stages.stage_generator.save.no_datapack_paths"));
        }
        Files.createDirectories(file.getParent());
        if (StageGenPaths.isPackContainerRoot(rootCfg)) {
            ensurePackMeta(StageGenPaths.generatedPackMeta(rootCfg));
        }

        JsonObject root;
        if (Files.isRegularFile(file)) {
            String existing = Files.readString(file, StandardCharsets.UTF_8);
            root = JsonParser.parseString(existing).getAsJsonObject();
        } else {
            root = new JsonObject();
            root.addProperty("type", StageGenPaths.jsonType(draft.category));
            root.add("restrictions", new JsonArray());
        }
        if (!root.has("restrictions") || !root.get("restrictions").isJsonArray()) {
            root.add("restrictions", new JsonArray());
        }
        JsonArray restrictions = root.getAsJsonArray("restrictions");
        JsonObject neu = JsonExporter.restrictionObject(draft);
        boolean replaced = false;
        for (int i = 0; i < restrictions.size(); i++) {
            JsonElement el = restrictions.get(i);
            if (el.isJsonObject() && el.getAsJsonObject().has("id")
                    && draft.id.equals(el.getAsJsonObject().get("id").getAsString())) {
                restrictions.set(i, neu);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            restrictions.add(neu);
        }
        root.addProperty("type", StageGenPaths.jsonType(draft.category));
        Files.writeString(file, ExportUtil.GSON.toJson(root) + "\n", StandardCharsets.UTF_8);

        String display = StageGenPaths.displayJsonPath(draft.category);
        return new Result(true, display, Component.translatable(
                "screen.another_stages.stage_generator.save.ok_json",
                display,
                StageGenPaths.displayFirstDatapackRoot()));
    }

    private static Result saveKube(RestrictionDraft draft) throws IOException {
        Path file = StageGenPaths.resolveKubeFile(draft.category);
        Files.createDirectories(file.getParent());
        List<RestrictionDraft> entries = new ArrayList<>();
        if (Files.isRegularFile(file)) {
            entries.addAll(StageGenKubeReader.loadCategory(draft.category));
        }
        for (RestrictionDraft e : entries) {
            e.category = draft.category;
        }
        if (draft.kubeEntryIndex >= 0 && draft.kubeEntryIndex < entries.size()) {
            RestrictionDraft copy = RestrictionDraft.fromJson(draft.toJson().toString());
            copy.category = draft.category;
            copy.kubeEntryIndex = draft.kubeEntryIndex;
            entries.set(draft.kubeEntryIndex, copy);
        } else {
            RestrictionDraft copy = RestrictionDraft.fromJson(draft.toJson().toString());
            copy.category = draft.category;
            copy.kubeEntryIndex = entries.size();
            entries.add(copy);
        }
        for (int i = 0; i < entries.size(); i++) {
            entries.get(i).kubeEntryIndex = i;
        }
        Files.writeString(file, KubeExporter.exportFile(draft.category, entries), StandardCharsets.UTF_8);
        String display = StageGenPaths.displayKubePath(draft.category);
        return new Result(true, display, Component.translatable(
                "screen.another_stages.stage_generator.save.ok_kube",
                display));
    }

    private static void ensurePackMeta(Path meta) throws IOException {
        if (Files.isRegularFile(meta)) {
            return;
        }
        Files.createDirectories(meta.getParent());
        String body = """
                {
                  "pack": {
                    "description": "Another Stages generator output",
                    "pack_format": 88
                  }
                }
                """;
        Files.writeString(meta, body, StandardCharsets.UTF_8);
    }
}
