package org.gotti.wurmunlimited.serverlauncher;

import net.wurmunlimited.forge.config.ForgeServerConfig;
import net.wurmunlimited.forge.util.FileUtil;
import org.gotti.wurmunlimited.modloader.ModLoader;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.ModEntry;
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;
import org.gotti.wurmunlimited.modloader.server.ServerHook;

import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DelegatedLauncher {

    public static void main(String[] args) {

        Properties properties = FileUtil.loadProperties("forge.properties");
        ForgeServerConfig.init(properties);

        try {
            ModLoader modLoader = new ModLoader();
            List<? extends ModEntry<WurmServerMod>> wurmMods = modLoader.loadModsFromModDir(Paths.get("mods"));

            ServerHook serverHook = ServerHook.createServerHook();
            serverHook.addMods(wurmMods);
            serverHook.addVersionHandler(modLoader.getVersion(),modLoader.getGameVersion(),wurmMods);
            HookManager.getInstance().initCallbacks();

            String[] classes = {
                "com.wurmonline.server.gui.WurmServerGuiMainDeferred",
                "com.wurmonline.server.gui.WurmServerGuiMain"
            };

            for(String classname : classes) {

                try {
                    HookManager.getInstance().getLoader().run(classname,args);
                    return;
                } catch(ClassNotFoundException e) {
                    continue;
                }
            }

            throw new ClassNotFoundException("com.wurmonline.server.gui.WurmServerGuiMain");
        } catch(Throwable e) {
            Logger.getLogger(DelegatedLauncher.class.getName()).log(Level.SEVERE,e.getMessage(),e);
            e.printStackTrace();
            System.exit(-1);
        }
    }

}
