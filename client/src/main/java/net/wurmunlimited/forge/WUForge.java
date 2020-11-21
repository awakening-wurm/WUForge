package net.wurmunlimited.forge;

import com.wurmonline.client.WurmClientBase;
import com.wurmonline.client.startup.ServerBrowserFX;
import com.wurmonline.client.steam.SteamHandler;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import net.wurmunlimited.forge.config.ForgeClientConfig;
import net.wurmunlimited.forge.config.ForgeConfig;
import net.wurmunlimited.forge.launcherfx.ModsSettingsFX;
import net.wurmunlimited.forge.packs.ServerPacks;
import net.wurmunlimited.forge.util.FileUtil;
import org.gotti.wurmunlimited.modloader.ModLoader;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class WUForge {
    private static final Logger logger = Logger.getLogger(WUForge.class.getName());

    //    private static final String CONSOLE_LOGGER = "com.wurmonline.console";
    //    private static ThreadLocal<Boolean> inLogHandler = new ThreadLocal<Boolean>();

    private static String creditsURL = null;

    public static void main(String[] args) {
        WUForge.getInstance().init(args);
    }

    @SuppressWarnings("unused")
    public static URL getResource(String s) {
        if(s.startsWith("/")) s = s.substring(1);
        return WUForge.class.getResource("launcher/"+s);
    }

    @SuppressWarnings("unused")
    public static void ServerBrowserFX_initialize(ComboBox<String> modsConfigBox) {
        logger.info("ServerBrowserFX_initialize");
        ForgeClientConfig config = ForgeClientConfig.getInstance();
        List<Path> profileDirs = FileUtil.findDirectories(config.getModsProfilesDir(),false,false);
        List<String> profiles = profileDirs.stream().map(p -> p.getFileName().toString()).collect(Collectors.toList());
        String profile = config.getModsProfile();
        if(!profiles.contains(profile)) {
            profile = profiles.get(0);
            config.setModsProfile(profile);
        }
        modsConfigBox.setItems(FXCollections.observableArrayList(profiles));
        modsConfigBox.getSelectionModel().select(profile);
    }

    @SuppressWarnings("unused")
    public static void changeModsConfig(ComboBox<String> modsConfigBox) {
        String value = modsConfigBox.getValue();
        logger.info("changeModsConfig(value="+value+")");
    }

    @SuppressWarnings("unused")
    public static void launchModsSettings(ComboBox<String> modsConfigBox) {
        String value = modsConfigBox.getValue();
        ServerBrowserFX serverBrowserFX = null;
        try {
            Field serverBrowserFXField = SteamHandler.class.getDeclaredField("serverBrowserFX");
            serverBrowserFXField.setAccessible(true);
            serverBrowserFX = (ServerBrowserFX)serverBrowserFXField.get(WurmClientBase.steamHandler);
        } catch(NoSuchFieldException|IllegalArgumentException|IllegalAccessException e) {
            e.printStackTrace();
        }
        logger.info("launchModsSettings(value="+value+",serverBrowserFX="+serverBrowserFX+")");
        ModsSettingsFX modsSettingsFX = ModsSettingsFX.getInstance(true);
        modsSettingsFX.setLauncherWindow(serverBrowserFX);
        modsSettingsFX.restart();
        modsSettingsFX.show();
    }

    public static String getCreditsURL() {
        return creditsURL==null? "" : creditsURL;
    }

    public static boolean verbose = false;
    public static boolean debug = false;

    private static WUForge instance = null;

    public static WUForge getInstance() {
        if(instance==null) instance = new WUForge();
        return instance;
    }

    private WUForge() {

    }

    private void init(String[] args) {
        System.out.println("Running: WUForge");

        Path baseDir = Paths.get("").toAbsolutePath();
        Properties properties = FileUtil.loadProperties(baseDir.resolve("forge.properties"));
        ForgeClientConfig.init(baseDir,properties);
        initSteamAppId();
        ServerConnection.getInstance().init();
        CodeInjections.preInit();
        ServerPacks.getInstance().init();

        try {
            logger.info("loadModsFromModDir");
            new ModLoader().loadModsFromModDir();

            extractCredits();

            HookManager.getInstance().getLoader().run("com.wurmonline.client.launcherfx.WurmMain",args);
        } catch(Throwable e) {
            Logger.getLogger(WUForge.class.getName()).log(Level.SEVERE,e.getMessage(),e);
            e.printStackTrace();
            try {
                Thread.sleep(10000L);
            } catch(InterruptedException ex) {}
            System.exit(-1);
        }
    }

    private void initSteamAppId() {
        Path steamAppId = Paths.get("steam_appid.txt");
        FileUtil.extractFile(WUForge.class,steamAppId,"/steam_appid.txt",false);
    }

    private void extractCredits() {
        ForgeConfig config = ForgeClientConfig.getInstance();
        Path htmlDir = config.getForgeDir().resolve("html");
        if(!Files.exists(htmlDir)) {
            try {
                FileUtil.createDirectory(htmlDir);
            } catch(IOException e) {
                return;
            }
        }
        Path file = FileUtil.extractFile(WurmClientBase.class,htmlDir.resolve("credits_wu.html"),"html/credits_wu.html",false);
        FileUtil.extractFile(WurmClientBase.class,htmlDir.resolve("unlimited.png"),"html/unlimited.png",false);
        if(file!=null) {
            try {
                creditsURL = file.toUri().toURL().toString();
                logger.info("Credits URL: "+creditsURL);
            } catch(MalformedURLException e) {}
        }
    }
}
