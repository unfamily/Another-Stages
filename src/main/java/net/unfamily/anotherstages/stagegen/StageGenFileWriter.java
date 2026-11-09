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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    /** Removes a restriction by id from the category JSON file (Kube delete not supported yet). */
    public static Result delete(ServerPlayer player, RestrictionDraft.Category category, String id) {
        if (id == null || id.isBlank()) {
            return new Result(false, "", Component.translatable(
                    "screen.another_stages.stage_generator.save.failed", "missing id"));
        }
        if (AsConfig.preferKubejs()) {
            return new Result(false, "", Component.translatable(
                    "screen.another_stages.stage_generator.delete.kube_unsupported"));
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
        String block = KubeExporter.export(draft);
        String content;
        if (Files.isRegularFile(file)) {
            content = Files.readString(file, StandardCharsets.UTF_8);
            Matcher m = Pattern.compile(
                    "(?s)// stagegen:" + Pattern.quote(draft.id) + "\n.*?// end-stagegen:"
                            + Pattern.quote(draft.id) + "\n?").matcher(content);
            if (m.find()) {
                content = m.replaceFirst(Matcher.quoteReplacement(block));
            } else {
                if (!content.endsWith("\n") && !content.isEmpty()) {
                    content = content + "\n";
                }
                content = content + "\n" + block;
            }
        } else {
            content = block;
        }
        Files.writeString(file, content, StandardCharsets.UTF_8);
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
