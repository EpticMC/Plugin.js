package org.nulldev.pluginjs;

import org.bukkit.event.Event;

public interface EventCallback<T extends Event> { public void callback(T t); }
