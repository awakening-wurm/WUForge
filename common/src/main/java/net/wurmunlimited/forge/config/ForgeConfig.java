package net.wurmunlimited.forge.config;

import org.gotti.wurmunlimited.modloader.interfaces.Configurable;

import java.nio.file.Path;
import java.util.Properties;
import java.util.logging.Logger;

public class ForgeConfig implements Configurable {

    private static final Logger logger = Logger.getLogger(ForgeConfig.class.getName());

    protected static ForgeConfig instance = null;

    public static ForgeConfig init(Path baseDir,Properties properties) {
        if(instance==null) {
            instance = new ForgeConfig(baseDir);
            instance.configure(properties);
        }
        return instance;
    }

    public static void close() {
        instance = null;
    }

    public static ForgeConfig getInstance() {
        return instance;
    }

    protected final Path baseDir;
    protected String modsProfile = "default";
    protected Path forgeDir = null;
    protected Path modsDir = null;
    protected Path modsProfilesDir = null;
    protected Path modsDefaultDir = null;
    protected Path modsProfileDir = null;
    protected Path modsLibDir = null;
    protected Path cacheDir = null;

    protected ForgeConfig(Path baseDir) {
        this.baseDir = baseDir.toAbsolutePath();
    }

    @Override
    public void configure(Properties properties) {
        modsProfile = properties.getProperty("modsProfile",modsProfile);
        forgeDir = baseDir.resolve(properties.getProperty("forgeDir","forge"));
        modsDir = forgeDir.resolve("mods");
        modsProfilesDir = modsDir.resolve("profiles");
        modsDefaultDir = modsProfilesDir.resolve("default");
        modsProfileDir = modsProfilesDir.resolve(modsProfile);
        modsLibDir = modsDir.resolve("lib");
        cacheDir = forgeDir.resolve("cache");
    }

    public Path getBaseDir() {
        return baseDir;
    }

    public String getModsProfile() {
        return modsProfile;
    }

    public void setModsProfile(String modsProfile) {
        this.modsProfile = modsProfile;
        this.modsProfileDir = modsProfilesDir.resolve(this.modsProfile);
    }

    public Path getForgeDir() {
        return forgeDir;
    }

    public Path getModsDir() {
        return modsDir;
    }

    public Path getModsProfilesDir() {
        return modsProfilesDir;
    }

    public Path getModsDefaultDir() {
        return modsDefaultDir;
    }

    public Path getModsProfileDir() {
        return modsProfileDir;
    }

    public Path getModsLibDir() {
        return modsLibDir;
    }

    public Path getCacheDir() {
        return cacheDir;
    }
}
