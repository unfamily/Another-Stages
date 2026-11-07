package net.unfamily.anotherstages.kubejs;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.function.Predicate;

/** Auto-id helper: {@code another_stages:kjs/{kind}/{fingerprint}} with {@code _2}, {@code _3} collisions. */
public final class ScriptIdAllocator {
    private ScriptIdAllocator() {}

    public static String allocate(String kind, String fingerprint, Predicate<String> exists) {
        String base = "another_stages:kjs/" + kind + "/" + fingerprint;
        if (!exists.test(base)) {
            return base;
        }
        int n = 2;
        while (exists.test(base + "_" + n)) {
            n++;
        }
        return base + "_" + n;
    }

    public static String fingerprint(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part != null) {
                sb.append(part).append('\0');
            }
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] dig = md.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(16);
            for (int i = 0; i < 8; i++) {
                hex.append(String.format("%02x", dig[i]));
            }
            return hex.toString();
        } catch (Exception e) {
            return Integer.toHexString(sb.toString().hashCode());
        }
    }
}
