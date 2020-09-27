package net.wurmunlimited.forge;

import net.wurmunlimited.forge.util.FileUtil;
import net.wurmunlimited.forge.util.HttpClient;
import net.wurmunlimited.forge.util.PopupUtil;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static net.wurmunlimited.forge.interfaces.ForgeConstants.BASE_URL;

public class WUForgeInstaller {

    public static void main(final String[] args) {
        WUForgeInstaller installer = new WUForgeInstaller();
        installer.install();
    }

    class WurmFiles {
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
            if(FileUtil.isWindows()) {
                wurmLauncher = clientDir.resolve("WurmLauncher.exe");
                libsteamApi = clientDir.resolve("libsteam_api.dll");
            } else {
                wurmLauncher = clientDir.resolve("WurmLauncher");
                libsteamApi = clientDir.resolve("libsteam_api.so");
            }
            clientJar = clientDir.resolve("client.jar");
            commonJar = clientDir.resolve("common.jar");
            launchConfig = clientDir.resolve("LaunchConfig.ini");
            libsteamApi = clientDir.resolve("libsteam_api.so");
            libDir = clientDir.resolve("lib");
            nativeLibsDir = clientDir.resolve("nativelibs");
            packsDir = clientDir.resolve("packs");
            playerFilesDir = clientDir.resolve("PlayerFiles");
        }

        boolean isInstalled() {
            return Files.exists(wurmLauncher) && Files.isRegularFile(wurmLauncher) &&
                   Files.exists(clientJar) && Files.isRegularFile(clientJar) &&
                   Files.exists(commonJar) && Files.isRegularFile(commonJar) &&
                   Files.exists(launchConfig) && Files.isRegularFile(launchConfig) &&
                   Files.exists(libsteamApi) && Files.isRegularFile(libsteamApi) &&
                   Files.exists(libDir) && Files.isDirectory(libDir) &&
                   Files.exists(nativeLibsDir) && Files.isDirectory(nativeLibsDir) &&
                   Files.exists(packsDir) && Files.isDirectory(packsDir) &&
                   Files.exists(playerFilesDir) && Files.isDirectory(playerFilesDir);
        }
    }

    class AgoFiles {
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
        }

        boolean isInstalled() {
            return Files.exists(wurmClient) && Files.isRegularFile(wurmClient) &&
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
            if(FileUtil.deleteFiles(files)) {
                try {
                    Files.move(wurmClient,agoClient,REPLACE_EXISTING);
                    return true;
                } catch(IOException e) {}
            }
            return false;
        }

        boolean uninstallMods() {
            return FileUtil.deleteFiles(FileUtil.findFiles(modsDir));
        }
    }

    class ForgeFiles {
        Path forgeProperties;
        Path forgeClient;
        Path wurmClient;
        Path loggingProperties;
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
            loggingProperties = clientDir.resolve("logging.properties");
            javassist = clientDir.resolve("javassist.jar");
            forgeDir = clientDir.resolve("forge");
            modsDir = forgeDir.resolve("mods");
            profilesDir = modsDir.resolve("profiles");
            profilesDefaultDir = profilesDir.resolve("default");
            modsLibDir = modsDir.resolve("lib");
        }

        boolean isInstalled() {
            return Files.exists(forgeProperties) && Files.isRegularFile(forgeProperties) &&
                   Files.exists(wurmClient) && Files.isRegularFile(wurmClient) &&
                   Files.exists(forgeDir) && Files.isDirectory(forgeDir);
        }

        boolean uninstall() {
            Path[] files = {
                forgeClient,
                javassist,
                forgeProperties,
                loggingProperties
            };
            if(FileUtil.deleteFiles(files)) {
                try {
                    FileUtil.deleteFiles(FileUtil.findFiles(forgeDir));
                    Files.move(wurmClient,forgeClient,REPLACE_EXISTING);
                    return true;
                } catch(IOException e) {}
            }
            return false;
        }

        void writeProperties() {
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
                return FileUtil.createDirectories(dirs);
            } catch(IOException e) {}
            return false;
        }

        boolean installForge() {
            try {
                Files.move(forgeClient,wurmClient,REPLACE_EXISTING);
            } catch(IOException e) {
                return false;
            }
            boolean ret = true;
            ret = HttpClient.download(BASE_URL+"download/client.jar",forgeClient) && ret;
            ret = HttpClient.download(BASE_URL+"download/javassist.jar",javassist) && ret;
            ret = FileUtil.extractFile(WUForgeInstaller.class,forgeProperties,"/forge.properties",false)!=null && ret;
            ret = FileUtil.extractFile(WUForgeInstaller.class,loggingProperties,"/logging.properties",false)!=null && ret;
            return ret;
        }

        void installAgoMods(Path agoModsDir) {
            if(!Files.exists(agoModsDir) || !Files.isDirectory(agoModsDir)) return;
            List<Path> files = FileUtil.findFiles(agoModsDir);
            for(Path file : files) {
                if(!Files.exists(file)) continue;
                try {
                    Path rel = agoModsDir.relativize(file);
                    int n = rel.getNameCount();
                    if(Files.isDirectory(file)) {
                        if(n==1) continue;
                        Path dir = modsLibDir.resolve(rel);
                        FileUtil.createDirectory(dir);
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

    Path clientDir = null;
    WurmFiles wurmFiles = new WurmFiles();
    AgoFiles agoFiles = new AgoFiles();
    ForgeFiles forgeFiles = new ForgeFiles();

    WUForgeInstaller() {
    }

    void install() {
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
        boolean wuIsInstalled = wurmFiles.isInstalled();
        boolean agoIsInstalled = agoFiles.isInstalled();
        boolean forgeIsInstalled = forgeFiles.isInstalled();
        if(!wuIsInstalled || (agoIsInstalled && forgeIsInstalled)) {
            PopupUtil.errorMessage("The Wurm Unlimited client's file structure is\n"+
                                   "corrupted. Please uninstall and remove all\n"+
                                   "files and directories except PlayerFiles, and\n"+
                                   "re-install Wurm Unlimited, then run WUForge\n"
                                   +"again.");
        }
        if(forgeIsInstalled) {
            if(PopupUtil.confirmBoxYesNoCancel("Wurm Unlimited Forge is already installed.\n"+
                                               "\n"+
                                               "Would you like to uninstall Wurm Unlimited Forge? This will\n"+
                                               "remove all installed mods including configurations and restore\n"+
                                               "files to original state.")) {
                forgeFiles.uninstall();
            }
            System.exit(0);
        }
        if(agoIsInstalled) {
            agoFiles.uninstall();
        }
        forgeFiles.writeProperties();
        if(!forgeFiles.createDirectories()) {
            PopupUtil.errorMessage("Could not create Wurm Unlimited Forge directories.\n"+
                                   "Please make sure you have the right permissions set,\n"+
                                   "if unsure contact support.");
        }
        if(agoIsInstalled) {
            forgeFiles.installAgoMods(agoFiles.modsDir);
            agoFiles.uninstallMods();
        }

        forgeFiles.installForge();
    }

    Path getWurmLauncherDirectory() {
        Path directory;
        String sep = File.separator;
        Path wuDir = Paths.get("steamapps"+sep+"common"+sep+"Wurm Unlimited"+sep+"WurmLauncher");
        if(FileUtil.isWindows()) {
            directory = Paths.get("C:\\Program Files (x86)\\Steam").resolve(wuDir);
            if(Files.exists(directory) && Files.isDirectory(directory)) return directory;
            directory = Paths.get("D:\\Program Files (x86)\\Steam").resolve(wuDir);
            if(Files.exists(directory) && Files.isDirectory(directory)) return directory;
        } else if(FileUtil.isMacintosh()) {
            String homeDir = System.getProperty("user.home");
            directory = Paths.get(homeDir+"/Library/Application Support/Steam").resolve(wuDir);
            if(Files.exists(directory) && Files.isDirectory(directory)) return directory;
        } else if(FileUtil.isLinux()) {
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
        } else {
            System.exit(0);
        }
        return null;
    }
}
