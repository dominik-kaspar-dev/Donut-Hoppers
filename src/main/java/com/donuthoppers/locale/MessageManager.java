package com.donuthoppers.locale;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class MessageManager {

    private final JavaPlugin plugin;
    private FileConfiguration messages;

    public MessageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.saveResource("messages.yml", false);
        File file = new File(plugin.getDataFolder(), "messages.yml");
        this.messages = YamlConfiguration.loadConfiguration(file);
    }

    public void send(CommandSender recipient, String path, Object... replacements) {
        String message = get(path, replacements);
        if (message != null && !message.isEmpty()) {
            recipient.sendMessage(message);
        }
    }

    public String get(String path, Object... replacements) {
        String raw = messages.getString(path);
        if (raw == null) {
            return "";
        }

        String formatted = ChatColor.translateAlternateColorCodes('&', raw);
        if (replacements.length % 2 == 0) {
            Map<String, String> variables = new HashMap<>();
            for (int i = 0; i < replacements.length; i += 2) {
                Object key = replacements[i];
                Object value = replacements[i + 1];
                if (key != null && value != null) {
                    variables.put(key.toString(), value.toString());
                }
            }
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                formatted = formatted.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return formatted;
    }

    public List<String> getLines(String path, Object... replacements) {
        List<String> rawLines = messages.getStringList(path);
        if (rawLines == null || rawLines.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> output = new ArrayList<>(rawLines.size());
        for (String raw : rawLines) {
            output.add(getRaw(raw, replacements));
        }
        return output;
    }

    private String getRaw(String raw, Object... replacements) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        String formatted = ChatColor.translateAlternateColorCodes('&', raw);
        if (replacements.length % 2 == 0) {
            Map<String, String> variables = new HashMap<>();
            for (int i = 0; i < replacements.length; i += 2) {
                Object key = replacements[i];
                Object value = replacements[i + 1];
                if (key != null && value != null) {
                    variables.put(key.toString(), value.toString());
                }
            }
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                formatted = formatted.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return formatted;
    }
}
