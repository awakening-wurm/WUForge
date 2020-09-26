package net.wurmunlimited.forge.config;

import java.util.Properties;

public class ForgeClientConfig extends ForgeConfig {

    public static boolean connectionFix = true;
    public static boolean autoUpdate = false;
    public static boolean useWindowedFullscreenSizeAndPosition = false;
    public static boolean showExtraTooltips = true;
    public static boolean autoSaveToolBelt = true;
    public static boolean customMap = true;

    public static ForgeClientConfig getInstance() {
        if(instance==null) instance = new ForgeClientConfig();
        if(!(instance instanceof ForgeClientConfig)) {
            throw new RuntimeException("Configurations error, wrong type of configuration class requested.");
        }
        return (ForgeClientConfig)instance;
    }

    private ForgeClientConfig() {

    }

    @Override
    public void configure(Properties properties) {
        super.configure(properties);

        connectionFix = Boolean.parseBoolean(properties.getProperty("connectionFix","true"));

        autoUpdate = Boolean.parseBoolean(properties.getProperty("autoUpdate","true"));
        useWindowedFullscreenSizeAndPosition = Boolean.parseBoolean(properties.getProperty("useWindowedFullscreenSizeAndPosition","false"));
        showExtraTooltips = Boolean.parseBoolean(properties.getProperty("showExtraTooltips","true"));
        autoSaveToolBelt = Boolean.parseBoolean(properties.getProperty("autoSaveToolBelt","true"));
        customMap = Boolean.parseBoolean(properties.getProperty("customMap","true"));
    }
}
