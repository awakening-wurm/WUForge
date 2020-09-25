package net.wurmunlimited.forge;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
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
            if(deleteFiles(files)) {
                try {
                    Files.move(wurmClient,agoClient,REPLACE_EXISTING);
                    return true;
                } catch(IOException e) {}
            }
            return false;
        }

        boolean uninstallMods() {
            return deleteFiles(findFiles(modsDir));
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
            if(deleteFiles(files)) {
                try {
                    deleteFiles(findFiles(forgeDir));
                    Files.move(wurmClient,forgeClient,REPLACE_EXISTING);
                    return true;
                } catch(IOException e) {}
            }
            return false;
        }

        void writeProperties() {
            extractFile(WUForgeInstaller.class,clientDir,"","forge.properties",false);
        }

        boolean createDirectories() {
            try {
                createDirectory(forgeDir);
                createDirectory(modsDir);
                createDirectory(profilesDir);
                createDirectory(profilesDefaultDir);
                createDirectory(modsLibDir);
                return true;
            } catch(IOException e) {}
            return false;
        }

        void installAgoMods(Path agoModsDir) {
            if(!Files.exists(agoModsDir) || !Files.isDirectory(agoModsDir)) return;
            List<Path> files = findFiles(agoModsDir);
            for(Path file : files) {
                if(!Files.exists(file)) continue;
                try {
                    Path rel = agoModsDir.relativize(file);
                    int n = rel.getNameCount();
                    if(Files.isDirectory(file)) {
                        if(n==1) continue;
                        Path dir = modsLibDir.resolve(rel);
                        createDirectory(dir);
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
            errorMessage("Could not find the installation directory\n"+
                         "of the Wurm Unlimited client.\n"+
                         "\n"+
                         "Please contact support for help.");
        }
        wurmFiles.init();
        agoFiles.init();
        forgeFiles.init();
        if(!wurmFiles.isInstalled || (agoFiles.isInstalled && forgeFiles.isInstalled)) {
            errorMessage("The Wurm Unlimited client's file structure is\n"+
                         "corrupted. Please uninstall and remove all\n"+
                         "files and directories except PlayerFiles, and\n"+
                         "re-install Wurm Unlimited, then run WUForge\n"
                         +"again.");
        }
        if(forgeFiles.isInstalled) {
            if(confirmBoxYesNoCancel("Wurm Unlimited Forge is already installed.\n"+
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
            errorMessage("Could not create Wurm Unlimited Forge directories.\n"+
                         "Please make sure you have the right permissions set,\n"+
                         "if unsure contact support.");
        }
        if(agoFiles.isInstalled) {
            forgeFiles.installAgoMods(agoFiles.modsDir);
            agoFiles.uninstallMods();
        }
    }

    private void messageBox(String message) {
        messageBox(message,JOptionPane.PLAIN_MESSAGE,0);
    }

    private void errorMessage(String message) {
        messageBox(message,JOptionPane.ERROR_MESSAGE,1);
    }

    private void messageBox(String message,int messageType,int exitStatus) {
        JOptionPane.showMessageDialog(null,message,"Wurm Unlimited Forge",messageType);
        System.exit(exitStatus);
    }

    private boolean confirmBoxOkCancel(String question) {
        return confirmBox(question,JOptionPane.OK_CANCEL_OPTION);
    }

    private boolean confirmBoxYesNo(String question) {
        return confirmBox(question,JOptionPane.YES_NO_OPTION);
    }

    private boolean confirmBoxYesNoCancel(String question) {
        return confirmBox(question,JOptionPane.YES_NO_CANCEL_OPTION);
    }

    private boolean confirmBox(String question,int optionType) {
        int result = JOptionPane.showConfirmDialog(null,question,"Wurm Unlimited Forge",optionType);
        return result==JOptionPane.OK_OPTION || result==JOptionPane.YES_OPTION;
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

    private void writeTextFile(Path file,String text) {
        writeFile(file,text.getBytes(Charset.forName("UTF-8")));
    }

    public void writeFile(Path file,byte[] bytes) {
        OutputStream os = null;
        try {
            if(!Files.exists(file)) Files.createFile(file);
            os = Files.newOutputStream(file);
            os.write(bytes);
            os.flush();
            System.out.println("File Written Successfully: "+file.toAbsolutePath());
        } catch(IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(os!=null) os.close();
            } catch(IOException e) {
                System.out.println("Error in closing the file.");
            }
        }
    }

    public Path extractFile(Class cl,Path outputFile,String resourcePath,String resource,boolean overwrite) {
        InputStream link = null;
        try {
            if(!Files.exists(outputFile) || overwrite) {
                if(!resourcePath.endsWith("/")) resourcePath += "/";
                link = cl.getResourceAsStream(resourcePath+resource);
                Files.copy(link,outputFile.toAbsolutePath(),REPLACE_EXISTING);
            }
        } catch(IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(link!=null) link.close();
            } catch(IOException e) {
                System.out.println("Error in closing the stream.");
            }
        }
        return outputFile;
    }

    private List<Path> findFiles(Path dir) {
        if(!Files.exists(dir) || !Files.isDirectory(dir)) return null;
        List<Path> files = new ArrayList<>();
        try {
            LinkedList<Path> dirs = new LinkedList<>();
            dirs.push(dir);
            while(!dirs.isEmpty()) {
                Path d = dirs.pop();
                try(DirectoryStream<Path> ds = Files.newDirectoryStream(d)) {
                    for(Path f : ds) {
                        if(Files.isDirectory(f)) dirs.push(f);
                        else if(Files.isRegularFile(f)) files.add(f);
                    }
                }
            }
        } catch(IOException e) {}
        return files;
    }

    private boolean deleteFiles(Path[] files) {
        if(files!=null && files.length>0) {
            try {
                for(int i=files.length-1; i>=0; --i) {
                    if(files[i]==null) continue;
                    Files.deleteIfExists(files[i]);
                    files[i] = null;
                }
            } catch(IOException e) {
                return false;
            }
        }
        return true;
    }

    private boolean deleteFiles(List<Path> files) {
        if(files!=null && !files.isEmpty()) {
            try {
                for(int i=files.size()-1; i>=0; --i) {
                    Files.deleteIfExists(files.get(i));
                }
            } catch(IOException e) {
                return false;
            }
        }
        return true;
    }

    private boolean createDirectory(Path dir) throws IOException {
        if(!Files.exists(dir)) {
            try {
                Files.createDirectory(dir);
                return true;
            } catch(IOException e) {}
        } else if(!Files.isDirectory(dir)) {
            throw new IOException("File "+dir.toAbsolutePath()+" is not a directory!");
        }
        return false;
    }
}
