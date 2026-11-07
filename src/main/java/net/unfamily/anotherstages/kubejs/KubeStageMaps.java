package net.unfamily.anotherstages.kubejs;

import net.unfamily.anotherstages.item.StageItemHandler;
import net.unfamily.anotherstages.recipe.StageCondition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/** Shared helpers to read stage arrays / string lists from KubeJS script values. */
public final class KubeStageMaps {
    private KubeStageMaps() {}

    public static List<String> stringList(Object raw) {
        List<String> out = new ArrayList<>();
        if (raw == null) {
            return out;
        }
        if (raw instanceof CharSequence cs) {
            out.add(cs.toString());
            return out;
        }
        if (raw instanceof Collection<?> col) {
            for (Object o : col) {
                if (o != null) {
                    out.add(o.toString());
                }
            }
            return out;
        }
        if (raw.getClass().isArray()) {
            int len = java.lang.reflect.Array.getLength(raw);
            for (int i = 0; i < len; i++) {
                Object o = java.lang.reflect.Array.get(raw, i);
                if (o != null) {
                    out.add(o.toString());
                }
            }
            return out;
        }
        out.add(raw.toString());
        return out;
    }

    public static List<Integer> intList(Object raw) {
        List<Integer> out = new ArrayList<>();
        if (raw == null) {
            return out;
        }
        if (raw instanceof Number n) {
            out.add(n.intValue());
            return out;
        }
        if (raw instanceof Collection<?> col) {
            for (Object o : col) {
                if (o instanceof Number n) {
                    out.add(n.intValue());
                } else if (o != null) {
                    try {
                        out.add(Integer.parseInt(o.toString()));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            return out;
        }
        if (raw.getClass().isArray()) {
            int len = java.lang.reflect.Array.getLength(raw);
            for (int i = 0; i < len; i++) {
                Object o = java.lang.reflect.Array.get(raw, i);
                if (o instanceof Number n) {
                    out.add(n.intValue());
                }
            }
        }
        return out;
    }

    public static void fillItemStages(Object raw, List<StageItemHandler.StageCondition> out) {
        out.clear();
        if (!(raw instanceof Collection<?> col)) {
            return;
        }
        for (Object o : col) {
            StageItemHandler.StageCondition c = parseItemStage(o);
            if (c != null) {
                out.add(c);
            }
        }
    }

    public static void fillRecipeStages(Object raw, List<StageCondition> out) {
        out.clear();
        if (!(raw instanceof Collection<?> col)) {
            return;
        }
        for (Object o : col) {
            StageCondition c = parseRecipeStage(o);
            if (c != null) {
                out.add(c);
            }
        }
    }

    private static StageItemHandler.StageCondition parseItemStage(Object o) {
        if (!(o instanceof Map<?, ?> map)) {
            return null;
        }
        StageItemHandler.StageCondition c = new StageItemHandler.StageCondition();
        c.stageType = str(map, "stage_type", "player");
        c.stage = str(map, "stage", "");
        c.is = bool(map, "is", true);
        return c;
    }

    private static StageCondition parseRecipeStage(Object o) {
        if (!(o instanceof Map<?, ?> map)) {
            return null;
        }
        StageCondition c = new StageCondition();
        c.stageType = str(map, "stage_type", "player");
        c.stage = str(map, "stage", "");
        c.is = bool(map, "is", true);
        return c;
    }

    private static String str(Map<?, ?> map, String key, String def) {
        Object v = map.get(key);
        return v == null ? def : v.toString();
    }

    private static boolean bool(Map<?, ?> map, String key, boolean def) {
        Object v = map.get(key);
        if (v == null) {
            return def;
        }
        if (v instanceof Boolean b) {
            return b;
        }
        return !"false".equalsIgnoreCase(v.toString());
    }
}
