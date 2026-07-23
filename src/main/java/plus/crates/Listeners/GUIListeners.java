package plus.crates.Listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import plus.crates.Utils.GUI;

public class GUIListeners implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (!(topInventory.getHolder() instanceof GUI.Holder holder)) return;
        event.setCancelled(true);
        if (event.getRawSlot() >= topInventory.getSize() || event.getCurrentItem() == null || event.getCurrentItem().getType().equals(Material.AIR)) return;
        holder.getGui().handleClick((Player) event.getWhoClicked(), event.getRawSlot());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof GUI.Holder holder)) return;
        if (GUI.ignoreClosing.contains(event.getPlayer().getUniqueId())) {
            GUI.ignoreClosing.remove(event.getPlayer().getUniqueId());
            return;
        }
        if (GUI.guis.get(event.getPlayer().getUniqueId()) == holder.getGui())
            GUI.guis.remove(event.getPlayer().getUniqueId());
        if (GUI.pageTracker.containsKey(event.getPlayer().getUniqueId()))
            GUI.pageTracker.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (GUI.ignoreClosing.contains(event.getPlayer().getUniqueId()))
            GUI.ignoreClosing.remove(event.getPlayer().getUniqueId());
        if (GUI.guis.containsKey(event.getPlayer().getUniqueId()))
            GUI.guis.remove(event.getPlayer().getUniqueId());
        if (GUI.pageTracker.containsKey(event.getPlayer().getUniqueId()))
            GUI.pageTracker.remove(event.getPlayer().getUniqueId());
    }

}
