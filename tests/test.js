sender.sendMessage("I am JS");

// array
sender.sendMessage("You sent " + args.length + " arguments")

// If the sender is a player
if (player) { player.sendMessage("You are a player!") }

// If the sender is a command block
if (block) { server.broadcastMessage("CommandBlock at " + block.x + ", " + block.y + ", " + block.z + " called javascript") }

// Functions and variables from libraries are available here.
libraryFunction()
