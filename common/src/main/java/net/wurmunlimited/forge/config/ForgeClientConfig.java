package net.wurmunlimited.forge.config;

import java.nio.file.Path;
import java.util.Properties;

import static net.wurmunlimited.forge.interfaces.ForgeConstants.FORGE_BASE_URL;

public class ForgeClientConfig extends ForgeConfig {

    protected Path clientJar = null;
    protected Path forgeJar = null;
    protected Path updateForgeJar = null;

    public static boolean connectionFix = true;
    public static boolean autoUpdate = false;
    public static boolean useWindowedFullscreenSizeAndPosition = false;
    public static boolean showExtraTooltips = true;
    public static boolean autoSaveToolBelt = true;
    public static boolean customMap = true;

    public static ForgeClientConfig init(Path baseDir,Properties properties) {
        if(ForgeConfig.instance==null) {
            ForgeConfig.instance = new ForgeClientConfig(baseDir);
            ForgeConfig.instance.configure(properties);
        }
        return (ForgeClientConfig)ForgeConfig.instance;
    }

    public static ForgeClientConfig getInstance() {
        if(!(ForgeConfig.instance instanceof ForgeClientConfig)) {
            throw new RuntimeException("Configurations error, wrong type of configuration class requested.");
        }
        return (ForgeClientConfig)ForgeConfig.instance;
    }

    private ForgeClientConfig(Path baseDir) {
        super(baseDir);
    }

    @Override
    public void configure(Properties properties) {
        super.configure(properties);

        clientJar = baseDir.resolve("client.jar");
        forgeJar = forgeDir.resolve("forge.jar");
        updateForgeJar = forgeDir.resolve(".forge.jar");

        connectionFix = Boolean.parseBoolean(properties.getProperty("connectionFix","true"));

        autoUpdate = Boolean.parseBoolean(properties.getProperty("autoUpdate","true"));
        useWindowedFullscreenSizeAndPosition = Boolean.parseBoolean(properties.getProperty("useWindowedFullscreenSizeAndPosition","false"));
        showExtraTooltips = Boolean.parseBoolean(properties.getProperty("showExtraTooltips","true"));
        autoSaveToolBelt = Boolean.parseBoolean(properties.getProperty("autoSaveToolBelt","true"));
        customMap = Boolean.parseBoolean(properties.getProperty("customMap","true"));
    }

    public Path getClientJar() {
        return clientJar;
    }

    public Path getForgeJar() {
        return forgeJar;
    }

    public Path getUpdateForgeJar() {
        return updateForgeJar;
    }

    @Override
    public String getModsUrl() {
        return FORGE_BASE_URL+"mods/client";
    }

    @Override
    public String getRepositoryUrl() {
        return FORGE_BASE_URL+"repository/client";
    }
}
