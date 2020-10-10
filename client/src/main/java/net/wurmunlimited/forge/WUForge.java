package net.wurmunlimited.forge;

import com.wurmonline.client.WurmClientBase;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import net.wurmunlimited.forge.config.ForgeClientConfig;
import net.wurmunlimited.forge.config.ForgeConfig;
import net.wurmunlimited.forge.packs.ServerPacks;
import net.wurmunlimited.forge.util.FileUtil;
import org.gotti.wurmunlimited.modloader.ModLoader;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WUForge {
    private static final Logger logger = Logger.getLogger(WUForge.class.getName());

    //    private static final String CONSOLE_LOGGER = "com.wurmonline.console";
    //    private static ThreadLocal<Boolean> inLogHandler = new ThreadLocal<Boolean>();

    private static String creditsURL = null;

    public static void main(String[] args) {
        WUForge.getInstance().init(args);
    }

    public static URL getResource(String s) {
        if(s.startsWith("/")) s = s.substring(1);
        return WUForge.class.getResource("launcher/"+s);
    }

    public static void ServerBrowserFX_initialize(ComboBox<String> modsConfigBox) {
        logger.info("ServerBrowserFX_initialize");
        String[] modsProfiles = { "default","test","test2" };
        modsConfigBox.setItems(FXCollections.observableArrayList(modsProfiles));
        modsConfigBox.getSelectionModel().select(modsProfiles[0]);
    }

    public static void changeModsConfig(ComboBox<String> modsConfigBox) {
        logger.info("changeModsConfig");
    }

    public static void launchModsSettings(ComboBox<String> modsConfigBox) {
        logger.info("launchModsSettings");
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

        Properties properties = FileUtil.loadProperties("forge.properties");
        ForgeClientConfig.init(Paths.get(""),properties);
        initSteamAppId();
        ServerConnection.getInstance().getAvailableMods(config.getModsLibDir());
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
