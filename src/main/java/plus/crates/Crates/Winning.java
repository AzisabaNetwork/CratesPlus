package plus.crates.Crates;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import plus.crates.CratesPlus;
import plus.crates.Handlers.ConfigHandler;
import plus.crates.Handlers.MessageHandler;
import plus.crates.Utils.ComponentUtil;
import plus.crates.Utils.LinfootUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Winning {
    private CratesPlus cratesPlus;
    private Crate crate;
    private boolean valid = false;
    private boolean command = false;
    private boolean always = false;
    private boolean pity = false;
    private double percentage = 0;
    private ItemStack previewItemStack;
    private ItemStack winningItemStack;
    private List<String> commands = new ArrayList<>();
    private List<String> lore = new ArrayList<>();
    private String entityType = "";

    public Winning(Crate crate, String path, CratesPlus cratesPlus, ConfigHandler configHandler) {
        this.cratesPlus = cratesPlus;
        this.crate = crate;

        if (configHandler != null && configHandler.isDebugMode()) {
            cratesPlus.getLogger().info("Loading data for \"" + path + "\"");
        }

        FileConfiguration config = cratesPlus.getCratesConfig();
        if (!config.isSet(path))
            return;

        if (!config.isSet(path + ".Type"))
            return;

        if (config.isSet(path + ".Always"))
            always = config.getBoolean(path + ".Always");
        if (config.isSet(path + ".Pity"))
            pity = config.getBoolean(path + ".Pity");

        String type = config.getString(path + ".Type");
        ItemStack itemStack;
        if (type.equalsIgnoreCase("item") || type.equalsIgnoreCase("block")) {
            Material itemType = null;
            String configuredItemType = null;
            if (config.isSet(path + ".Item Type"))
                configuredItemType = config.getString(path + ".Item Type");
            else if (config.isSet(path + ".Block Type"))
                configuredItemType = config.getString(path + ".Block Type");

            Integer itemData = 0;
            if (config.isSet(path + ".Item Data"))
                itemData = config.getInt(path + ".Item Data");

            if (config.isSet(path + ".Entity Type"))
                entityType = config.getString(path + ".Entity Type");

            if (config.isSet(path + ".Percentage"))
                percentage = config.getDouble(path + ".Percentage");

            Integer amount = 1;
            if (config.isSet(path + ".Amount"))
                amount = config.getInt(path + ".Amount");

            if (!entityType.isEmpty() && isLegacySpawnEgg(configuredItemType)) {
                try {
                    itemStack = cratesPlus.getVersion_util().getSpawnEgg(EntityType.valueOf(entityType.toUpperCase()), amount);
                } catch (IllegalArgumentException exception) {
                    cratesPlus.getLogger().warning("Invalid Entity Type '" + entityType + "' for " + path);
                    return;
                }
            } else {
                itemType = configuredItemType == null ? null : Material.matchMaterial(configuredItemType);
                if (itemType == null || itemType.isLegacy()) {
                    cratesPlus.getLogger().warning("Invalid or legacy item type '" + configuredItemType + "' for " + path);
                    return;
                }
                itemStack = new ItemStack(itemType, amount);
            }
        } else if (type.equalsIgnoreCase("command")) {
            command = true;
            if (config.isSet(path + ".Commands") && config.getStringList(path + ".Commands").size() != 0) {
                commands = config.getStringList(path + ".Commands");
            } else if (config.isSet(path + ".commands") && config.getStringList(path + ".commands").size() != 0) {
                commands = config.getStringList(path + ".commands");
            }

            if (commands.isEmpty()) {
                cratesPlus.getLogger().warning("No \"Commands\" found for " + path);
                return;
            }


            Material itemType = Material.PAPER;
            if (config.isSet(path + ".Item Type"))
                itemType = Material.matchMaterial(config.getString(path + ".Item Type"));

            if (itemType == null || itemType.isLegacy())
                return;

            Integer itemData = 0;
            if (config.isSet(path + ".Item Data"))
                itemData = config.getInt(path + ".Item Data");

            if (config.isSet(path + ".Percentage"))
                percentage = config.getDouble(path + ".Percentage");

            Integer amount = 1;
            if (config.isSet(path + ".Amount"))
                amount = config.getInt(path + ".Amount");

            itemStack = new ItemStack(itemType, amount);
        } else {
            return;
        }
        ItemStack winningItemStack = itemStack.clone();
        ItemStack previewItemStack = itemStack.clone();

        boolean showAmountInTitle = false;
        int originalAmount = 0;
        if (previewItemStack.getAmount() > previewItemStack.getMaxStackSize()) { // Stop multiple stacks for the same item!
            originalAmount = previewItemStack.getAmount();
            showAmountInTitle = true;
            previewItemStack.setAmount(previewItemStack.getMaxStackSize());
        }

        ItemMeta previewItemStackItemMeta = getConfiguredMeta(config, path, previewItemStack.getItemMeta());

        if (config.isSet(path + ".Flags")) {
            previewItemStackItemMeta = cratesPlus.getVersion_util().handleItemFlags(previewItemStackItemMeta, config.getStringList(path + ".Flags"));
        }

        String displayName = "";
        if (config.isSet(path + ".Name") && !config.getString(path + ".Name").equals("NONE"))
            displayName = ComponentUtil.legacyString(config.getString(path + ".Name"));
        if (showAmountInTitle)
            displayName = displayName + " x" + originalAmount;
        if (!displayName.equals(""))
            previewItemStackItemMeta.displayName(ComponentUtil.legacy(displayName));
        previewItemStack.setItemMeta(previewItemStackItemMeta);

        if (config.isSet(path + ".Enchantments")) {
            List<?> enchtantments = config.getList(path + ".Enchantments");
            for (Object object : enchtantments) {
                String enchantment = (String) object;
                String[] args = enchantment.split("-");
                try {
                    Integer level = 1;
                    if (args.length > 1)
                        level = Integer.valueOf(args[1]);
                    Enchantment resolved = LinfootUtil.getEnchantmentFromNiceName(args[0]);
                    if (resolved != null) {
                        previewItemStack.addUnsafeEnchantment(resolved, level);
                    }
                } catch (Exception ignored) {
                }
            }
        }

        if (config.isSet(path + ".Lore")) {
            List<String> lines = config.getStringList(path + ".Lore");
            for (String line : lines) {
                this.lore.add(ComponentUtil.legacyString(line));
            }
        }

        ItemMeta winningItemStackItemMeta = getConfiguredMeta(config, path, winningItemStack.getItemMeta());

        if (config.isSet(path + ".Flags")) {
            winningItemStackItemMeta = cratesPlus.getVersion_util().handleItemFlags(winningItemStackItemMeta, config.getStringList(path + ".Flags"));
        }

        displayName = "";
        if (config.isSet(path + ".Name") && !config.getString(path + ".Name").equals("NONE"))
            displayName = ComponentUtil.legacyString(config.getString(path + ".Name"));
        if (!displayName.equals(""))
            winningItemStackItemMeta.displayName(ComponentUtil.legacy(displayName));
        winningItemStackItemMeta.lore(ComponentUtil.legacy(this.lore));
        winningItemStack.setItemMeta(winningItemStackItemMeta);

        if (config.isSet(path + ".Enchantments")) {
            List<?> enchtantments = config.getList(path + ".Enchantments");
            for (Object object : enchtantments) {
                String enchantment = (String) object;
                String[] args = enchantment.split("-");
                Integer level = 1;
                if (args.length > 1)
                    level = Integer.valueOf(args[1]);
                Enchantment enchantment1 = LinfootUtil.getEnchantmentFromNiceName(args[0].toUpperCase());
                if (enchantment1 == null)
                    Bukkit.getLogger().warning("Invalid enchantment \"" + args[0].toUpperCase() + "\" found for item \"" + ComponentUtil.plain(ComponentUtil.legacy(displayName)) + "\"");
                else
                    winningItemStack.addUnsafeEnchantment(enchantment1, level);
            }
        }

        this.winningItemStack = winningItemStack;

        previewItemStackItemMeta = previewItemStack.getItemMeta();
        List<String> lore = new ArrayList<>(this.lore);
        if (percentage > 0 && !crate.isHidePercentages()) {
            if (cratesPlus.getConfig().getBoolean("Chance Message Gap", true))
                lore.add("&d");
            lore.add(MessageHandler.getMessage("crate.chance", null, crate, this).replaceAll("\\n", ""));
        }
        previewItemStackItemMeta.lore(ComponentUtil.legacy(lore));
        previewItemStack.setItemMeta(previewItemStackItemMeta);

        // Done :D
        valid = true;
        this.previewItemStack = previewItemStack;
    }

    /**
     * Serialized 1.15 ItemMeta cannot always be reconstructed by a current
     * Paper runtime. Recover portable YAML fields when Paper exposes it as a
     * map, rather than silently discarding the custom-model data and flags.
     */
    private ItemMeta getConfiguredMeta(FileConfiguration config, String path, ItemMeta fallback) {
        Object stored = config.get(path + ".Metadata");
        if (stored instanceof ItemMeta itemMeta) {
            return itemMeta.clone();
        }
        if (stored instanceof ConfigurationSection section) {
            return getConfiguredMeta(section.getValues(false), fallback);
        }
        if (!(stored instanceof Map<?, ?> metadata)) {
            return fallback;
        }

        return getConfiguredMeta(metadata, fallback);
    }

    private ItemMeta getConfiguredMeta(Map<?, ?> metadata, ItemMeta fallback) {

        Object unbreakable = metadata.get("Unbreakable");
        if (unbreakable instanceof Boolean value) {
            fallback.setUnbreakable(value);
        }
        applyLegacyItemFlags(metadata.get("ItemFlags"), fallback);
        applyCustomModelData(metadata.get("custom-model-data"), fallback);
        applyDamage(metadata.get("Damage"), fallback);
        return fallback;
    }

    private void applyLegacyItemFlags(Object value, ItemMeta itemMeta) {
        if (!(value instanceof Iterable<?> flags)) {
            return;
        }
        List<String> names = new ArrayList<>();
        for (Object flag : flags) {
            if (flag != null) {
                names.add(flag.toString());
            }
        }
        cratesPlus.getVersion_util().handleItemFlags(itemMeta, names);
    }

    private void applyCustomModelData(Object value, ItemMeta itemMeta) {
        Number number = value instanceof Number direct ? direct : null;
        if (number == null && value instanceof ConfigurationSection section) {
            value = section.getValues(false);
        }
        if (number == null && value instanceof Map<?, ?> values) {
            Object floats = values.get("floats");
            if (floats instanceof Iterable<?> iterable) {
                for (Object entry : iterable) {
                    if (entry instanceof Number floatValue) {
                        number = floatValue;
                        break;
                    }
                }
            }
        }
        if (number == null) {
            return;
        }
        CustomModelDataComponent customModelData = itemMeta.getCustomModelDataComponent();
        customModelData.setFloats(List.of(number.floatValue()));
        itemMeta.setCustomModelDataComponent(customModelData);
    }

    private void applyDamage(Object value, ItemMeta itemMeta) {
        if (value instanceof Number number && itemMeta instanceof Damageable damageable) {
            damageable.setDamage(Math.max(0, number.intValue()));
        }
    }

    public boolean isValid() {
        return valid;
    }

    public ItemStack getPreviewItemStack() {
        return previewItemStack.clone(); // Clone it so it can't be changed
    }

    public ItemStack getWinningItemStack() {
        return winningItemStack.clone(); // Clone it so it can't be changed and because Bukkit resets the stack size? Check issue #198 on this bug.
    }

    public ItemStack runWin(final Player player) {
        final Winning winning = this;

        if (isCommand() && getCommands().size() > 0) {
            Bukkit.getScheduler().runTask(cratesPlus, () -> runCommands(player));
        } else if (!isCommand()) {
            return winning.getWinningItemStack();
        }

        if (crate.isBroadcast())
            Bukkit.broadcast(MessageHandler.component("crate.broadcast", player, crate, winning));

        if (crate.isFirework())
            cratesPlus.getCrateHandler().spawnFirework(player.getLocation());

        return null;
    }

    private boolean isLegacySpawnEgg(String materialName) {
        return materialName != null && (materialName.equalsIgnoreCase("MONSTER_EGG")
                || materialName.equalsIgnoreCase("LEGACY_MONSTER_EGG"));
    }

    private void runCommands(Player player) {
        for (String command : getCommands()) {
            command = command.replaceAll("%name%", player.getName());
            command = command.replaceAll("%uuid%", player.getUniqueId().toString());
            command = command.replaceAll("%displayname%", player.getDisplayName());

            Pattern randPattern = Pattern.compile("%rand;(.*?)[,|;](.*?)%");
            Matcher randMatches = randPattern.matcher(command);
            while (randMatches.find()) {
                String start = randMatches.group(1);
                String end = randMatches.group(2);
                try {
                    if (start != null && Integer.valueOf(start) != null && end != null && Integer.valueOf(end) != null) {
                        int val = cratesPlus.getCrateHandler().randInt(Integer.valueOf(start), Integer.valueOf(end));
                        command = command.replaceAll("%rand;" + start + "," + end + "%", String.valueOf(val)).replaceAll("%rand;" + start + ";" + end + "%", String.valueOf(val));
                    }
                } catch (Exception ignored) {
                }
            }

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }
    }

    public boolean isCommand() {
        return command;
    }

    public double getPercentage() {
        return percentage;
    }

    public List<String> getCommands() {
        return commands;
    }

    public boolean isAlways() {
        return always;
    }

    /** Whether this reward resets and can satisfy this crate's pity system. */
    public boolean isPity() {
        return pity;
    }

}
