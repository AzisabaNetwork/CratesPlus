package plus.crates.Handlers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import plus.crates.Crates.Crate;
import plus.crates.Crates.Winning;
import plus.crates.CratesPlus;
import plus.crates.Utils.ComponentUtil;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Localised player-facing messages, with legacy ampersand colour-code support. */
public final class MessageHandler {
    private static final String DEFAULT_LOCALE = "en_US";
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer SECTION_LEGACY = LegacyComponentSerializer.legacySection();
    private static final Map<String, YamlConfiguration> locales = new HashMap<>();
    private static CratesPlus cratesPlus;
    public static boolean testMessages = false;

    private MessageHandler() {
    }

    public static void loadMessageConfiguration(CratesPlus plugin, YamlConfiguration ignored, File ignoredFile) {
        cratesPlus = plugin;
        locales.clear();
        loadLocale(DEFAULT_LOCALE);
        loadLocale("ja_JP");
        migrateLegacyMessages(ignored);
    }

    private static void loadLocale(String locale) {
        File directory = new File(cratesPlus.getDataFolder(), "messages");
        if (!directory.exists() && !directory.mkdirs()) {
            cratesPlus.getLogger().warning("Could not create messages directory");
            return;
        }
        File file = new File(directory, locale + ".yml");
        if (!file.exists()) {
            cratesPlus.saveResource("messages/" + locale + ".yml", false);
        }
        locales.put(locale, YamlConfiguration.loadConfiguration(file));
    }

    private static void migrateLegacyMessages(YamlConfiguration legacy) {
        if (legacy == null) {
            return;
        }
        Map<String, String> legacyKeys = Map.ofEntries(
                Map.entry("Command No Permission", "command.no_permission"),
                Map.entry("Crate No Permission", "crate.no_permission"),
                Map.entry("Crate Open Without Key", "crate.open_without_key"),
                Map.entry("Key Given", "key.given"),
                Map.entry("Broadcast", "crate.broadcast"),
                Map.entry("Cant Place", "key.cannot_place"),
                Map.entry("Cant Drop", "key.cannot_drop"),
                Map.entry("Chance Message", "crate.chance"),
                Map.entry("Inventory Full Claim", "key.inventory_full_claim"),
                Map.entry("Claim Join", "key.claim_join"),
                Map.entry("Possible Wins Title", "crate.possible_wins"),
                Map.entry("Crate Given", "crate.given")
        );
        YamlConfiguration target = localeFor(null);
        boolean changed = false;
        for (Map.Entry<String, String> entry : legacyKeys.entrySet()) {
            if (legacy.isString(entry.getKey())) {
                target.set("messages." + entry.getValue(), legacy.getString(entry.getKey()));
                changed = true;
            }
        }
        if (changed) {
            try {
                target.save(new File(new File(cratesPlus.getDataFolder(), "messages"),
                        cratesPlus.getConfig().getString("Locale", DEFAULT_LOCALE) + ".yml"));
            } catch (Exception exception) {
                cratesPlus.getLogger().warning("Could not migrate legacy messages: " + exception.getMessage());
            }
        }
        if (legacy.isString("Prefix")) {
            cratesPlus.getConfig().set("Prefix", legacy.getString("Prefix"));
            cratesPlus.saveConfig();
        }
    }

    private static YamlConfiguration localeFor(Player player) {
        String configured = cratesPlus.getConfig().getString("Locale", DEFAULT_LOCALE);
        if (player != null) {
            Locale playerLocale = player.locale();
            String playerKey = playerLocale.getLanguage() + "_" + playerLocale.getCountry();
            if (locales.containsKey(playerKey)) {
                return locales.get(playerKey);
            }
        }
        return locales.getOrDefault(configured, locales.get(DEFAULT_LOCALE));
    }

    private static String template(String key, Player player) {
        if (testMessages) {
            return "&7[&e" + key + "&7] ";
        }
        String message = localeFor(player).getString("messages." + key);
        if (message == null) {
            YamlConfiguration fallback = locales.get(DEFAULT_LOCALE);
            message = fallback == null ? null : fallback.getString("messages." + key);
            if (message == null) {
                cratesPlus.getLogger().warning("Missing message key: " + key);
                return key;
            }
        }
        return message;
    }

    public static String convertPlaceholders(String message, Player player, Crate crate, Winning winning) {
        if (player != null) {
            message = message.replace("%name%", player.getName())
                    .replace("%displayname%", player.getName())
                    .replace("%uuid%", player.getUniqueId().toString());
        }
        if (crate != null) {
            message = message.replace("%crate%", crate.getName(true));
        }
        if (winning != null) {
            String name = winning.getWinningItemStack().hasItemMeta() && winning.getWinningItemStack().getItemMeta().displayName() != null
                    ? ComponentUtil.plain(winning.getWinningItemStack().getItemMeta().displayName())
                    : winning.getWinningItemStack().getType().translationKey();
            message = message.replace("%prize%", name)
                    .replace("%winning%", name)
                    .replace("%percentage%", String.valueOf(winning.getPercentage()));
        }
        return message;
    }

    /** Returns a legacy string for inventories and older configuration paths. */
    public static String getMessage(String key, Player player, Crate crate, Winning winning) {
        return LEGACY.serialize(component(key, player, crate, winning));
    }

    public static Component component(String key, Player player, Crate crate, Winning winning) {
        return LEGACY.deserialize(convertPlaceholders(template(key, player), player, crate, winning));
    }

    /** Converts existing section-sign messages while the remaining command UI is migrated to locale keys. */
    public static Component legacyComponent(String message) {
        return ComponentUtil.legacy(message);
    }

    public static void sendLegacy(CommandSender sender, String message) {
        sender.sendMessage(legacyComponent(message));
    }

    public static void sendMessage(Player player, String key, Crate crate, Winning winning) {
        String prefix = cratesPlus.getConfig().getString("Prefix", "&7[&bCratesPlus&7]") + " ";
        player.sendMessage(LEGACY.deserialize(prefix).append(component(key, player, crate, winning)));
    }
}
