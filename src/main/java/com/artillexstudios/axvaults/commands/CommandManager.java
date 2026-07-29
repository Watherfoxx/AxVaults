package com.artillexstudios.axvaults.commands;

import com.artillexstudios.axvaults.AxVaults;
import com.artillexstudios.axvaults.utils.CommandMessages;
import com.artillexstudios.axvaults.vaults.VaultManager;
import com.artillexstudios.axvaults.vaults.VaultPlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import revxrsal.commands.bukkit.BukkitCommandHandler;
import revxrsal.commands.orphan.Orphans;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.artillexstudios.axvaults.AxVaults.CONFIG;

public class CommandManager {
    private static BukkitCommandHandler handler = null;

    public static void load() {
        handler = BukkitCommandHandler.create(AxVaults.getInstance());

        handler.getAutoCompleter().registerSuggestion("vaults", (args, sender, command) -> {
            final Player player = Bukkit.getPlayer(sender.getUniqueId());
            if (!player.hasPermission("axvaults.openremote")) return new ArrayList<>();

            ArrayList<String> numbers = new ArrayList<>();
            VaultPlayer vaultPlayer = VaultManager.getPlayerOrNull(player);
            if (vaultPlayer == null) return numbers;
            for (Integer i : vaultPlayer.getVaultMap().keySet()) {
                numbers.add(String.valueOf(i));
            }
            return numbers;
        });

        handler.getTranslator().add(new CommandMessages());
        handler.setLocale(Locale.of("en", "US"));

        reload();
    }

    public static void reload() {
        handler.unregisterAllCommands();
        handler.register(Orphans.path(CONFIG.getStringList("player-command-aliases").toArray(String[]::new)).handler(new PlayerCommand()));
        handler.register(Orphans.path(CONFIG.getStringList("admin-command-aliases").toArray(String[]::new)).handler(new AdminCommand()));
        handler.registerBrigadier();
        registerBukkitTabCompleters();
    }

    private static void registerBukkitTabCompleters() {
        for (String alias : CONFIG.getStringList("player-command-aliases")) {
            final PluginCommand command = Bukkit.getPluginCommand(alias);
            if (command != null) command.setTabCompleter(CommandManager::completePlayerCommand);
        }

        for (String alias : CONFIG.getStringList("admin-command-aliases")) {
            final PluginCommand command = Bukkit.getPluginCommand(alias);
            if (command != null) command.setTabCompleter(CommandManager::completeAdminCommand);
        }
    }

    private static List<String> completePlayerCommand(CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
        if (!(sender instanceof Player player) || args.length != 1 || !player.hasPermission("axvaults.openremote"))
            return List.of();

        final VaultPlayer vaultPlayer = VaultManager.getPlayerOrNull(player);
        if (vaultPlayer == null) return List.of();

        return vaultPlayer.getVaultMap().keySet().stream()
                .sorted()
                .map(String::valueOf)
                .filter(value -> value.startsWith(args[0]))
                .toList();
    }

    private static List<String> completeAdminCommand(CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
        if (args.length == 2 && args[0].equalsIgnoreCase("converter") && sender.hasPermission("axvaults.admin.converter")) {
            return filterSuggestions(List.of("PlayerVaultsX", "EnderVaults"), args[1]);
        }
        if (args.length != 1) return List.of();

        final ArrayList<String> suggestions = new ArrayList<>();
        addIfPermitted(suggestions, sender, "help", "axvaults.admin");
        addIfPermitted(suggestions, sender, "reload", "axvaults.admin.reload");
        addIfPermitted(suggestions, sender, "forceopen", "axvaults.admin.forceopen");
        addIfPermitted(suggestions, sender, "view", "axvaults.admin.view");
        addIfPermitted(suggestions, sender, "delete", "axvaults.admin.delete");
        addIfPermitted(suggestions, sender, "set", "axvaults.admin.set");
        addIfPermitted(suggestions, sender, "stats", "axvaults.admin.stats");
        addIfPermitted(suggestions, sender, "converter", "axvaults.admin.converter");
        addIfPermitted(suggestions, sender, "replaceitems", "axvaults.admin.replaceitems");
        addIfPermitted(suggestions, sender, "save", "axvaults.admin.save");
        addIfPermitted(suggestions, sender, "debug", "axvaults.admin.debug");
        return filterSuggestions(suggestions, args[0]);
    }

    private static void addIfPermitted(List<String> suggestions, CommandSender sender, String suggestion, String permission) {
        if (sender.hasPermission(permission)) suggestions.add(suggestion);
    }

    private static List<String> filterSuggestions(List<String> suggestions, String input) {
        final String prefix = input.toLowerCase(Locale.ROOT);
        return suggestions.stream()
                .filter(suggestion -> suggestion.toLowerCase(Locale.ROOT).startsWith(prefix))
                .toList();
    }
}
