package plus.crates.Utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import plus.crates.CratesPlus;

import java.util.List;

public class Version_Util {
    protected CratesPlus cratesPlus;

    public Version_Util(CratesPlus cratesPlus) {
        this.cratesPlus = cratesPlus;
    }

    public ItemStack getItemInPlayersHand(Player player) {
        return player.getInventory().getItemInMainHand();
    }

    public ItemStack getItemInPlayersOffHand(Player player) {
        return player.getInventory().getItemInOffHand();
    }

    public void removeItemInOffHand(Player player) {
        player.getInventory().setItemInOffHand(null);
    }

    public ItemStack getSpawnEgg(EntityType entityType, Integer amount) {
        Material material = Material.getMaterial(entityType.name() + "_SPAWN_EGG");
        if (material == null) {
            throw new IllegalArgumentException("No spawn egg exists for " + entityType);
        }
        return new ItemStack(material, amount);
    }

    public EntityType getEntityTypeFromItemStack(ItemStack itemStack) {
        if (itemStack == null || !itemStack.getType().name().endsWith("_SPAWN_EGG")) {
            return null;
        }
        String entityName = itemStack.getType().name().replace("_SPAWN_EGG", "");
        try {
            return EntityType.valueOf(entityName);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public ItemMeta handleItemFlags(ItemMeta itemMeta, List<String> flags) {
        for (String flagName : flags) {
            try {
                itemMeta.addItemFlags(ItemFlag.valueOf(flagName.toUpperCase(java.util.Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
                cratesPlus.getLogger().warning("Ignoring unknown item flag '" + flagName + "'.");
            }
        }
        return itemMeta;
    }

}
