package plus.crates.Listeners;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import plus.crates.Crate;
import plus.crates.CratesPlus;
import plus.crates.Events.CrateOpenEvent;
import plus.crates.Events.CratePreviewEvent;

public class PlayerInteract implements Listener {
	private final CratesPlus cratesPlus;
	private final HashMap<String, Long> lastOpended = new HashMap<>();

	public PlayerInteract(CratesPlus cratesPlus) {
		this.cratesPlus = cratesPlus;
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEvent e) {

		Player p = e.getPlayer();

		if (!e.getAction().toString().endsWith("_CLICK_BLOCK")) {
			return;
		}

		Block clickedBlock = e.getClickedBlock();
		ItemStack item = cratesPlus.getVersion_util().getItemInPlayersHand(p);
		ItemStack itemOff = cratesPlus.getVersion_util().getItemInPlayersOffHand(p);

		String crateType;
		if (!clickedBlock.hasMetadata("CrateType") || clickedBlock.getMetadata("CrateType").isEmpty()) {
			// Try to use the old method of getting the crate!
			if (clickedBlock.getType() != Material.CHEST) {
				return;
			}

			Chest chest = (Chest) e.getClickedBlock().getState();
			String title = chest.getCustomName();
			if (title == null || !title.contains(" Crate!")) {
				return;
			}

			crateType = ChatColor.stripColor(title.replaceAll(" Crate!", ""));
		} else {
			crateType = clickedBlock.getMetadata("CrateType").get(0).asString();
		}

		if (!cratesPlus.getConfig().isSet("Crates." + crateType)) {
			return;
		}

		Crate crate = cratesPlus.getConfigHandler().getCrates().get(crateType.toLowerCase());

		if (crate == null) {
			return; // Not sure if we should do some warning here? TODO
		}

		if (crate.getPermission() != null && !p.hasPermission(crate.getPermission())) {
			e.setCancelled(true);
			p.sendMessage(cratesPlus.getPluginPrefix()
					+ cratesPlus.getMessageHandler().getMessage("Crate No Permission", p, crate, null));
			return;
		}
		String title = ChatColor.stripColor(crate.getKey().getName());
		String lore = crate.getKey().getLore().toString();
		if (e.getAction().toString().contains("LEFT")) {
			if (e.getPlayer().isSneaking()) {
				return;
			}
			/** Do preview */
			CratePreviewEvent cratePreviewEvent = new CratePreviewEvent(p, crateType, cratesPlus);
			if (!cratePreviewEvent.isCanceled()) {
				cratePreviewEvent.doEvent();
			}
		} else {

			if (item == null && itemOff == null) {
				p.sendMessage(cratesPlus.getPluginPrefix()
						+ cratesPlus.getMessageHandler().getMessage("Crate Open Without Key", p, crate, null));
				if (crate.getKnockback() != 0) {
					p.setVelocity(p.getLocation().getDirection().multiply(-crate.getKnockback()));
				}
				e.setCancelled(true);
			}
			/** Opening of Crate **/
			boolean usingOffHand = false;
			if (itemOff != null && itemOff.hasItemMeta() && !itemOff.getType().equals(Material.AIR)
					&& itemOff.getItemMeta().getDisplayName() != null
					&& itemOff.getItemMeta().getDisplayName().equals(title)) {
				item = itemOff;
				usingOffHand = true;
			}

			if (cratesPlus.getCrateHandler()
					.hasOpening(p.getUniqueId())) { /** If already opening crate, show GUI for said crate **/
				cratesPlus.getCrateHandler().getOpening(p.getUniqueId()).doReopen(p, crate,
						e.getClickedBlock().getLocation());
				e.setCancelled(true);
				return;
			}

			/** Checks if holding valid key **/

			e.setCancelled(true);

			if (!isKey(item, crate, title, lore)) { // Not valid key
				p.sendMessage(cratesPlus.getPluginPrefix()
						+ cratesPlus.getMessageHandler().getMessage("Crate Open Without Key", p, crate, null));
				if (crate.getKnockback() != 0) {
					p.setVelocity(p.getLocation().getDirection().multiply(-crate.getKnockback()));
				}
				return;
			}

			if (p.getInventory().firstEmpty() == -1) {
				p.sendMessage(cratesPlus.getPluginPrefix() + ChatColor.RED
						+ "You can't open a Crate while your inventory is full");
				return;
			}

			long secondsNow = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());

			if (crate.getCooldown() > 0 && lastOpended.getOrDefault(p.getUniqueId().toString(), 0L)
					+ crate.getCooldown() > secondsNow) {

				long whenCooldownEnds = lastOpended.get(p.getUniqueId().toString())
						+ cratesPlus.getConfigHandler().getDefaultCooldown();
				long remaining = whenCooldownEnds - secondsNow;
				p.sendMessage(cratesPlus.getPluginPrefix() + ChatColor.RED + "You must wait another "
						+ remaining + " seconds before opening another crate");
				return;
			}

			// Store time in seconds of when the player opened the crate
			lastOpended.put(p.getUniqueId().toString(), secondsNow);

			if (item.getAmount() > 1) {
				item.setAmount(item.getAmount() - 1);
			} else {
				if (usingOffHand) {
					cratesPlus.getVersion_util().removeItemInOffHand(p);
				} else {
					p.setItemInHand(null);
				}
			}

			CrateOpenEvent crateOpenEvent = new CrateOpenEvent(p, crateType, e.getClickedBlock().getLocation(),
					cratesPlus);
			crateOpenEvent.doEvent();
		}
	}

	private boolean isKey(ItemStack item, Crate crate, String title, String lore) {
		if (item == null)
			return false;
		if (!item.hasItemMeta())
			return false;

		ItemMeta meta = item.getItemMeta();

		if (!meta.hasDisplayName())
			return false;
		if (!ChatColor.stripColor(meta.getDisplayName()).equals(title))
			return false;

		if (!meta.hasLore())
			return false;
		if (!meta.getLore().toString().equals(lore))
			return false;

		return true;
	}
}
