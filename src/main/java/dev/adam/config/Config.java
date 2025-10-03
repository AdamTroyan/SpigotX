package dev.adam.config;

import org.bukkit.plugin.Plugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;

public class Config {
    private final File file;
    private final FileConfiguration config;

    public Config(Plugin plugin, String fileName) {
        this.file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) plugin.saveResource(fileName, false);
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public String getString(String path, String def) {
        return config.getString(path, def);
    }

    public int getInt(String path, int def) {
        return config.getInt(path, def);
    }

    public void save() {
        try { config.save(file); } catch (Exception ignored) {}
    }
}