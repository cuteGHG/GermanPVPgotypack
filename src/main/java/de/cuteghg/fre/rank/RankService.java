package de.cuteghg.fre.rank;

import de.cuteghg.fre.FlagRanksEmotesPlugin;
import de.cuteghg.fre.config.ConfigManager;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class RankService {
    private final FlagRanksEmotesPlugin plugin;
    private final ConfigManager configManager;
    private final LuckPermsBridge luckPermsBridge;

    public RankService(FlagRanksEmotesPlugin plugin, ConfigManager configManager) {
        this(plugin, configManager, new LuckPermsBridge());
    }

    RankService(FlagRanksEmotesPlugin plugin, ConfigManager configManager, LuckPermsBridge luckPermsBridge) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.luckPermsBridge = luckPermsBridge;
    }

    public RankDefinition resolveRank(Player player) {
        List<RankDefinition> ranks = configManager.getRanks();
        Optional<String> group = luckPermsBridge.getPrimaryGroup(player).map(String::toLowerCase);

        return ranks.stream()
                .filter(rank -> player.hasPermission(rank.permission())
                        || group.map(g -> rank.luckPermsGroup() != null && rank.luckPermsGroup().equalsIgnoreCase(g)).orElse(false))
                .max(Comparator.comparingInt(RankDefinition::priority))
                .orElseGet(configManager::getDefaultRank);
    }

    public String resolveRankLabel(Player player) {
        return resolveRank(player).label();
    }

    public String resolveRankIconToken(Player player) {
        return resolveRank(player).icon();
    }
}
