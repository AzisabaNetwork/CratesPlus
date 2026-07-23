package plus.crates.Utils;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LinfootUtil {

    public static Enchantment getEnchantmentFromNiceName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String normalizedName = name.toLowerCase(Locale.ROOT);
        NamespacedKey key = normalizedName.contains(":")
                ? NamespacedKey.fromString(normalizedName)
                : NamespacedKey.minecraft(normalizedName);
        Enchantment enchantment = key == null ? null : Registry.ENCHANTMENT.get(key);

        if (enchantment != null)
            return enchantment;

        switch (normalizedName) {
            case "sharpness":
            case "damage_all":
                enchantment = Enchantment.SHARPNESS;
                break;
            case "unbreaking":
            case "durability":
                enchantment = Enchantment.UNBREAKING;
                break;
            case "efficiency":
            case "dig_speed":
                enchantment = Enchantment.EFFICIENCY;
                break;
            case "protection":
            case "protection_environmental":
                enchantment = Enchantment.PROTECTION;
                break;
            case "power":
            case "arrow_damage":
                enchantment = Enchantment.POWER;
                break;
            case "punch":
            case "arrow_knockback":
                enchantment = Enchantment.PUNCH;
                break;
            case "infinite":
            case "arrow_infinite":
                enchantment = Enchantment.INFINITY;
                break;
        }

        return enchantment;
    }

    public static ItemStack buildItemStack(ItemStack itemStack, String name, List<String> lore) {
        if (name == null && lore == null) {
            return itemStack;
        }
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (name != null) {
            itemMeta.displayName(ComponentUtil.legacy(name));
        }
        if (lore != null) {
            itemMeta.lore(ComponentUtil.legacy(lore));
        }
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public static void copyConfigSection(FileConfiguration config, String fromPath, String toPath) {
        Map<String, Object> vals = config.getConfigurationSection(fromPath).getValues(true);
        String toDot = toPath.equals("") ? "" : ".";
        for (String s : vals.keySet()) {
            Object val = vals.get(s);
            if (val instanceof List<?>)
                val = new ArrayList<>((List<?>) val);
            config.set(toPath + toDot + s, val);
        }
    }

    // System.out.println(versionCompare("1.6", "1.8")); // -1 as 1.8 is newer
    // System.out.println(versionCompare("1.7", "1.8")); // -1 as 1.8 is newer
    // System.out.println(versionCompare("1.8", "1.8")); // 0 as same
    // System.out.println(versionCompare("1.9", "1.8")); // 1 as 1.9 is newer
    public static int versionCompare(String str1, String str2) {
        String[] vals1 = str1.split("\\.");
        String[] vals2 = str2.split("\\.");
        int i = 0;
        while (i < vals1.length && i < vals2.length && vals1[i].equals(vals2[i])) {
            i++;
        }
        if (i < vals1.length && i < vals2.length) {
            int diff = Integer.valueOf(vals1[i]).compareTo(Integer.valueOf(vals2[i]));
            return Integer.signum(diff);
        }
        return Integer.signum(vals1.length - vals2.length);
    }

}
