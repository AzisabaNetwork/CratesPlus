package plus.crates.Commands;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import plus.crates.Crates.Crate;
import plus.crates.Crates.KeyCrate;
import plus.crates.Crates.MysteryCrate;
import plus.crates.CratesPlus;
import plus.crates.Handlers.LegacyMigrationService;
import plus.crates.Handlers.MessageHandler;
import plus.crates.Opener.Opener;
import plus.crates.Utils.GUI;
import plus.crates.Utils.LinfootUtil;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class CrateCommand implements CommandExecutor {
    private final CratesPlus cratesPlus;

    public CrateCommand(CratesPlus cratesPlus) {
        this.cratesPlus = cratesPlus;
    }

    @Override
    public boolean onCommand(final CommandSender sender, Command command, String string, String[] args) {

        if (sender instanceof Player && !sender.hasPermission("cratesplus.admin")) {
            if (args.length == 0 || (args.length > 0 && args[0].equalsIgnoreCase("claim"))) {
                // Assume player and show "claim" GUI
                doClaim((Player) sender);
                return true;
            }
            MessageHandler.sendMessage((Player) sender, "command.no_permission", null, null);
            return false;
        }

        if (args.length >= 1) {
            switch (args[0].toLowerCase()) {
                default:
                    message(sender, cratesPlus.getPluginPrefix() + "&cUnknown arg");
                    break;
                case "testmessages":
                    MessageHandler.testMessages = !MessageHandler.testMessages;
                    message(sender, "&aTest Messages " + (MessageHandler.testMessages ? "ENABLED" : "DISABLED"));
                    break;
                case "testeggs":
                    Player player = null;
                    if (sender instanceof Player)
                        player = (Player) sender;

                    message(sender, "&bCreating creeper egg...");
                    ItemStack itemStack = cratesPlus.getVersion_util().getSpawnEgg(EntityType.CREEPER, 1);
                    message(sender, "&bTesting creeper egg...");
                    if (EntityType.CREEPER.equals(cratesPlus.getVersion_util().getEntityTypeFromItemStack(itemStack))) {
                        message(sender, "&aCreeper egg successful");
                        if (player != null)
                            player.getInventory().addItem(itemStack);
                    } else {
                        message(sender, "&cCreeper egg failed, please post console on GitHub");
                    }

                    message(sender, "&bCreating spider egg...");
                    itemStack = cratesPlus.getVersion_util().getSpawnEgg(EntityType.SPIDER, 2);
                    message(sender, "&bTesting spider egg...");
                    if (EntityType.SPIDER.equals(cratesPlus.getVersion_util().getEntityTypeFromItemStack(itemStack))) {
                        message(sender, "&aSpider egg successful");
                        if (player != null)
                            player.getInventory().addItem(itemStack);
                    } else {
                        message(sender, "&cSpider egg failed, please post console on GitHub");
                    }

                    message(sender, "&bCreating silverfish egg...");
                    itemStack = cratesPlus.getVersion_util().getSpawnEgg(EntityType.SILVERFISH, 3);
                    message(sender, "&bTesting silverfish egg...");
                    if (EntityType.SILVERFISH.equals(cratesPlus.getVersion_util().getEntityTypeFromItemStack(itemStack))) {
                        message(sender, "&aSilverfish egg successful");
                        if (player != null)
                            player.getInventory().addItem(itemStack);
                    } else {
                        message(sender, "&cSilverfish egg failed, please post console on GitHub");
                    }
                    break;
                case "claim":
                    if (sender instanceof Player) {
                        doClaim((Player) sender);
                    }
                    break;
                /* Debug export intentionally removed: it transmitted server configuration and data to a third party.
                case "debug":
                    message(sender, "&bGathering debug data...");
                    String plugins = "";
                    for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
                        plugins += plugin.getName() + " - Version: " + plugin.getDescription().getVersion() + "\n";
                    }
                    final String pluginList = plugins;

                    Bukkit.getScheduler().runTaskAsynchronously(cratesPlus, () -> {
                        sendOnPrimaryThread(sender, "&bUploading config.yml...");
                        String configLink = cratesPlus.uploadConfig();
                        sendOnPrimaryThread(sender, "&bUploaded config.yml");

                        sendOnPrimaryThread(sender, "&bUploading data.yml...");
                        String dataLink = cratesPlus.uploadData();
                        sendOnPrimaryThread(sender, "&bUploaded data.yml");

                        sendOnPrimaryThread(sender, "&bUploading messages.yml...");
                        String messagesLink = cratesPlus.uploadMessages();
                        sendOnPrimaryThread(sender, "&bUploaded messages.yml");

                        sendOnPrimaryThread(sender, "&bUploading plugin list...");

                        String pluginsLink = MCDebug.paste("plugins.txt", pluginList);
                        sendOnPrimaryThread(sender, "&bUploaded plugin list");

                        sendOnPrimaryThread(sender, "&bUploading data to MC Debug...");
                        String finalLinks = uploadDebugData(configLink, dataLink, messagesLink, pluginsLink);
                        String[] links = null;
                        if (finalLinks != null) {
                            links = finalLinks.split("\\|");
                        }

                        sendOnPrimaryThread(sender, "&aCompleted uploading debug data!");
                        if (links != null && links.length == 2) {
                            sendOnPrimaryThread(sender, "&aYou can use the following link to manage your data &6" + links[1]);
                            sendOnPrimaryThread(sender, "&aYou can use the following link to share your data &6" + links[0]);
                        } else {
                            sendOnPrimaryThread(sender, "&aYou can use the following link to share your data &6" + finalLinks);
                        }

                    });
                    break; */
                case "opener":
                case "openers":
                    if (args.length > 1) {
                        if (args.length < 3) {
                            message(sender, cratesPlus.getPluginPrefix() + "&cCorrect usage: /" + string + " " + args[0] + " <crate> <opener>");
                        } else {
                            if (CratesPlus.getOpenHandler().openerExist(args[2])) {
                                Opener opener = CratesPlus.getOpenHandler().getOpener(args[2]);
                                if (cratesPlus.getConfigHandler().getCrate(args[1].toLowerCase()) == null) {
                                    message(sender, cratesPlus.getPluginPrefix() + "&cNo crate exists with that name");
                                } else if (!cratesPlus.getConfigHandler().getCrate(args[1].toLowerCase()).supportsOpener(opener)) {
                                    message(sender, cratesPlus.getPluginPrefix() + "&cOpener does not support crate type");
                                } else {
//									cratesPlus.getConfigHandler().getCrate(args[1].toLowerCase()).setOpener(args[2]);
                                    message(sender, cratesPlus.getPluginPrefix() + "&aSet opener to " + args[2]);
                                }
                            } else {
                                message(sender, cratesPlus.getPluginPrefix() + "&cNo opener is registered with that name");
                            }
                        }

                    } else {
                        message(sender, "&6Registered Openers:");
                        message(sender, "&bName&7 | &ePlugin");
                        message(sender, "&b");
                        for (Map.Entry<String, Opener> map : CratesPlus.getOpenHandler().getRegistered().entrySet()) {
                            message(sender, "&b" + map.getKey() + "&7 | &e" + map.getValue().getPlugin().getDescription().getName());
                        }
                    }
                    break;
                case "reload":
                    cratesPlus.reloadPlugin();
                    message(sender, cratesPlus.getPluginPrefix() + "&aCratesPlus was reloaded.");
                    break;
                case "migratelegacy":
                    if (args.length < 2 || args[1].equalsIgnoreCase("report")) {
                        sendMigrationReport(sender, cratesPlus.getLegacyMigrationService().inspect(false), false);
                    } else if (args[1].equalsIgnoreCase("apply")) {
                        sendMigrationReport(sender, cratesPlus.getLegacyMigrationService().inspect(true), true);
                    } else if (args[1].equalsIgnoreCase("keys")) {
                        Player target;
                        if (args.length >= 3) {
                            target = Bukkit.getPlayer(args[2]);
                        } else if (sender instanceof Player) {
                            target = (Player) sender;
                        } else {
                            message(sender, "&cUsage: /crate migratelegacy keys <online-player>");
                            return false;
                        }
                        if (target == null) {
                            message(sender, "&cThat player must be online to migrate keys.");
                            return false;
                        }
                        int migrated = cratesPlus.getLegacyMigrationService().migrateLegacyKeys(target);
                        message(sender, "&aMigrated " + migrated + " legacy key(s) for " + target.getName() + ".");
                    } else {
                        message(sender, "&cUsage: /crate migratelegacy [report|apply|keys <online-player>]");
                        return false;
                    }
                    break;
                case "settings":
                    if (!(sender instanceof Player)) {
                        message(sender, cratesPlus.getPluginPrefix() + "&cThis command must be ran as a player");
                        return false;
                    }
                    cratesPlus.getSettingsHandler().openSettings((Player) sender);
                    break;
                case "create":
                    // TODO Handle different crate types lol, default is KeyCrate for now
                    if (sender instanceof Player && args.length < 2) {
                        final Player creatingPlayer = (Player) sender;
                        cratesPlus.addCreating(creatingPlayer.getUniqueId());
                        cratesPlus.getTextInputHandler().request(creatingPlayer, "Create crate", name -> {
                            cratesPlus.removeCreating(creatingPlayer.getUniqueId());
                            if (!name.isBlank()) {
                                Bukkit.dispatchCommand(creatingPlayer, "crate create " + name.trim());
                            }
                        });
                        return true;
                    }

                    if (args.length < 2) {
                        message(sender, cratesPlus.getPluginPrefix() + "&cCorrect Usage: /crate create <name>");
                        return false;
                    }

                    String name = args[1];
                    FileConfiguration config = cratesPlus.getConfig();
                    if (config.isSet("Crates." + name)) {
                        message(sender, cratesPlus.getPluginPrefix() + "&c" + name + " crate already exists");
                        return false;
                    }

                    // Setup example item
                    config.set("Crates." + name + ".Winnings.1.Type", "ITEM");
                    config.set("Crates." + name + ".Winnings.1.Item Type", "IRON_SWORD");
                    config.set("Crates." + name + ".Winnings.1.Item Data", 0);
                    config.set("Crates." + name + ".Winnings.1.Percentage", 0);
                    config.set("Crates." + name + ".Winnings.1.Name", "&6&lExample Sword");
                    config.set("Crates." + name + ".Winnings.1.Amount", 1);

                    // Setup key with defaults
                    config.set("Crates." + name + ".Key.Item", "TRIPWIRE_HOOK");
                    config.set("Crates." + name + ".Key.Name", "%type% Crate Key");
                    config.set("Crates." + name + ".Key.Enchanted", true);

                    config.set("Crates." + name + ".Knockback", 0.0);
                    config.set("Crates." + name + ".Broadcast", false);
                    config.set("Crates." + name + ".Firework", false);
                    config.set("Crates." + name + ".Preview", true);
                    config.set("Crates." + name + ".Block", "CHEST");
                    config.set("Crates." + name + ".Color", "WHITE");
                    config.set("Crates." + name + ".Type", "KeyCrate");
                    cratesPlus.saveConfig();
                    cratesPlus.reloadPlugin();

                    message(sender, cratesPlus.getPluginPrefix() + "&a" + name + " crate has been created");
                    break;
                case "rename":
                    if (args.length < 3) {
                        message(sender, cratesPlus.getPluginPrefix() + "&cCorrect Usage: /crate rename <old name> <new name>");
                        return false;
                    }

                    String oldName = args[1];
                    String newName = args[2];

                    if (!cratesPlus.getConfigHandler().getCrates().containsKey(oldName.toLowerCase())) {
                        message(sender, cratesPlus.getPluginPrefix() + "&c" + oldName + " crate was not found");
                        return false;
                    }
                    Crate crate = cratesPlus.getConfigHandler().getCrates().get(oldName.toLowerCase());

                    config = cratesPlus.getConfig();
                    if (config.isSet("Crates." + newName)) {
                        message(sender, cratesPlus.getPluginPrefix() + "&c" + newName + " crate already exists");
                        return false;
                    }

                    LinfootUtil.copyConfigSection(config, "Crates." + crate.getName(), "Crates." + newName);

                    config.set("Crates." + crate.getName(), null);
                    cratesPlus.saveConfig();
                    cratesPlus.reloadPlugin();

                    message(sender, cratesPlus.getPluginPrefix() + "&a" + oldName + " has been renamed to " + newName);
                    break;
                case "delete":
                    if (args.length < 2) {
                        message(sender, cratesPlus.getPluginPrefix() + "&cCorrect Usage: /crate delete <name>");
                        return false;
                    }

                    name = args[1];
                    config = cratesPlus.getConfig();
                    if (!config.isSet("Crates." + name)) {
                        message(sender, cratesPlus.getPluginPrefix() + "&c" + name + " crate doesn't exist");
                        return false;
                    }

                    config.set("Crates." + name, null);
                    cratesPlus.saveConfig();
                    cratesPlus.reloadPlugin();

                    message(sender, cratesPlus.getPluginPrefix() + "&a" + name + " crate has been deleted");
                    break;
                case "mysterygui":
                    if (args.length < 2) {
                        message(sender, cratesPlus.getPluginPrefix() + "&cCorrect Usage: /crate mysterygui <crate>");
                        return false;
                    }

                    String crateType = args[1];

                    crate = cratesPlus.getConfigHandler().getCrates().get(crateType.toLowerCase());
                    if (crate == null) {
                        message(sender, cratesPlus.getPluginPrefix() + "&cCrate not found");
                        return false;
                    }

                    if (!(crate instanceof MysteryCrate) || !(sender instanceof Player)) { // Too lazy to do separate messages
                        message(sender, cratesPlus.getPluginPrefix() + "&cCrate is not a Mystery Crate!");
                        return false;
                    }

                    ((MysteryCrate) crate).openGUI((Player) sender);
                    break;
                case "key":
                    cratesPlus.getLogger().warning("\"/crate key\" was used but is deprecated from version 5, please use \"give\" instead.");
                    if (sender instanceof Player) {
                        message(sender, "&e\"/crate key\" was used but is deprecated from version 5; please use \"give\" instead.");
                    }
                case "give":
                    if (args.length < 3) {
                        message(sender, cratesPlus.getPluginPrefix() + "&cCorrect Usage: /crate give <player/all/alloffline> <crate> [amount]");
                        return false;
                    }

                    Integer amount = 1;
                    if (args.length > 3) {
                        try {
                            amount = Integer.parseInt(args[3]);
                        } catch (Exception ignored) {
                            message(sender, cratesPlus.getPluginPrefix() + "&cInvalid amount");
                            return false;
                        }
                    }

                    OfflinePlayer offlinePlayer = null;
                    if (!args[1].equalsIgnoreCase("all") && !args[1].equalsIgnoreCase("alloffline")) {
                        offlinePlayer = Bukkit.getOfflinePlayer(args[1]);
                        if (offlinePlayer == null || (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline())) { // Check if the player is online as "hasPlayedBefore" doesn't work until they disconnect?
                            message(sender, cratesPlus.getPluginPrefix() + "&cThe player " + args[1] + " was not found");
                            return false;
                        }
                    }

                    crateType = args[2];

                    crate = cratesPlus.getConfigHandler().getCrates().get(crateType.toLowerCase());
                    if (crate == null) {
                        message(sender, cratesPlus.getPluginPrefix() + "&cCrate not found");
                        return false;
                    }

                    if (offlinePlayer == null) {
                        if (args[1].equalsIgnoreCase("all")) {
                            crate.giveAll(amount);
                            message(sender, cratesPlus.getPluginPrefix() + "&aGiven all online players a crate/key");
                        } else if (args[1].equalsIgnoreCase("alloffline")) {
                            /**
                             * TODO TEST THIS and maybe give better explanation when they do `/crate give`?
                             */
                            crate.giveAllOffline(amount);
                            message(sender, cratesPlus.getPluginPrefix() + "&aGiven all online and offline players a crate/key");
                        }
                    } else {
                        if (crate.give(offlinePlayer, amount))
                            message(sender, cratesPlus.getPluginPrefix() + "&aGiven " + offlinePlayer.getName() + " a crate/key");
                        else
                            message(sender, cratesPlus.getPluginPrefix() + "&cFailed to give crate/key");
                    }

                    break;
                case "crate":
                case "keycrate":
                    if (args.length == 1) {
                        message(sender, cratesPlus.getPluginPrefix() + "&cCorrect Usage: /crate crate <type> [player]");
                        return false;
                    }

                    if (args.length == 3) {
                        player = Bukkit.getPlayer(args[2]);
                    } else if (sender instanceof Player) {
                        player = (Player) sender;
                    } else {
                        message(sender, cratesPlus.getPluginPrefix() + "&cCorrect Usage: /crate crate <type> [player]");
                        return false;
                    }

                    if (player == null) {
                        message(sender, cratesPlus.getPluginPrefix() + "&cThe player " + args[2] + " was not found");
                        return false;
                    }

                    try {
                        crateType = args[1];
                    } catch (IllegalArgumentException e) {
                        message(sender, cratesPlus.getPluginPrefix() + "&cPlease specify a valid crate type");
                        return false;
                    }

                    if (cratesPlus.getConfigHandler().getCrates().get(crateType.toLowerCase()) == null || !(cratesPlus.getConfigHandler().getCrates().get(crateType.toLowerCase()) instanceof KeyCrate)) {
                        message(sender, cratesPlus.getPluginPrefix() + "&cKeyCrate not found");
                        return false;
                    }

                    cratesPlus.getCrateHandler().giveCrate(player,
                            cratesPlus.getConfigHandler().getCrates().get(crateType.toLowerCase()));

                    message(sender, cratesPlus.getPluginPrefix() + "&aGiven " + player.getName() + " a crate");
                    break;
            }
        } else {

            // Help Messages
            message(sender, cratesPlus.getPluginPrefix() + "&b----- CratePlus v" + cratesPlus.getPluginMeta().getVersion() + " Help -----");
            message(sender, cratesPlus.getPluginPrefix() + "&b/crate reload &eReload configuration for CratesPlus");
            message(sender, cratesPlus.getPluginPrefix() + "&b/crate create <name> &eCreate a new crate");
            message(sender, cratesPlus.getPluginPrefix() + "&b/crate rename <old name> <new name> &eRename a crate");
            message(sender, cratesPlus.getPluginPrefix() + "&b/crate delete <name> &eDelete a crate");
            message(sender, cratesPlus.getPluginPrefix() + "&b/crate give <player/all> [crate] [amount] &eGive player a crate/key, if no crate given it will be random");
            message(sender, cratesPlus.getPluginPrefix() + "&b/crate crate <type> [player] &eGive player a crate to be placed, for use by admins");
            message(sender, cratesPlus.getPluginPrefix() + "&b/crate debug &eGenerates a debug link for sending info about your server and config");


        }

        return true;
    }

    private void sendMigrationReport(CommandSender sender, LegacyMigrationService.Report report, boolean applied) {
        message(sender, "&b" + report.summary(applied));
        for (String warning : report.warnings()) {
            message(sender, "&e - " + warning);
        }
        if (!applied) {
            message(sender, "&7Run /crate migratelegacy apply after reviewing this report.");
        }
    }

    private void sendOnPrimaryThread(CommandSender sender, String message) {
        Bukkit.getScheduler().runTask(cratesPlus, () -> MessageHandler.sendLegacy(sender, message));
    }

    private void message(CommandSender sender, String message) {
        MessageHandler.sendLegacy(sender, message);
    }

    private void doClaim(Player player) {
        if (!cratesPlus.getCrateHandler().hasPendingKeys(player.getUniqueId())) {
            player.closeInventory();
            message(player, "&cYou currently don't have any keys to claim");
            return;
        }
        GUI gui = new GUI("Claim Crate Keys");
        gui.setAllowsKeyMovement(true);
        Integer i = 0;
        for (Map.Entry<String, Integer> map : cratesPlus.getCrateHandler().getPendingKey(player.getUniqueId()).entrySet()) {
            final String crateName = map.getKey();
            final KeyCrate crate = (KeyCrate) cratesPlus.getConfigHandler().getCrates().get(crateName.toLowerCase());
            if (crate == null)
                return; // Crate must have been removed?
            ItemStack keyItem = crate.getKey().getKeyItem(1);
            if (map.getValue() > 1) {
                ItemMeta itemMeta = keyItem.getItemMeta();
                itemMeta.displayName(itemMeta.displayName().append(Component.text(" x" + map.getValue())));
                keyItem.setItemMeta(itemMeta);
            }
            gui.setItem(i, keyItem, new GUI.ClickHandler() {
                @Override
                public void doClick(Player player, GUI gui) {
                    cratesPlus.getCrateHandler().claimKey(player.getUniqueId(), crateName);
                    if (cratesPlus.getCrateHandler().hasPendingKeys(player.getUniqueId())) {
                        GUI.ignoreClosing.add(player.getUniqueId());
                        doClaim(player);
                    } else {
                        player.closeInventory();
                    }
                }
            });
            i++;
        }
        gui.open(player);
    }

    private String uploadDebugData(String configLink, String dataLink, String messagesLink, String pluginsLink) {
        String urlStr = "http://mcdebug.xyz/api/v2/submit/?plugin=cratesplus&config=" + configLink + "&data=" + dataLink + "&messages=" + messagesLink + "&plugins=" + pluginsLink + "&bukkitVer=" + cratesPlus.getBukkitVersion();

        HttpURLConnection connection;
        try {
            //Create connection
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
//			connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setDoOutput(true);

            //Send request
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.flush();
            wr.close();

            //Get Response
            BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            JSONParser jsonParser = new JSONParser();
            JSONObject obj = (JSONObject) jsonParser.parse(rd.readLine());
            return "https://mcdebug.xyz/cratesplus/share/" + obj.get("id") + "|" + "https://mcdebug.xyz/cratesplus/admin/" + obj.get("adminid");
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

}
