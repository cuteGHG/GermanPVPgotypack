package de.cuteghg.fre.command;

import de.cuteghg.fre.FlagRanksEmotesPlugin;
import de.cuteghg.fre.config.ConfigManager;
import de.cuteghg.fre.format.PlayerFormatService;
import de.cuteghg.fre.pack.ResourcePackService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class FreCommand implements CommandExecutor, TabCompleter {
    private final FlagRanksEmotesPlugin plugin;
    private final ConfigManager configManager;
    private final ResourcePackService resourcePackService;
    private final PlayerFormatService formatService;

    public FreCommand(FlagRanksEmotesPlugin plugin, ConfigManager configManager, ResourcePackService resourcePackService,
                      PlayerFormatService formatService) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.resourcePackService = resourcePackService;
        this.formatService = formatService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("fre.reload")) {
                sender.sendMessage("§cNo permission: fre.reload");
                return true;
            }
            configManager.reload();
            formatService.applyAllOnlinePlayers();
            sender.sendMessage("§aFlagRanksEmotes reloaded.");
            return true;
        }

        if (args[0].equalsIgnoreCase("pack")) {
            return handlePack(sender, args);
        }

        sendUsage(sender);
        return true;
    }

    private boolean handlePack(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§eUsage: /fre pack <build|send>");
            return true;
        }

        if (args[1].equalsIgnoreCase("build")) {
            if (!sender.hasPermission("fre.pack.build")) {
                sender.sendMessage("§cNo permission: fre.pack.build");
                return true;
            }
            sender.sendMessage("§7Building resource pack asynchronously...");
            resourcePackService.buildPack().whenComplete((result, throwable) -> Bukkit.getScheduler().runTask(plugin, () -> {
                if (throwable != null) {
                    sender.sendMessage("§cPack build failed: " + throwable.getMessage());
                    return;
                }
                sender.sendMessage("§aPack built: " + result.zipFile().getAbsolutePath());
                sender.sendMessage("§aSHA-1: " + result.sha1());
            }));
            return true;
        }

        if (args[1].equalsIgnoreCase("send")) {
            if (!sender.hasPermission("fre.pack.send")) {
                sender.sendMessage("§cNo permission: fre.pack.send");
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage("§eUsage: /fre pack send <player>");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found.");
                return true;
            }
            resourcePackService.sendConfiguredPack(target);
            sender.sendMessage("§aPack request sent to " + target.getName());
            return true;
        }

        sender.sendMessage("§eUsage: /fre pack <build|send>");
        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("§6FlagRanksEmotes commands:");
        sender.sendMessage("§e/fre reload §7- Reload config");
        sender.sendMessage("§e/fre pack build §7- Build resource pack");
        sender.sendMessage("§e/fre pack send <player> §7- Send configured pack");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            out.add("reload");
            out.add("pack");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("pack")) {
            out.add("build");
            out.add("send");
        } else if (args.length == 3 && args[0].equalsIgnoreCase("pack") && args[1].equalsIgnoreCase("send")) {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                out.add(onlinePlayer.getName());
            }
        }
        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        return out.stream().filter(s -> s.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }
}
