package plus.crates.Handlers;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import plus.crates.Crates.Crate;
import plus.crates.Crates.KeyCrate;
import plus.crates.CratesPlus;

import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Explicit, report-first migration for pre-1.21 CratesPlus data. */
public final class LegacyMigrationService {
    private static final Map<String, String> ENCHANTMENT_ALIASES = Map.of(
            "DAMAGE_ALL", "SHARPNESS",
            "DURABILITY", "UNBREAKING",
            "DIG_SPEED", "EFFICIENCY",
            "PROTECTION_ENVIRONMENTAL", "PROTECTION",
            "ARROW_DAMAGE", "POWER",
            "ARROW_KNOCKBACK", "PUNCH",
            "ARROW_INFINITE", "INFINITY"
    );

    private final CratesPlus plugin;

    public LegacyMigrationService(CratesPlus plugin) {
        this.plugin = plugin;
    }

    public Report inspect(boolean apply) {
        FileConfiguration config = plugin.getConfig();
        Report report = new Report();
        if (apply) {
            report.backupFile = backupConfig();
        }

        if (config.isConfigurationSection("Crates")) {
            for (String crate : config.getConfigurationSection("Crates").getKeys(false)) {
                String winningsPath = "Crates." + crate + ".Winnings";
                if (!config.isConfigurationSection(winningsPath)) continue;
                for (String winning : config.getConfigurationSection(winningsPath).getKeys(false)) {
                    String path = winningsPath + "." + winning;
                    migrateItemData(config, path, report, apply);
                    migrateEnchantments(config, path, report, apply);
                }
            }
        }
        inspectLocations(report);
        if (apply && report.changed > 0) {
            plugin.saveConfig();
        }
        return report;
    }

    private String backupConfig() {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            java.io.File source = new java.io.File(plugin.getDataFolder(), "config.yml");
            java.io.File backup = new java.io.File(plugin.getDataFolder(), "config.yml.legacy-migration-" + timestamp + ".bak");
            Files.copy(source.toPath(), backup.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
            return backup.getName();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not back up config.yml; migration was not started.", exception);
        }
    }

    private void migrateItemData(FileConfiguration config, String path, Report report, boolean apply) {
        String dataPath = path + ".Item Data";
        if (!config.isSet(dataPath)) return;
        int data = config.getInt(dataPath);
        String type = config.getString(path + ".Item Type", "").toUpperCase(Locale.ROOT);
        if (data == 0 || "PLAYER_HEAD".equals(type)) {
            report.itemDataRemoved++;
            if (apply) {
                config.set(dataPath, null);
                report.changed++;
            }
        } else {
            report.warnings.add(path + " has non-zero Item Data " + data + " for " + type + "; convert it manually.");
        }
    }

    private void migrateEnchantments(FileConfiguration config, String path, Report report, boolean apply) {
        String enchantmentPath = path + ".Enchantments";
        List<String> original = config.getStringList(enchantmentPath);
        if (original.isEmpty()) return;
        List<String> migrated = new ArrayList<>();
        boolean changed = false;
        for (String entry : original) {
            String[] split = entry.split("-", 2);
            String replacement = ENCHANTMENT_ALIASES.getOrDefault(split[0].toUpperCase(Locale.ROOT), split[0].toUpperCase(Locale.ROOT));
            String converted = split.length == 2 ? replacement + "-" + split[1] : replacement;
            migrated.add(converted);
            if (!entry.equals(converted)) {
                report.enchantmentsNormalized++;
                changed = true;
            }
        }
        if (apply && changed) {
            config.set(enchantmentPath, migrated);
            report.changed++;
        }
    }

    private void inspectLocations(Report report) {
        FileConfiguration data = plugin.getStorageHandler().getFlatConfig();
        if (!data.isConfigurationSection("Crate Locations")) return;
        for (String crateName : data.getConfigurationSection("Crate Locations").getKeys(false)) {
            Crate crate = plugin.getConfigHandler().getCrate(crateName.toLowerCase(Locale.ROOT));
            if (!(crate instanceof KeyCrate)) {
                report.warnings.add("data.yml: crate location group '" + crateName + "' has no configured KeyCrate.");
                continue;
            }
            for (String location : data.getStringList("Crate Locations." + crateName)) {
                String[] parts = location.split("\\|", -1);
                if (parts.length != 4) {
                    report.warnings.add("data.yml: invalid location '" + location + "'.");
                } else {
                    World world = Bukkit.getWorld(parts[0]);
                    if (world == null) report.warnings.add("data.yml: world '" + parts[0] + "' is not loaded for '" + location + "'.");
                }
            }
        }
    }

    public int migrateLegacyKeys(Player player) {
        int migrated = 0;
        ItemStack[] contents = player.getInventory().getContents();
        for (ItemStack item : contents) {
            if (item == null) continue;
            for (Crate crate : plugin.getConfigHandler().getCrates().values()) {
                if (crate instanceof KeyCrate keyCrate && keyCrate.getKey() != null && keyCrate.getKey().migrateLegacy(item)) {
                    migrated++;
                    break;
                }
            }
        }
        player.getInventory().setContents(contents);
        return migrated;
    }

    public static final class Report {
        private int changed;
        private int itemDataRemoved;
        private int enchantmentsNormalized;
        private String backupFile;
        private final List<String> warnings = new ArrayList<>();

        public String summary(boolean applied) {
            return (applied ? "Applied" : "Preview") + ": Item Data removals=" + itemDataRemoved
                    + ", enchantment normalizations=" + enchantmentsNormalized + ", changed paths=" + changed
                    + ", warnings=" + warnings.size()
                    + (backupFile == null ? "" : ", backup=" + backupFile);
        }

        public List<String> warnings() {
            return warnings;
        }
    }
}
