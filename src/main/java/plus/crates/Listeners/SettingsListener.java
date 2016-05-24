package plus.crates.Listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import plus.crates.Crate;
import plus.crates.CratesPlus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SettingsListener implements Listener {
	private CratesPlus cratesPlus;

	public SettingsListener(CratesPlus cratesPlus) {
		this.cratesPlus = cratesPlus;
	}

	@EventHandler
	public void onInventoryClose(InventoryCloseEvent event) {
		if (!(event.getPlayer() instanceof Player)) return;
		if (event.getInventory().getTitle() != null && event.getInventory().getTitle().contains("Crate Winnings")) {
			String crateName = ChatColor.stripColor(event.getInventory().getTitle().replaceAll("Edit ", "").replaceAll(" Crate Winnings", ""));
			Crate crate = cratesPlus.getConfigHandler().getCrates().get(crateName.toLowerCase());
			if (crate == null) {
				return;
			}

			cratesPlus.getConfig().set("Crates." + crateName + ".Winnings", null);
			cratesPlus.saveConfig();
			for (ItemStack itemStack : event.getInventory().getContents()) {
				if (itemStack == null)
					continue;
				int id = getFreeID(crateName, 1);

				String type = "ITEM";
				String itemtype = itemStack.getType().toString().toUpperCase();
				Byte itemData = itemStack.getData().getData();
				String name = "NONE";
				if (itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName())
					name = itemStack.getItemMeta().getDisplayName();
				Integer amount = itemStack.getAmount();
				List<String> enchantments = new ArrayList<String>();
				if (itemStack.getEnchantments() != null && !itemStack.getEnchantments().isEmpty()) {
					for (Map.Entry<Enchantment, Integer> entry : itemStack.getEnchantments().entrySet()) {
						Enchantment enchantment = entry.getKey();
						Integer level = entry.getValue();
						enchantments.add(enchantment.getName().toUpperCase() + "-" + level);
					}
				}


				List<String> lore = new ArrayList<String>();
				if (itemStack.hasItemMeta() && itemStack.getItemMeta().hasLore())
					lore = itemStack.getItemMeta().getLore();

				// Save to config and creating winning instance
				FileConfiguration config = cratesPlus.getConfig();
				String path = "Crates." + crateName + ".Winnings." + id;
				config.set(path + ".Type", type);
				config.set(path + ".Item Type", itemtype);
				config.set(path + ".Item Data", itemData);
				config.set(path + ".Name", name);
				config.set(path + ".Amount", amount);
				config.set(path + ".Enchantments", enchantments);
				config.set(path + ".Lore", lore);
			}

			cratesPlus.saveConfig();
			crate.reloadWinnings();
			event.getPlayer().sendMessage(cratesPlus.getPluginPrefix() + ChatColor.GREEN + "Crate winnings updated");
		}
	}

	private int getFreeID(String crate, int check) {
		if (cratesPlus.getConfig().isSet("Crates." + crate + ".Winnings." + check))
			return getFreeID(crate, check + 1);
		return check;
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		ItemStack itemStack = event.getCurrentItem();
		Player player = (Player) event.getWhoClicked();

		if (event.getInventory().getTitle() == null)
			return;


		if (event.getInventory().getTitle().contains("CratesPlus Settings")) {

			if (itemStack == null || itemStack.getType() == Material.AIR || !event.getCurrentItem().hasItemMeta() || !event.getCurrentItem().getItemMeta().hasDisplayName()) {
				event.setCancelled(true);
				return;
			}

			if (event.getCurrentItem().getItemMeta().getDisplayName().equals(ChatColor.GREEN + "Edit Crates")) {
				event.setCancelled(true);
				player.closeInventory();
				cratesPlus.getSettingsHandler().openCrates(player);
				return;
			}

			if (event.getCurrentItem().getItemMeta().getDisplayName().equals(ChatColor.GREEN + "Reload Config")) {
				event.setCancelled(true);
				player.closeInventory();
				cratesPlus.reloadPlugin();
				player.sendMessage(cratesPlus.getPluginPrefix() + ChatColor.GREEN + "CratesPlus configuration was reloaded - This feature is not fully tested and may not work correctly");
				return;
			}

			if (event.getCurrentItem().getItemMeta().getDisplayName().contains(ChatColor.RED + "")) {
				event.setCancelled(true);
				player.closeInventory();
				player.sendMessage(ChatColor.RED + "Coming Soon");
			}

		} else if (event.getInventory().getTitle().contains("Crates")) {

			if (itemStack == null || itemStack.getType() == Material.AIR || !event.getCurrentItem().hasItemMeta() || !event.getCurrentItem().getItemMeta().hasDisplayName()) {
				event.setCancelled(true);
				return;
			}

			if (event.getCurrentItem().getType() == Material.CHEST) {
				player.closeInventory();
				cratesPlus.getSettingsHandler().openCrate(player, ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()));
			}

		} else if (event.getInventory().getTitle().contains("Edit Crate Color")) {
			event.setCancelled(true);
			if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.WOOL) {
				player.closeInventory();
				ChatColor color = ChatColor.valueOf(ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName().toUpperCase().replaceAll(" ", "_")));
				if (color != null) {
					String lastCrate = cratesPlus.getSettingsHandler().getLastCrateEditing().get(player.getUniqueId().toString());
					if (lastCrate == null)
						return;
					Crate crate = cratesPlus.getConfigHandler().getCrates().get(lastCrate.toLowerCase());
					crate.setColor(ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName().toUpperCase().replaceAll(" ", "_")));
					player.sendMessage(ChatColor.GREEN + "Updated color, you may need to replace the crate for colors to update in holograms");
				}
			}
		} else if (event.getInventory().getTitle().contains("Edit ") && !event.getInventory().getTitle().contains("Winnings") && !event.getInventory().getTitle().contains("Color")) {
			if (itemStack == null || itemStack.getType() == Material.AIR || !event.getCurrentItem().hasItemMeta() || !event.getCurrentItem().getItemMeta().hasDisplayName()) {
				event.setCancelled(true);
				return;
			}

			if (event.getCurrentItem().getItemMeta().getDisplayName().contains("Edit Crate Winnings")) {
				event.setCancelled(true);
				String name = ChatColor.stripColor(event.getInventory().getTitle().replaceAll("Edit ", "").replaceAll(" Crate", ""));
				cratesPlus.getSettingsHandler().openCrateWinnings(player, name);
			} else if (event.getCurrentItem().getItemMeta().getDisplayName().contains("Delete")) {
				event.setCancelled(true);
				String name = ChatColor.stripColor(event.getInventory().getTitle().replaceAll("Edit ", "").replaceAll(" Crate", ""));
				cratesPlus.getConfig().set("Crates." + name, null);
				cratesPlus.saveConfig();
				cratesPlus.reloadConfig();
				cratesPlus.getConfigHandler().getCrates().remove(name.toLowerCase());
				cratesPlus.getSettingsHandler().setupCratesInventory();
				player.sendMessage(cratesPlus.getPluginPrefix() + ChatColor.GREEN + name + " crate has been deleted");
			} else if (event.getCurrentItem().getItemMeta().getDisplayName().contains("Rename Crate")) {
				event.setCancelled(true);
			} else if (event.getCurrentItem().getItemMeta().getDisplayName().contains("Edit Crate Color")) {
				event.setCancelled(true);
				Inventory inventory = Bukkit.createInventory(null, 18, "Edit Crate Color");

				ItemStack aqua = new ItemStack(Material.WOOL, 1, (short) 3);
				ItemMeta aquaMeta = aqua.getItemMeta();
				aquaMeta.setDisplayName(ChatColor.AQUA + "Aqua");
				aqua.setItemMeta(aquaMeta);
				inventory.addItem(aqua);

				ItemStack black = new ItemStack(Material.WOOL, 1, (short) 15);
				ItemMeta blackMeta = black.getItemMeta();
				blackMeta.setDisplayName(ChatColor.BLACK + "Black");
				black.setItemMeta(blackMeta);
				inventory.addItem(black);

				ItemStack blue = new ItemStack(Material.WOOL, 1, (short) 9);
				ItemMeta blueMeta = blue.getItemMeta();
				blueMeta.setDisplayName(ChatColor.BLUE + "Blue");
				blue.setItemMeta(blueMeta);
				inventory.addItem(blue);

				ItemStack darkAqua = new ItemStack(Material.WOOL, 1, (short) 3);
				ItemMeta darkAquaMeta = darkAqua.getItemMeta();
				darkAquaMeta.setDisplayName(ChatColor.DARK_AQUA + "Dark Aqua");
				darkAqua.setItemMeta(darkAquaMeta);
				inventory.addItem(darkAqua);

				ItemStack darkBlue = new ItemStack(Material.WOOL, 1, (short) 11);
				ItemMeta darkBlueMeta = darkBlue.getItemMeta();
				darkBlueMeta.setDisplayName(ChatColor.DARK_BLUE + "Dark Blue");
				darkBlue.setItemMeta(darkBlueMeta);
				inventory.addItem(darkBlue);

				ItemStack darkGray = new ItemStack(Material.WOOL, 1, (short) 7);
				ItemMeta darkGrayMeta = darkGray.getItemMeta();
				darkGrayMeta.setDisplayName(ChatColor.DARK_GRAY + "Dark Gray");
				darkGray.setItemMeta(darkGrayMeta);
				inventory.addItem(darkGray);

				ItemStack darkGreen = new ItemStack(Material.WOOL, 1, (short) 13);
				ItemMeta darkGreenMeta = darkGreen.getItemMeta();
				darkGreenMeta.setDisplayName(ChatColor.DARK_GREEN + "Dark Green");
				darkGreen.setItemMeta(darkGreenMeta);
				inventory.addItem(darkGreen);

				ItemStack darkPurple = new ItemStack(Material.WOOL, 1, (short) 10);
				ItemMeta darkPurpleMeta = darkPurple.getItemMeta();
				darkPurpleMeta.setDisplayName(ChatColor.DARK_PURPLE + "Dark Purple");
				darkPurple.setItemMeta(darkPurpleMeta);
				inventory.addItem(darkPurple);

				ItemStack darkRed = new ItemStack(Material.WOOL, 1, (short) 14);
				ItemMeta darkRedMeta = darkRed.getItemMeta();
				darkRedMeta.setDisplayName(ChatColor.DARK_RED + "Dark Red");
				darkRed.setItemMeta(darkRedMeta);
				inventory.addItem(darkRed);

				ItemStack glass = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 8);
				ItemMeta glassMeta = glass.getItemMeta();
				glassMeta.setDisplayName(ChatColor.GOLD + "");
				glass.setItemMeta(glassMeta);
				inventory.addItem(glass);

				ItemStack gold = new ItemStack(Material.WOOL, 1, (short) 1);
				ItemMeta goldMeta = gold.getItemMeta();
				goldMeta.setDisplayName(ChatColor.GOLD + "Gold");
				gold.setItemMeta(goldMeta);
				inventory.addItem(gold);

				ItemStack gray = new ItemStack(Material.WOOL, 1, (short) 8);
				ItemMeta grayMeta = gray.getItemMeta();
				grayMeta.setDisplayName(ChatColor.GRAY + "Gray");
				gray.setItemMeta(grayMeta);
				inventory.addItem(gray);

				ItemStack green = new ItemStack(Material.WOOL, 1, (short) 5);
				ItemMeta greenMeta = gray.getItemMeta();
				greenMeta.setDisplayName(ChatColor.GREEN + "Green");
				green.setItemMeta(greenMeta);
				inventory.addItem(green);

				ItemStack lightPurple = new ItemStack(Material.WOOL, 1, (short) 2);
				ItemMeta lightPurpleMeta = lightPurple.getItemMeta();
				lightPurpleMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "Light Purple");
				lightPurple.setItemMeta(lightPurpleMeta);
				inventory.addItem(lightPurple);

				ItemStack red = new ItemStack(Material.WOOL, 1, (short) 14);
				ItemMeta redMeta = red.getItemMeta();
				redMeta.setDisplayName(ChatColor.RED + "Red");
				red.setItemMeta(redMeta);
				inventory.addItem(red);

				ItemStack white = new ItemStack(Material.WOOL, 1, (short) 0);
				ItemMeta whiteMeta = white.getItemMeta();
				whiteMeta.setDisplayName(ChatColor.WHITE + "White");
				white.setItemMeta(whiteMeta);
				inventory.addItem(white);

				ItemStack yellow = new ItemStack(Material.WOOL, 1, (short) 4);
				ItemMeta yellowMeta = yellow.getItemMeta();
				yellowMeta.setDisplayName(ChatColor.YELLOW + "Yellow");
				yellow.setItemMeta(yellowMeta);
				inventory.addItem(yellow);

				glass = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 8);
				glassMeta = glass.getItemMeta();
				glassMeta.setDisplayName(ChatColor.GOLD + "");
				glass.setItemMeta(glassMeta);
				inventory.setItem(17, glass);

				String name = ChatColor.stripColor(event.getInventory().getTitle().replaceAll("Edit ", "").replaceAll(" Crate", ""));
				cratesPlus.getSettingsHandler().getLastCrateEditing().put(player.getUniqueId().toString(), name);

				player.openInventory(inventory);
			}

		}

	}

}
