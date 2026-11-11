package net.unfamily.anotherstages.client.gui.stagegen;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.unfamily.iskalib.stage.StageCatalog;
import org.jetbrains.annotations.Nullable;

/** One known-stage catalog row for Stage Generator (type + id). */
public final class KnownStageDraft {
    public String stageType = "player";
    public String stage = "";

    public KnownStageDraft() {}

    public KnownStageDraft(String stageType, String stage) {
        this.stageType = stageType == null || stageType.isBlank() ? "player" : stageType.trim().toLowerCase();
        this.stage = stage == null ? "" : stage.trim();
    }

    public boolean isReady() {
        return StageCatalog.normalizeStageType(stageType) != null && stage != null && !stage.isBlank();
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("stage_type", stageType == null ? "player" : stageType);
        o.addProperty("stage", stage == null ? "" : stage);
        return o;
    }

    public static KnownStageDraft fromJson(String json) {
        return fromJsonObject(JsonParser.parseString(json).getAsJsonObject());
    }

    public static KnownStageDraft fromJsonObject(JsonObject o) {
        KnownStageDraft d = new KnownStageDraft();
        if (o.has("stage_type")) {
            d.stageType = o.get("stage_type").getAsString();
        }
        if (o.has("stage")) {
            d.stage = o.get("stage").getAsString();
        }
        String norm = StageCatalog.normalizeStageType(d.stageType);
        if (norm != null) {
            d.stageType = norm;
        }
        return d;
    }

    public String summary() {
        String t = stageType == null || stageType.isBlank() ? "?" : stageType;
        String s = stage == null || stage.isBlank() ? "(empty)" : stage;
        return t + " / " + s;
    }

    @Nullable
    public String incompleteReasonKey() {
        if (StageCatalog.normalizeStageType(stageType) == null) {
            return "screen.another_stages.stage_generator.known.incomplete.type";
        }
        if (stage == null || stage.isBlank()) {
            return "screen.another_stages.stage_generator.known.incomplete.id";
        }
        return null;
    }
}
