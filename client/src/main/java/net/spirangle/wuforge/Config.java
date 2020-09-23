package net.spirangle.wuforge;

import org.gotti.wurmunlimited.modloader.interfaces.Configurable;

import java.io.File;
import java.util.Properties;
import java.util.logging.Logger;

public class Config implements Configurable {

    private static final Logger logger = Logger.getLogger(Config.class.getName());

    public static String forgeDir = "forge";
    public static String modsProfile = "default";
    public static String modsDir = null;

    public static boolean connectionFix = true;

    public static boolean autoUpdate = false;
    public static boolean useWindowedFullscreenSizeAndPosition = false;
    public static boolean showExtraTooltips = true;
    public static boolean autoSaveToolBelt = true;
    public static boolean customMap = true;

    private static Config instance = null;

    public static Config getInstance() {
        if(instance==null) instance = new Config();
        return instance;
    }

    private Config() {
    }

    @Override
    public void configure(Properties properties) {
        modsProfile = properties.getProperty("modsProfile",modsProfile);
        modsDir = forgeDir+File.separator+"mods";

        connectionFix = Boolean.parseBoolean(properties.getProperty("connectionFix","true"));

        autoUpdate = Boolean.parseBoolean(properties.getProperty("autoUpdate","true"));
        useWindowedFullscreenSizeAndPosition = Boolean.parseBoolean(properties.getProperty("useWindowedFullscreenSizeAndPosition","false"));
        showExtraTooltips = Boolean.parseBoolean(properties.getProperty("showExtraTooltips","true"));
        autoSaveToolBelt = Boolean.parseBoolean(properties.getProperty("autoSaveToolBelt","true"));
        customMap = Boolean.parseBoolean(properties.getProperty("customMap","true"));
    }
}
