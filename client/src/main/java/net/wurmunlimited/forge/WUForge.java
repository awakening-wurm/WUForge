package net.wurmunlimited.forge;

import com.wurmonline.client.WurmClientBase;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import net.wurmunlimited.forge.packs.ServerPacks;
import org.gotti.wurmunlimited.modloader.ModLoader;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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

        Properties properties = loadProperties("forge.properties");
        Config.getInstance().configure(properties);
        initSteamAppId();
        ServerConnection.getInstance().getAvailableMods(Config.modsLibDir);
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
        File dir = new File("");
        extractFile(WUForge.class,dir,"","steam_appid.txt");
        /*try {
            Path steamAppId = Paths.get("steam_appid.txt");
            if(!Files.exists(steamAppId)) {
                InputStream src = ClientLauncher.class.getResourceAsStream("/steam_appid.txt");
                Files.copy(src,steamAppId,StandardCopyOption.REPLACE_EXISTING);
            }
        } catch(IOException e) {
            System.out.println("Unable to write steam appid: "+e.getMessage());
            e.printStackTrace();
        }*/
    }

    private void extractCredits() {
        File htmlDir = new File(Config.forgeDir+File.separator+"html");
        if(!htmlDir.exists()) htmlDir.mkdir();
        File file = extractFile(WurmClientBase.class,htmlDir,"html","credits_wu.html");
        extractFile(WurmClientBase.class,htmlDir,"html","unlimited.png");
        if(file!=null) {
            try {
                creditsURL = file.toURI().toURL().toString();
                logger.info("Credits URL: "+creditsURL);
            } catch(MalformedURLException e) {}
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

    public static Properties loadProperties(String file) {
        Properties properties = new Properties();
        Path path = Paths.get(file);
        if(!Files.exists(path)) {
            logger.warning("The config file seems to be missing.");
            return properties;
        }
        InputStream stream = null;
        try {
            logger.info("Opening the config file.");
            stream = Files.newInputStream(path);
            logger.info("Reading from the config file.");
            properties.load(stream);
            logger.info("Configuration loaded.");
        } catch(Exception e) {
            logger.log(Level.SEVERE,"Error while reloading properties file.",e);
        } finally {
            try {
                if(stream!=null) stream.close();
            } catch(Exception e) {
                logger.log(Level.SEVERE,"Properties file not closed, possible file lock.",e);
            }
        }
        return properties;
    }

    /*private void initLogger() {
        Formatter formatter = new OneLineLogMessageFormatter();
        Handler handler = new StreamHandler(System.out,new SimpleFormatter()) {
            @Override
            public void publish(LogRecord record) {
                if(!CONSOLE_LOGGER.equals(record.getLoggerName())) {
                    try {
                        inLogHandler.set(true);
                        System.out.println(formatter.format(record));
                    } finally {
                        inLogHandler.remove();
                    }
                }
            }
        };
        Logger.getLogger("").addHandler(handler);
    }

    public ConsoleListenerClass createConsoleListener() {
        initLogger();
        return new ConsoleListenerClass() {
            @Override
            public void consoleOutput(String message) {
                Boolean b = inLogHandler.get();
                if(b==null || !b) {
                    Logger.getLogger(CONSOLE_LOGGER).log(Level.INFO,message);
                }
            }

            @Override
            public void consoleClosed() {}
        };
    }*/
}
