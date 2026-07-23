package plus.crates.Crates;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import plus.crates.CratesPlus;
import plus.crates.Utils.ComponentUtil;
import plus.crates.Handlers.MessageHandler;

import java.util.ArrayList;
import java.util.List;

public class Key {
    private final CratesPlus cratesPlus;
    private final KeyCrate crate;
    private final Material material;
    private final short data;
    private final String name;
    private final List<String> lore = new ArrayList<>();
    private final boolean enchanted;

    public Key(KeyCrate crate, Material material, short data, String name, boolean enchanted, List<String> lore, CratesPlus cratesPlus) {
        this.cratesPlus = cratesPlus;
        this.crate = crate;
        if (material == null)
            material = Material.TRIPWIRE_HOOK;
        this.material = material;
        this.data = data;
        this.name = name;
        this.enchanted = enchanted;
        if (!lore.isEmpty()) {
            for (String line : lore) {
                this.lore.add(MessageHandler.convertPlaceholders(line, null, crate, null));
            }
        }
    }

    public Material getMaterial() {
        return material;
    }

    public short getData() {
        return data;
    }

    public String getName() {
        return ChatColor.translateAlternateColorCodes('&', name);
    }

    public List<String> getLore() {
        if (this.lore.isEmpty()) {
            this.lore.add(ChatColor.GRAY + "Right-Click on a \"" + getCrate().getName(true) + ChatColor.GRAY + "\" crate");
            this.lore.add(ChatColor.GRAY + "to win an item!");
            this.lore.add("");
        }
        return this.lore;
    }

    public boolean isEnchanted() {
        return enchanted;
    }

    public ItemStack getKeyItem(Integer amount) {
        ItemStack keyItem = new ItemStack(getMaterial(), amount);
        if (isEnchanted())
            keyItem.addUnsafeEnchantment(Enchantment.UNBREAKING, 1);
        ItemMeta keyItemMeta = keyItem.getItemMeta();
        String title = getName().replaceAll("%type%", getCrate().getName(true));
        keyItemMeta.displayName(ComponentUtil.legacy(title));
        keyItemMeta.lore(ComponentUtil.legacy(getLore()));
        ArrayList<String> flags = new ArrayList<>();
        flags.add("HIDE_ENCHANTS");
        keyItemMeta = cratesPlus.getVersion_util().handleItemFlags(keyItemMeta, flags);
        keyItem.setItemMeta(keyItemMeta);
        keyItem.editPersistentDataContainer(pdc -> pdc.set(
                cratesPlus.getKeyCrateKey(), PersistentDataType.STRING, getCrate().getSlug()));
        return keyItem;
    }

    public boolean matches(ItemStack itemStack) {
        return itemStack != null && getCrate().getSlug().equals(
                itemStack.getPersistentDataContainer().get(cratesPlus.getKeyCrateKey(), PersistentDataType.STRING));
    }

    /** Converts an exact pre-PDC key held by an online player. */
    public boolean migrateLegacy(ItemStack itemStack) {
        if (matches(itemStack) || itemStack == null || itemStack.getType() != material || !itemStack.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = itemStack.getItemMeta();
        String expectedName = getName().replace("%type%", getCrate().getName(true));
        if (meta.displayName() == null || !ComponentUtil.legacy(expectedName).equals(meta.displayName())
                || !ComponentUtil.legacy(getLore()).equals(meta.lore())) {
            return false;
        }
        itemStack.editPersistentDataContainer(pdc -> pdc.set(
                cratesPlus.getKeyCrateKey(), PersistentDataType.STRING, getCrate().getSlug()));
        return true;
    }

    public KeyCrate getCrate() {
        return crate;
    }

}
