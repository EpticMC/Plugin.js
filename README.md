# Plugin.js
Develop MC Plugins in JavaScript

**Note:** DO NOT USE THAT RIGHT NOW. Because it is:

- ...Very new
- ...Unstable
- ...probably not even compiling
- ...just a test

## Table of Contents:

- [About](#about)
- [Installation](#installation)
  - [Requirements](#requirements)
    - [Java 1.6 with included JS support](#16inc)
    - [Java 1.6 with updated JS support](#16upd)
  - [Downloads](#downloads)
- [Configuration](#configuration)
  - [autoreload](#autoreload)
  - [libraries](#libraries)
  - [scripts](#scripts)
- [Commands](#commands)
  - [scriptreload](#com1)
  - [script](#com2)
- [Variables](#variables)
  - [Library variables](#lvar)
  - [Script variables](#svar)
- [Functions](#functions)
- [Examples](#examples)

-------

## About

This is an attempt to make a framework for developing MC (Bukkit & Spigot) Plugins in JavaScript including support for command registration and handling all events.

- Ability to hook into any bukkit event.
- Support for registering bukkit command handlers.
- Support for autoreloading of script files when they change.

<hr>

## Installation

### Requirements

The plugin requires Java 1.6, and to support command registration CraftBukkit 1.4.7. Oracle's java 1.6 includes support for javascript, to support other languages (or to install a different javascript version) you'll need to modify the startup classpath of your server and add additional jar files to the classpath.

<a name="16inc"></a>
**Java 1.6 with included Javascript support:** 

Oracle's version of Java comes with a built in javascript engine, if you are using that it should just work. However if you are using Apple's version of Java then it may not be present (I don't own a Mac, and have not tested, if someone can let me know if it works or not I would appreciate it). Apple's JVM includes support for AppleScript, so you could try that.

<a name="16upd"></a>
**Java 1.6 with updated Javascript support:** 

The version of javascript shipped with Java 1.6 is known to be slow, also you will not be able to directly access plugin classes as it is not classloader aware. To update the javascript engine you need to download the latest version of Mozilla Rhino [here](https://developer.mozilla.org/en-US/docs/Rhino). Extract the zip file, and copy js.jar to your server directory. You will also need an implementation of ScriptEngine for Rhino, there is one [here](https://github.com/cevou/rhino-script-engine). Place that in your server's directory as well.

Once you have both files in your server directory you need to modify your startup script to run craftbukkit with these jars in the classpath. Example:

```shell
#!/bin/sh
CLASSPATH=craftbukkit-1.4.7-R1.0.jar
CLASSPATH="$CLASSPATH:js.jar:RhinoScriptEngine.jar"
java -classpath $CLASSPATH -Xmx2G -XX:MaxPermSize=256m org.bukkit.craftbukkit.Main
```

When thats done, you'll need to set the language to rhino-script-engine in `config.yml`, as it cannot override the Javascript engine shipped with Java.

### Downloads 

Please refer to the [releases here on github](https://github.com/EpticMC/Plugin.js/releases)

<hr>

## Configuration

The configuration file is fairly minimal and allows you to configure the scripting language to use as well as some library scripts to autoload, and directories which contain scripts to be executed with the /script command. Most of it is self explaining.

### autoreload

Whether to autoreload scripts when they have been edited. This is checked when a user executes /script or a script registered command or when a bukkit event is delivered to your script. For development purposes you will want this set to true. On a production server where you are not developing your scripts setting it to false will provide a small performance improvement.

### libraries

A list of script files which are to be loaded on startup. This will search for a file in `plugins/pluginjs/<name>.<suffix>`. Where `<name>` is the name of the library and `<suffix>` is the file suffix for the scripting language, for example with javascript it is js, and with python it is py. The libraries are loaded in the order specified.

### scripts

A list of directories containing script files, which is searched when a user executes `/script <name>` for a file called `<name>.<suffix>` the directories are relative to `plugins/pluginjs`, for example the default setting searches under `plugins/pluginjs/scripts/`.

<hr>

## Commands

The plugin only ships with two commands, both are fairly self explanatory.

<a name="com1"></a>
```Assembly
/scriptreload
```
**Permission**: `pluginjs.reload` <br>
**Description**: Force a reload of all your scripts.

<a name="com2"></a>
```Assembly
/script <name>
```
**Permission**: `pluginjs.script` <br>
**Description**: Execute the script named <name>.

<hr>

## Variables
There are some default variables already set:

<a name="lvar"></a>
### Library variables

| Variable | Purpose |
|----------|---------|
| `server` | Equivalent to `getServer()` in a plugin. |
| `log`    | Equivalent to `getLogger()` in a plugin. |
| `plugin` | The PluginJS plugin | 

<a name="svar"></a>
### Script variables
In addition to the variables available in library scripts, the following are available

| Variable | Purpose |
|----------|---------|
| `sender` | The CommandSender. |
| `args`   | Array of arguments to the script. |
| `player` | Set to the player if CommandSender is a Player. | 
| `block`  | Set to the command block if CommandSender is a BlockCommandSender. | 

<hr>

## Functions

The plugin provides function to make it easy to add new commands and hook into bukkit events. 

<p align="center"><s>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</s> 1 <s>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</s></p>

```javascript
void registerEvent(Class type, Function function)
```
Listen for bukkit events with specified class, and pass them to the function in the script.

<p align="center"><s>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</s> 2 <s>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</s></p>

```javascript
void registerEvent(Class type, EventPriority priority, Function function)
```
Same as above, but allows you to specify an event priority.

<p align="center"><s>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</s> 3 <s>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</s></p>

```javascript
void registerEvent(Class type, EventPrioirity priority, boolean ignoreCancelled, Function function)
```
Same as above, but allows you to pass in an ignoreCancelled flag.

<p align="center"><s>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</s> 4 <s>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</s></p>

```javascript
PluginCommand registerCommand(String name, Function function)
```
Register a command handler, returns a PluginCommand with which you can set the Usage and Help on.

<p align="center"><s>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</s> 5 <s>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</s></p>

```javascript
Config getServerConfig()
```
Returns a Config object for persisting server wide data.

<p align="center"><s>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</s> 6 <s>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</s></p>

```javascript
Config getWorldConfig(World world)
```
Returns a Config object for persisting per world data.

<p align="center"><s>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</s> 7 <s>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</s></p>

```javascript
Config getWorldConfig(String world)
```
Returns a Config object for persisting per world data.

<p align="center"><s>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</s> 8 <s>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</s></p>

```javascript
Config getPlayerConfig(Player player)
```
Returns a Config object for persisting per player data. If configured to use UUIDs, this will get the config by UUID, else it will use the Player's name.

<p align="center"><s>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</s> 9 <s>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</s></p>

```javascript
Config getPlayerConfig(String player)
```
Returns a Config object for persisting per player data. If a user is offline and the plugin is configured to use UUIDs, this will error.

<p align="center"><s>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</s> 10 <s>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</s></p>

```javascript
Config getPlayerConfig(UUID player)
```
Returns a Config object for persisting per player data.

<hr>

## Examples

Javascript usage examples:

Save the following as `echo.js` under `plugins/pluginjs/scripts/` <br>
Then execute it via `/script echo <message>`. 

**You can use & for Minecraft formatting codes.**

```javascript
var world;

if (block) world = block.getLocation().getWorld();

if (player) world = player.getWorld();

if (world) {
    var msg = args.join(' ').replace(/&/g, '§');
    var iter = world.getPlayers().iterator();
    while (iter.hasNext()) {
        var p = iter.next();
        if (p.isOnline()) p.sendMessage(msg);
    }
}
```

### Explanation:

All the references are above at the [functions](#functions) section but here is a quick summary:


> `server` -> the Server. <br>
> `plugin` -> PluginJS plugin. <br>
> `log` -> PluginJS Logger. <br>
> `sender` -> the sender of the command. <br>
> `args` -> String[] of arguments. <br>
> `player` -> the player (if sent from a player) or null. <br>
> `block` -> the block (if sent from a command block) or null.


For more simple examples refer to the examples folder in this repository: <br>
https://github.com/EpticMC/Plugin.js/tree/master/examples

If you made some good scripts that you want to share, feel free to contribute here by making a [pull request](https://github.com/EpticMC/Plugin.js/pulls). :smile:

<hr>
