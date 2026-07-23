package plus.crates.Utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Connor Linfoot's GUI Library
 *
 * @version 4.0
 */
public class GUI {
    public static HashMap<UUID, GUI> guis = new HashMap<>();
    public static HashMap<UUID, Integer> pageTracker = new HashMap<>();
    public static ArrayList<UUID> ignoreClosing = new ArrayList<>();
    private static final HashMap<Integer, Integer> itemMapping = new HashMap<Integer, Integer>() {{
        put(0, 10);
        put(1, 11);
        put(2, 12);
        put(3, 13);
        put(4, 14);
        put(5, 15);
        put(6, 16);
        put(7, 19);
        put(8, 20);
        put(9, 21);
        put(10, 22);
        put(11, 23);
        put(12, 24);
        put(13, 25);
        put(14, 28);
        put(15, 29);
        put(16, 30);
        put(17, 31);
        put(18, 32);
        put(19, 33);
        put(20, 34);
    }};

    private Component title;
    private NavigableMap<Integer, ItemStack> items = new TreeMap<>();
    private HashMap<Integer, ClickHandler> clickHandlers = new HashMap<>();
    private ClickHandler goBackHandler = null;
    private boolean showPages = false;
    private boolean allowsKeyMovement = false;

    public GUI(String title) {
        this(ComponentUtil.legacy(title));
    }

    public GUI(Component title) {
        this.title = title;
    }

    public void setTitle(Component title) {
        this.title = title;
    }

    public Component getTitle() {
        return title;
    }

    public NavigableMap<Integer, ItemStack> getItems() {
        return items;
    }

    public void addItem(ItemStack itemStack) {
        addItem(itemStack, null);
    }

    public void addItem(ItemStack itemStack, ClickHandler clickHandler) {
        setItem(getItems().isEmpty() ? 0 : getItems().lastKey() + 1, itemStack, clickHandler);
    }

    public void setItem(Integer slot, ItemStack itemStack) {
        setItem(slot, itemStack, null);
    }

    public void setItem(Integer slot, ItemStack itemStack, ClickHandler clickHandler) {
        items.put(slot, itemStack);
        clickHandlers.put(slot, clickHandler);
    }

    private Integer getPlayerPage(Player player) {
        if (pageTracker.containsKey(player.getUniqueId()))
            return pageTracker.get(player.getUniqueId());
        return 1;
    }

    public void handleClick(Player player, Integer slot) {
        if (slot == 48 && getPlayerPage(player) <= 1 && getGoBackHandler() != null) {
            getGoBackHandler().doClick(player, this);
        } else if (slot == 48 && getPlayerPage(player) > 1) {
            GUI.ignoreClosing.add(player.getUniqueId());
            open(player, getPlayerPage(player) - 1);
        } else if (slot == 50) {
            GUI.ignoreClosing.add(player.getUniqueId());
            open(player, getPlayerPage(player) + 1);
        } else {
            Integer item = calculateItemFromSlot(player, slot);
            if (clickHandlers.containsKey(item) && clickHandlers.get(item) != null) {
                ClickHandler clickHandler = clickHandlers.get(item);
                clickHandler.doClick(player, this);
            }
        }
    }

    private Integer calculateItemFromSlot(Player player, Integer slot) {
        int actualI = 0;
        Integer page = getPlayerPage(player);

        if (page > 1) {
            actualI = 21 * (page - 1);
        }

        // We start at slot 10
        for (int i = 10; i < 54; i++) {
            if (i == 17 || i == 18 || i == 26 || i == 27 || i >= 35)
                continue;
            if (i == slot) {
                // GOT IT
                return actualI;
            }
            actualI++;
        }
        return null;
    }

    public boolean isShowPages() {
        return showPages;
    }

    public void setShowPages(boolean showPages) {
        this.showPages = showPages;
    }

    public boolean allowsKeyMovement() {
        return allowsKeyMovement;
    }

    public void setAllowsKeyMovement(boolean allowsKeyMovement) {
        this.allowsKeyMovement = allowsKeyMovement;
    }

    public ClickHandler getGoBackHandler() {
        return goBackHandler;
    }

    public void setGoBackHandler(ClickHandler goBackHandler) {
        this.goBackHandler = goBackHandler;
    }

    protected Inventory create(Integer page) {
        if (page < 1) page = 1;
        int itemsPerPage = 21;
        int pages = 1;
        if (!getItems().isEmpty())
            pages = (int) Math.ceil(getItems().lastKey() / itemsPerPage) + 1;
        int startFrom = ((page - 1) * itemsPerPage);

        int size = 45;
        if (getGoBackHandler() != null || page > 1 || pages > page)
            size = size + 9;

        Component title = getTitle();
        if (isShowPages()) {
            title = title.append(Component.text(" (Page " + page + "/" + pages + ")"));
        }
        Holder holder = new Holder(this, page);
        Inventory inventory = Bukkit.createInventory(holder, size, title);
        holder.setInventory(inventory);

        for (int i = startFrom; i < startFrom + itemsPerPage; i++) {
            if (!getItems().containsKey(i))
                continue;
            Integer slot = itemMapping.get(i - ((page - 1) * itemsPerPage));
            inventory.setItem(slot, getItems().get(i));
        }

        if (page == 1 && getGoBackHandler() != null) {
            ItemStack back = new ItemStack(Material.ARROW);
            ItemMeta backMeta = back.getItemMeta();
            backMeta.displayName(Component.text("Go Back", NamedTextColor.YELLOW));
            back.setItemMeta(backMeta);
            inventory.setItem(48, back);
        } else if (page > 1) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prev.getItemMeta();
            prevMeta.displayName(Component.text("Previous Page", NamedTextColor.YELLOW));
            prev.setItemMeta(prevMeta);
            inventory.setItem(48, prev);
        }

        if (pages > page) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prev.getItemMeta();
            prevMeta.displayName(Component.text("Next Page", NamedTextColor.YELLOW));
            prev.setItemMeta(prevMeta);
            inventory.setItem(50, prev);
        }

        return inventory;
    }

    public void open(Player player) {
        open(player, 1);
    }

    public void open(Player player, Integer page) {
        guis.put(player.getUniqueId(), this);
        pageTracker.put(player.getUniqueId(), page);
        player.openInventory(create(page));
    }

    public abstract static class ClickHandler {
        public abstract void doClick(Player player, GUI gui);
    }

    /** Stable identity for a plugin GUI; never use the display title for routing. */
    public static final class Holder implements InventoryHolder {
        private final GUI gui;
        private final int page;
        private Inventory inventory;

        private Holder(GUI gui, int page) {
            this.gui = gui;
            this.page = page;
        }

        private void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        public GUI getGui() {
            return gui;
        }

        public int getPage() {
            return page;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

}
