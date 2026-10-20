package de.cuteghg.fre.emote;

import de.cuteghg.fre.FlagRanksEmotesPlugin;
import de.cuteghg.fre.config.ConfigManager;
import de.cuteghg.fre.format.PlayerFormatService;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class ChatListener implements Listener {
    private final FlagRanksEmotesPlugin plugin;
    private final EmoteParser emoteParser;
    private final PlayerFormatService formatService;

    public ChatListener(FlagRanksEmotesPlugin plugin, ConfigManager configManager, PlayerFormatService formatService) {
        this.plugin = plugin;
        this.emoteParser = new EmoteParser(configManager);
        this.formatService = formatService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String plain = PlainTextComponentSerializer.plainText().serialize(event.message());
        Component emoteMessage = emoteParser.parse(player, plain, formatService.emoteGlyphs());

        event.renderer((source, sourceDisplayName, message, viewer) -> Component.text("..."));
        formatService.formatChat(player, emoteMessage).thenAccept(formatted ->
                Bukkit.getScheduler().runTask(plugin, () -> event.renderer((source, sourceDisplayName, message, viewer) -> formatted))
        );
    }
}
