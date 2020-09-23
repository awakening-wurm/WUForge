package org.gotti.wurmunlimited.modloader;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(HookManagerTestRunner.class)
public class ModInstanceBuilderTest {
	
	@Test
	public void testShared() throws ClassNotFoundException, IOException {
		checkResources(true);
	}
	
	@Test
	public void test() throws ClassNotFoundException, IOException {
		checkResources(false);
	}
	
	private void checkResources(boolean sharedClassLoader) throws IOException {
		
		ModInstanceBuilder<Object> instanceBuilder = new ModInstanceBuilder<>(Object.class);
		
		final String modName = "test";
		
		Properties properties = new Properties();
		properties.setProperty("sharedClassLoader", Boolean.toString(sharedClassLoader));
		properties.setProperty("classname", "DummyMod");
		properties.setProperty("classpath", "test.jar,files");
		ModInfo modEntry = new ModInfo(properties, modName);

		Object mod = instanceBuilder.createModInstance(modEntry);
		
		final Path modPath = Paths.get("mods").resolve(modName);
		
		final URL jarUrl = new URL("jar:" + modPath.resolve("test.jar").toUri().toString() + "!/test.txt");
		final URL fileUrl = modPath.resolve("files/test.txt").toUri().toURL();

		final ClassLoader classLoader = mod.getClass().getClassLoader();
		assertThat(jarUrl).isNotNull();

		ArrayList<URL> files = Collections.list(classLoader.getResources("test.txt"));
		assertThat(files).containsExactly(jarUrl, fileUrl);
	}
	
	@Test
	public void testWildcard() throws ClassNotFoundException, IOException {
		
		ModInstanceBuilder<Object> instanceBuilder = new ModInstanceBuilder<>(Object.class);
		
		final String modName = "test";
		
		Properties properties = new Properties();
		properties.setProperty("sharedClassLoader", Boolean.toString(false));
		properties.setProperty("classname", "DummyMod");
		properties.setProperty("classpath", "*.jar,files");
		ModInfo modEntry = new ModInfo(properties, modName);

		Object mod = instanceBuilder.createModInstance(modEntry);
		
		final Path modPath = Paths.get("mods").resolve(modName);
		
		final URL jarUrl = new URL("jar:" + modPath.resolve("test.jar").toUri().toString() + "!/test.txt");
		final URL fileUrl = modPath.resolve("files/test.txt").toUri().toURL();

		final ClassLoader classLoader = mod.getClass().getClassLoader();
		assertThat(jarUrl).isNotNull();

		ArrayList<URL> files = Collections.list(classLoader.getResources("test.txt"));
		assertThat(files).containsExactly(jarUrl, fileUrl);
	}
	
}
