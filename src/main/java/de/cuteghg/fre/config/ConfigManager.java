package de.cuteghg.fre.config;

import de.cuteghg.fre.FlagRanksEmotesPlugin;
import de.cuteghg.fre.country.CountryInfo;
import de.cuteghg.fre.rank.RankDefinition;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class ConfigManager {
    private final FlagRanksEmotesPlugin plugin;

    private List<RankDefinition> ranks = List.of();
    private Map<String, EmoteDefinition> emotesByAlias = Map.of();

    private CountryInfo unknownCountryInfo;
    private String chatFormat;
    private String playerListFormat;
    private boolean emoteCaseSensitive;
    private String emoteEscapePrefix;
    private int emoteMaxReplacements;
    private int emoteMaxMessageLength;
    private String rankFallbackIcon;

    private String packNamespace;
    private int packFormat;
    private String packDescription;
    private String packZipName;
    private String packUrl;
    private String packSha1;
    private boolean packRequired;
    private String packPrompt;

    private String geoIpDatabasePath;
    private long geoIpCacheMillis;
    private boolean geoIpDebugLogging;

    public ConfigManager(FlagRanksEmotesPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        this.chatFormat = config.getString("format.chat", "%fre_rank_icon% %fre_flag% %player_name% &8» &f%message%");
        this.playerListFormat = config.getString("format.player-list", "%fre_rank_icon% %fre_flag% %player_name%");

        this.rankFallbackIcon = config.getString("ranks.default-icon", "?");
        this.ranks = loadRanks(config.getConfigurationSection("ranks.entries"));

        this.unknownCountryInfo = CountryInfo.unknown(
                config.getString("geoip.unknown.code", "??"),
                config.getString("geoip.unknown.name", "Unknown"),
                config.getString("geoip.unknown.flag", "🏳")
        );
        this.geoIpDatabasePath = config.getString("geoip.database", "GeoLite2-Country.mmdb");
        this.geoIpCacheMillis = Math.max(1000, config.getLong("geoip.cache-seconds", 3600) * 1000L);
        this.geoIpDebugLogging = config.getBoolean("geoip.debug-log", false);

        this.emoteCaseSensitive = config.getBoolean("emotes.case-sensitive", false);
        this.emoteEscapePrefix = config.getString("emotes.escape-prefix", "\\");
        this.emoteMaxReplacements = Math.max(0, config.getInt("emotes.max-replacements", 10));
        this.emoteMaxMessageLength = Math.max(0, config.getInt("emotes.max-message-length", 256));
        this.emotesByAlias = loadEmotes(config.getConfigurationSection("emotes.entries"));

        this.packNamespace = config.getString("resource-pack.namespace", "fre");
        this.packFormat = config.getInt("resource-pack.pack-format", 61);
        this.packDescription = config.getString("resource-pack.description", "FlagRanksEmotes generated pack");
        this.packZipName = config.getString("resource-pack.output-zip", "flag-ranks-emotes.zip");
        this.packUrl = config.getString("resource-pack.url", "");
        this.packSha1 = config.getString("resource-pack.sha1", "");
        this.packRequired = config.getBoolean("resource-pack.required", false);
        this.packPrompt = config.getString("resource-pack.prompt", "Please enable the server resource pack.");
    }

    private List<RankDefinition> loadRanks(ConfigurationSection section) {
        if (section == null) {
            return List.of(defaultRank());
        }
        List<RankDefinition> list = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection rankSection = section.getConfigurationSection(key);
            if (rankSection == null) {
                continue;
            }
            list.add(new RankDefinition(
                    key.toUpperCase(Locale.ROOT),
                    rankSection.getString("permission", "fre.rank." + key.toLowerCase(Locale.ROOT)),
                    rankSection.getString("luckperms-group", key.toLowerCase(Locale.ROOT)),
                    rankSection.getString("label", key.toUpperCase(Locale.ROOT)),
                    rankSection.getString("icon", key.toLowerCase(Locale.ROOT)),
                    rankSection.getInt("priority", 0)
            ));
        }
        if (list.isEmpty()) {
            list.add(defaultRank());
        }
        list.sort(Comparator.comparingInt(RankDefinition::priority).reversed());
        return List.copyOf(list);
    }

    private Map<String, EmoteDefinition> loadEmotes(ConfigurationSection section) {
        if (section == null) {
            return Map.of();
        }
        Map<String, EmoteDefinition> map = new HashMap<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection emoteSection = section.getConfigurationSection(key);
            if (emoteSection == null) {
                continue;
            }
            String alias = emoteSection.getString("alias", ":" + key + ":");
            EmoteDefinition def = new EmoteDefinition(
                    key,
                    alias,
                    emoteSection.getString("file", key + ".png"),
                    emoteSection.getString("permission", "fre.emote." + key.toLowerCase(Locale.ROOT)),
                    emoteSection.getString("fallback", alias)
            );
            map.put(normalizeAlias(alias), def);
        }
        return Map.copyOf(map);
    }

    public String normalizeAlias(String alias) {
        return emoteCaseSensitive ? alias : alias.toLowerCase(Locale.ROOT);
    }

    private RankDefinition defaultRank() {
        return new RankDefinition("MEMBER", "fre.rank.member", "member", "MEMBER", "member", 0);
    }

    public RankDefinition getDefaultRank() {
        return ranks.stream().min(Comparator.comparingInt(RankDefinition::priority)).orElse(defaultRank());
    }

    public List<RankDefinition> getRanks() {
        return ranks;
    }

    public CountryInfo getUnknownCountryInfo() {
        return unknownCountryInfo;
    }

    public String getChatFormat() {
        return chatFormat;
    }

    public String getPlayerListFormat() {
        return playerListFormat;
    }

    public Map<String, EmoteDefinition> getEmotesByAlias() {
        return emotesByAlias;
    }

    public boolean isEmoteCaseSensitive() {
        return emoteCaseSensitive;
    }

    public String getEmoteEscapePrefix() {
        return emoteEscapePrefix;
    }

    public int getEmoteMaxReplacements() {
        return emoteMaxReplacements;
    }

    public int getEmoteMaxMessageLength() {
        return emoteMaxMessageLength;
    }

    public String getRankFallbackIcon() {
        return rankFallbackIcon;
    }

    public String getPackNamespace() {
        return packNamespace;
    }

    public int getPackFormat() {
        return packFormat;
    }

    public String getPackDescription() {
        return packDescription;
    }

    public String getPackZipName() {
        return packZipName;
    }

    public String getPackUrl() {
        return packUrl;
    }

    public String getPackSha1() {
        return packSha1;
    }

    public boolean isPackRequired() {
        return packRequired;
    }

    public String getPackPrompt() {
        return packPrompt;
    }

    public File getGeoIpDatabaseFile() {
        File absolute = new File(geoIpDatabasePath);
        return absolute.isAbsolute() ? absolute : new File(plugin.getDataFolder(), geoIpDatabasePath);
    }

    public long getGeoIpCacheMillis() {
        return geoIpCacheMillis;
    }

    public boolean isGeoIpDebugLogging() {
        return geoIpDebugLogging;
    }

    public File getRanksDirectory() {
        return new File(plugin.getDataFolder(), "ranks");
    }

    public File getFlagsDirectory() {
        return new File(plugin.getDataFolder(), "flags");
    }

    public File getEmotesDirectory() {
        return new File(plugin.getDataFolder(), "emotes");
    }

    public File getGeneratedDirectory() {
        return new File(plugin.getDataFolder(), "generated");
    }

    public File getPackZipFile() {
        return new File(getGeneratedDirectory(), packZipName);
    }

    public String requirePackUrl() {
        return Objects.requireNonNullElse(packUrl, "");
    }
}
