package de.cuteghg.fre.country;

import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

public interface CountryProvider {
    CompletableFuture<CountryInfo> lookup(Player player);

    void shutdown();
}
