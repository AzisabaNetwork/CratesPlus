package plus.crates.Handlers;

import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import plus.crates.CratesPlus;

import java.util.Locale;

/**
 * Stores a placed crate's type with the chunk, so it survives chunk unloads and restarts.
 * Chunk PDC works for every block type, unlike TileState PDC which only works for tile entities.
 */
public final class CrateBlockStorage implements Listener {
    private final CratesPlus plugin;

    public CrateBlockStorage(CratesPlus plugin) {
        this.plugin = plugin;
    }

    public void set(Block block, String crateName) {
        block.getChunk().getPersistentDataContainer().set(key(block), PersistentDataType.STRING,
                crateName.toLowerCase(Locale.ROOT));
    }

    public String get(Block block) {
        return block.getChunk().getPersistentDataContainer().get(key(block), PersistentDataType.STRING);
    }

    public void remove(Block block) {
        block.getChunk().getPersistentDataContainer().remove(key(block));
    }

    private NamespacedKey key(Block block) {
        return new NamespacedKey(plugin, "crate_" + block.getX() + "_" + block.getY() + "_" + block.getZ());
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        plugin.loadCrateLocations(event.getWorld());
    }
}
