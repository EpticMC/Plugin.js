package org.nulldev.pluginjs;

import com.google.common.io.ByteStreams;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.bukkit.World;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.nulldev.pluginjs.config.PJSConf;
import org.nulldev.pluginjs.config.PJSVar;
import org.nulldev.pluginjs.comreg.CommandRegistration;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.introspector.BeanAccess;

public class PluginJS extends JavaPlugin {
    private static final String CONFIG = "config.yml";
    private static final String JS_LIBRARY = "library.js";
    private static final String JS_LIBRARY_RHINO = "library_rhino.js";
    private static final String JS_LIBRARY_NASHORN = "library_nashorn.js";
    private static final String NASHORN_ENGINE = "nashorn";
    private static final String RHINO_ENGINE = "rhino";
    private static final String[] DEFAULTS = new String[] {
        JS_LIBRARY_RHINO,
        JS_LIBRARY_NASHORN
    };
    public static final String SERVER_CONFIG = "server.yml";
    public static final String WORLD_CONFIG_DIRECTORY = "world";
    public static final String PLAYER_CONFIG_DIRECTORY = "player";

    private PJSConf config;

    private ScriptEngine engine;
    private Invocable invocable;
    private Compilable compilable;
    private ScriptContext engineContext;
    private ClassLoader libClassLoader;
    private Map<File,Long> libraries = new HashMap<File,Long>();
    private Map<String, ScriptHolder> scripts = new HashMap<String, ScriptHolder>();
    private Map<String, PluginCommand> scriptCommands = new HashMap<String, PluginCommand>();
    private final List<ScriptEventExecutor> registeredEventExecutors = new ArrayList<ScriptEventExecutor>();

    private Config serverConfig;
    private Map<String, Config> worldConfig = new HashMap<String, Config>();
    private Map<UUID, Config> playerUuidConfig = new HashMap<UUID, Config>();

    public PluginJS() { }

    private File getConfigFile() { return new File(getDataFolder(), CONFIG); }

    private void loadConfig() {
        String _defErr = "Error loading configuration";
        try {
            Yaml configLoader = new Yaml(new CustomClassLoaderConstructor(PJSConf.class, getClass().getClassLoader()));
            configLoader.setBeanAccess(BeanAccess.FIELD);
            config = configLoader.loadAs(new FileReader(getConfigFile()), PJSConf.class);
            config.init();
            return;
        } 
        catch (IOException ex) { getLogger().log(Level.SEVERE, _defErr, ex); } 
        catch (YAMLException ex) { getLogger().log(Level.SEVERE, _defErr, ex); }
        getLogger().severe("config.yml couln't be validated... Using defaults.");
        config = new PJSConf();
    }

    private void loadClassLoader() {
        File lib = new File(getDataFolder(), config.getJarDirectory());
        List<URL> libClasspath = new ArrayList<URL>();
        for (
            File libFile : lib.listFiles(
                new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        name = name.toLowerCase();
                        return name.endsWith(".jar") || name.endsWith(".zip");
                    }
                }
            )
        ){
            try { libClasspath.add(libFile.toURI().toURL()); } 
            catch (MalformedURLException ex) { getLogger().log(Level.SEVERE, null, ex); }
        }
        
        if (libClasspath.isEmpty()) this.libClassLoader = getClass().getClassLoader(); } 
        else {
            getLogger().log(Level.INFO, "Creating classloader for language runtime: {0}", libClasspath);
            this.libClassLoader = URLClassLoader.newInstance(libClasspath.toArray(new URL[libClasspath.size()]), getClass().getClassLoader());
        }
        serverConfig = new Config(this, new File(getDataFolder(), SERVER_CONFIG));
    }

    private boolean copyDefault(String source, String dest) {
        File destFile = new File(getDataFolder(), dest);
        if (!destFile.exists()) {
            try {
                destFile.getParentFile().mkdirs();
                InputStream in = getClass().getClassLoader().getResourceAsStream(source);
                if (in != null) {
                    try {
                        OutputStream out = new FileOutputStream(destFile);
                        try { ByteStreams.copy(in, out); } 
                        finally { out.close(); }
                    } 
                    finally { in.close(); }
                    return true;
                }
            } 
            catch (IOException ex) { Logger.getLogger(PluginJS.class.getName()).log(Level.WARNING, "Error copying default " + dest, ex); }
        }
        return false;
    }

    private void copyDefaults() {
        if (copyDefault(CONFIG, CONFIG)) {
            for (String s : DEFAULTS) copyDefault(s, s);
            ScriptEngineManager manager = new ScriptEngineManager();
            for (ScriptEngineFactory f : manager.getEngineFactories()) {
                if (f.getEngineName().toLowerCase().contains(NASHORN_ENGINE)) {
                    copyDefault(JS_LIBRARY_NASHORN, JS_LIBRARY);
                    break;
                } 
                else if (f.getEngineName().toLowerCase().contains(RHINO_ENGINE)) {
                    copyDefault(JS_LIBRARY_RHINO, JS_LIBRARY);
                    break;
                }
            }
        }
    }

    private void copyLibs() {
        File lib = new File(getDataFolder(), config.getJarDirectory());
        if (!lib.exists()) lib.mkdirs();
    }
    
    @Override
    public void onEnable() {
        copyDefaults();
        loadConfig();
        copyLibs();
        loadClassLoader();
        enableEngine();
    }

    @Override
    public void onDisable() {
        disableEngine();
        serverConfig.save();
        serverConfig = null;
        for (Config c : worldConfig.values()) c.save();
        worldConfig.clear();
        for (Config c : playerUuidConfig.values()) c.save();
        playerUuidConfig.clear();
    }

    private void enableEngine() {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            for (Map.Entry<String,String> e : config.getSystemProperties().entrySet()) System.setProperty(e.getKey(), e.getValue());
            Thread.currentThread().setContextClassLoader(libClassLoader);
            ScriptEngineManager manager = new ScriptEngineManager(libClassLoader);
            this.engine = manager.getEngineByName(config.getLanguage());

            if (this.engine == null) {
                getLogger().severe("Script engine named: " + config.getLanguage() + " not found, disabling plugin.");
                getServer().getPluginManager().disablePlugin(this);
                return;
            } 
            else {
                if (engine.getFactory() != null) {
                    getLogger().info("Loaded scripting engine: " + engine.getFactory().getEngineName() + " version: " + engine.getFactory().getEngineVersion());
                    StringBuilder tmp = new StringBuilder();
                    tmp.append("Valid file extensions for your scripts are: ");
                    for (String s : engine.getFactory().getExtensions()) {
                        tmp.append(' ');
                        tmp.append(s);
                    }
                    getLogger().info(tmp.toString());
                } 
                else getLogger().info("Broken script engine, engine.getFactory() returned null");
            }
            if ((this.engine instanceof Invocable))  this.invocable  = ((Invocable)  this.engine);
            if ((this.engine instanceof Compilable)) this.compilable = ((Compilable) this.engine); 
            else {
                getLogger().severe("Selected scripting engine does not implement javax.script.Compilable, disabling plugin.");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            this.engineContext = this.engine.getContext();
            this.engineContext.setAttribute(config.getVariableNames().get(ESVariable.SERVER), getServer(), 100);
            this.engineContext.setAttribute(config.getVariableNames().get(ESVariable.PLUGIN), this, 100);
            this.engineContext.setAttribute(config.getVariableNames().get(ESVariable.LOG), getLogger(), 100);
            this.engineContext.setWriter(new LogWriter(Level.INFO));
            this.engineContext.setErrorWriter(new LogWriter(Level.WARNING));
            for (String s : config.getLibraries()) {
                boolean found = false;
                for (String suffix : engine.getFactory().getExtensions()) {
                    File library = new File(getDataFolder(), s + '.' + suffix);
                    if (library.isFile()) {
                        this.libraries.put(library, Long.valueOf(library.lastModified()));
                        this.engineContext.setAttribute(ScriptEngine.FILENAME, library.getPath(), ScriptContext.ENGINE_SCOPE);
                        try { this.engine.eval(new FileReader(library)); } 
                        catch (ScriptException ex) { getLogger().log(Level.WARNING, "Error in library: " + library + ": " + ex.getMessage()); } 
                        catch (FileNotFoundException ex) { getLogger().log(Level.SEVERE, null, ex); }
                        found = true;
                        break;
                    }
                }
                if (!found) getLogger().warning("Failed to find library file : " + s);
            }
            try { CommandRegistration.updateHelp(this, getServer()); } 
            catch (UnsupportedOperationException e) { getLogger().log(Level.WARNING, "Failed to update help map", e); }
        } 
        finally { Thread.currentThread().setContextClassLoader(oldClassLoader); }
    }

    private void disableEngine() {
        for (Listener i : this.registeredEventExecutors) HandlerList.unregisterAll(i);
        this.registeredEventExecutors.clear();

        try {
            CommandRegistration.unregisterPluginCommands(getServer(), new HashSet(this.scriptCommands.values()));
            CommandRegistration.updateHelp(this, getServer());
        } 
        catch (UnsupportedOperationException e) { getLogger().log(Level.WARNING, "There was an error unregistering a command, scriptreload will not correctly unregister commands"); }
        this.scriptCommands.clear();

        this.engine = null;
        this.invocable = null;
        this.compilable = null;
        this.engineContext = null;
        this.libraries.clear();
        this.scripts.clear();
    }

    public void setInvocable(Invocable invocable) { this.invocable = invocable; }

    public Object invokeLibraryFunction(String function, Object... args) throws ScriptException, NoSuchMethodException {
        checkLibraries();
        if (invocable != null) return invocable.invokeFunction(function, args);
    }

    public Object invokeScript(String script, Map<String,Object> params) throws ScriptException, NoSuchMethodException {
        ScriptHolder holder = getScript(script);
        if (holder == null) throw new NoSuchMethodException("Script: " + script + " not found");
        if (config.isUseScriptScope()) {
            ScriptContext context = new PluginJSContext(this.engine, this.engineContext);
            context.setAttribute(ScriptEngine.FILENAME, holder.getSource().getPath(), PluginJSContext.SCRIPT_SCOPE);
            for (Map.Entry<String,Object> e : params.entrySet()) context.setAttribute(e.getKey(), e.getValue(), PluginJSContext.SCRIPT_SCOPE);
            return holder.getScript().eval(context);
        } 
        else {
            for (Map.Entry<String,Object> e : params.entrySet()) this.engineContext.setAttribute(e.getKey(), e.getValue(), ScriptContext.ENGINE_SCOPE);
            try { return holder.getScript().eval(this.engineContext); } 
            finally { for (String attr : params.keySet()) this.engineContext.removeAttribute(attr, ScriptContext.ENGINE_SCOPE); }
        }
    }

    public void reload(boolean classLoader) {
        disableEngine();
        loadConfig();
        if (classLoader) loadClassLoader();
        enableEngine();
    }

    public PluginCommand registerCommand(String cmd, final String function) {
        return registerCommand(cmd, new CommandCallback() {
            @Override
            public boolean callback(CommandSender sender, String command, String[] args) {
                try { return Boolean.TRUE == invokeLibraryFunction(function, sender, command, args); } 
                catch (ScriptException ex) { getLogger().log(Level.WARNING, ex.getMessage()); } 
                catch (NoSuchMethodException ex) { getLogger().log(Level.WARNING, ex.getMessage()); } 
                catch (RuntimeException ex) { getLogger().log(Level.WARNING, ex.getMessage()); }
                return false;
            }
        });
    }

    public PluginCommand registerCommand(String cmd, CommandCallback callback) {
        try {
            final PluginCommand command = CommandRegistration.registerCommand(this, cmd);
            if (command != null) {
                scriptCommands.put(cmd, command);
                command.setExecutor(new ScriptCommandExecutor(this, callback));
                return command;
            }
        } 
        catch (UnsupportedOperationException ex) { getLogger().log(Level.WARNING, null, ex); }
        return null;
    }

    public void registerEvent(Class<? extends Event> eventClass, String function) { registerEvent(eventClass, EventPriority.NORMAL, function); }
    public <T extends Event> void registerEvent(Class<T> eventClass, EventCallback<T> callback) { registerEvent(eventClass, EventPriority.NORMAL, callback); }
    public <T extends Event> void registerEvent(Class<T> eventClass, EventPriority priority, String function) { registerEvent(eventClass, priority, true, function); }
    public <T extends Event> void registerEvent(Class<T> eventClass, EventPriority priority, EventCallback<T> callback) { registerEvent(eventClass, priority, true, callback); }
    public <T extends Event> void registerEvent(Class<T> eventClass, EventPriority priority, boolean ignoreCancelled, final String function) {
        registerEvent(eventClass, priority, ignoreCancelled, new EventCallback<T>() {
            @Override
            public void callback(T t) {
                try { invokeLibraryFunction(function, t); } 
                catch (ScriptException ex) { getLogger().log(Level.WARNING, ex.getMessage()); } 
                catch (NoSuchMethodException ex) { getLogger().log(Level.WARNING, ex.getMessage()); } 
                catch (RuntimeException ex) { getLogger().log(Level.WARNING, ex.getMessage()); }
            }
        });
    }

    public <T extends Event> void registerEvent(Class<? extends Event> eventClass, EventPriority priority, boolean ignoreCancelled, EventCallback<T> callback) {
        ScriptEventExecutor executor = new ScriptEventExecutor(this, eventClass, callback);
        registeredEventExecutors.add(executor);
        getServer().getPluginManager().registerEvent(eventClass, executor, priority, executor, this, ignoreCancelled);
    }

    public File getWorldConfigDirectory()  { return new File(getDataFolder(), WORLD_CONFIG_DIRECTORY);  }
    public File getPlayerConfigDirectory() { return new File(getDataFolder(), PLAYER_CONFIG_DIRECTORY); }

    public Config getServerConfig() { return serverConfig; }
    public Config getWorldConfig(World world) { return getWorldConfig(world.getName()); }

    public Config getWorldConfig(String world) {
        Config config = worldConfig.get(world);
        if (config == null) {
            config = new Config(this, new File(getWorldConfigDirectory(), world + ".yml"));
            worldConfig.put(world, config);
        }
        return config;
    }

    public Config getPlayerConfig(Player player) { return getPlayerConfig(player.getUniqueId()); }

    public Config getPlayerConfig(UUID uuid) {
        Config config = playerUuidConfig.get(uuid);
        if (config == null) {
            config = new Config(this, new File(getPlayerConfigDirectory(), uuid + ".yml"));
            playerUuidConfig.put(uuid, config);
        }
        return config;
    }
        

    private boolean checkLibraries() {
        if (config.isAutoreload()) {
            for (Map.Entry<File, Long> e : this.libraries.entrySet()) {
                if (((File) e.getKey()).lastModified() > ((Long) e.getValue()).longValue()) {
                    reload(false);
                    return false;
                }
            }
            return true;
        } 
        else return false;
    }

    private ScriptHolder getScript(String name) throws ScriptException {
        ScriptHolder cached = this.scripts.get(name);

        if (cached != null) {
            if (config.isAutoreload()) {
                File source = cached.getSource();
                if ((source.isFile()) && (source.lastModified() <= cached.getLastModified().longValue())) return cached;
                this.scripts.remove(name);
            } 
            else return cached;
        }

        LOOP:
        for (String dir : config.getScriptPath()) {
            for (String suffix : engine.getFactory().getExtensions()) {
                File script = new File(new File(getDataFolder(), dir), name + '.' + suffix);
                if (script.isFile()) {
                    try {
                        CompiledScript compiledScript = this.compilable.compile(new FileReader(script));
                        cached = new ScriptHolder(compiledScript, script, Long.valueOf(script.lastModified()));
                        this.scripts.put(name, cached);
                    } 
                    catch (FileNotFoundException ex) { getLogger().log(Level.SEVERE, null, ex); }
                    break LOOP;
                }
            }
        }
        return cached;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if ("script".equals(command.getName())) {
            if (args.length < 1) return false;

            String script = args[0];
            String[] shiftArgs = new String[args.length - 1];
            System.arraycopy(args, 1, shiftArgs, 0, shiftArgs.length);

            Map<String,Object> env = new HashMap<String,Object>();
            if ((sender instanceof BlockCommandSender)) env.put(config.getVariableNames().get(ESVariable.BLOCK), ((BlockCommandSender)sender).getBlock()); 
            else env.put(config.getVariableNames().get(ESVariable.BLOCK), null);
            
            if ((sender instanceof Player)) env.put(config.getVariableNames().get(ESVariable.PLAYER), sender);
            else env.put(config.getVariableNames().get(ESVariable.PLAYER), null);
            
            env.put(config.getVariableNames().get(ESVariable.SENDER), sender);
            env.put(config.getVariableNames().get(ESVariable.ARGS), shiftArgs);
            
            try { invokeScript(script, env); } 
            catch (ScriptException ex) { sender.sendMessage("Error in script " + script + " " + ex.getMessage()); } 
            catch (NoSuchMethodException ex) { sender.sendMessage("Error in script " + script + " " + ex.getMessage()); } 
            catch (RuntimeException ex) {
                sender.sendMessage("Error in script, see server console.");
                getLogger().log(Level.WARNING, "Error in script: " + script, ex);
            }
            return true;
        }
        if ("scriptreload".equals(command.getName())) {
            if (args.length == 0) {
                reload(false);
                sender.sendMessage("Scripts reloaded.");
            } 
            else if(args.length == 1 && "classpath".equalsIgnoreCase(args[0])){
                reload(true);
                sender.sendMessage("Scripts and classpath reloaded.");
            }
        }
        return false;
    }

    private class LogWriter extends Writer {

        StringBuilder line = new StringBuilder();
        private final Level level;

        public LogWriter(Level level) { this.level = level; }

        public void write(char[] cbuf, int off, int len) throws IOException {
            for (int i = off; i < len; i++) {
                if (cbuf[i] == '\n') flush();
                else this.line.append(cbuf[i]);
            }
        }

        public void flush() throws IOException {
            PluginJS.this.getLogger().log(this.level, this.line.toString());
            this.line.setLength(0);
        }

        public void close() throws IOException { }
    }

    private static final class ScriptHolder {

        private final CompiledScript script;
        private final Long lastModified;
        private final File source;

        public ScriptHolder(CompiledScript script, File source, Long lastModified) {
            this.script = script;
            this.lastModified = lastModified;
            this.source = source;
        }
        public Long getLastModified()     { return this.lastModified; }
        public CompiledScript getScript() { return this.script;       }
        public File getSource()           { return this.source;       }
    }
}
