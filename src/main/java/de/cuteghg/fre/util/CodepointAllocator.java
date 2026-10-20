package de.cuteghg.fre.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CodepointAllocator {
    public static final int RANK_BASE = 0xE000;
    public static final int FLAG_BASE = 0xE100;
    public static final int EMOTE_BASE = 0xE500;

    private CodepointAllocator() {
    }

    public static Map<String, Integer> allocate(List<String> keys, int base) {
        List<String> sorted = keys.stream().map(k -> k.toLowerCase(Locale.ROOT)).sorted().toList();
        Map<String, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < sorted.size(); i++) {
            map.put(sorted.get(i), base + i);
        }
        return map;
    }

    public static String toChar(int codepoint) {
        return new String(Character.toChars(codepoint));
    }
}
