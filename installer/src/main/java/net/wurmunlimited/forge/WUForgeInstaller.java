package net.wurmunlimited.forge;

import net.wurmunlimited.forge.util.FileUtil;
import net.wurmunlimited.forge.util.HttpClient;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static net.wurmunlimited.forge.interfaces.ForgeConstants.FORGE_BASE_URL;
import static net.wurmunlimited.forge.interfaces.ForgeConstants.VERSION;

public class WUForgeInstaller {

    private static class InstallationException extends Exception {
        InstallationException(String message) {
            super(message);
        }
    }

    public static void main(final String[] args) {
        WUForgeInstaller installer = new WUForgeInstaller();
        installer.install();
    }

    class WurmFiles {
        Path wurmLauncher;
        Path libsteamApi;
        Path clientJar;
        Path commonJar;
        Path launchConfig;
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
            libDir = clientDir.resolve("lib");
            nativeLibsDir = clientDir.resolve("nativelibs");
            packsDir = clientDir.resolve("packs");
            playerFilesDir = clientDir.resolve("PlayerFiles");
        }

        boolean isInstalled() {
            return Files.exists(wurmLauncher) && Files.isRegularFile(wurmLauncher) &&
                   Files.exists(libsteamApi) && Files.isRegularFile(libsteamApi) &&
                   Files.exists(clientJar) && Files.isRegularFile(clientJar) &&
                   Files.exists(commonJar) && Files.isRegularFile(commonJar) &&
                   Files.exists(launchConfig) && Files.isRegularFile(launchConfig) &&
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
        Path forgeDir;
        Path modsDir;
        Path profilesDir;
        Path profilesDefaultDir;
        Path modsLibDir;
        Path cacheDir;
        Path forgeProperties;
        Path forgeClient;
        Path wurmClient;
        Path forgeJar;
        Path loggingProperties;

        void init() {
            forgeDir = clientDir.resolve("forge");
            modsDir = forgeDir.resolve("mods");
            profilesDir = modsDir.resolve("profiles");
            profilesDefaultDir = profilesDir.resolve("default");
            modsLibDir = modsDir.resolve("lib");
            cacheDir = forgeDir.resolve("cache");
            forgeProperties = clientDir.resolve("forge.properties");
            forgeClient = clientDir.resolve("client.jar");
            wurmClient = clientDir.resolve("forge.client.jar");
            forgeJar = forgeDir.resolve("forge.jar");
            loggingProperties = forgeDir.resolve("logging.properties");
        }

        boolean isInstalled() {
            return Files.exists(forgeProperties) && Files.isRegularFile(forgeProperties) &&
                   Files.exists(forgeJar) && Files.isRegularFile(forgeJar) &&
                   Files.exists(wurmClient) && Files.isRegularFile(wurmClient);
        }

        boolean uninstall() {
            Path[] files = {
                forgeClient,
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

        boolean createDirectories() {
            try {
                Path[] dirs = {
                    forgeDir,
                    modsDir,
                    cacheDir,
                    profilesDir,
                    profilesDefaultDir,
                    modsLibDir
                };
                return FileUtil.createDirectories(dirs);
            } catch(IOException e) {}
            return false;
        }

        void installForge(final ProgressMonitor progressMonitor) throws InstallationException {
            boolean ok = true;
            Path forgeClientTemp = cacheDir.resolve("client.jar");
            Path forgeJarTemp = cacheDir.resolve("forge.jar");
            final Point range = new Point(0,100);
            if(progressMonitor!=null) {
                new Thread(() -> {
                    while(range.x<100 && !progressMonitor.isCanceled()) {
                        if(range.x<range.y) {
                            progressMonitor.setProgress(++range.x);
                        }
                        try {
                            Thread.sleep(200L);
                        } catch(InterruptedException e) {}
                    }
                }).start();
            }
            pause(200);
            range.y = 10;
            if((progressMonitor==null || !progressMonitor.isCanceled()) && ok) {
                log("Downloading client.jar ...");
                ok = HttpClient.download(FORGE_BASE_URL+"download/client.jar",forgeClientTemp) && ok;
            }
            range.x = range.y;
            range.y = 60;
            if((progressMonitor==null || !progressMonitor.isCanceled()) && ok) {
                log("Downloading forge.jar ...");
                ok = HttpClient.download(FORGE_BASE_URL+"download/forge.jar",forgeJarTemp) && ok;
            }
            range.x = range.y;
            range.y = 100;
            if((progressMonitor==null || !progressMonitor.isCanceled()) && ok) {
                pause(200);
            }
            range.x = 100;
            if((progressMonitor==null || !progressMonitor.isCanceled()) && ok) {
                try {
                    log("Moving client.jar to forge.client.jar...");
                    Files.move(forgeClient,wurmClient,REPLACE_EXISTING);
                    log("Installing downloaded files...");
                    Files.move(forgeClientTemp,forgeClient,REPLACE_EXISTING);
                    Files.move(forgeJarTemp,forgeJar,REPLACE_EXISTING);
                } catch(IOException e) {
                    log(e.toString());
                    for(StackTraceElement ste : e.getStackTrace()) {
                        log("\tat "+ste);
                    }
                    ok = false;
                }
                if(ok) {
                    log("Installing configurations files...");
                    ok = FileUtil.extractFile(WUForgeInstaller.class,forgeProperties,"/forge.properties",false)!=null && ok;
                }
                if(ok) ok = FileUtil.extractFile(WUForgeInstaller.class,loggingProperties,"/logging.properties",false)!=null && ok;
                if(ok) return;
            }
            if(!ok) {
                throw new InstallationException("Something went wrong with the installation.\n\n"+
                                                "Please try again later, and otherwise contact\n"+
                                                "support for assistance.\n\n"+
                                                "Copy log output and keep ready to help support\n"+
                                                "with your case.");
            }
            String cancelled = "Installation was cancelled.";
            log(cancelled);
            throw new InstallationException(cancelled);
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

    InstallerWindow window = null;
    Path clientDir = null;
    WurmFiles wurmFiles = new WurmFiles();
    AgoFiles agoFiles = new AgoFiles();
    ForgeFiles forgeFiles = new ForgeFiles();

    WUForgeInstaller() {
    }

    void install() {
        window = new InstallerWindow();
        log("Wurm Unlimited Forge installer v"+VERSION);
        pause(750);

        if(!confirmBoxYesNo("This will install the Wurm Unlimited Forge, which is an extension based\n"+
                            "on Ago's Client Mod Launcher. For more information about this extension,\n"+
                            "please visit the web page at: https://forge.wurm-unlimited.net\n\n"+
                            "Would you like to proceed with the installation?")) {
            log("Exiting");
            window.close();
            return;
        }

        log("Checking for directory where Wurm Unlimited is installed...");

        clientDir = getWurmLauncherDirectory();
        if(clientDir==null) {
            log("Couldn't find Wurm.");
            errorMessage("Could not find the installation directory\n"+
                         "of the Wurm Unlimited client.\n\n"+
                         "Please contact support for help.");
            return;
        }
        log("Found Wurm Unlimited at: "+clientDir.toAbsolutePath());
        log("Initializing...");
        wurmFiles.init();
        agoFiles.init();
        forgeFiles.init();
        boolean wuIsInstalled = wurmFiles.isInstalled();
        boolean agoIsInstalled = agoFiles.isInstalled();
        boolean forgeIsInstalled = forgeFiles.isInstalled();
        if(!wuIsInstalled || (agoIsInstalled && forgeIsInstalled)) {
            if(!wuIsInstalled) log("The Client has a corrupted file structure. Please uninstall the client "+
                                   "application and remove all files and directories except PlayerFiles, and then "+
                                   "reinstall it again, then run the WU Forge installer again.");
            else log("Ago's mod loader and WU Forge seems to be installed in parallel, this will cause conflicts.");
            errorMessage("The Wurm Unlimited client's file structure is\n"+
                         "corrupted. Please uninstall and remove all\n"+
                         "files and directories except PlayerFiles, and\n"+
                         "reinstall Wurm Unlimited, then run the WU Forge\n"+
                         "installer again.");
            return;
        }

        if(forgeIsInstalled) {
            log("Wurm Unlimited Forge is already installed.");
            if(confirmBoxYesNoCancel("Wurm Unlimited Forge is already installed.\n\n"+
                                     "Would you like to uninstall Wurm Unlimited Forge? This will\n"+
                                     "remove all installed mods including configurations and restore\n"+
                                     "files to original state.")) {
                if(!forgeFiles.uninstall()) {
                    errorMessage("Could not remove WU Forge, please contact support.");
                    return;
                }
                log("Removing Forge was successful.");
                messageBox("Wurm Unlimited Forge has been completely removed.");
            }
            window.close();
            return;
        }

        log("Wurm Unlimited client is installed, and WU Forge can be installed.");
        if(agoIsInstalled) {
            log("Ago's Client Mod Launcher is installed, uninstalling...");
            if(!agoFiles.uninstall()) {
                errorMessage("Could not remove Ago's Client Mod Launcher, please contact support.");
                return;
            }
            log("Ago's Client Mod Launcher was removed.");
        }

        log("Creating WU Forge directories...");
        if(!forgeFiles.createDirectories()) {
            errorMessage("Could not create Wurm Unlimited Forge directories.\n"+
                         "Please make sure you have the write permissions\n"+
                         "set, if unsure contact support.");
            return;
        }

        log("Directories created.");
        if(agoIsInstalled) {
            log("Exporting mods to WU Forge...");
            forgeFiles.installAgoMods(agoFiles.modsDir);
            log("Finishing uninstalling Ago's Client Mod Launcher...");
            if(!agoFiles.uninstallMods()) {
                errorMessage("Could not remove Ago's Client Mod Launcher, please contact support.");
                return;
            }
            log("Exporting mods to WU Forge worked.");
        }

        log("Installing WU Forge files...");
        ProgressMonitor progressMonitor = new ProgressMonitor(window.frame,"Installing...","",0,100);
        try {
            forgeFiles.installForge(progressMonitor);
        } catch(InstallationException e) {
            errorMessage(e.getMessage());
            return;
        } finally {
            progressMonitor.close();
        }
        log("Finished.");
        messageBox("Wurm Unlimited Forge has been installed.\n\n"+
                   "To uninstall run this application again.");
        window.close();
    }

    Path getWurmLauncherDirectory() {
        Path directory = null;
        Path steam = null;
        String sep = File.separator;
        Path wuDir = Paths.get("steamapps"+sep+"common"+sep+"Wurm Unlimited"+sep+"WurmLauncher");
        log("Looking for default installation directory...");
        String homeDir = System.getProperty("user.home");
        if(FileUtil.isWindows()) {
            steam = Paths.get("C:\\Program Files (x86)\\Steam");
            if(!Files.exists(steam)) {
                steam = Paths.get("D:\\Program Files (x86)\\Steam");
            }
            directory = steam.resolve(wuDir);
            if(Files.exists(directory) && Files.isDirectory(directory)) return directory;
            directory = Paths.get("D:\\Program Files (x86)\\Steam").resolve(wuDir);
            if(Files.exists(directory) && Files.isDirectory(directory)) return directory;
        } else if(FileUtil.isMacintosh()) {
            steam = Paths.get(homeDir+"/Library/Application Support/Steam");
        } else if(FileUtil.isLinux()) {
            steam = Paths.get(homeDir+"/.steam/steam");
            if(!Files.exists(steam)) {
                steam = Paths.get(homeDir+"/.local/share/Steam").resolve(wuDir);
            }
        }
        if(steam!=null && Files.exists(steam)) {
            log("Found Steam directory at: "+steam.toAbsolutePath().toString());
            directory = steam.resolve(wuDir);
            if(Files.exists(directory)) return directory;
            log("Wurm wasn't installed in the default directory, looking into Steam configurations...");
            directory = findWurmLauncherDirectoryFromVDF(steam,wuDir);
            if(Files.exists(directory)) return directory;
        }
        log("No Wurm installation was found in the default locations or Steam configurations, "+
            "choose the installation directory instead.");
        log("If you didn't install Wurm, and don't know where it is located, please ask the person "+
            "who did it for you.");
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File(homeDir));
        chooser.setDialogTitle("Wurm Unlimited Client Directory");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        if(chooser.showOpenDialog(null)==JFileChooser.APPROVE_OPTION) {
            directory = chooser.getSelectedFile().toPath();
            log("Got path: "+directory.toAbsolutePath());
            if(Files.exists(directory) && Files.isDirectory(directory)) {
                String name = directory.getFileName().toString();
                if(name.equals("WurmLauncher")) return directory;
                log("Directory name: "+name);
                if(name.equals("Wurm Unlimited")) {
                    directory = directory.resolve("WurmLauncher");
                    log("Checking if "+directory.toAbsolutePath()+" is a directory...");
                    if(Files.exists(directory) && Files.isDirectory(directory)) return directory;
                } else if(name.equals("common")) {
                    directory = directory.resolve("Wurm Unlimited"+sep+"WurmLauncher");
                    log("Checking if "+directory.toAbsolutePath()+" is a directory...");
                    if(Files.exists(directory) && Files.isDirectory(directory)) return directory;
                } else if(name.equals("steamapps")) {
                    directory = directory.resolve("common"+sep+"Wurm Unlimited"+sep+"WurmLauncher");
                    log("Checking if "+directory.toAbsolutePath()+" is a directory...");
                    if(Files.exists(directory) && Files.isDirectory(directory)) return directory;
                } else if(name.equalsIgnoreCase("steam")) {
                    directory = directory.resolve(wuDir);
                    log("Checking if "+directory.toAbsolutePath()+" is a directory...");
                    if(Files.exists(directory) && Files.isDirectory(directory)) return directory;
                } else {
                    Path parent = directory.getParent();
                    log("Testing if parent directory is right: "+parent.toAbsolutePath());
                    if(parent.getFileName().toString().equals("WurmLauncher")) return parent;
                }
            }
        } else {
            if(window!=null) window.close();
            System.exit(0);
        }
        return null;
    }

    private Path findWurmLauncherDirectoryFromVDF(Path steam,Path wuDir) {
        Path libraryfolders = steam.resolve("steamapps").resolve("libraryfolders.vdf");
        if(Files.exists(libraryfolders)) {
            String file = FileUtil.readTextFile(libraryfolders);
            VDF vdf = new VDF();
            try {
                VDF.VDFObject vdfObject = vdf.parse(file);
                VDF.VDFObject o1 = vdfObject.getValue("LibraryFolders");
                if(o1!=null) {
                    for(Map.Entry<String,VDF.VDFObject> entry : o1.values.entrySet()) {
                        String key = entry.getKey();
                        VDF.VDFObject o2 = entry.getValue();
                        try {
                            int n = Integer.parseInt(key);
                            Path dir = Paths.get(o2.value);
                            if(Files.exists(dir)) {
                                dir = dir.resolve(wuDir);
                                if(Files.exists(dir)) return dir;
                            }
                        } catch(NumberFormatException e) {}
                    }
                }
            } catch(VDF.VDFException e) {}
        }
        return null;
    }

    private void log(String text) {
        if(window!=null) window.log(text);
    }

    public static void messageBox(String message) {
        messageBox(message,JOptionPane.PLAIN_MESSAGE,0);
    }

    public static void errorMessage(String message) {
        messageBox(message,JOptionPane.ERROR_MESSAGE,1);
    }

    public static void messageBox(String message,int messageType,int exitStatus) {
        JOptionPane.showMessageDialog(null,message,"Wurm Unlimited Forge",messageType);
    }

    public static boolean confirmBoxOkCancel(String question) {
        return confirmBox(question,JOptionPane.OK_CANCEL_OPTION);
    }

    public static boolean confirmBoxYesNo(String question) {
        return confirmBox(question,JOptionPane.YES_NO_OPTION);
    }

    public static boolean confirmBoxYesNoCancel(String question) {
        return confirmBox(question,JOptionPane.YES_NO_CANCEL_OPTION);
    }

    public static boolean confirmBox(String question,int optionType) {
        int result = JOptionPane.showConfirmDialog(null,question,"Wurm Unlimited Forge",optionType);
        return result==JOptionPane.OK_OPTION || result==JOptionPane.YES_OPTION;
    }

    public static void pause(int ms) {
        try {
            Thread.sleep(ms);
        } catch(InterruptedException e) {
            System.err.format("IOException: %s%n",e);
        }
    }
}
