importClass(org.bukkit.event.EventPriority);
importClass(org.bukkit.event.player.PlayerJoinEvent);

log.info("I am JS");

plugin.registerEvent(
    PlayerJoinEvent,
    EventPriority.MONITOR,
    true,
    function(event) { server.broadcastMessage("Welcome " + event.player.displayName + "!"); }
);

var commandScripttest = plugin.registerCommand(
    "scripttest",
    function(sender, command, args) {
        sender.sendMessage("I am JS");
        return true;
    }
);

commandScripttest.setUsage("/<command>");
commandScripttest.setDescription("A command written in JS");

var libraryFunction = function() { server.broadcastMessage("Referenced a lib function"); }

plugin.invokeLibraryFunction("libaryFunction");
