package net.wurmunlimited.forge;

import net.wurmunlimited.forge.config.ForgeServerConfig;
import net.wurmunlimited.forge.util.FileUtil;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Logger;

public class WUForge {
    private static final Logger logger = Logger.getLogger(WUForge.class.getName());

    public static void main(String[] args) {
        WUForge.getInstance().init(args);
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

        Path baseDir = Paths.get("");
        Properties properties = FileUtil.loadProperties("forge.properties");
        ForgeServerConfig.init(baseDir,properties);
    }
}
