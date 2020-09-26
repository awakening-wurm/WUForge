package org.gotti.wurmunlimited.modloader;

import net.wurmunlimited.forge.config.ForgeConfig;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(HookManagerTestRunner.class)
public class ModInstanceBuilderTest {

    @BeforeClass
    public static void setupClass() {
        Properties properties = new Properties();
        properties.setProperty("forgeDir","test");
        ForgeConfig.init(properties);
    }

    @AfterClass
    public static void closeClass() {
        ForgeConfig.close();
    }

    @Test
    public void testShared() throws IOException {
        checkResources(true);
    }

    @Test
    public void test() throws IOException {
        checkResources(false);
    }

    private void checkResources(boolean sharedClassLoader) throws IOException {
        ForgeConfig config = ForgeConfig.getInstance();
        Properties properties = new Properties();
        properties.setProperty("sharedClassLoader",Boolean.toString(sharedClassLoader));
        properties.setProperty("classname","DummyMod");
        properties.setProperty("classpath","test.jar,files");
        ModInstanceBuilder<Object> instanceBuilder = new ModInstanceBuilder<>(Object.class);
        final String modName = "test";
        ModInfo modEntry = new ModInfo(properties,modName);
        Object mod = instanceBuilder.createModInstance(modEntry);
        final Path modPath = config.getModsLibDir();
        final URL jarUrl = new URL("jar:"+modPath.resolve("test.jar").toUri().toString()+"!/test.txt");
        final URL fileUrl = modPath.resolve("files/test.txt").toUri().toURL();
        final ClassLoader classLoader = mod.getClass().getClassLoader();
        assertThat(jarUrl).isNotNull();
        ArrayList<URL> files = Collections.list(classLoader.getResources("test.txt"));
        assertThat(files).containsExactly(jarUrl,fileUrl);
    }

    @Test
    public void testWildcard() throws IOException {
        ForgeConfig config = ForgeConfig.getInstance();
        Properties properties = new Properties();
        properties.setProperty("sharedClassLoader",Boolean.toString(false));
        properties.setProperty("classname","DummyMod");
        properties.setProperty("classpath","*.jar,files");
        ModInstanceBuilder<Object> instanceBuilder = new ModInstanceBuilder<>(Object.class);
        final String modName = "test";
        ModInfo modEntry = new ModInfo(properties,modName);
        Object mod = instanceBuilder.createModInstance(modEntry);
        final Path modPath = config.getModsLibDir();
        final URL jarUrl = new URL("jar:"+modPath.resolve("test.jar").toUri().toString()+"!/test.txt");
        final URL fileUrl = modPath.resolve("files/test.txt").toUri().toURL();
        final ClassLoader classLoader = mod.getClass().getClassLoader();
        assertThat(jarUrl).isNotNull();
        ArrayList<URL> files = Collections.list(classLoader.getResources("test.txt"));
        assertThat(files).containsExactly(jarUrl,fileUrl);
    }

}
