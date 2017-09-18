package org.nulldev.pluginjs;

import java.util.logging.Level;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;

final class ScriptEventExecutor<T extends Event> implements EventExecutor, Listener {
    private final EasyScript plugin;
    private final EventCallback<T> callback;
    private final Class<T> eventType;

    public ScriptEventExecutor(EasyScript plugin, Class<T> eventType, EventCallback<T> callback) {
        this.plugin = plugin;
        this.eventType = eventType;
        this.callback = callback;
    }

    @Override
    public void execute(Listener l, Event event) throws EventException {
        if (eventType.isInstance(event)) {
            T t = eventType.cast(event);
            try { callback.callback(t); } 
            catch (RuntimeException ex) { plugin.getLogger().log(Level.WARNING, null, ex); }
        }
    }
}