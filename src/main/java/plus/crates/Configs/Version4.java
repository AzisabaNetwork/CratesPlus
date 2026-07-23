package plus.crates.Configs;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import plus.crates.Utils.ComponentUtil;
import plus.crates.CratesPlus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Version4 extends ConfigVersion {

    public Version4(CratesPlus cratesPlus) {
        super(cratesPlus, 4);
    }

    @Override
    protected void update() {
        int count = 1;
        for (String name : getConfig().getConfigurationSection("Crates").getKeys(false)) {
            List<?> items = getConfig().getList("Crates." + name + ".Items");
            for (Object object : items) {
                String i = object.toString();
                if (i.toUpperCase().startsWith("COMMAND:")) {
                    ItemStack itemStack = getCratesPlus().getCrateHandler().stringToItemstackOld(i);
                    if (itemStack == null)
                        return;

                    getConfig().set("Crates." + name + ".Winnings." + count + ".Type", "COMMAND");
                    getConfig().set("Crates." + name + ".Winnings." + count + ".Item Type", itemStack.getType().toString());
                    getConfig().set("Crates." + name + ".Winnings." + count + ".Amount", itemStack.getAmount());
                    if (itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName())
                        getConfig().set("Crates." + name + ".Winnings." + count + ".Name", ComponentUtil.plain(itemStack.getItemMeta().displayName()));

                    ArrayList<String> enchantments = new ArrayList<String>();
                    for (Map.Entry<Enchantment, Integer> entry : itemStack.getEnchantments().entrySet()) {
                        Enchantment enchantment = entry.getKey();
                        Integer level = entry.getValue();

                        if (level > 1) {
                            enchantments.add(enchantment.getKey().getKey().toUpperCase() + "-" + level);
                        } else {
                            enchantments.add(enchantment.getKey().getKey().toUpperCase());
                        }
                    }
                    getConfig().set("Crates." + name + ".Winnings." + count + ".Enchantments", enchantments);

                    ArrayList<String> commands = new ArrayList<String>();
                    commands.add(ComponentUtil.plain(itemStack.getItemMeta().displayName()).replaceAll("Command: /", ""));
                    getConfig().set("Crates." + name + ".Winnings." + count + ".Commands", commands);

                    getConfig().set("Crates." + name + ".Items", null);
                } else {
                    ItemStack itemStack = getCratesPlus().getCrateHandler().stringToItemstackOld(i);
                    if (itemStack == null)
                        return;

                    getConfig().set("Crates." + name + ".Winnings." + count + ".Type", "ITEM");
                    getConfig().set("Crates." + name + ".Winnings." + count + ".Item Type", itemStack.getType().toString());
                    getConfig().set("Crates." + name + ".Winnings." + count + ".Amount", itemStack.getAmount());
                    if (itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName())
                        getConfig().set("Crates." + name + ".Winnings." + count + ".Name", ComponentUtil.plain(itemStack.getItemMeta().displayName()));

                    ArrayList<String> enchantments = new ArrayList<String>();
                    for (Map.Entry<Enchantment, Integer> entry : itemStack.getEnchantments().entrySet()) {
                        Enchantment enchantment = entry.getKey();
                        Integer level = entry.getValue();

                        if (level > 1) {
                            enchantments.add(enchantment.getKey().getKey().toUpperCase() + "-" + level);
                        } else {
                            enchantments.add(enchantment.getKey().getKey().toUpperCase());
                        }
                    }
                    getConfig().set("Crates." + name + ".Winnings." + count + ".Enchantments", enchantments);
                    getConfig().set("Crates." + name + ".Items", null);

                    count++;
                }
            }
        }
    }

}
