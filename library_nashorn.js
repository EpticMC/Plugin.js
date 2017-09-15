var EventPriority   = Java.type('org.bukkit.event.EventPriority');
var PlayerJoinEvent = Java.type('org.bukkit.event.player.PlayerJoinEvent');
var EventCallback   = Java.type('org.nulldev.pluginjs.EventCallback');
var CommandCallback = Java.type('org.nulldev.pluginjs.CommandCallback');

log.info("I am JS");

plugin.registerEvent(
    PlayerJoinEvent.class,
    EventPriority.MONITOR,
    true,
    new EventCallback() { callback: function(event) { server.broadcastMessage("Welcome " + event.player.displayName + "!"); } }
);

var commandScripttest = plugin.registerCommand(
    "scripttest", new CommandCallback() {
        callback: function(sender, command, args) {
            sender.sendMessage("I am JS");
            return true;
        }
    }
);

commandScripttest.setUsage("/<command>");
commandScripttest.setDescription("A command written in JS");

var libraryFunction = function() { server.broadcastMessage("Referenced a lib function"); }

plugin.invokeLibraryFunction("libraryFunction");
