package org.gotti.wurmunlimited.clientlauncher;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;

public class PatchedLauncher {

    public static void main(String[] args) {

        try {
            URL[] urls = new URL[]{
                //Paths.get("modlauncher.jar").toUri().toURL(),
                Paths.get("javassist.jar").toUri().toURL()
            };
            try(URLClassLoader urlClassLoader = new URLClassLoader(urls)) {
                Class<?> launcher = urlClassLoader.loadClass("org.gotti.wurmunlimited.clientlauncher.ClientLauncher");
                Method method = launcher.getDeclaredMethod("main",new Class[]{ String[].class });
                method.invoke(launcher,new Object[]{ args });
            }
        } catch(Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

}
