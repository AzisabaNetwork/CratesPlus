package plus.crates.Handlers;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import plus.crates.Crates.Crate;
import plus.crates.Crates.Winning;
import plus.crates.CratesPlus;
import plus.crates.Utils.GUI;
import plus.crates.Utils.ComponentUtil;
import java.util.*;

public class SettingsHandler implements Listener {
    private HashMap<UUID, String> renaming = new HashMap<>();
    private CratesPlus cratesPlus;
    private GUI settings;
    private GUI crates;
    private HashMap<String, String> lastCrateEditing = new HashMap<>();

    public SettingsHandler(CratesPlus cratesPlus) {
        this.cratesPlus = cratesPlus;
        Bukkit.getPluginManager().registerEvents(this, cratesPlus);
        setupSettingsInventory();
        setupCratesInventory();
    }

    public void setupSettingsInventory() {
        settings = new GUI("CratesPlus Settings");

        ItemStack itemStack;
        ItemMeta itemMeta;
        List<String> lore;

        itemStack = new ItemStack(Material.CHEST);
        itemMeta = itemStack.getItemMeta();
        itemMeta.displayName(Component.text("Edit Crates", NamedTextColor.GREEN));
        lore = new ArrayList<>();
        lore.add("");
        itemMeta.lore(ComponentUtil.legacy(lore));
        itemStack.setItemMeta(itemMeta);
        settings.setItem(1, itemStack, new GUI.ClickHandler() {
            @Override
            public void doClick(Player player, GUI gui) {
                GUI.ignoreClosing.add(player.getUniqueId());
                openCrates(player);
            }
        });

        itemStack = new ItemStack(Material.BARRIER);
        itemMeta = itemStack.getItemMeta();
        itemMeta.displayName(Component.text("Reload Config", NamedTextColor.GREEN));
        lore = new ArrayList<>();
        lore.add("");
        itemMeta.lore(ComponentUtil.legacy(lore));
        itemStack.setItemMeta(itemMeta);
        settings.setItem(5, itemStack, new GUI.ClickHandler() {
            @Override
            public void doClick(Player player, GUI gui) {
                player.closeInventory();
                cratesPlus.reloadPlugin();
                MessageHandler.sendLegacy(player, "&aReloaded config");
            }
        });
    }

    public void setupCratesInventory() {
        crates = new GUI("Crates");

        ItemStack itemStack;
        ItemMeta itemMeta;

        for (Map.Entry<String, Crate> entry : cratesPlus.getConfigHandler().getCrates().entrySet()) {
            Crate crate = entry.getValue();

            itemStack = new ItemStack(Material.CHEST);
            itemMeta = itemStack.getItemMeta();
            itemMeta.displayName(ComponentUtil.legacy(crate.getName(true)));
            itemStack.setItemMeta(itemMeta);
            final String crateName = crate.getName();
            crates.addItem(itemStack, new GUI.ClickHandler() {
                @Override
                public void doClick(Player player, GUI gui) {
                    GUI.ignoreClosing.add(player.getUniqueId());
                    openCrate(player, crateName);
                }
            });
        }
    }

    public void openSettings(final Player player) {
        Bukkit.getScheduler().runTaskLater(cratesPlus, () -> settings.open(player), 1L);
    }

    public void openCrates(final Player player) {
        Bukkit.getScheduler().runTaskLater(cratesPlus, () -> crates.open(player), 1L);
    }

    public void openCrateWinnings(final Player player, String crateName) {
        Crate crate = cratesPlus.getConfigHandler().getCrates().get(crateName.toLowerCase());
        if (crate == null) {
            MessageHandler.sendLegacy(player, "&cUnable to find " + crateName + " crate");
            return;
        }

        if (crate.containsCommandItem()) {
            MessageHandler.sendLegacy(player, "&cYou can not currently edit a crate in the GUI which has command items");
            player.closeInventory();
            return;
        }

        final GUI gui = new GUI("Edit " + crate.getName(false) + " Crate Winnings");

        for (Winning winning : crate.getWinnings()) {
            gui.addItem(winning.getWinningItemStack());
        }

        Bukkit.getScheduler().runTaskLater(cratesPlus, () -> gui.open(player), 1L);

    }

    public void openCrate(final Player player, final String crateName) {
        Crate crate = cratesPlus.getConfigHandler().getCrates().get(crateName.toLowerCase());
        if (crate == null) {
            return; // TODO Error handling here
        }

        final GUI gui = new GUI("Edit " + crate.getName(false) + " Crate");

        ItemStack itemStack;
        ItemMeta itemMeta;
        List<String> lore;


        // Rename Crate

        itemStack = new ItemStack(Material.NAME_TAG);
        itemMeta = itemStack.getItemMeta();
        itemMeta.displayName(Component.text("Rename Crate", NamedTextColor.GREEN));
        lore = new ArrayList<>();
        lore.add("");
        itemMeta.lore(ComponentUtil.legacy(lore));
        itemStack.setItemMeta(itemMeta);
        gui.setItem(0, itemStack, new GUI.ClickHandler() {
            @Override
            public void doClick(Player player, GUI gui) {
                player.closeInventory();
                renaming.put(player.getUniqueId(), crateName);
                cratesPlus.getTextInputHandler().request(player, "Rename crate", newName -> {
                    renaming.remove(player.getUniqueId());
                    if (!newName.isBlank()) {
                        Bukkit.dispatchCommand(player, "crate rename " + crateName + " " + newName.trim());
                        cratesPlus.getSettingsHandler().openCrate(player, newName.trim());
                    }
                });
            }
        });


        // Edit Crate Winnings

        itemStack = new ItemStack(Material.DIAMOND);
        itemMeta = itemStack.getItemMeta();
        itemMeta.displayName(Component.text("Edit Crate Winnings", NamedTextColor.RED));
        lore = new ArrayList<>();
        lore.add("");
        itemMeta.lore(ComponentUtil.legacy(lore));
        itemStack.setItemMeta(itemMeta);
        gui.setItem(2, itemStack, new GUI.ClickHandler() {
            @Override
            public void doClick(Player player, GUI gui) {
                MessageHandler.sendLegacy(player, "&cThis feature is currently disabled!");
//                GUI.ignoreClosing.add(player.getUniqueId());
//                openCrateWinnings(player, crateName);
            }
        });


        // Edit Crate Color

        itemStack = new ItemStack(Material.CYAN_WOOL);
        itemMeta = itemStack.getItemMeta();
        itemMeta.displayName(Component.text("Edit Crate Color", NamedTextColor.GREEN));
        lore = new ArrayList<>();
        lore.add("");
        itemMeta.lore(ComponentUtil.legacy(lore));
        itemStack.setItemMeta(itemMeta);
        gui.setItem(4, itemStack, new GUI.ClickHandler() {
            @Override
            public void doClick(Player player, GUI gui) {
                GUI.ignoreClosing.add(player.getUniqueId());
                openCrateColor(player, crate);
            }
        });


        // Delete Crate

        itemStack = new ItemStack(Material.BARRIER);
        itemMeta = itemStack.getItemMeta();
        itemMeta.displayName(Component.text("Delete Crate", NamedTextColor.GREEN));
        lore = new ArrayList<>();
        lore.add("");
        itemMeta.lore(ComponentUtil.legacy(lore));
        itemStack.setItemMeta(itemMeta);
        gui.setItem(6, itemStack, new GUI.ClickHandler() {
            @Override
            public void doClick(Player player, GUI gui) {
                GUI.ignoreClosing.add(player.getUniqueId());
                confirmDelete(player, crate);
            }
        });

        Bukkit.getScheduler().runTaskLater(cratesPlus, () -> gui.open(player), 1L);

    }

    private void openCrateColor(final Player player, final Crate crate) {
        GUI gui = new GUI("Edit Crate Color");

        addColorOption(gui, crate, Material.CYAN_WOOL, NamedTextColor.AQUA, "Aqua");
        addColorOption(gui, crate, Material.BLACK_WOOL, NamedTextColor.BLACK, "Black");
        addColorOption(gui, crate, Material.BLUE_WOOL, NamedTextColor.BLUE, "Blue");
        addColorOption(gui, crate, Material.CYAN_WOOL, NamedTextColor.DARK_AQUA, "Dark Aqua");
        addColorOption(gui, crate, Material.BLUE_WOOL, NamedTextColor.DARK_BLUE, "Dark Blue");
        addColorOption(gui, crate, Material.GRAY_WOOL, NamedTextColor.DARK_GRAY, "Dark Gray");
        addColorOption(gui, crate, Material.GREEN_WOOL, NamedTextColor.DARK_GREEN, "Dark Green");
        addColorOption(gui, crate, Material.PURPLE_WOOL, NamedTextColor.DARK_PURPLE, "Dark Purple");
        addColorOption(gui, crate, Material.RED_WOOL, NamedTextColor.DARK_RED, "Dark Red");
        addColorOption(gui, crate, Material.ORANGE_WOOL, NamedTextColor.GOLD, "Gold");
        addColorOption(gui, crate, Material.LIGHT_GRAY_WOOL, NamedTextColor.GRAY, "Gray");
        addColorOption(gui, crate, Material.LIME_WOOL, NamedTextColor.GREEN, "Green");
        addColorOption(gui, crate, Material.MAGENTA_WOOL, NamedTextColor.LIGHT_PURPLE, "Light Purple");
        addColorOption(gui, crate, Material.RED_WOOL, NamedTextColor.RED, "Red");
        addColorOption(gui, crate, Material.WHITE_WOOL, NamedTextColor.WHITE, "White");
        addColorOption(gui, crate, Material.YELLOW_WOOL, NamedTextColor.YELLOW, "Yellow");

        gui.open(player);
    }

    private void addColorOption(GUI gui, Crate crate, Material material, NamedTextColor color, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, color));
        item.setItemMeta(meta);
        gui.addItem(item, getColorClickHandler(crate, color));
    }

    private GUI.ClickHandler getColorClickHandler(Crate crate, NamedTextColor color) {
        return new GUI.ClickHandler() {
            @Override
            public void doClick(Player player, GUI gui) {
                GUI.ignoreClosing.add(player.getUniqueId());
                crate.setColor(color);
                player.sendMessage(Component.text(NamedTextColor.NAMES.key(color), color));
                openCrate(player, crate.getName());
            }
        };
    }

    private void confirmDelete(final Player player, final Crate crate) {
        final GUI gui = new GUI("Confirm Delete of \"" + crate.getName(false) + "\"");

        ItemStack crateItem = new ItemStack(crate.getBlock());
        ItemMeta crateMeta = crateItem.getItemMeta();
        crateMeta.displayName(ComponentUtil.legacy(crate.getName()));
        crateItem.setItemMeta(crateMeta);
        gui.setItem(3, crateItem);

        ItemStack cancel = new ItemStack(Material.RED_WOOL);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.displayName(Component.text("Cancel", NamedTextColor.RED));
        cancel.setItemMeta(cancelMeta);
        gui.setItem(16, cancel, new GUI.ClickHandler() {
            @Override
            public void doClick(Player player, GUI gui) {
                GUI.ignoreClosing.add(player.getUniqueId());
                openCrate(player, crate.getName(false));
            }
        });

        ItemStack confirm = new ItemStack(Material.LIME_WOOL);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.displayName(Component.text("Confirm", NamedTextColor.GREEN));
        confirm.setItemMeta(confirmMeta);
        gui.setItem(18, confirm, new GUI.ClickHandler() {
            @Override
            public void doClick(Player player, GUI gui) {
                player.closeInventory();
                cratesPlus.getConfig().set("Crates." + crate.getName(false), null);
                cratesPlus.saveConfig();
                cratesPlus.reloadPlugin();
                MessageHandler.sendLegacy(player, "&aDeleted crate " + crate.getName(false) + ".");
            }
        });

        gui.open(player);
    }

    public HashMap<String, String> getLastCrateEditing() {
        return lastCrateEditing;
    }

}
