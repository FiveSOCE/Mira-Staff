package gg.mira.staff.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public final class StaffStateChangeEvent extends Event {
    public enum State { STAFF_MODE, VANISH, FREEZE }

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final State state;
    private final boolean enabled;

    public StaffStateChangeEvent(Player player, State state, boolean enabled) {
        this.player = player;
        this.state = state;
        this.enabled = enabled;
    }

    public Player player() { return player; }
    public State state() { return state; }
    public boolean enabled() { return enabled; }

    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static @NotNull HandlerList getHandlerList() { return HANDLERS; }
}
