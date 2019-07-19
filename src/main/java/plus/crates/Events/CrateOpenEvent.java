package plus.crates.Events;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import plus.crates.Crate;
import plus.crates.CratesPlus;

public class CrateOpenEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final CratesPlus cratesPlus;
    private final Player player;
    private final Crate crate;
    private final Location blockLocation;

    public CrateOpenEvent(Player player, String crateName, Location blockLocation, CratesPlus cratesPlus) {
        this.cratesPlus = cratesPlus;
        this.player = player;
        this.blockLocation = blockLocation;
        crate = cratesPlus.getConfigHandler().getCrates().get(crateName.toLowerCase());
    }

    public void doEvent() {
        CratesPlus.getOpenHandler().getOpener(crate).startOpening(player, crate, blockLocation);
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public Player getPlayer() {
        return player;
    }

    public Crate getCrate() {
        return crate;
    }

    public Location getBlockLocation() {
        return blockLocation;
    }

    public CratesPlus getCratesPlus() {
        return cratesPlus;
    }

}