package net.unfamily.anotherstages.client.gui.stagegen.export;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Shared helpers for Stage Generator exporters. */
public final class ExportUtil {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private ExportUtil() {}

    public static List<String> splitCsv(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        for (String part : raw.split("[,\\s]+")) {
            String t = part.trim();
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        return out;
    }

    public static String jsonString(String s) {
        if (s == null) {
            return "\"\"";
        }
        String escaped = s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        return "\"" + escaped + "\"";
    }

    public static String jsString(String s) {
        if (s == null) {
            return "''";
        }
        String escaped = s
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
        return "'" + escaped + "'";
    }

    public static String jsonStringArray(List<String> values) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(jsonString(values.get(i)));
        }
        sb.append(']');
        return sb.toString();
    }

    public static String jsStringArray(List<String> values) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(jsString(values.get(i)));
        }
        sb.append(']');
        return sb.toString();
    }

    public static String jsonIntArray(List<Integer> values) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(values.get(i));
        }
        sb.append(']');
        return sb.toString();
    }

    public static List<Integer> splitInts(String raw) {
        List<Integer> out = new ArrayList<>();
        for (String s : splitCsv(raw)) {
            try {
                out.add(Integer.parseInt(s));
            } catch (NumberFormatException ignored) {
                // skip invalid
            }
        }
        return out;
    }

    public static String normalizeLogic(String logic) {
        if (logic == null || logic.isBlank()) {
            return "AND";
        }
        return logic.trim().toUpperCase(Locale.ROOT);
    }
}
