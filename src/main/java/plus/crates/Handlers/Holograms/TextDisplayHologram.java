package plus.crates.Handlers.Holograms;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import plus.crates.Crates.Crate;
import plus.crates.Utils.ComponentUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Paper-native holograms backed by TextDisplay entities. */
public final class TextDisplayHologram implements Hologram {
    private static final double LINE_HEIGHT = 0.28D;
    private final Map<String, List<UUID>> displays = new HashMap<>();

    @Override
    public void create(Location location, Crate crate, ArrayList<String> lines) {
        remove(location, crate);
        if (location.getWorld() == null || lines.isEmpty()) return;

        String id = id(location);
        NamespacedKey marker = marker(crate);
        List<UUID> spawned = new ArrayList<>();
        Location base = location.clone().add(0, 1.25D, 0);
        for (int index = 0; index < lines.size(); index++) {
            Location lineLocation = base.clone().add(0, (lines.size() - index - 1) * LINE_HEIGHT, 0);
            Component text = ComponentUtil.legacy(lines.get(index));
            TextDisplay display = location.getWorld().spawn(lineLocation, TextDisplay.class, entity -> {
                entity.text(text);
                entity.setBillboard(Display.Billboard.CENTER);
                entity.setSeeThrough(true);
                entity.setShadowed(true);
                entity.setDefaultBackground(false);
                entity.setPersistent(true);
                entity.getPersistentDataContainer().set(marker, PersistentDataType.STRING, id);
            });
            spawned.add(display.getUniqueId());
        }
        displays.put(id, spawned);
    }

    @Override
    public void remove(Location location, Crate crate) {
        if (location.getWorld() == null) return;
        String id = id(location);
        List<UUID> known = displays.remove(id);
        if (known != null) {
            for (UUID uuid : known) {
                Entity entity = Bukkit.getEntity(uuid);
                if (entity != null) entity.remove();
            }
        }
        NamespacedKey marker = marker(crate);
        for (TextDisplay display : location.getWorld().getNearbyEntitiesByType(TextDisplay.class,
                location.clone().add(0.5D, 1.5D, 0.5D), 1.5D)) {
            if (id.equals(display.getPersistentDataContainer().get(marker, PersistentDataType.STRING))) display.remove();
        }
    }

    @Override
    public void clear() {
        for (List<UUID> ids : displays.values()) {
            for (UUID uuid : ids) {
                Entity entity = Bukkit.getEntity(uuid);
                if (entity != null) entity.remove();
            }
        }
        displays.clear();
    }

    private String id(Location location) {
        return location.getWorld().getUID() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }

    private NamespacedKey marker(Crate crate) {
        return new NamespacedKey(crate.getCratesPlus(), "crate_hologram");
    }
}
