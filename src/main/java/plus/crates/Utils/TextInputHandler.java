package plus.crates.Utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import net.kyori.adventure.text.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Server API-only text input. This replaces the version-specific fake-sign packet
 * and Netty decoder used by the legacy plugin.
 */
public final class TextInputHandler implements Listener {
    private final Map<UUID, PendingInput> pendingInputs = new HashMap<>();

    public TextInputHandler(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void request(Player player, String title, Consumer<String> callback) {
        Inventory inventory = Bukkit.createInventory(null, InventoryType.ANVIL, Component.text(title));
        ItemStack input = new ItemStack(Material.PAPER);
        ItemMeta meta = input.getItemMeta();
        meta.displayName(Component.text("Enter text here"));
        input.setItemMeta(meta);
        inventory.setItem(0, input);
        pendingInputs.put(player.getUniqueId(), new PendingInput(inventory, callback));
        player.openInventory(inventory);
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        PendingInput pending = pendingInputs.get(event.getWhoClicked().getUniqueId());
        if (pending == null || event.getView().getTopInventory() != pending.inventory) {
            return;
        }
        event.setCancelled(true);
        if (event.getRawSlot() != 2) {
            return;
        }
        ItemStack result = event.getCurrentItem();
        pendingInputs.remove(event.getWhoClicked().getUniqueId());
        event.getWhoClicked().closeInventory();
        if (result != null && result.hasItemMeta() && result.getItemMeta().displayName() != null) {
            pending.callback.accept(ComponentUtil.plain(result.getItemMeta().displayName()));
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        PendingInput pending = pendingInputs.get(event.getPlayer().getUniqueId());
        if (pending != null && event.getInventory() == pending.inventory) {
            pendingInputs.remove(event.getPlayer().getUniqueId());
        }
    }

    private record PendingInput(Inventory inventory, Consumer<String> callback) {
    }
}
