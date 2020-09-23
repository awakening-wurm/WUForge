package org.gotti.wurmunlimited.modloader;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.gotti.wurmunlimited.modloader.classhooks.HookException;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.dependency.DependencyResolver;
import org.gotti.wurmunlimited.modloader.interfaces.Configurable;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.ModEntry;
import org.gotti.wurmunlimited.modloader.interfaces.ModListener;
import org.gotti.wurmunlimited.modloader.interfaces.PreInitable;
import org.gotti.wurmunlimited.modloader.interfaces.Versioned;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.NotFoundException;

public abstract class ModLoaderShared<T extends Versioned> implements Versioned {
	
	private static Logger logger = Logger.getLogger(ModLoaderShared.class.getName());
	
	private class Entry extends ModInfo implements ModEntry<T> {
		
		T mod;
		
		public Entry(T mod, Properties properties, String name) {
			super(properties, name);
			this.mod = mod;
		}
		
		@Override
		public T getWurmMod() {
			return mod;
		}
	}
	
	private Class<? extends T> modClass;
	
	public ModLoaderShared(Class<? extends T> modClass) {
		this.modClass = modClass;
	}

	protected abstract void modcommInit();
	protected abstract void preInit();
	protected abstract void init();
	
	/**
	 * Find installed mods and read the properties.
	 * <p>
	 * It loads the properties in the following order with later entries overriding earlier ones
	 * <ul>
	 * <li>Properties from mods/modname/modname.jar!META-INF/org.gotti.wurmunlimited.modloader/modname.properties</li>
	 * <li>Properties from mods/modname.properties</li>
	 * <li>Properties from mods/modname.config</li>
	 * </ul>
	 * <p>
	 * The mod will be set to ondemand loading if properties from the jar file exist but no .properties file. This will
	 * enable support mods like httpserver to load without a .properties file when needed. Mods can set depend.ondemand=false
	 * in the properties file in the jar to force loading the mod. Setting depend.ondemand=false in modname.config will then
	 * disable the mod per user request, although removing the files of the mod would probably be a better option.
	 * <p>
	 *
	 * @param modDir mods folder
	 * @return
	 * @throws IOException
	 */
	private List<ModInfo> discoverMods(Path modDir) throws IOException {
		final List<ModInfo> unorderedMods = new ArrayList<>();
		final Set<String> handled = new HashSet<>();
		
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(modDir, "*.properties")) {
			for (Path modInfo : directoryStream) {
				String modName = modInfo.getFileName().toString().replaceAll("\\.properties$", "");
				Path modJar = modDir.resolve(modName).resolve(modName + ".jar");
				ModInfo mod = loadModFromInfo(modName, modInfo, modJar);
				unorderedMods.add(mod);
				handled.add(modName);
			}
		}
		
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(modDir, path -> Files.isDirectory(path) && !handled.contains(path.getFileName().toString()))) {
			for (Path modInfo : directoryStream) {
				String modName = modInfo.getFileName().toString();
				Path modJar = modDir.resolve(modName).resolve(modName + ".jar");
				if (Files.exists(modJar)) {
					ModInfo mod = loadModFromInfo(modName, null, modJar);
					unorderedMods.add(mod);
					handled.add(modName);
				}
			}
		}
		
		return unorderedMods;
	}
	
	public List<? extends ModEntry<T>> loadModsFromModDir(Path modDir) throws IOException {
		final String version = this.getVersion();
		logger.info(String.format("ModLoader version %1$s", version));
		
		String modLoaderProvided = "modloader";
		if (version != null && !version.isEmpty()) {
			modLoaderProvided += "@" + version;
		}
		
		Set<String> provided = new LinkedHashSet<>();
		provided.add(modLoaderProvided);
		
		String steamVersion = getGameVersion();
		provided.add("wurmunlimited@" + steamVersion);
		logger.info(String.format("Game version %1$s", steamVersion));
		
		// Discover installed (and possibly enabled) mods from modDir
		final List<ModInfo> unorderedMods = discoverMods(modDir);
		
		ModInstanceBuilder<T> entryBuilder = new ModInstanceBuilder<T>(modClass);
		List<Entry> mods = new DependencyResolver<ModInfo>().provided(Collections.singleton(modLoaderProvided)).order(unorderedMods).stream().map(modInfo -> {
			try (EarlyLoadingChecker c = EarlyLoadingChecker.init(modInfo.getName(), "load")) {
				modInfo.getProperties().put("steamVersion", steamVersion);
				return new Entry(entryBuilder.createModInstance(modInfo), modInfo.getProperties(), modInfo.getName());
			}
		}).collect(Collectors.toList());
		
		mods.stream().forEach(modEntry -> {
			String implementationVersion = modEntry.mod.getVersion();
			if (implementationVersion == null || implementationVersion.isEmpty()) {
				implementationVersion = "unversioned";
			}
			logger.info(String.format("Loading %1$s as %2$s (%3$s)", modEntry.mod.getClass().getName(),  modEntry.getName(), implementationVersion));
		});
		
		// new style mods with initable will do configure, preInit, init
		mods.stream().filter(modEntry -> (modEntry.mod instanceof Initable || modEntry.mod instanceof PreInitable) && modEntry.mod instanceof Configurable).forEach(modEntry -> {
			try (EarlyLoadingChecker c = EarlyLoadingChecker.init(modEntry.getName(), "configure")) {
				((Configurable) modEntry.mod).configure(modEntry.getProperties());
				}
			});

		try (EarlyLoadingChecker c = EarlyLoadingChecker.init("ModComm", "init")) {
			modcommInit();
		}

		mods.stream().filter(modEntry -> modEntry.mod instanceof PreInitable).forEach(modEntry -> {
			try (EarlyLoadingChecker c = EarlyLoadingChecker.init(modEntry.getName(), "preinit")) {
				((PreInitable)modEntry.mod).preInit();
				}
			});

		preInit();

		mods.stream().filter(modEntry -> modEntry.mod instanceof Initable).forEach(modEntry -> {
			try (EarlyLoadingChecker c = EarlyLoadingChecker.init(modEntry.getName(), "init")) {
				((Initable)modEntry.mod).init();
				}
			});

		init();

		// old style mods without initable or preinitable will just be configured, but they are handled last
		mods.stream().filter(modEntry -> !(modEntry.mod instanceof Initable || modEntry.mod instanceof PreInitable) && modEntry.mod instanceof Configurable).forEach(modEntry -> {
			try (EarlyLoadingChecker c = EarlyLoadingChecker.init(modEntry.getName(), "configure")) {
				((Configurable) modEntry.mod).configure(modEntry.getProperties());
				}
			});
		
		// Send the list of initialized mods to all modlisteners
		mods.stream().filter(modEntry -> modEntry.mod instanceof ModListener).forEach(modEntry -> {
			try (EarlyLoadingChecker c = EarlyLoadingChecker.init(modEntry.getName(), "modListener")) {
				mods.stream().forEach(mod -> ((ModListener)modEntry.mod).modInitialized(mod));
				}
			});
		
		
		return mods;
	}
	
	/**
	 * Load the default properties from mods/modname/modname.jar!META-INF/org.gotti.wurmunlimited.modloader/modname.properties
	 *
	 * @param modName Modname
	 * @param jarFile Jar-file of the mod
	 * @return Properties
	 * @throws IOException
	 */
	private Properties loadDefaultPropertiesFromJar(String modName, Path jarFile) throws IOException {
		try (FileSystem fs = FileSystems.newFileSystem(URI.create("jar:" + jarFile.toUri()), new HashMap<>())) {
			Path propsFile = fs.getPath("/META-INF/" + ModLoaderShared.class.getPackage().getName() + "/" + modName + ".properties");
			if (Files.exists(propsFile)) {
				logger.log(Level.INFO, "Reading " + jarFile.toString() + "!" + propsFile.toString());
				try (InputStream inputStream = Files.newInputStream(propsFile)) {
					Properties properties = new Properties();
					properties.load(inputStream);
					return properties;
				}
			}
		} catch (IOException e) {
			logger.log(Level.WARNING, e.getMessage(), e);
		}
		return new Properties();
	}
	
	/**
	 * Extract the packaged config file
	 * @param modName Mod name
	 * @param jarFile Mod jar
	 * @param configFile Target config file
	 * @throws IOException
	 */
	private void copyConfigFromJar(String modName, Path jarFile, Path configFile) throws IOException {
		try (FileSystem fs = FileSystems.newFileSystem(URI.create("jar:" + jarFile.toUri()), new HashMap<>())) {
			Path configTemplate = fs.getPath("/META-INF/" + ModLoaderShared.class.getPackage().getName() + "/" + modName + ".config");
			if (Files.exists(configTemplate)) {
				logger.log(Level.INFO, "Copying " + jarFile + "!" + configTemplate + " to " + configFile);
				Files.copy(configTemplate, configFile);
			}
		} catch (IOException e) {
			logger.log(Level.WARNING, e.getMessage(), e);
		}
	}
	
	/**
	 * Load mod properties from .properties and .config files
	 * @param modName Modname
	 * @param modInfo properties file
	 * @param jarFile jar file
	 * @return Mod properties
	 * @throws IOException
	 */
	private ModInfo loadModFromInfo(String modName, Path modInfo, Path jarFile) throws IOException {
		Path configFile = Paths.get("mods", modName + ".config");
		
		Properties properties = new Properties();
		if (jarFile != null && Files.exists(jarFile)) {
			if (modInfo == null || !Files.exists(modInfo)) {
				properties.put("depend.ondemand", "true");
			}
			properties.putAll(loadDefaultPropertiesFromJar(modName, jarFile));
			
			if (!Files.exists(configFile)) {
				copyConfigFromJar(modName, jarFile, configFile);
			}
		}

		if (modInfo != null) {
			logger.log(Level.INFO, "Reading " + modInfo.toString());
			try (InputStream inputStream = Files.newInputStream(modInfo)) {
				properties.load(inputStream);
			}
		}

		if (Files.exists(configFile)) {
			try (InputStream inputStream = Files.newInputStream(configFile)) {
				logger.log(Level.INFO, "Reading " + configFile.toString());
				properties.load(inputStream);
			}
		}
		
		return new ModInfo(properties, modName);
	}
	
	/**
	 * Get the server version as announced on steam
	 * @return Steam game version
	 */
	public String getGameVersion() {
		final ClassPool classPool = HookManager.getInstance().getClassPool();
		
		try (DefrostingClassLoader loader = new DefrostingClassLoader(classPool)) {
			final Class<?> clazz = loader.loadClass("com.wurmonline.shared.constants.SteamVersion");
			final Method getCurrentVersion = ReflectionUtil.getMethod(clazz, "getCurrentVersion");
			return getCurrentVersion.invoke(clazz).toString();
		} catch (NotFoundException | CannotCompileException | ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
			throw new HookException(e);
		}
	}
}
