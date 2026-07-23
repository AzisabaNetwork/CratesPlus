package plus.crates.Listeners;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Inventory;
import plus.crates.Crates.*;
import plus.crates.CratesPlus;
import plus.crates.Events.CrateOpenEvent;
import plus.crates.Handlers.MessageHandler;
import plus.crates.Utils.ComponentUtil;

import java.util.List;
import java.util.Map;
import java.util.Iterator;

public class BlockListeners implements Listener {
    private CratesPlus cratesPlus;

    public BlockListeners(CratesPlus cratesPlus) {
        this.cratesPlus = cratesPlus;
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent event) {
        if (!cratesPlus.getConfigHandler().isDisableKeySwapping())
            return;
        String title;
        ItemStack item = event.getItemDrop().getItemStack();

        for (Map.Entry<String, Crate> crate : cratesPlus.getConfigHandler().getCrates().entrySet()) {
            if (!(crate.getValue() instanceof KeyCrate)) {
                continue;
            }
            KeyCrate keyCrate = (KeyCrate) crate.getValue();
            Key key = keyCrate.getKey();
            if (key == null)
                continue;
            title = key.getName();

            if (key.matches(item)) {
                MessageHandler.sendMessage(event.getPlayer(), "key.cannot_drop", crate.getValue(), null);
                event.setCancelled(true);
                return;
            }
        }

    }

    // meh idc where I put my listeners ;)
    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (!cratesPlus.getConfigHandler().isDisableKeySwapping())
            return;
        String title;
        List<ItemStack> items = event.getDrops();
        for (Iterator<ItemStack> iterator = items.iterator(); iterator.hasNext(); ) {
            ItemStack item = iterator.next();
            for (Map.Entry<String, Crate> crate : cratesPlus.getConfigHandler().getCrates().entrySet()) {
                if (!(crate.getValue() instanceof KeyCrate)) {
                    continue;
                }
                KeyCrate keyCrate = (KeyCrate) crate.getValue();
                Key key = keyCrate.getKey();
                if (key == null)
                    continue;
                title = key.getName();

                if (key.matches(item)) {
                    // Removing from the list inside a for-each loop threw a
                    // ConcurrentModificationException and could lose keys on death.
                    iterator.remove();
                    cratesPlus.getCrateHandler().giveCrateKey(event.getEntity(), crate.getValue().getName(), item.getAmount(), false, true);
                    break;
                }
            }
        }
    }

    @EventHandler
    public void onInventoryMove(InventoryMoveItemEvent event) {
        if (!cratesPlus.getConfigHandler().isDisableKeySwapping())
            return;
        String title;
        ItemStack item = event.getItem();

        for (Map.Entry<String, Crate> crate : cratesPlus.getConfigHandler().getCrates().entrySet()) {
            if (!(crate.getValue() instanceof KeyCrate)) {
                continue;
            }
            KeyCrate keyCrate = (KeyCrate) crate.getValue();
            Key key = keyCrate.getKey();
            if (key == null)
                continue;
            title = key.getName();

            if (key.matches(item)) {
                // Send message?
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryPickup(InventoryPickupItemEvent event) {
        if (!cratesPlus.getConfigHandler().isDisableKeySwapping())
            return;
        if (event.getItem().getItemStack() != null) {
            String title;
            ItemStack item = event.getItem().getItemStack();
            for (Map.Entry<String, Crate> crate : cratesPlus.getConfigHandler().getCrates().entrySet()) {
                if (!(crate.getValue() instanceof KeyCrate)) {
                    continue;
                }
                KeyCrate keyCrate = (KeyCrate) crate.getValue();
                Key key = keyCrate.getKey();
                if (key == null)
                    continue;
                title = key.getName();

                if (key.matches(item)) {
                    // Send message?
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (!cratesPlus.getConfigHandler().isDisableKeySwapping()
                || (topInventory.getHolder() instanceof plus.crates.Utils.GUI.Holder holder && holder.getGui().allowsKeyMovement()))
            return;
        if (!event.getInventory().getType().toString().contains("PLAYER") && event.getCurrentItem() != null) {
            String title;
            ItemStack item = event.getCurrentItem();
            for (Map.Entry<String, Crate> crate : cratesPlus.getConfigHandler().getCrates().entrySet()) {
                if (!(crate.getValue() instanceof KeyCrate)) {
                    continue;
                }
                KeyCrate keyCrate = (KeyCrate) crate.getValue();
                Key key = keyCrate.getKey();
                if (key == null)
                    continue;
                title = key.getName();

                if (key.matches(item)) {
                    // Send message?
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        String title;
        Player player = event.getPlayer();
        ItemStack item = cratesPlus.getVersion_util().getItemInPlayersHand(player);
        ItemStack itemOff = cratesPlus.getVersion_util().getItemInPlayersOffHand(player);

        for (Map.Entry<String, Crate> crate : cratesPlus.getConfigHandler().getCrates().entrySet()) {
            if (!(crate.getValue() instanceof KeyCrate)) {
                continue;
            }
            KeyCrate keyCrate = (KeyCrate) crate.getValue();
            Key key = keyCrate.getKey();
            if (key == null)
                continue;
            title = key.getName();

            if (key.matches(itemOff)) {
                item = itemOff;
            }

            if (key.matches(item)) {
                MessageHandler.sendMessage(event.getPlayer(), "key.cannot_place", crate.getValue(), null);
                event.setCancelled(true);
                return;
            }
        }

        String crateSlug = cratesPlus.getCrateType(item);
        if (crateSlug == null && item.hasItemMeta() && item.getItemMeta().displayName() != null
                && ComponentUtil.plain(item.getItemMeta().displayName()).contains("Crate")) {
            // Compatibility for crate items issued before the PDC marker was introduced.
            crateSlug = ComponentUtil.plain(item.getItemMeta().displayName()).replaceAll(" Crate", "").toLowerCase();
        }
        if (crateSlug != null) {
            final Crate crate = cratesPlus.getConfigHandler().getCrate(crateSlug);

            if (crate instanceof MysteryCrate) {
                // TODO????????
            } else if (crate instanceof SupplyCrate) {
                System.out.println(crate.toString());
                // Handle supply crate
                SupplyCrate supplyCrate = (SupplyCrate) crate;
                if (!event.isCancelled()) {
                    CrateOpenEvent crateOpenEvent = new CrateOpenEvent(player, supplyCrate, event.getBlock().getLocation(), cratesPlus);
                    crateOpenEvent.doEvent();
                }
            } else if (crate instanceof KeyCrate) {
                KeyCrate keyCrate = (KeyCrate) crate;
                Location location = event.getBlock().getLocation();
                keyCrate.addLocation(location.getBlockX() + "-" + location.getBlockY() + "-" + location.getBlockZ(), location);
                keyCrate.addToConfig(location);
                cratesPlus.getCrateBlockStorage().set(event.getBlock(), crate.getName(false));

                Location location1 = location.getBlock().getLocation().add(0.5, 0.5, 0.5);
                keyCrate.loadHolograms(location1);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        String crateType = cratesPlus.getCrateBlockStorage().get(event.getBlock());
        if (crateType == null) {
            return;
        }
        Crate crate = cratesPlus.getConfigHandler().getCrates().get(crateType.toLowerCase());
        if (crate == null) // TODO Better handling of crates removed from the config
            return;
        if (!(crate instanceof KeyCrate)) {
            return;
        }
        KeyCrate keyCrate = (KeyCrate) crate;
        Location location = event.getBlock().getLocation();

        if (event.getPlayer().isSneaking() && (cratesPlus.getConfig().getBoolean("Crate Protection") && !event.getPlayer().hasPermission("cratesplus.admin"))) {
            MessageHandler.sendMessage(event.getPlayer(), "crate.remove_no_permission", crate, null);
            event.setCancelled(true);
            return;
        } else if (!event.getPlayer().isSneaking()) {
            MessageHandler.sendMessage(event.getPlayer(), "crate.remove_sneak", crate, null);
            event.setCancelled(true);
            return;
        }
        cratesPlus.getCrateBlockStorage().remove(location.getBlock());
        keyCrate.removeFromConfig(location);
        keyCrate.removeHolograms(location.getBlock().getLocation());
    }

}
