package de.cuteghg.fre.config;

public record EmoteDefinition(
        String key,
        String alias,
        String file,
        String permission,
        String fallbackText
) {
}
