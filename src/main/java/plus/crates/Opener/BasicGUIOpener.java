package plus.crates.Opener;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import plus.crates.Crates.Crate;
import plus.crates.Crates.Winning;
import plus.crates.CratesPlus;
import plus.crates.Utils.ComponentUtil;
import net.kyori.adventure.text.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

public class BasicGUIOpener extends Opener implements Listener {
    private static final org.bukkit.Material[] ROLLING_PANES = {
            org.bukkit.Material.WHITE_STAINED_GLASS_PANE, org.bukkit.Material.ORANGE_STAINED_GLASS_PANE,
            org.bukkit.Material.MAGENTA_STAINED_GLASS_PANE, org.bukkit.Material.LIGHT_BLUE_STAINED_GLASS_PANE,
            org.bukkit.Material.YELLOW_STAINED_GLASS_PANE, org.bukkit.Material.LIME_STAINED_GLASS_PANE,
            org.bukkit.Material.PINK_STAINED_GLASS_PANE, org.bukkit.Material.GRAY_STAINED_GLASS_PANE,
            org.bukkit.Material.LIGHT_GRAY_STAINED_GLASS_PANE, org.bukkit.Material.CYAN_STAINED_GLASS_PANE,
            org.bukkit.Material.PURPLE_STAINED_GLASS_PANE, org.bukkit.Material.BLUE_STAINED_GLASS_PANE,
            org.bukkit.Material.BROWN_STAINED_GLASS_PANE, org.bukkit.Material.GREEN_STAINED_GLASS_PANE,
            org.bukkit.Material.RED_STAINED_GLASS_PANE, org.bukkit.Material.BLACK_STAINED_GLASS_PANE
    };
    private CratesPlus cratesPlus;
    private HashMap<UUID, Integer> tasks = new HashMap<>();
    private HashMap<UUID, Inventory> guis = new HashMap<>();
    private int length = 5;
    private String rollingText = "Rolling...";
    private String winnerText = "Winner!";
    private boolean sound = true;

    public BasicGUIOpener(CratesPlus cratesPlus) {
        super(cratesPlus, "BasicGUI");
        this.cratesPlus = cratesPlus;
    }

    @Override
    public void doSetup() {
        FileConfiguration config = getOpenerConfig();
        if (!config.isSet("Length")) {
            config.set("Length", cratesPlus.getConfigHandler().getCrateGUITime());
            try {
                config.save(getOpenerConfigFile());
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        if (!config.isSet("Rolling Text")) {
            config.set("Rolling Text", "Rolling...");
            try {
                config.save(getOpenerConfigFile());
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        if (!config.isSet("Winner Text")) {
            config.set("Winner Text", "Winner!");
            try {
                config.save(getOpenerConfigFile());
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        if (!config.isSet("Sound")) {
            config.set("Sound", true);
            try {
                config.save(getOpenerConfigFile());
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        length = config.getInt("Length", cratesPlus.getConfigHandler().getCrateGUITime());
        rollingText = config.getString("Rolling Text", "Rolling...");
        winnerText = config.getString("Winner Text", "Winner!");
        sound = config.getBoolean("Sound", true);
        cratesPlus.getServer().getPluginManager().registerEvents(this, cratesPlus);
    }

    @Override
    public void doOpen(final Player player, final Crate crate, Location blockLocation) {
        final Inventory winGUI;
        final Integer[] timer = {0};
        final Integer[] currentItem = new Integer[1];

        if (crate.getWinnings().isEmpty()) {
            plus.crates.Handlers.MessageHandler.sendMessage(player, "crate.no_winnings", crate, null);
            finish(player);
            return;
        }

        Random random = new Random();
        int max = crate.getWinnings().size() - 1;
        int min = 0;
        currentItem[0] = random.nextInt((max - min) + 1) + min;
        WinningInventoryHolder holder = new WinningInventoryHolder();
        winGUI = Bukkit.createInventory(holder, 45, Component.text(crate.getName(), crate.getColor()).append(Component.text(" Win")));
        holder.setInventory(winGUI);
        guis.put(player.getUniqueId(), winGUI);
        player.openInventory(winGUI);
        final int maxTimeTicks = length * 10;
        // Bukkit inventories and player state are main-thread-only. Running this asynchronously
        // caused intermittent item loss, duplicated openings, and server-thread safety violations.
        tasks.put(player.getUniqueId(), Bukkit.getScheduler().runTaskTimer(cratesPlus, () -> {
            if (!player.isOnline()) {
                finish(player);
                //TODO Want to re-explore what we should do here, this happens if the player logs off mid-opening.
                Bukkit.getScheduler().runTask(cratesPlus, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "crate key " + player.getName() + " " + crate.getName() + " 1"));
                Integer taskId = tasks.remove(player.getUniqueId());
                if (taskId != null) Bukkit.getScheduler().cancelTask(taskId);
                return;
            }
            Integer i = 0;
            while (i < 45) {
                if (i == 22) {
                    i++;
                    if (crate.getWinnings().size() == currentItem[0])
                        currentItem[0] = 0;
                    final Winning winning;
                    if (timer[0] == maxTimeTicks) {
                        winning = crate.handleWin(player);
                    } else {
                        winning = crate.getWinnings().get(currentItem[0]);
                    }

                    final ItemStack currentItemStack = winning.getPreviewItemStack();
                    winGUI.setItem(22, currentItemStack);

                    currentItem[0]++;
                    continue;
                }
                ItemStack itemStack = new ItemStack(ROLLING_PANES[cratesPlus.getCrateHandler().randInt(0, ROLLING_PANES.length - 1)]);
                ItemMeta itemMeta = itemStack.getItemMeta();
                if (timer[0] == maxTimeTicks) {
                    itemMeta.displayName(ComponentUtil.legacy(winnerText));
                } else {
                    if (sound) {
                        final Sound finalSound = Sound.BLOCK_NOTE_BLOCK_HARP;
                        Bukkit.getScheduler().runTask(cratesPlus, () -> {
                            if (player.getOpenInventory().getTopInventory().getHolder() instanceof WinningInventoryHolder)
                                player.playSound(player.getLocation(), finalSound, (float) 0.2, 2);
                        });
                    }
                    itemMeta.displayName(ComponentUtil.legacy(rollingText));
                }
                itemStack.setItemMeta(itemMeta);
                winGUI.setItem(i, itemStack);
                i++;
            }
            if (timer[0] == maxTimeTicks) {
                finish(player);
                Integer taskId = tasks.remove(player.getUniqueId());
                if (taskId != null) Bukkit.getScheduler().cancelTask(taskId);
                return;
            }
            timer[0]++;
        }, 0L, 2L).getTaskId());
    }

    @Override
    public void doReopen(Player player, Crate crate, Location location) {
        player.openInventory(guis.get(player.getUniqueId()));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getView().getTopInventory();
        if (!(inventory.getHolder() instanceof WinningInventoryHolder)) return;
        event.setCancelled(true);
        if (event.getRawSlot() < inventory.getSize()) {
            event.getWhoClicked().closeInventory();
        }
    }

    public boolean doesSupport(Crate crate) {
        return true;
    }

    private static final class WinningInventoryHolder implements InventoryHolder {
        private Inventory inventory;

        private void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

}
