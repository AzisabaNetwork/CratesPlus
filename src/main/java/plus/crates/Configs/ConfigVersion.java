package plus.crates.Configs;

import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import plus.crates.CratesPlus;
import plus.crates.Handlers.MessageHandler;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public abstract class ConfigVersion {
    private final CratesPlus cratesPlus;
    private final Integer version;

    public ConfigVersion(CratesPlus cratesPlus, Integer version) {
        this.cratesPlus = cratesPlus;
        this.version = version;
    }

    public CratesPlus getCratesPlus() {
        return cratesPlus;
    }

    public Integer getVersion() {
        return version;
    }

    public boolean shouldUpdate() {
        return shouldUpdate(false);
    }

    public boolean shouldUpdate(boolean actuallyUpdate) {
        if (!getConfig().isSet("Config Version") || getConfig().getInt("Config Version") >= getVersion()) {
            return false;
        }

        if (actuallyUpdate) {
            runUpdate();
        }
        return true;
    }

    private String backupConfig() {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            File source = new File(getCratesPlus().getDataFolder(), "config.yml");
            File backup = new File(getCratesPlus().getDataFolder(), "config.yml.v"
                    + getVersion() + "-migration-" + timestamp + ".bak");
            Files.copy(source.toPath(), backup.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
            return backup.getName();
        } catch (Exception exception) {
            getCratesPlus().getLogger().warning("Could not back up config.yml before migration: " + exception.getMessage());
            return null;
        }
    }

    protected void runUpdate() {
        String configBackup = backupConfig();
        ConsoleCommandSender console = Bukkit.getConsoleSender();

        MessageHandler.sendLegacy(console, getCratesPlus().getPluginPrefix() + "&aConverting config to version " + getVersion() + "...");

        update();
        getConfig().set("Config Version", getVersion());
        save();

        MessageHandler.sendLegacy(console, getCratesPlus().getPluginPrefix() + "&aConversion of config has completed.");
        if (configBackup != null && !configBackup.equalsIgnoreCase("")) {
            getCratesPlus().setConfigBackup(configBackup);
            MessageHandler.sendLegacy(console, getCratesPlus().getPluginPrefix() + "&aYour old config was backed up to " + configBackup);
        }
    }

    protected abstract void update();

    public void directMap(String from, String to) {
        directMap(from, to, true);
    }

    public void directMap(String from, String to, boolean delete) {
        if (getConfig().isSet(from)) {
            getConfig().set(to, getConfig().get(from));
            if (delete)
                delete(from);
        }
    }

    public void delete(String path) {
        if (getConfig().isSet(path))
            getConfig().set(path, null);
    }

    public void save() {
        cratesPlus.saveConfig();
    }

    protected FileConfiguration getConfig() {
        return getCratesPlus().getConfig();
    }
}
