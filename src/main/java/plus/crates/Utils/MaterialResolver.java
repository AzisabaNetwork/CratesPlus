package plus.crates.Utils;

import org.bukkit.Material;
import plus.crates.CratesPlus;

/** Resolves configuration material names without allowing one bad legacy value to stop startup. */
public final class MaterialResolver {
    private MaterialResolver() {
    }

    public static Material resolve(CratesPlus plugin, String value, Material fallback, String path) {
        // Do not enable legacy-name matching here. On current Paper it can map
        // valid names such as PLAYER_HEAD to a LEGACY_* material, causing a
        // valid configuration entry to be rejected below.
        Material material = value == null ? null : Material.matchMaterial(value);
        if (material == null || material.isLegacy()) {
            plugin.getLogger().warning("Invalid or legacy material '" + value + "' at " + path
                    + "; using " + fallback + ". Run /crate migratelegacy report for old item data.");
            return fallback;
        }
        return material;
    }
}
