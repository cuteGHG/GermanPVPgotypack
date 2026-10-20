package de.cuteghg.fre.emote;

import de.cuteghg.fre.config.ConfigManager;
import de.cuteghg.fre.config.EmoteDefinition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class EmoteParser {
    private final ConfigManager configManager;

    public EmoteParser(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public Component parse(Player player, String message, Map<String, String> emoteChars) {
        if (message.length() > configManager.getEmoteMaxMessageLength()) {
            return Component.text(message);
        }

        List<EmoteDefinition> emotes = configManager.getEmotesByAlias().values().stream()
                .sorted(Comparator.comparingInt((EmoteDefinition e) -> e.alias().length()).reversed())
                .toList();

        int replacements = 0;
        TextComponent.Builder out = Component.text();
        int i = 0;
        while (i < message.length()) {
            boolean matched = false;
            for (EmoteDefinition emote : emotes) {
                String alias = emote.alias();
                if (i + alias.length() > message.length()) {
                    continue;
                }
                String piece = message.substring(i, i + alias.length());
                boolean eq = configManager.isEmoteCaseSensitive() ? piece.equals(alias) : piece.equalsIgnoreCase(alias);
                if (!eq) {
                    continue;
                }

                boolean escaped = i > 0 && message.substring(Math.max(0, i - configManager.getEmoteEscapePrefix().length()), i)
                        .equals(configManager.getEmoteEscapePrefix());
                if (escaped) {
                    out.append(Component.text(alias));
                    i += alias.length();
                    matched = true;
                    break;
                }

                if (replacements >= configManager.getEmoteMaxReplacements() || !player.hasPermission(emote.permission())) {
                    out.append(Component.text(emote.fallbackText()));
                } else {
                    String key = configManager.normalizeAlias(alias);
                    String glyph = emoteChars.getOrDefault(key, emote.fallbackText());
                    out.append(Component.text(glyph));
                    replacements++;
                }

                i += alias.length();
                matched = true;
                break;
            }

            if (!matched) {
                out.append(Component.text(message.charAt(i)));
                i++;
            }
        }

        return out.build();
    }

    public List<String> allAliases() {
        return new ArrayList<>(configManager.getEmotesByAlias().keySet());
    }
}
