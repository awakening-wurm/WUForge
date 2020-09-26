package org.gotti.wurmunlimited.clientlauncher;

import javassist.*;
import javassist.bytecode.Descriptor;
import net.wurmunlimited.forge.config.ForgeClientConfig;
import net.wurmunlimited.forge.util.FileUtil;
import org.gotti.wurmunlimited.modloader.ModLoader;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DelegatedLauncher {

    private static final String WURM_MAIN_CLASS = "com.wurmonline.client.launcherfx.WurmMain";

    public static void main(String[] args) {

        Properties properties = FileUtil.loadProperties("forge.properties");
        ForgeClientConfig.init(properties);

        try {
            new ModLoader().loadModsFromModDir();

            setupClientLoggerHooks();

            HookManager.getInstance().getLoader().run(WURM_MAIN_CLASS,args);
        } catch(Throwable e) {
            Logger.getLogger(DelegatedLauncher.class.getName()).log(Level.SEVERE,e.getMessage(),e);
            e.printStackTrace();
            try {
                Thread.sleep(10000);
            } catch(InterruptedException e2) {
            }
            System.exit(-1);
        }
    }

    private static void setupClientLoggerHooks() throws CannotCompileException, NotFoundException {
        String code =
            "org.gotti.wurmunlimited.clientlauncher.ClientLogger.initLogger();"+
            "consoleOutputStream.addCopy(org.gotti.wurmunlimited.clientlauncher.ClientLogger.createConsoleListener());\n";
        ClassPool classPool = HookManager.getInstance().getClassPool();
        String descriptor = Descriptor.ofMethod(classPool.get("void"),new CtClass[]{classPool.get("java.lang.String[]")});
        CtMethod ctMain = classPool.get(WURM_MAIN_CLASS).getMethod("main",descriptor);
        ctMain.insertBefore(code);
    }
}
