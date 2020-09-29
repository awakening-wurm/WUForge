package org.gotti.wurmunlimited.clientlauncher;

import javassist.ClassPool;
import javassist.Loader;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.LogManager;

public class ClientLauncher {

    public static void main(String[] args) {
        initLogger();

        try {
            ClassPool classPool = HookManager.getInstance().getClassPool();
            classPool.insertClassPath("forge.client.jar");

            Loader loader = HookManager.getInstance().getLoader();
            loader.delegateLoadingOf("javafx.");
            loader.delegateLoadingOf("com.sun.");
            loader.delegateLoadingOf("org.controlsfx.");
            loader.delegateLoadingOf("impl.org.controlsfx");
            loader.delegateLoadingOf("com.mysql.");
            loader.delegateLoadingOf("org.sqlite.");
            loader.delegateLoadingOf("org.gotti.wurmunlimited.modloader.classhooks.");
            //loader.delegateLoadingOf("org.gotti.wurmunlimited.modloader.");
            loader.delegateLoadingOf("javassist.");

            Thread.currentThread().setContextClassLoader(loader);

            loader.run("net.wurmunlimited.forge.WUForge",args);
        } catch(Throwable e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private static void initLogger() {
        try {
            //  Use externally configured loggers
            if(System.getProperty("java.util.logging.config.file")==null &&
               System.getProperty("java.util.logging.config.class")==null) {
                // Use a provider logging.properties file
                Path loggingPropertiesFile = Paths.get("forge/logging.properties");
                if(Files.isRegularFile(loggingPropertiesFile)) {
                    System.setProperty("java.util.logging.config.file",loggingPropertiesFile.toString());
                    LogManager manager = LogManager.getLogManager();
                    manager.readConfiguration();
                }
            }
        } catch(IOException e) {
            System.out.println("Unable to init logging: "+e.getMessage());
            e.printStackTrace();
        }
    }
}
