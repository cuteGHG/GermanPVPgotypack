package de.cuteghg.fre.country;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CountryResponse;
import de.cuteghg.fre.FlagRanksEmotesPlugin;
import de.cuteghg.fre.config.ConfigManager;
import de.cuteghg.fre.util.FlagUtil;
import org.bukkit.entity.Player;

import java.io.File;
import java.net.InetAddress;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public final class GeoIpCountryProvider implements CountryProvider {
    private final FlagRanksEmotesPlugin plugin;
    private final ConfigManager configManager;
    private final Executor executor;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public GeoIpCountryProvider(FlagRanksEmotesPlugin plugin, ConfigManager configManager, Executor executor) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.executor = executor;
    }

    @Override
    public CompletableFuture<CountryInfo> lookup(Player player) {
        InetAddress address = player.getAddress() != null ? player.getAddress().getAddress() : null;
        if (address == null) {
            return CompletableFuture.completedFuture(configManager.getUnknownCountryInfo());
        }

        String key = player.getUniqueId() + "|" + address.getHostAddress();
        long now = System.currentTimeMillis();
        CacheEntry cached = cache.get(key);
        if (cached != null && cached.expiresAt > now) {
            return CompletableFuture.completedFuture(cached.countryInfo);
        }

        return CompletableFuture.supplyAsync(() -> lookupInternal(player.getUniqueId(), address), executor)
                .exceptionally(ex -> configManager.getUnknownCountryInfo())
                .thenApply(info -> {
                    cache.put(key, new CacheEntry(info, now + configManager.getGeoIpCacheMillis()));
                    return info;
                });
    }

    private CountryInfo lookupInternal(UUID uuid, InetAddress address) {
        File database = configManager.getGeoIpDatabaseFile();
        if (!database.exists()) {
            return configManager.getUnknownCountryInfo();
        }

        try (DatabaseReader reader = new DatabaseReader.Builder(database).build()) {
            CountryResponse response = reader.country(address);
            String code = Objects.requireNonNullElse(response.getCountry().getIsoCode(), configManager.getUnknownCountryInfo().countryCode())
                    .toUpperCase(Locale.ROOT);
            String name = Objects.requireNonNullElse(response.getCountry().getName(), configManager.getUnknownCountryInfo().countryName());
            String flag = FlagUtil.countryCodeToFlag(code);
            if (flag.isEmpty()) {
                flag = configManager.getUnknownCountryInfo().unicodeFlag();
            }
            return new CountryInfo(code, name, flag, true);
        } catch (Exception e) {
            if (configManager.isGeoIpDebugLogging()) {
                plugin.getLogger().warning("GeoIP lookup failed for player " + uuid + ": " + e.getMessage());
            }
            return configManager.getUnknownCountryInfo();
        }
    }

    @Override
    public void shutdown() {
        cache.clear();
    }

    private record CacheEntry(CountryInfo countryInfo, long expiresAt) {
    }
}
