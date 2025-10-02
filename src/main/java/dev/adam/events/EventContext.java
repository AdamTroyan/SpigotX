package dev.adam.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerEvent;

public class EventContext<T extends Event> {
    private final T event;
    private final Player player;

    public EventContext(T event) {
        this.event = event;
        if (event instanceof EntityDamageEvent ede && ede.getEntity() instanceof Player p) {
            this.player = p;
        } else if (event instanceof PlayerEvent pe) {
            this.player = pe.getPlayer();
        } else {
            this.player = null;
        }
    }

    public T getEvent() {
        return event;
    }

    public Player getPlayer() {
        return player;
    }
}