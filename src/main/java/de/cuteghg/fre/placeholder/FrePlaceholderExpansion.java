package de.cuteghg.fre.placeholder;

import de.cuteghg.fre.FlagRanksEmotesPlugin;
import de.cuteghg.fre.country.CountryInfo;
import de.cuteghg.fre.country.CountryProvider;
import de.cuteghg.fre.format.PlayerFormatService;
import de.cuteghg.fre.rank.RankService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public final class FrePlaceholderExpansion extends PlaceholderExpansion {
    private final FlagRanksEmotesPlugin plugin;
    private final RankService rankService;
    private final CountryProvider countryProvider;
    private final PlayerFormatService formatService;

    public FrePlaceholderExpansion(FlagRanksEmotesPlugin plugin, RankService rankService,
                                   CountryProvider countryProvider, PlayerFormatService formatService) {
        this.plugin = plugin;
        this.rankService = rankService;
        this.countryProvider = countryProvider;
        this.formatService = formatService;
    }

    @Override
    public String getIdentifier() {
        return "fre";
    }

    @Override
    public String getAuthor() {
        return "cuteGHG";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null) {
            return "";
        }

        CountryInfo country = countryProvider.lookup(player).getNow(plugin.getConfigManager().getUnknownCountryInfo());
        return switch (params.toLowerCase()) {
            case "rank" -> rankService.resolveRankLabel(player);
            case "rank_icon" -> formatService.applyTokens("%fre_rank_icon%", player, country, "");
            case "prefix" -> formatService.applyTokens("%fre_prefix%", player, country, "");
            case "flag" -> formatService.applyTokens("%fre_flag%", player, country, "");
            case "country" -> country.countryName();
            case "country_code" -> country.countryCode();
            case "player_format" -> formatService.applyTokens("%fre_player_format%", player, country, "");
            default -> "";
        };
    }
}
