package net.wurmunlimited.forge.config;

import net.wurmunlimited.forge.interfaces.ForgeConstants;
import org.gotti.wurmunlimited.modloader.interfaces.Configurable;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Logger;

import static net.wurmunlimited.forge.interfaces.ForgeConstants.FORGE_BASE_URL;

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
    protected Path javaHome = null;
    protected Path java = null;
    protected String modsProfile = ForgeConstants.DEFAULT_PROFILE;
    protected Path forgeDir = null;
    protected Path modsDir = null;
    protected Path modsProfilesDir = null;
    protected Path modsDefaultDir = null;
    protected Path modsProfileDir = null;
    protected Path modsLibDir = null;
    protected Path cacheDir = null;
    protected Path installedFile = null;
    protected Path repositoryFile = null;

    protected ForgeConfig(Path baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public void configure(Properties properties) {
        findJavaDir();
        modsProfile = properties.getProperty("modsProfile",modsProfile);
        forgeDir = baseDir.resolve(properties.getProperty("forgeDir","forge"));
        modsDir = forgeDir.resolve("mods");
        modsProfilesDir = modsDir.resolve("profiles");
        modsDefaultDir = modsProfilesDir.resolve(ForgeConstants.DEFAULT_PROFILE);
        modsProfileDir = modsProfilesDir.resolve(modsProfile);
        modsLibDir = modsDir.resolve("lib");
        cacheDir = forgeDir.resolve("cache");
        installedFile = modsDir.resolve("installed.json");
        repositoryFile = modsDir.resolve("repository.json");
    }

    private void findJavaDir() {
        Path runtimeDir = baseDir.resolve("runtime");
        if(!Files.exists(runtimeDir) || !Files.isDirectory(runtimeDir)) {
            runtimeDir = baseDir.getParent().resolve("runtime");
        }
        if(Files.exists(runtimeDir) && Files.isDirectory(runtimeDir)) {
            try(DirectoryStream<Path> ds = Files.newDirectoryStream(runtimeDir)) {
                for(Path f : ds) {
                    if(Files.isDirectory(f)) {
                        String fileName = f.getFileName().toString();
                        if(fileName.startsWith("jre")) {
                            javaHome = f;
                            java = javaHome.resolve("bin").resolve("java");
                            return;
                        }
                    }
                }
            } catch(IOException e) {}
        }
        String JAVA_HOME = System.getenv("JAVA_HOME");
        if(JAVA_HOME!=null && !JAVA_HOME.isEmpty()) {
            javaHome = Paths.get(JAVA_HOME);
            java = Paths.get("java");
            return;
        }
        throw new RuntimeException("Could not find the JRE-directory!");
    }

    public Path getBaseDir() {
        return baseDir;
    }

    public Path getJavaHome() {
        return javaHome;
    }

    public Path getJava() {
        return java;
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

    public Path getInstalledFile() {
        return installedFile;
    }

    public Path getRepositoryFile() {
        return repositoryFile;
    }

    public String getBaseUrl() {
        return FORGE_BASE_URL;
    }

    public String getModsUrl() {
        return FORGE_BASE_URL;
    }

    public String getRepositoryUrl() {
        return FORGE_BASE_URL;
    }
}
