package org.nulldev.pluginjs.config;

public enum PJSVar {
    SERVER("server"),
    PLUGIN("plugin"),
    LOG("log"),
    SENDER("sender"),
    PLAYER("player"),
    BLOCK("block"),
    ARGS("args");
    private String defaultName;

    private PJSVar(String defaultName) { this.defaultName = defaultName; }

    public String getDefaultName() { return defaultName; }
}
