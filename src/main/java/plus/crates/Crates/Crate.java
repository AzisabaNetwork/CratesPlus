package plus.crates.Crates;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import plus.crates.CratesPlus;
import plus.crates.Handlers.ConfigHandler;
import plus.crates.Opener.Opener;
import plus.crates.Utils.ComponentUtil;
import plus.crates.Utils.MaterialResolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public abstract class Crate {
    protected final ConfigHandler configHandler;
    protected String name;
    protected String slug;
    protected String opener = null;
    protected NamedTextColor color = NamedTextColor.WHITE;
    protected Material block = Material.CHEST;
    protected int blockData = 0;
    protected String permission = null;
    protected ArrayList<Winning> winnings = new ArrayList<>();
    protected double totalPercentage = 0;
    protected boolean firework = false;
    protected boolean broadcast = false;
    protected boolean hidePercentages = false;
    protected Integer cooldown = null;
    /** Number of non-pity rewards allowed before the next pull is guaranteed. Zero disables it. */
    protected int pityLimit = 0;
    protected final List<Winning> pityWinnings = new ArrayList<>();

    public Crate(ConfigHandler configHandler, String name) {
        this.configHandler = configHandler;
        this.name = name;
        this.slug = name.toLowerCase();
        loadCrateBase();
    }

    public double getTotalPercentage() {
        return totalPercentage;
    }

    protected abstract void loadCrate();

    private void loadCrateBase() {
        CratesPlus cratesPlus = configHandler.getCratesPlus();
        if (cratesPlus.getCratesConfig().isSet("Crates." + name + ".Hide Percentages"))
            this.hidePercentages = cratesPlus.getCratesConfig().getBoolean("Crates." + name + ".Hide Percentages");
        if (cratesPlus.getCratesConfig().isSet("Crates." + name + ".Color")) {
            NamedTextColor parsedColor = NamedTextColor.NAMES.value(cratesPlus.getCratesConfig().getString("Crates." + name + ".Color").toLowerCase());
            if (parsedColor == null) {
                cratesPlus.getLogger().warning("Invalid crate color for " + name + "; using white.");
            } else {
                this.color = parsedColor;
            }
        }
        if (cratesPlus.getCratesConfig().isSet("Crates." + name + ".Block"))
            this.block = MaterialResolver.resolve(cratesPlus,
                    cratesPlus.getCratesConfig().getString("Crates." + name + ".Block"), Material.CHEST,
                    "Crates." + name + ".Block");
        if (cratesPlus.getCratesConfig().isSet("Crates." + name + ".Block Data"))
            this.blockData = cratesPlus.getCratesConfig().getInt("Crates." + name + ".Block Data", 0);
        if (cratesPlus.getCratesConfig().isSet("Crates." + name + ".Permission"))
            this.permission = cratesPlus.getCratesConfig().getString("Crates." + name + ".Permission");
        if (cratesPlus.getCratesConfig().isSet("Crates." + name + ".Firework"))
            this.firework = cratesPlus.getCratesConfig().getBoolean("Crates." + name + ".Firework");
        if (cratesPlus.getCratesConfig().isSet("Crates." + name + ".Broadcast"))
            this.broadcast = cratesPlus.getCratesConfig().getBoolean("Crates." + name + ".Broadcast");
        if (cratesPlus.getCratesConfig().isSet("Crates." + name + ".Opener"))
            this.opener = cratesPlus.getCratesConfig().getString("Crates." + name + ".Opener");
        if (cratesPlus.getCratesConfig().isSet("Crates." + name + ".Cooldown"))
            this.cooldown = cratesPlus.getCratesConfig().getInt("Crates." + name + ".Cooldown");

        if (!cratesPlus.getCratesConfig().isSet("Crates." + name + ".Winnings"))
            return;

        if (cratesPlus.getCratesConfig().getConfigurationSection("Crates." + name + ".Winnings") != null) {
            for (String id : cratesPlus.getCratesConfig().getConfigurationSection("Crates." + name + ".Winnings").getKeys(false)) {
                String path = "Crates." + name + ".Winnings." + id;
                Winning winning = new Winning(this, path, cratesPlus, null);
                if (!winning.isValid()) {
                    Bukkit.getLogger().warning(path + " is an invalid winning.");
                    continue;
                }
                totalPercentage = totalPercentage + winning.getPercentage();
                winnings.add(winning);
            }
        }

        loadPity(cratesPlus);
    }

    private void loadPity(CratesPlus cratesPlus) {
        String path = "Crates." + name + ".Pity";
        if (!cratesPlus.getCratesConfig().isConfigurationSection(path)) {
            return;
        }

        pityLimit = cratesPlus.getCratesConfig().getInt(path + ".Limit", 0);
        if (pityLimit < 1) {
            if (pityLimit != 0) {
                cratesPlus.getLogger().warning("Pity.Limit for crate '" + name + "' must be at least 1; pity is disabled.");
            }
            pityLimit = 0;
            return;
        }

        for (Winning winning : winnings) {
            if (winning.isPity()) {
                if (winning.isAlways()) {
                    cratesPlus.getLogger().warning("Winning marked Pity: true in crate '" + name
                            + "' is also Always: true and will be ignored by pity.");
                } else {
                    pityWinnings.add(winning);
                }
            }
        }
        if (pityWinnings.isEmpty()) {
            cratesPlus.getLogger().warning("Pity is configured for crate '" + name + "' but no winning has Pity: true; pity is disabled.");
            pityLimit = 0;
        }
    }

    public CratesPlus getCratesPlus() {
        return configHandler.getCratesPlus();
    }

    public ConfigHandler getConfigHandler() {
        return configHandler;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getName(boolean includecolor) {
        if (includecolor) return ComponentUtil.legacy(Component.text(this.name, getColor()));
        return this.name;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getSlug() {
        return slug;
    }

    public void setColor(NamedTextColor color) {
        this.color = color;
        String path = "Crates." + name + ".Color";
        getCratesPlus().getCratesConfig().set(path, NamedTextColor.NAMES.key(color));
        getCratesPlus().saveCratesConfig();
        getCratesPlus().reloadPlugin();
    }

    public NamedTextColor getColor() {
        return color;
    }

    public void setBlock(Material block) {
        this.block = block;
    }

    public String getOpener() {
        return opener;
    }

    public void setOpener(String opener) {
        this.opener = opener;
    }

    public Material getBlock() {
        return block;
    }

    public int getBlockData() {
        return blockData;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public String getPermission() {
        return permission;
    }

    public boolean isFirework() {
        return firework;
    }

    public boolean isBroadcast() {
        return broadcast;
    }

    public ArrayList<Winning> getWinnings() {
        return winnings;
    }

    public ArrayList<Winning> getWinningsExcludeAlways() {
        ArrayList<Winning> winningsExcludeAlways = new ArrayList<>();
        for (Winning winning : getWinnings()) {
            if (!winning.isAlways())
                winningsExcludeAlways.add(winning);
        }
        return winningsExcludeAlways;
    }

    public boolean containsCommandItem() {
        for (Winning winning : getWinnings()) {
            if (winning.isCommand())
                return true;
        }
        return false;
    }

    public boolean supportsOpener(Opener opener) {
        return opener.doesSupport(this);
    }

    public void giveAll(Integer amount) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            give(player, amount);
        }
    }

    public void giveAllOffline(Integer amount) {
        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
            give(player, amount);
        }
    }

    public abstract boolean give(OfflinePlayer offlinePlayer, Integer amount);

    public Winning handleWin(Player player) {
        return handleWin(player, null);
    }

    public Winning handleWin(Player player, Winning actualWinning) {
        boolean pityGuaranteed = false;
        if (actualWinning == null)
            pityGuaranteed = isPityDue(player);
        if (actualWinning == null)
            actualWinning = pityGuaranteed ? getRandomPityWinning() : getRandomWinning();

        for (Winning winning : getWinnings()) {
            if (winning.isAlways()) {
                ItemStack itemStack = winning.runWin(player);
                if (itemStack != null) {
                    // By default we'll give the item to the player
                    HashMap<Integer, ItemStack> left = player.getInventory().addItem(itemStack);
                    for (Map.Entry<Integer, ItemStack> item : left.entrySet()) {
                        player.getLocation().getWorld().dropItemNaturally(player.getLocation(), item.getValue());
                    }
                }
            }
        }

        ItemStack itemStack = actualWinning.runWin(player);
        if (itemStack != null) {
            HashMap<Integer, ItemStack> left = player.getInventory().addItem(itemStack);
            for (Map.Entry<Integer, ItemStack> item : left.entrySet()) {
                player.getLocation().getWorld().dropItemNaturally(player.getLocation(), item.getValue());
            }
        }
        recordPity(player, actualWinning, pityGuaranteed);
        return actualWinning;
    }

    public Winning getRandomWinning() {
        return getRandomWinning(getWinningsExcludeAlways());
    }

    private Winning getRandomPityWinning() {
        return getRandomWinning(pityWinnings);
    }

    private Winning getRandomWinning(List<Winning> candidates) {
        if (candidates.isEmpty()) {
            throw new IllegalStateException("Crate '" + name + "' has no non-always winnings");
        }

        Winning winning;
        double totalWeight = candidates.stream().mapToDouble(Winning::getPercentage).sum();
        if (totalWeight > 0) {
            double random = ThreadLocalRandom.current().nextDouble(totalWeight);
            for (int i = 0; i < candidates.size(); ++i) {
                random -= candidates.get(i).getPercentage();
                if (random <= 0.0d) {
                    return candidates.get(i);
                }
            }
            // Protect against floating-point rounding at the upper boundary.
            winning = candidates.get(candidates.size() - 1);
        } else {
            winning = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        }
        return winning;
    }

    protected Winning getRandomWinning(Player player) {
        return isPityDue(player) ? getRandomPityWinning() : getRandomWinning();
    }

    protected void recordPity(Player player, Winning winning) {
        recordPity(player, winning, isPityDue(player));
    }

    private void recordPity(Player player, Winning winning, boolean pityGuaranteed) {
        if (!isPityEnabled()) {
            return;
        }
        if (pityWinnings.contains(winning)) {
            getCratesPlus().getStorageHandler().setPityCount(player.getUniqueId(), name, 0);
            if (pityGuaranteed) {
                plus.crates.Handlers.MessageHandler.sendMessage(player, "crate.pity_guaranteed", this, winning);
            }
            return;
        }

        int nextCount = Math.min(pityLimit - 1,
                getCratesPlus().getStorageHandler().getPityCount(player.getUniqueId(), name) + 1);
        getCratesPlus().getStorageHandler().setPityCount(player.getUniqueId(), name, nextCount);
    }

    private boolean isPityDue(Player player) {
        return isPityEnabled()
                && getCratesPlus().getStorageHandler().getPityCount(player.getUniqueId(), name) >= pityLimit - 1;
    }

    private boolean isPityEnabled() {
        return pityLimit > 0 && !pityWinnings.isEmpty();
    }

    public int getPityLimit() {
        return pityLimit;
    }

    public boolean isHidePercentages() {
        return hidePercentages;
    }

    public void onDisable() {

    }

}
