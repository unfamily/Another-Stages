package net.unfamily.anotherstages.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Shared id / tag / mod / regex matchers for stage gating lists.
 * <p>
 * Entry forms (tried in order per entry):
 * <ul>
 *   <li>{@code regex:<javaPattern>} — {@link Pattern#find()} on the full id string; invalid pattern = no hit</li>
 *   <li>{@code #tag_id} — item/block tag (item/block helpers only)</li>
 *   <li>{@code @modid} — namespace equals</li>
 *   <li>exact id string</li>
 * </ul>
 */
public final class StageIdMatchers {
    public static final String REGEX_PREFIX = "regex:";

    private static final Map<String, Pattern> PATTERN_CACHE = new ConcurrentHashMap<>();
    private static final Pattern INVALID = Pattern.compile("(?!)"); // never matches

    private StageIdMatchers() {}

    public static boolean hasRegexPrefix(@Nullable String entry) {
        return entry != null && entry.startsWith(REGEX_PREFIX);
    }

    /** True if any entry in {@code list} starts with {@code regex:}. */
    public static boolean listHasRegex(@Nullable List<String> list) {
        if (list == null || list.isEmpty()) {
            return false;
        }
        for (String entry : list) {
            if (hasRegexPrefix(entry)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Match a registry id string (recipe id, recipe type id, menu class/type id, …)
     * against one list entry.
     */
    public static boolean matchesIdString(@Nullable String idStr, @Nullable String entry) {
        if (idStr == null || idStr.isEmpty() || entry == null || entry.isEmpty()) {
            return false;
        }
        if (hasRegexPrefix(entry)) {
            return regexFind(idStr, entry.substring(REGEX_PREFIX.length()));
        }
        if (entry.startsWith("@")) {
            String mod = entry.substring(1).trim();
            int colon = idStr.indexOf(':');
            String ns = colon >= 0 ? idStr.substring(0, colon) : idStr;
            return !mod.isEmpty() && ns.equals(mod);
        }
        if (entry.startsWith("#")) {
            return false;
        }
        return idStr.equals(entry);
    }

    /** True if {@code idStr} matches any entry in {@code list}. */
    public static boolean idInList(@Nullable String idStr, @Nullable List<String> list) {
        if (idStr == null || list == null || list.isEmpty()) {
            return false;
        }
        for (String entry : list) {
            if (matchesIdString(idStr, entry)) {
                return true;
            }
        }
        return false;
    }

    public static boolean matchesItem(@Nullable Item item, @Nullable List<String> list) {
        if (item == null || list == null || list.isEmpty()) {
            return false;
        }
        Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
        String itemIdStr = itemId.toString();
        String modId = itemId.getNamespace();
        for (String entry : list) {
            if (entry == null || entry.isEmpty()) {
                continue;
            }
            if (hasRegexPrefix(entry)) {
                if (regexFind(itemIdStr, entry.substring(REGEX_PREFIX.length()))) {
                    return true;
                }
            } else if (entry.startsWith("#")) {
                Identifier tagId = Identifier.tryParse(entry.substring(1));
                if (tagId != null && item.builtInRegistryHolder().is(ItemTags.create(tagId))) {
                    return true;
                }
            } else if (entry.startsWith("@")) {
                String entryMod = entry.substring(1).trim();
                if (!entryMod.isEmpty() && modId.equals(entryMod)) {
                    return true;
                }
            } else if (itemIdStr.equals(entry)) {
                return true;
            }
        }
        return false;
    }

    public static boolean matchesBlock(@Nullable Block block, @Nullable List<String> list) {
        if (block == null || list == null || list.isEmpty()) {
            return false;
        }
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        String idStr = id.toString();
        String mod = id.getNamespace();
        for (String entry : list) {
            if (entry == null || entry.isEmpty()) {
                continue;
            }
            if (hasRegexPrefix(entry)) {
                if (regexFind(idStr, entry.substring(REGEX_PREFIX.length()))) {
                    return true;
                }
            } else if (entry.startsWith("#")) {
                Identifier tag = Identifier.tryParse(entry.substring(1));
                if (tag != null && block.builtInRegistryHolder().is(BlockTags.create(tag))) {
                    return true;
                }
            } else if (entry.startsWith("@")) {
                if (mod.equals(entry.substring(1).trim())) {
                    return true;
                }
            } else if (idStr.equals(entry)) {
                return true;
            }
        }
        return false;
    }

    private static boolean regexFind(String haystack, String patternSource) {
        if (patternSource.isEmpty()) {
            return false;
        }
        Pattern pattern = PATTERN_CACHE.computeIfAbsent(patternSource, StageIdMatchers::compileOrInvalid);
        return pattern.matcher(haystack).find();
    }

    private static Pattern compileOrInvalid(String source) {
        try {
            return Pattern.compile(source);
        } catch (PatternSyntaxException ignored) {
            return INVALID;
        }
    }
}
