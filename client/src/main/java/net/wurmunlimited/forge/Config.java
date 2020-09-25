package net.wurmunlimited.forge;

import org.gotti.wurmunlimited.modloader.interfaces.Configurable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Logger;

public class Config implements Configurable {

    private static final Logger logger = Logger.getLogger(Config.class.getName());

    public static String modsProfile = "default";
    public static Path forgeDir = null;
    public static Path modsDir = null;
    public static Path modsProfilesDir = null;
    public static Path modsProfileDir = null;
    public static Path modsLibDir = null;

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
        Config.modsProfile = properties.getProperty("modsProfile",Config.modsProfile);

        initDirs(Paths.get("forge"));

        Config.connectionFix = Boolean.parseBoolean(properties.getProperty("connectionFix","true"));

        Config.autoUpdate = Boolean.parseBoolean(properties.getProperty("autoUpdate","true"));
        Config.useWindowedFullscreenSizeAndPosition = Boolean.parseBoolean(properties.getProperty("useWindowedFullscreenSizeAndPosition","false"));
        Config.showExtraTooltips = Boolean.parseBoolean(properties.getProperty("showExtraTooltips","true"));
        Config.autoSaveToolBelt = Boolean.parseBoolean(properties.getProperty("autoSaveToolBelt","true"));
        Config.customMap = Boolean.parseBoolean(properties.getProperty("customMap","true"));
    }

    public void initDirs(Path forgeDir) {
        Config.forgeDir = forgeDir;
        Config.modsDir = Config.forgeDir.resolve("mods");
        Config.modsProfilesDir = Config.modsDir.resolve("profiles");
        Config.modsProfileDir = Config.modsProfilesDir.resolve(Config.modsProfile);
        Config.modsLibDir = Config.modsDir.resolve("lib");
    }
}
