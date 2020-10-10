package net.wurmunlimited.forge;

import net.wurmunlimited.forge.util.FileUtil;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class WUForgeInstallerTest {

    private static final int WU_CLIENT_FILES = 9;
    private static final int AGO_MODLOADER_FILES = 19;

    @Test
    public void testInstallForgeOnWurmUnlimited() {
        Path testDir = Paths.get("test");
        Path wuDir = testDir.resolve("wu");
        Path tempDir = testDir.resolve("wu_temp");

        FileUtil.deleteFiles(FileUtil.findFiles(tempDir));

        copyTempFiles(wuDir,tempDir,WU_CLIENT_FILES);

        WUForgeInstaller installer = initInstaller(tempDir);
        Assert.assertTrue("WU client files are correctly installed.",installer.wurmFiles.isInstalled());
        Assert.assertFalse("Ago modloader isn't installed.",installer.agoFiles.isInstalled());
        Assert.assertFalse("WU Forge isn't installed.",installer.forgeFiles.isInstalled());

        boolean ret = installer.forgeFiles.createDirectories();
        Assert.assertTrue("Forge directories are properly installed.",ret);

        installForge(testDir,installer);
        Assert.assertTrue("Forge is properly installed.",installer.forgeFiles.isInstalled());

        FileUtil.deleteFiles(FileUtil.findFiles(tempDir));
        Assert.assertFalse(Files.exists(tempDir));
    }

    @Test
    public void testInstallForgeOnAgoModloader() {
        Path testDir = Paths.get("test");
        Path wuDir = testDir.resolve("ago");
        Path tempDir = testDir.resolve("ago_temp");

        FileUtil.deleteFiles(FileUtil.findFiles(tempDir));

        copyTempFiles(wuDir,tempDir,AGO_MODLOADER_FILES);

        WUForgeInstaller installer = initInstaller(tempDir);
        Assert.assertTrue("WU client files are correctly installed.",installer.wurmFiles.isInstalled());
        Assert.assertTrue("Ago modloader is installed.",installer.agoFiles.isInstalled());
        Assert.assertFalse("WU Forge isn't installed.",installer.forgeFiles.isInstalled());

        installer.agoFiles.uninstall();

        boolean ret = installer.forgeFiles.createDirectories();
        Assert.assertTrue("Forge directories are properly installed.",ret);

        installer.forgeFiles.installAgoMods(installer.agoFiles.modsDir);
        installer.agoFiles.uninstallMods();
        Assert.assertFalse("Ago modloader is uninstalled.",installer.agoFiles.isInstalled());

        installForge(testDir,installer);
        Assert.assertTrue("Forge is properly installed.",installer.forgeFiles.isInstalled());

        FileUtil.deleteFiles(FileUtil.findFiles(tempDir));
        Assert.assertFalse(Files.exists(tempDir));
    }

    private void copyTempFiles(Path sourceDir,Path tempDir,int copyFiles) {
        int files = 0;
        try {
            FileUtil.createDirectory(tempDir);
            files = FileUtil.copyDirectory(sourceDir,tempDir,true);
        } catch(IOException e) {}
        Assert.assertEquals("Number of files copied is correct.",files,copyFiles);
    }

    private WUForgeInstaller initInstaller(Path tempDir) {
        WUForgeInstaller installer = new WUForgeInstaller();
        installer.clientDir = tempDir.resolve("steamapps").resolve("common").resolve("Wurm Unlimited").resolve("WurmLauncher");
        Assert.assertTrue("WU client dir exists and is a directory.",Files.exists(installer.clientDir) && Files.isDirectory(installer.clientDir));

        installer.wurmFiles.init();
        installer.agoFiles.init();
        installer.forgeFiles.init();
        return installer;
    }

    private void installForge(Path testDir,WUForgeInstaller installer) {
        Path forgeFilesDir = testDir.resolve("forge");
        try {
            Files.move(installer.forgeFiles.forgeClient,installer.forgeFiles.wurmClient,REPLACE_EXISTING);
            Files.copy(forgeFilesDir.resolve("client.jar"),installer.forgeFiles.forgeClient,REPLACE_EXISTING);
            Files.copy(forgeFilesDir.resolve("forge.properties"),installer.forgeFiles.forgeProperties,REPLACE_EXISTING);
            Files.copy(forgeFilesDir.resolve("forge.jar"),installer.forgeFiles.forgeJar,REPLACE_EXISTING);
            Files.copy(forgeFilesDir.resolve("logging.properties"),installer.forgeFiles.loggingProperties,REPLACE_EXISTING);
        } catch(IOException e) {}
    }
}
