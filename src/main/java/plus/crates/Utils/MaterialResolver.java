package plus.crates.Utils;

import org.bukkit.Material;
import plus.crates.CratesPlus;

/** Resolves configuration material names without allowing one bad legacy value to stop startup. */
public final class MaterialResolver {
    private MaterialResolver() {
    }

    public static Material resolve(CratesPlus plugin, String value, Material fallback, String path) {
        Material material = value == null ? null : Material.matchMaterial(value, true);
        if (material == null || material.isLegacy()) {
            plugin.getLogger().warning("Invalid or legacy material '" + value + "' at " + path
                    + "; using " + fallback + ". Run /crate migratelegacy report for old item data.");
            return fallback;
        }
        return material;
    }
}
