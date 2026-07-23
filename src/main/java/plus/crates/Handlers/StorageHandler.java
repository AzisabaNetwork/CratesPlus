package plus.crates.Handlers;

import org.bukkit.configuration.file.YamlConfiguration;
import plus.crates.CratesPlus;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/**
 * // TODO - Perhaps rewrite to support other plugins being able to extend and create custom storage handlers?
 */
public class StorageHandler {
    private CratesPlus cratesPlus;
    private StorageType storageType;
    private File flatFile;
    private YamlConfiguration flatConfig;

    public enum StorageType {
        FLAT, SQLITE, MYSQL
    }

    public StorageHandler(CratesPlus cratesPlus, StorageType storageType) {
        this.cratesPlus = cratesPlus;
        this.storageType = storageType;
        setupStorage();
    }

    private void setupStorage() {
        // Configure the flat file no matter what, we'll still use this for "Crate Locations" and maybe other data that is per instance
        flatFile = new File(cratesPlus.getDataFolder(), "data.yml");
        flatConfig = YamlConfiguration.loadConfiguration(flatFile);
        try {
            flatConfig.save(flatFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        updateDataFile();

        switch (getStorageType()) {
            case SQLITE:
                break;
            case MYSQL:
                break;
        }
    }

    private void updateDataFile() {
        if (!flatConfig.isSet("Data Version") || flatConfig.getInt("Data Version") == 1) {
            flatConfig.set("Data Version", 2);
            if (flatConfig.isSet("Crate Locations"))
                flatConfig.set("Crate Locations", null);
            try {
                flatConfig.save(flatFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public StorageType getStorageType() {
        return storageType;
    }

    public File getFlatFile() {
        return flatFile;
    }

    public YamlConfiguration getFlatConfig() {
        return flatConfig;
    }

    public void saveFlat() {
        try {
            flatConfig.save(flatFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Object getPlayerData(UUID uuid, String key) {
        switch (getStorageType()) {
            case FLAT:
                return flatConfig.get("Player." + uuid.toString() + "." + key, null);
        }
        return null;
    }

    public void setPlayerData(UUID uuid, String key, Object value) {
        if (getStorageType() != StorageType.FLAT) {
            return;
        }
        flatConfig.set("Player." + uuid + "." + key, value);
        saveFlat();
    }

    public void incPlayerData(UUID uuid, String key, Integer value) {
        switch (getStorageType()) {
            case FLAT:
                Integer current = flatConfig.getInt("Player." + uuid.toString() + "." + key, 0);
                if (value > 0) {
                    current += value;
                } else if (value < 0) {
                    current -= value;
                }
                flatConfig.set("Player." + uuid.toString() + "." + key, current);
                saveFlat();
                break;
        }
    }

    /** Stores per-player, per-crate pity state in data.yml without a schema migration. */
    public int getPityCount(UUID uuid, String crateName) {
        return Math.max(0, flatConfig.getInt(pityPath(uuid, crateName), 0));
    }

    public void setPityCount(UUID uuid, String crateName, int count) {
        // data.yml is initialized for every storage mode and is already used for
        // per-instance state. This avoids silently disabling pity on an existing
        // server configured with one of the not-yet-implemented SQL backends.
        flatConfig.set(pityPath(uuid, crateName), Math.max(0, count));
        saveFlat();
    }

    private String pityPath(UUID uuid, String crateName) {
        String encodedName = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(crateName.toLowerCase().getBytes(StandardCharsets.UTF_8));
        return "Player." + uuid + ".Pity." + encodedName;
    }

}
