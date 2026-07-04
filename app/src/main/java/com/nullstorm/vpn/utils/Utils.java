package com.nullstorm.vpn.utils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class Utils {
    public static String sanitizeVlessLink(String link) {
        try {
            int hashIndex = link.indexOf("#");
            if (hashIndex == -1) return link;

            String base = link.substring(0, hashIndex);
            String fragment = link.substring(hashIndex + 1);

            // encode fragment (remove emoji / illegal chars)
            String encodedFragment = URLEncoder.encode(fragment, StandardCharsets.UTF_8.toString());

            return base + "#" + encodedFragment;

        } catch (Exception e) {
            return link; // fallback
        }
    }
}
