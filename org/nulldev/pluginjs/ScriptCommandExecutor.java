package org.nulldev.pluginjs;

import java.util.logging.Level;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

class ScriptCommandExecutor implements CommandExecutor {
    private final PluginJS plugin;
    private final CommandCallback callback;

    public ScriptCommandExecutor(PluginJS plugin, CommandCallback callback) {
        this.plugin = plugin;
        this.callback = callback;
    }

    public boolean onCommand(CommandSender cs, Command cmnd, String string, String[] strings) {
        try { return callback.callback(cs, string, strings); } 
        catch (RuntimeException ex) {
            plugin.getLogger().log(Level.WARNING, ex.getMessage());
            return false;
        }
    }
    
}
