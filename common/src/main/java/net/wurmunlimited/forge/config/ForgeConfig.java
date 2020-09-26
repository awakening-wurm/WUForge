package net.wurmunlimited.forge.config;

import org.gotti.wurmunlimited.modloader.interfaces.Configurable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Logger;

public abstract class ForgeConfig implements Configurable {

    private static final Logger logger = Logger.getLogger(ForgeConfig.class.getName());


    protected static ForgeConfig instance = null;

    public static ForgeConfig getInstance() {
        return instance;
    }

    protected String modsProfile = null;
    protected Path forgeDir = null;
    protected Path modsDir = null;
    protected Path modsProfilesDir = null;
    protected Path modsProfileDir = null;
    protected Path modsLibDir = null;

    protected ForgeConfig() {
    }

    @Override
    public void configure(Properties properties) {
        modsProfile = properties.getProperty("modsProfile",modsProfile);
        forgeDir = Paths.get("forge");
        modsDir = forgeDir.resolve("mods");
        modsProfilesDir = modsDir.resolve("profiles");
        modsProfileDir = modsProfilesDir.resolve(modsProfile);
        modsLibDir = modsDir.resolve("lib");
    }

    public String getModsProfile() {
        return modsProfile;
    }

    public void setModsProfile(String modsProfile) {
        this.modsProfile = modsProfile;
    }

    public Path getForgeDir() {
        return forgeDir;
    }

    public void setForgeDir(Path forgeDir) {
        this.forgeDir = forgeDir;
    }

    public Path getModsDir() {
        return modsDir;
    }

    public void setModsDir(Path modsDir) {
        this.modsDir = modsDir;
    }

    public Path getModsProfilesDir() {
        return modsProfilesDir;
    }

    public void setModsProfilesDir(Path modsProfilesDir) {
        this.modsProfilesDir = modsProfilesDir;
    }

    public Path getModsProfileDir() {
        return modsProfileDir;
    }

    public void setModsProfileDir(Path modsProfileDir) {
        this.modsProfileDir = modsProfileDir;
    }

    public Path getModsLibDir() {
        return modsLibDir;
    }

    public void setModsLibDir(Path modsLibDir) {
        this.modsLibDir = modsLibDir;
    }
}
