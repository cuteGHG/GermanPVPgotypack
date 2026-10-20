package de.cuteghg.fre.util;

import java.util.Locale;

public final class FlagUtil {
    private FlagUtil() {
    }

    public static String countryCodeToFlag(String code) {
        if (code == null || code.length() != 2) {
            return "";
        }
        String upper = code.toUpperCase(Locale.ROOT);
        char first = upper.charAt(0);
        char second = upper.charAt(1);
        if (first < 'A' || first > 'Z' || second < 'A' || second > 'Z') {
            return "";
        }
        return new String(Character.toChars(0x1F1E6 + (first - 'A'))) + new String(Character.toChars(0x1F1E6 + (second - 'A')));
    }
}
