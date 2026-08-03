package plus.crates;

import com.google.common.io.ByteStreams;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import plus.crates.Commands.CrateCommand;
import plus.crates.Crates.Crate;
import plus.crates.Crates.KeyCrate;
import plus.crates.Handlers.*;
import plus.crates.Listeners.BlockListeners;
import plus.crates.Listeners.GUIListeners;
import plus.crates.Listeners.PlayerInteract;
import plus.crates.Listeners.PlayerJoin;
import plus.crates.Utils.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CratesPlus extends JavaPlugin implements Listener {
    private String pluginPrefix = "";
    private String updateMessage = "";
    private String configBackup = null;
    private boolean updateAvailable = false;
    private ConfigHandler configHandler;
    private CrateHandler crateHandler;
    private SettingsHandler settingsHandler;
    private HologramHandler hologramHandler;
    private StorageHandler storageHandler;
    private LegacyMigrationService legacyMigrationService;
    private CrateBlockStorage crateBlockStorage;
    private String bukkitVersion = "0.0";
    private Version_Util version_util;
    private TextInputHandler textInputHandler;
    private File cratesFile;
    private YamlConfiguration cratesConfig;
    private NamespacedKey keyCrateKey;
    private NamespacedKey crateBlockItemKey;
    private static OpenHandler openHandler;
    private ArrayList<UUID> creatingCrate = new ArrayList<>();

    public void onEnable() {
        Server server = getServer();
        Pattern pattern = Pattern.compile("(^[^\\-]*)");
        Matcher matcher = pattern.matcher(server.getBukkitVersion());
        if (!matcher.find()) {
            getLogger().severe("Could not find Bukkit version... Disabling plugin");
            setEnabled(false);
            return;
        }
        bukkitVersion = matcher.group(1);

        if (getConfig().isSet("Bukkit Version"))
            bukkitVersion = getConfig().getString("Bukkit Version");

        version_util = new Version_Util(this);
        textInputHandler = new TextInputHandler(this);
        keyCrateKey = new NamespacedKey(this, "crate_key");
        crateBlockItemKey = new NamespacedKey(this, "crate_block_item");

        final ConsoleCommandSender console = server.getConsoleSender();
        getConfig().options().copyDefaults(true);
        saveConfig();
        loadCratesConfig();

        hologramHandler = new HologramHandler();

        StorageHandler.StorageType storageType = StorageHandler.StorageType.FLAT;
        try {
            storageType = StorageHandler.StorageType.valueOf(getConfig().getString("Storage Type", "FLAT").toUpperCase());
        } catch (Exception e) {
            getLogger().warning(getConfig().getString("Storage Type", "FLAT") + " is not a valid storage type! Falling back to flat!");
        }
        storageHandler = new StorageHandler(this, storageType);

        // Load new messages.yml
        File messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            try {
                messagesFile.createNewFile();
                InputStream inputStream = getResource("messages.yml");
                OutputStream outputStream = new FileOutputStream(messagesFile);
                ByteStreams.copy(inputStream, outputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        YamlConfiguration messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        MessageHandler.loadMessageConfiguration(this, messagesConfig, messagesFile);

        configHandler = new ConfigHandler(getConfig(), getCratesConfig(), this);
        legacyMigrationService = new LegacyMigrationService(this);
        crateBlockStorage = new CrateBlockStorage(this);
        Bukkit.getPluginManager().registerEvents(crateBlockStorage, this);

        if (getConfig().getBoolean("Metrics")) {
            try {
                Metrics metrics = new Metrics(this);
                metrics.start();

            } catch (IOException e) {
                // Failed to submit the stats :-(
            }
        }

        // Load the crate handler
        crateHandler = new CrateHandler(this);

        // Do Prefix
        pluginPrefix = messagesConfig.getString("Prefix", "&7[&bCratesPlus&7]") + " ";

        // Register /crate command
        CrateCommand crateCommand = new CrateCommand(this);
        Bukkit.getPluginCommand("crate").setExecutor(crateCommand);
        Bukkit.getPluginCommand("crate").setTabCompleter(crateCommand);

        // Register Events
        Bukkit.getPluginManager().registerEvents(new BlockListeners(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerJoin(this), this);
        Bukkit.getPluginManager().registerEvents(new GUIListeners(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerInteract(this), this);

        openHandler = new OpenHandler(this);

        settingsHandler = new SettingsHandler(this);

        loadCrateLocations(null);

        MessageHandler.sendLegacy(console, "&b" + getPluginMeta().getName() + " Version " + getPluginMeta().getVersion());
        if (getPluginMeta().getVersion().contains("SNAPSHOT")) {
            MessageHandler.sendLegacy(console, "&cWarning: You are running a snapshot build of CratesPlus");
            MessageHandler.sendLegacy(console, "&cIt is advised that you do NOT run this on a production server!");
        }

        MessageHandler.sendLegacy(console, "&aUsing Paper TextDisplay holograms.");

        if (configBackup != null && Bukkit.getOnlinePlayers().size() > 0) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission("cratesplus.admin")) {
                    MessageHandler.sendLegacy(player, pluginPrefix + "&aYour config has been updated. Your old config was backed up to " + configBackup);
                    configBackup = null;
                }
            }
        }

        if (getConfig().getBoolean("Update Checks", true)) {
            getServer().getScheduler().runTaskLaterAsynchronously(this, () -> checkUpdate(console), 10L);
        }
    }

    public void onDisable() {
        if (hologramHandler != null) {
            hologramHandler.clear();
        }
        if (configHandler != null) {
            configHandler.getCrates().forEach((key, crate) -> crate.onDisable());
        }
        if (settingsHandler != null) {
            HandlerList.unregisterAll(settingsHandler);
        }
    }

    public String uploadConfig() {
        return uploadFile("config.yml");
    }

    public String uploadData() {
        return uploadFile("data.yml");
    }

    public String uploadMessages() {
        return uploadFile("messages.yml");
    }

    public String uploadFile(String fileName) {
        File file = new File(getDataFolder(), fileName);
        if (!file.exists())
            return null;
        LineIterator it;
        String lines = "";
        try {
            it = FileUtils.lineIterator(file, "UTF-8");
            try {
                while (it.hasNext()) {
                    String line = it.nextLine();
                    lines += line + "\n";
                }
            } finally {
                it.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return MCDebug.paste(fileName, lines);
    }

    private void checkUpdate(final ConsoleCommandSender console) {
        String updateBranch = getConfig().getString("Update Branch");

        if (getDescription().getVersion().contains("SNAPSHOT"))
            updateBranch = "snapshot";//Force snapshot branch on snapshot builds

        String branch = updateBranch.toLowerCase();

        if (branch.equalsIgnoreCase("snapshot")) {
            MessageHandler.sendLegacy(console, "&cWARNING: Snapshot updates are not recommended on production servers");
        }
        MessageHandler.sendLegacy(console, "&aChecking for updates via " + branch + " branch...");
        final LinfootUpdater updater = new LinfootUpdater(this, branch);
        final LinfootUpdater.UpdateResult snapShotResult = updater.getResult();
        switch (snapShotResult) {
            default:
            case FAILED:
                updateAvailable = false;
                updateMessage = pluginPrefix + "Failed to check for updates. Will try again later.";
                getServer().getScheduler().runTaskLaterAsynchronously(this, () -> checkUpdate(console), 60 * (60 * 20L)); // Checks again an hour later
                break;
            case NO_UPDATE:
                updateAvailable = false;
                updateMessage = pluginPrefix + "No update was found, you are running the latest version. Will check again later.";
                getServer().getScheduler().runTaskLaterAsynchronously(this, () -> checkUpdate(console), 60 * (60 * 20L)); // Checks again an hour later
                break;
            case SNAPSHOT_UPDATE_AVAILABLE:
                updateAvailable = true;
                updateMessage = pluginPrefix + "A snapshot update for CratesPlus is available, new version is " + updater.getVersion() + ". Your installed version is " + getDescription().getVersion() + ".\nPlease update to the latest version :)";
                break;
            case UPDATE_AVAILABLE:
                updateAvailable = true;
                updateMessage = pluginPrefix + "An update for CratesPlus is available, new version is " + updater.getVersion() + ". Your installed version is " + getDescription().getVersion() + ".\nPlease update to the latest version :)";
                break;
        }

        if (updateMessage != null)
            MessageHandler.sendLegacy(console, updateMessage);

    }

    public void reloadPlugin() {
        if (configHandler != null) {
            configHandler.getCrates().forEach((key, crate) -> crate.onDisable());
        }
        if (settingsHandler != null) {
            HandlerList.unregisterAll(settingsHandler);
        }
        reloadConfig();

        // Do Prefix
        pluginPrefix = getConfig().getString("Prefix", "&7[&bCratesPlus&7]") + " ";

        // Reload Configuration
        loadCratesConfig();
        configHandler = new ConfigHandler(getConfig(), getCratesConfig(), this);

        // Settings Handler
        settingsHandler = new SettingsHandler(this);
        loadCrateLocations(null);
    }

    /** Restores PDC markers from legacy data.yml and recreates holograms for loaded worlds. */
    public void loadCrateLocations(World onlyWorld) {
        if (!getStorageHandler().getFlatConfig().isSet("Crate Locations"))
            return;
        for (String name : getStorageHandler().getFlatConfig().getConfigurationSection("Crate Locations").getKeys(false)) {
            final Crate crate = configHandler.getCrate(name.toLowerCase());
            if (crate == null)
                continue;
            if (!(crate instanceof KeyCrate))
                continue;
            KeyCrate keyCrate = (KeyCrate) crate;
            String path = "Crate Locations." + name;
            List<String> locations = getStorageHandler().getFlatConfig().getStringList(path);

            for (String serializedLocation : locations) {
                String[] parts = serializedLocation.split("\\|");
                if (parts.length != 4) {
                    getLogger().warning("Ignoring malformed crate location in data.yml: " + serializedLocation);
                    continue;
                }
                World world = Bukkit.getWorld(parts[0]);
                if (world == null || (onlyWorld != null && !world.equals(onlyWorld))) {
                    continue;
                }
                try {
                    Location locationObj = new Location(world, Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
                    Block block = locationObj.getBlock();
                    if (block.getType().isAir()) {
                        getLogger().warning("No block found at " + serializedLocation + "; keeping the data.yml entry for recovery.");
                        continue;
                    }
                    crateBlockStorage.set(block, crate.getName(false));
                    keyCrate.loadHolograms(block.getLocation().add(0.5, 0.5, 0.5));
                } catch (NumberFormatException exception) {
                    getLogger().warning("Ignoring malformed crate coordinates in data.yml: " + serializedLocation);
                }
            }


        }
    }

    public SettingsHandler getSettingsHandler() {
        return settingsHandler;
    }

    public String getPluginPrefix() {
        return pluginPrefix;
    }

    public ConfigHandler getConfigHandler() {
        return configHandler;
    }

    public HologramHandler getHologramHandler() {
        return hologramHandler;
    }

    public StorageHandler getStorageHandler() {
        return storageHandler;
    }

    /** Crate definitions are intentionally separate from general plugin settings. */
    public FileConfiguration getCratesConfig() {
        return cratesConfig;
    }

    public void saveCratesConfig() {
        try {
            cratesConfig.save(cratesFile);
        } catch (IOException exception) {
            getLogger().severe("Could not save crates.yml: " + exception.getMessage());
        }
    }

    private void loadCratesConfig() {
        cratesFile = new File(getDataFolder(), "crates.yml");
        cratesConfig = YamlConfiguration.loadConfiguration(cratesFile);
        if (!cratesFile.exists()) {
            if (getConfig().isConfigurationSection("Crates")) {
                copySection(getConfig().getConfigurationSection("Crates"), cratesConfig.createSection("Crates"));
                saveCratesConfig();
                getConfig().set("Crates", null);
                saveConfig();
                getLogger().info("Migrated crate definitions from config.yml to crates.yml.");
            } else {
                cratesConfig.createSection("Crates");
                saveCratesConfig();
            }
        }
        if (!cratesConfig.isConfigurationSection("Crates")) {
            cratesConfig.createSection("Crates");
            saveCratesConfig();
        }
    }

    private void copySection(ConfigurationSection source, ConfigurationSection target) {
        for (String key : source.getKeys(false)) {
            Object value = source.get(key);
            if (value instanceof ConfigurationSection child) {
                copySection(child, target.createSection(key));
            } else {
                target.set(key, value);
            }
        }
    }

    public LegacyMigrationService getLegacyMigrationService() {
        return legacyMigrationService;
    }

    public CrateBlockStorage getCrateBlockStorage() {
        return crateBlockStorage;
    }

    public String getUpdateMessage() {
        return updateMessage;
    }

    public String getConfigBackup() {
        return configBackup;
    }

    public void setConfigBackup(String configBackup) {
        this.configBackup = configBackup;
    }

    public Version_Util getVersion_util() {
        return version_util;
    }

    public TextInputHandler getTextInputHandler() {
        return textInputHandler;
    }

    public NamespacedKey getKeyCrateKey() {
        return keyCrateKey;
    }

    public ItemStack tagCrateItem(ItemStack item, Crate crate) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(crateBlockItemKey, PersistentDataType.STRING, crate.getSlug());
            item.setItemMeta(meta);
        }
        return item;
    }

    public String getCrateType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(crateBlockItemKey, PersistentDataType.STRING);
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public CrateHandler getCrateHandler() {
        return crateHandler;
    }

    public static OpenHandler getOpenHandler() {
        return openHandler;
    }

    public String getBukkitVersion() {
        return bukkitVersion;
    }

    public boolean isCreating(UUID uuid) {
        return creatingCrate.contains(uuid);
    }

    public void addCreating(UUID uuid) {
        creatingCrate.add(uuid);
    }

    public void removeCreating(UUID uuid) {
        creatingCrate.remove(uuid);
    }

}
