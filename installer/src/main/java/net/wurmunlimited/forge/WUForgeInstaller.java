package net.wurmunlimited.forge;

import net.wurmunlimited.forge.util.FileUtils;
import net.wurmunlimited.forge.util.PopupUtil;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class WUForgeInstaller {

    private static String OS = System.getProperty("os.name").toLowerCase();

    public static void main(final String[] args) {
        WUForgeInstaller installer = new WUForgeInstaller();
        installer.install();
    }

    private class WurmFiles {
        boolean isInstalled;
        Path wurmLauncher;
        Path clientJar;
        Path commonJar;
        Path launchConfig;
        Path libsteamApi;
        Path libDir;
        Path nativeLibsDir;
        Path packsDir;
        Path playerFilesDir;

        void init() {
            wurmLauncher = clientDir.resolve("WurmLauncher");
            clientJar = clientDir.resolve("client.jar");
            commonJar = clientDir.resolve("common.jar");
            launchConfig = clientDir.resolve("LaunchConfig.ini");
            libsteamApi = clientDir.resolve("libsteam_api.so");
            libDir = clientDir.resolve("lib");
            nativeLibsDir = clientDir.resolve("nativelibs");
            packsDir = clientDir.resolve("packs");
            playerFilesDir = clientDir.resolve("PlayerFiles");
            isInstalled = Files.exists(wurmLauncher) && Files.isRegularFile(wurmLauncher) &&
                          Files.exists(clientJar) && Files.isRegularFile(clientDir) &&
                          Files.exists(commonJar) && Files.isRegularFile(commonJar) &&
                          Files.exists(launchConfig) && Files.isRegularFile(launchConfig) &&
                          Files.exists(libsteamApi) && Files.isRegularFile(libsteamApi) &&
                          Files.exists(libDir) && Files.isDirectory(libDir) &&
                          Files.exists(nativeLibsDir) && Files.isDirectory(nativeLibsDir) &&
                          Files.exists(packsDir) && Files.isDirectory(packsDir) &&
                          Files.exists(playerFilesDir) && Files.isDirectory(playerFilesDir);
        }
    }

    private class AgoFiles {
        boolean isInstalled ;
        Path agoClient;
        Path wurmClient;
        Path modLauncher;
        Path patcherJar;
        Path patcherBat;
        Path patcherSh;
        Path loggingProperties;
        Path javassist;
        Path modsDir;

        void init() {
            agoClient = clientDir.resolve("client.jar");
            wurmClient = clientDir.resolve("client-patched.jar");
            modLauncher = clientDir.resolve("modlauncher.jar");
            patcherJar = clientDir.resolve("patcher.jar");
            patcherBat = clientDir.resolve("patcher.bat");
            patcherSh = clientDir.resolve("patcher.sh");
            loggingProperties = clientDir.resolve("logging.properties");
            javassist = clientDir.resolve("javassist.jar");
            modsDir = clientDir.resolve("mods");
            isInstalled = Files.exists(wurmClient) && Files.isRegularFile(wurmClient) &&
                          Files.exists(modsDir) && Files.isDirectory(modsDir);
        }

        boolean uninstall() {
            Path[] files = {
                agoClient,
                modLauncher,
                patcherJar,
                patcherBat,
                patcherSh,
                loggingProperties,
                javassist
            };
            if(FileUtils.deleteFiles(files)) {
                try {
                    Files.move(wurmClient,agoClient,REPLACE_EXISTING);
                    return true;
                } catch(IOException e) {}
            }
            return false;
        }

        boolean uninstallMods() {
            return FileUtils.deleteFiles(FileUtils.findFiles(modsDir));
        }
    }

    private class ForgeFiles {
        boolean isInstalled;
        Path forgeProperties;
        Path forgeClient;
        Path wurmClient;
        Path javassist;
        Path forgeDir;
        Path modsDir;
        Path profilesDir;
        Path profilesDefaultDir;
        Path modsLibDir;

        void init() {
            forgeProperties = clientDir.resolve("forge.properties");
            forgeClient = clientDir.resolve("client.jar");
            wurmClient = clientDir.resolve("forge.client.jar");
            javassist = clientDir.resolve("javassist.jar");
            forgeDir = clientDir.resolve("forge");
            modsDir = forgeDir.resolve("mods");
            profilesDir = modsDir.resolve("profiles");
            profilesDefaultDir = profilesDir.resolve("default");
            modsLibDir = modsDir.resolve("lib");
            isInstalled = Files.exists(forgeProperties) && Files.isRegularFile(forgeProperties) &&
                          Files.exists(wurmClient) && Files.isRegularFile(wurmClient) &&
                          Files.exists(forgeDir) && Files.isDirectory(forgeDir);
        }

        boolean uninstall() {
            Path[] files = {
                forgeClient,
                javassist,
                forgeProperties
            };
            if(FileUtils.deleteFiles(files)) {
                try {
                    FileUtils.deleteFiles(FileUtils.findFiles(forgeDir));
                    Files.move(wurmClient,forgeClient,REPLACE_EXISTING);
                    return true;
                } catch(IOException e) {}
            }
            return false;
        }

        void writeProperties() {
            FileUtils.extractFile(WUForgeInstaller.class,clientDir,"forge.properties",false);
        }

        boolean createDirectories() {
            try {
                Path[] dirs = {
                    forgeDir,
                    modsDir,
                    profilesDir,
                    profilesDefaultDir,
                    modsLibDir
                };
                return FileUtils.createDirectories(dirs);
            } catch(IOException e) {}
            return false;
        }

        void installAgoMods(Path agoModsDir) {
            if(!Files.exists(agoModsDir) || !Files.isDirectory(agoModsDir)) return;
            List<Path> files = FileUtils.findFiles(agoModsDir);
            for(Path file : files) {
                if(!Files.exists(file)) continue;
                try {
                    Path rel = agoModsDir.relativize(file);
                    int n = rel.getNameCount();
                    if(Files.isDirectory(file)) {
                        if(n==1) continue;
                        Path dir = modsLibDir.resolve(rel);
                        FileUtils.createDirectory(dir);
                    } else {
                        String fileName = rel.getFileName().toString();
                        Path forgeFile;
                        if(n==1) forgeFile = profilesDefaultDir.resolve(rel);
                        else {
                            if(n==2) forgeFile = modsLibDir.resolve(fileName);
                            else forgeFile = modsLibDir.resolve(rel);
                        }
                        Files.move(file,forgeFile,REPLACE_EXISTING);
                    }
                } catch(IOException e) {}
            }
        }
    }

    private Path clientDir = null;
    private WurmFiles wurmFiles = new WurmFiles();
    private AgoFiles agoFiles = new AgoFiles();
    private ForgeFiles forgeFiles = new ForgeFiles();

    private WUForgeInstaller() {
    }

    public void install() {
        clientDir = getWurmLauncherDirectory();
        if(clientDir==null) {
            PopupUtil.errorMessage("Could not find the installation directory\n"+
                                   "of the Wurm Unlimited client.\n"+
                                   "\n"+
                                   "Please contact support for help.");
        }
        wurmFiles.init();
        agoFiles.init();
        forgeFiles.init();
        if(!wurmFiles.isInstalled || (agoFiles.isInstalled && forgeFiles.isInstalled)) {
            PopupUtil.errorMessage("The Wurm Unlimited client's file structure is\n"+
                                   "corrupted. Please uninstall and remove all\n"+
                                   "files and directories except PlayerFiles, and\n"+
                                   "re-install Wurm Unlimited, then run WUForge\n"
                                   +"again.");
        }
        if(forgeFiles.isInstalled) {
            if(PopupUtil.confirmBoxYesNoCancel("Wurm Unlimited Forge is already installed.\n"+
                                               "\n"+
                                               "Would you like to uninstall Wurm Unlimited Forge? This will\n"+
                                               "remove all installed mods including configurations and restore\n"+
                                               "files to original state.")) {
                forgeFiles.uninstall();
            }
            System.exit(0);
        }
        if(agoFiles.isInstalled) {
            agoFiles.uninstall();
        }
        forgeFiles.writeProperties();
        if(!forgeFiles.createDirectories()) {
            PopupUtil.errorMessage("Could not create Wurm Unlimited Forge directories.\n"+
                                   "Please make sure you have the right permissions set,\n"+
                                   "if unsure contact support.");
        }
        if(agoFiles.isInstalled) {
            forgeFiles.installAgoMods(agoFiles.modsDir);
            agoFiles.uninstallMods();
        }
    }

    private Path getWurmLauncherDirectory() {
        Path directory;
        String sep = File.separator;
        Path wuDir = Paths.get("steamapps"+sep+"common"+sep+"Wurm Unlimited"+sep+"WurmLauncher");
        if(OS.contains("win")) {
            directory = Paths.get("C:\\Program Files (x86)\\Steam").resolve(wuDir);
            if(Files.exists(directory) && Files.isDirectory(directory)) return directory;
            directory = Paths.get("D:\\Program Files (x86)\\Steam").resolve(wuDir);
            if(Files.exists(directory) && Files.isDirectory(directory)) return directory;
        } else if(OS.contains("mac")) {
            String homeDir = System.getProperty("user.home");
            directory = Paths.get(homeDir+"/Library/Application Support/Steam").resolve(wuDir);
            if(Files.exists(directory) && Files.isDirectory(directory)) return directory;
        } else if(OS.contains("nix") || OS.contains("nux") || OS.contains("aix")) {
            String homeDir = System.getProperty("user.home");
            directory = Paths.get(homeDir+"/.steam/steam").resolve(wuDir);
            if(Files.exists(directory) && Files.isDirectory(directory)) return directory;
            directory = Paths.get(homeDir+"/.local/share/Steam").resolve(wuDir);
            if(Files.exists(directory) && Files.isDirectory(directory)) return directory;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File("."));
        chooser.setDialogTitle("Wurm Unlimited Client Directory");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        if(chooser.showOpenDialog(null)==JFileChooser.APPROVE_OPTION) {
            directory = chooser.getSelectedFile().toPath();
            System.out.println("Got path: "+directory.toAbsolutePath());
            if(Files.exists(directory) && Files.isDirectory(directory)) {
                String name = directory.getFileName().toString();
                if(name.equals("WurmLauncher")) return directory;
                System.out.println("Directory name: "+name);
                if(name.equals("Wurm Unlimited")) {
                    directory = directory.resolve("WurmLauncher");
                    System.out.println("Checking if "+directory.toAbsolutePath()+" is a directory...");
                    if(Files.exists(directory) && Files.isDirectory(directory)) return directory;
                } else if(name.equals("common")) {
                    directory = directory.resolve("Wurm Unlimited"+sep+"WurmLauncher");
                    System.out.println("Checking if "+directory.toAbsolutePath()+" is a directory...");
                    if(Files.exists(directory) && Files.isDirectory(directory)) return directory;
                } else if(name.equals("steamapps")) {
                    directory = directory.resolve("common"+sep+"Wurm Unlimited"+sep+"WurmLauncher");
                    System.out.println("Checking if "+directory.toAbsolutePath()+" is a directory...");
                    if(Files.exists(directory) && Files.isDirectory(directory)) return directory;
                } else if(name.equalsIgnoreCase("steam")) {
                    directory = directory.resolve(wuDir);
                    System.out.println("Checking if "+directory.toAbsolutePath()+" is a directory...");
                    if(Files.exists(directory) && Files.isDirectory(directory)) return directory;
                } else {
                    Path parent = directory.getParent();
                    System.out.println("Testing if parent directory is right: "+parent.toAbsolutePath());
                    if(parent.getFileName().toString().equals("WurmLauncher")) return parent;
                }
            }
        }
        return null;
    }
}
