package de.cuteghg.fre.format;

import de.cuteghg.fre.FlagRanksEmotesPlugin;
import de.cuteghg.fre.config.ConfigManager;
import de.cuteghg.fre.country.CountryInfo;
import de.cuteghg.fre.country.CountryProvider;
import de.cuteghg.fre.pack.ResourcePackService;
import de.cuteghg.fre.rank.RankService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class PlayerFormatService implements Listener {
    private final FlagRanksEmotesPlugin plugin;
    private final ConfigManager configManager;
    private final RankService rankService;
    private final CountryProvider countryProvider;
    private final ResourcePackService resourcePackService;

    public PlayerFormatService(FlagRanksEmotesPlugin plugin,
                               ConfigManager configManager,
                               RankService rankService,
                               CountryProvider countryProvider,
                               ResourcePackService resourcePackService) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.rankService = rankService;
        this.countryProvider = countryProvider;
        this.resourcePackService = resourcePackService;
    }

    public CompletableFuture<Component> formatChat(Player player, Component message) {
        return countryProvider.lookup(player).thenApply(country -> {
            return formatChatWithCountry(player, country, message);
        });
    }

    public Component formatChatWithCountry(Player player, CountryInfo country, Component message) {
        String formatted = applyTokens(configManager.getChatFormat(), player, country, "%MESSAGE%");
        Component base = LegacyComponentSerializer.legacyAmpersand().deserialize(formatted);
        return base.replaceText(TextReplacementConfig.builder().matchLiteral("%MESSAGE%").replacement(message).build());
    }

    public String applyTokens(String input, Player player, CountryInfo country, String messagePlaceholder) {
        String rankIconToken = rankService.resolveRankIconToken(player).toLowerCase();
        String rankIcon = resourcePackService.getRankGlyph(rankIconToken).orElse(configManager.getRankFallbackIcon());
        String countryGlyph = resourcePackService.getFlagGlyph(country.countryCode().toLowerCase())
                .orElse(country.unicodeFlag());

        return input
                .replace("%fre_rank%", rankService.resolveRankLabel(player))
                .replace("%fre_rank_icon%", rankIcon)
                .replace("%fre_prefix%", rankIcon + " " + countryGlyph)
                .replace("%fre_flag%", countryGlyph)
                .replace("%fre_country%", country.countryName())
                .replace("%fre_country_code%", country.countryCode())
                .replace("%fre_player_format%", rankIcon + " " + countryGlyph + " " + player.getName())
                .replace("%player_name%", player.getName())
                .replace("%message%", messagePlaceholder);
    }

    public void applyPlayerListName(Player player) {
        countryProvider.lookup(player).thenAccept(country -> {
            String line = applyTokens(configManager.getPlayerListFormat(), player, country, "");
            Component name = LegacyComponentSerializer.legacyAmpersand().deserialize(line);
            Bukkit.getScheduler().runTask(plugin, () -> player.playerListName(name));
        });
    }

    public void applyAllOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            applyPlayerListName(player);
        }
    }

    public Map<String, String> emoteGlyphs() {
        return resourcePackService.getEmoteGlyphs();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        applyPlayerListName(event.getPlayer());
    }
}
