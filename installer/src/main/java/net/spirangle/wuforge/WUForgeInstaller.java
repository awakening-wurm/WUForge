package net.spirangle.wuforge;

import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class WUForgeInstaller {

    private static String OS = System.getProperty("os.name").toLowerCase();

    public static void main(final String[] args) {
        WUForgeInstaller installer = new WUForgeInstaller();
        installer.install();
    }

    private class WurmFiles {
        boolean isInstalled;
        File wurmLauncher;
        File clientJar;
        File commonJar;
        File launchConfig;
        File libsteamApi;
        File libDir;
        File nativeLibsDir;
        File packsDir;
        File playerFilesDir;

        void findFiles() {
            wurmLauncher = new File(clientDir,"WurmLauncher");
            clientJar = new File(clientDir,"client.jar");
            commonJar = new File(clientDir,"common.jar");
            launchConfig = new File(clientDir,"LaunchConfig.ini");
            libsteamApi = new File(clientDir,"libsteam_api.so");
            libDir = new File(clientDir,"lib");
            nativeLibsDir = new File(clientDir,"nativelibs");
            packsDir = new File(clientDir,"packs");
            playerFilesDir = new File(clientDir,"PlayerFiles");
            isInstalled = wurmLauncher.exists() && wurmLauncher.isFile() &&
                          clientJar.exists() && clientDir.isFile() &&
                          commonJar.exists() && commonJar.isFile() &&
                          launchConfig.exists() && launchConfig.isFile() &&
                          libsteamApi.exists() && libsteamApi.isFile() &&
                          libDir.exists() && libDir.isDirectory() &&
                          nativeLibsDir.exists() && nativeLibsDir.isDirectory() &&
                          packsDir.exists() && packsDir.isDirectory() &&
                          playerFilesDir.exists() && playerFilesDir.isDirectory();
        }
    }

    private class AgoFiles {
        boolean isInstalled ;
        File clientPatched;
        File modLauncher;
        File patcherJar;
        File patcherBat;
        File patcherSh;
        File loggingProperties;
        File javassist;
        File modsDir;

        void findFiles() {
            clientPatched = new File(clientDir,"client-patched.jar");
            modLauncher = new File(clientDir,"modlauncher.jar");
            patcherJar = new File(clientDir,"patcher.jar");
            patcherBat = new File(clientDir,"patcher.bat");
            patcherSh = new File(clientDir,"patcher.sh");
            loggingProperties = new File(clientDir,"logging.properties");
            javassist = new File(clientDir,"javassist.jar");
            modsDir = new File(clientDir,"mods");
            isInstalled = clientPatched.exists() && clientPatched.isFile() &&
                          modsDir.exists() && modsDir.isDirectory();
        }

    }

    private class ForgeFiles {
        boolean isInstalled;
        File forgeProperties;
        File forgeDir;

        void findFiles() {
            forgeProperties = new File(clientDir,"forge.properties");
            forgeDir = new File(forgeDir,"forge");
            isInstalled = forgeProperties.exists() && forgeProperties.isFile() &&
                          forgeDir.exists() && forgeDir.isDirectory();
        }

        void uninstall() {
            deleteFile(forgeProperties);
            deleteFile(forgeDir);
        }
    }

    private File clientDir = null;
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
        wurmFiles.findFiles();
        agoFiles.findFiles();
        forgeFiles.findFiles();
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
                                     "Would you like to uninstall Wurm Unlimited Forge?")) {
                forgeFiles.uninstall();
            }
            System.exit(0);
        }
        writeForgeProperties();
        if(agoFiles.isInstalled) {

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

    private File getWurmLauncherDirectory() {
        File directory;
        String sep = File.separator;
        String wuDir = "steamapps"+sep+"common"+sep+"Wurm Unlimited"+sep+"WurmLauncher";
        if(OS.contains("win")) {
            directory = new File("C:\\Program Files (x86)\\Steam\\"+wuDir);
            if(!directory.exists() && directory.isDirectory()) return directory;
            directory = new File("D:\\Program Files (x86)\\Steam\\"+wuDir);
            if(directory.exists() && directory.isDirectory()) return directory;
        } else if(OS.contains("mac")) {
            String homeDir = System.getProperty("user.home");
            directory = new File(homeDir+"/Library/Application Support/Steam/"+wuDir);
            if(directory.exists() && directory.isDirectory()) return directory;
        } else if(OS.contains("nix") || OS.contains("nux") || OS.contains("aix")) {
            String homeDir = System.getProperty("user.home");
            directory = new File(homeDir+"/.steam/steam/"+wuDir);
            if(directory.exists() && directory.isDirectory()) return directory;
            directory = new File(homeDir+"/.local/share/Steam/"+wuDir);
            if(directory.exists() && directory.isDirectory()) return directory;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File("."));
        chooser.setDialogTitle("Wurm Unlimited Client Directory");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        if(chooser.showOpenDialog(null)==JFileChooser.APPROVE_OPTION) {
            directory = chooser.getSelectedFile();
            System.out.println("Got path: "+directory.getAbsolutePath());
            if(directory.exists() && directory.isDirectory()) {
                String name = directory.getName();
                if(name.equals("WurmLauncher")) return directory;
                System.out.println("Directory name: "+name);
                if(name.equals("Wurm Unlimited")) {
                    directory = new File(directory,"WurmLauncher");
                    System.out.println("Checking if "+directory.getAbsolutePath()+" is a directory...");
                    if(directory.exists() && directory.isDirectory()) return directory;
                } else if(name.equals("common")) {
                    directory = new File(directory,"Wurm Unlimited"+sep+"WurmLauncher");
                    System.out.println("Checking if "+directory.getAbsolutePath()+" is a directory...");
                    if(directory.exists() && directory.isDirectory()) return directory;
                } else if(name.equals("steamapps")) {
                    directory = new File(directory,"common"+sep+"Wurm Unlimited"+sep+"WurmLauncher");
                    System.out.println("Checking if "+directory.getAbsolutePath()+" is a directory...");
                    if(directory.exists() && directory.isDirectory()) return directory;
                } else if(name.equalsIgnoreCase("steam")) {
                    directory = new File(directory,wuDir);
                    System.out.println("Checking if "+directory.getAbsolutePath()+" is a directory...");
                    if(directory.exists() && directory.isDirectory()) return directory;
                } else {
                    File parent = directory.getParentFile();
                    System.out.println("Testing if parent directory is right: "+parent.getAbsolutePath());
                    if(parent.getName().equals("WurmLauncher")) return parent;
                }
            }
        }
        return null;
    }

    private void writeForgeProperties() {
        extractFile(WUForgeInstaller.class,clientDir,"","forge.properties");
    }

    private void writeTextFile(File file,String text) {
        writeFile(file,text.getBytes(Charset.forName("UTF-8")));
    }

    public void writeFile(File file,byte[] bytes) {
        FileOutputStream fos = null;
        try {
            if(!file.exists()) file.createNewFile();
            fos = new FileOutputStream(file);
            fos.write(bytes);
            fos.flush();
            System.out.println("File Written Successfully: "+file.getAbsolutePath());
        } catch(IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(fos!=null) fos.close();
            } catch(IOException e) {
                System.out.println("Error in closing the file.");
            }
        }
    }

    public File extractFile(Class cl,File outputDir,String resourcePath,String resource) {
        String dir = outputDir.toString();
        String out = resource;
        if(dir.length()>0) out = dir+(dir.endsWith(File.separator)? "" : File.separator)+out;
        File file = new File(out);
        InputStream link = null;
        try {
            if(!file.exists()) {
                if(!resourcePath.endsWith("/")) resourcePath += "/";
                link = cl.getResourceAsStream(resourcePath+resource);
                Files.copy(link,file.getAbsoluteFile().toPath(),StandardCopyOption.REPLACE_EXISTING);
            }
        } catch(IOException e) {
            e.printStackTrace();
            file = null;
        } finally {
            try {
                if(link!=null) link.close();
            } catch(IOException e) {
                System.out.println("Error in closing the stream.");
            }
        }
        return file;
    }

    private void deleteFile(File file) {
        if(file.isDirectory())
            for(File f : file.listFiles())
                deleteFile(f);
        file.delete();
    }
}
