package plus.crates.Handlers;

import plus.crates.Handlers.Holograms.Hologram;
import plus.crates.Handlers.Holograms.TextDisplayHologram;

/** Uses Paper's native TextDisplay entity; no external hologram plugin is required. */
public final class HologramHandler {
    private final Hologram hologram = new TextDisplayHologram();

    public Hologram getHologram() {
        return hologram;
    }

    public void clear() {
        hologram.clear();
    }
}
