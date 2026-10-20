package de.cuteghg.fre;

import de.cuteghg.fre.command.FreCommand;
import de.cuteghg.fre.config.ConfigManager;
import de.cuteghg.fre.country.CountryProvider;
import de.cuteghg.fre.country.GeoIpCountryProvider;
import de.cuteghg.fre.emote.ChatListener;
import de.cuteghg.fre.format.PlayerFormatService;
import de.cuteghg.fre.pack.ResourcePackService;
import de.cuteghg.fre.placeholder.FrePlaceholderExpansion;
import de.cuteghg.fre.rank.RankService;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class FlagRanksEmotesPlugin extends JavaPlugin {
    private ExecutorService ioExecutor;
    private ConfigManager configManager;
    private RankService rankService;
    private CountryProvider countryProvider;
    private ResourcePackService resourcePackService;
    private PlayerFormatService playerFormatService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.ioExecutor = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "fre-io");
            t.setDaemon(true);
            return t;
        });

        this.configManager = new ConfigManager(this);
        this.rankService = new RankService(this, configManager);
        this.countryProvider = new GeoIpCountryProvider(this, configManager, ioExecutor);
        this.resourcePackService = new ResourcePackService(this, configManager, ioExecutor);
        this.playerFormatService = new PlayerFormatService(this, configManager, rankService, countryProvider, resourcePackService);

        PluginCommand command = Objects.requireNonNull(getCommand("fre"), "fre command missing");
        FreCommand freCommand = new FreCommand(this, configManager, resourcePackService, playerFormatService);
        command.setExecutor(freCommand);
        command.setTabCompleter(freCommand);

        Bukkit.getPluginManager().registerEvents(new ChatListener(this, configManager, playerFormatService), this);
        Bukkit.getPluginManager().registerEvents(playerFormatService, this);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new FrePlaceholderExpansion(this, rankService, countryProvider, playerFormatService).register();
            getLogger().info("Registered PlaceholderAPI expansion: %fre_*%");
        } else {
            getLogger().warning("PlaceholderAPI not found. %fre_*% placeholders are disabled.");
        }

        playerFormatService.applyAllOnlinePlayers();
    }

    @Override
    public void onDisable() {
        if (countryProvider != null) {
            countryProvider.shutdown();
        }
        if (ioExecutor != null) {
            ioExecutor.shutdownNow();
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}
