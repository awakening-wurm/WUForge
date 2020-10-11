package org.gotti.wurmunlimited.modloader;

import javassist.Loader;
import javassist.NotFoundException;
import net.wurmunlimited.forge.config.ForgeConfig;
import org.gotti.wurmunlimited.modloader.classhooks.HookException;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.logging.Logger;

/**
 * Helper which creates mod instance from a mod class.
 *
 * @param <T>
 */
class ModInstanceBuilder<T> {

    private static Logger logger = Logger.getLogger(ModInstanceBuilder.class.getName());

    private Map<String,ClassLoader> classLoaders = new HashMap<>();
    private Class<? extends T> modClass;

    public ModInstanceBuilder(Class<? extends T> modClass) {
        this.modClass = modClass;
    }

    T createModInstance(ModInfo entry) {
        Properties properties = entry.getProperties();
        String modInfo = entry.getName();
        final String className = properties.getProperty("classname");
        if(className==null) {
            throw new HookException("Missing property classname for mod "+modInfo);
        }
        try {
            Loader loader = HookManager.getInstance().getLoader();
            final String classpath = properties.getProperty("classpath");
            final ClassLoader classloader;
            if(classpath!=null) {
                final Boolean sharedClassLoader = Boolean.valueOf(properties.getProperty("sharedClassLoader","false"));
                ClassLoader[] dependencies = entry.getImport().stream().map(classLoaders::get).filter(Objects::nonNull).toArray(ClassLoader[]::new);
                classloader = createClassLoader(classpath,loader,sharedClassLoader,dependencies);
                if(!sharedClassLoader) {
                    classLoaders.put(entry.getName(),classloader);
                }
            } else {
                classloader = loader;
            }
            return classloader.loadClass(className).asSubclass(modClass).newInstance();
        } catch(ClassNotFoundException|InstantiationException|IllegalAccessException|NotFoundException|MalformedURLException e) {
            throw new HookException(e);
        }
    }

    private List<Path> getClassLoaderEntries(String classpath) {
        ForgeConfig config = ForgeConfig.getInstance();
        List<Path> pathEntries = new ArrayList<>();
        String[] entries = classpath.split(",");
        Path libDir = config.getModsLibDir();
        for(String entry : entries) {
            try {
                Path resolved = libDir.resolve(entry);
                if(Files.exists(resolved) && !libDir.relativize(resolved).startsWith("../")) {
                    pathEntries.add(resolved);
                    continue;
                }
            } catch(InvalidPathException e) {
                // Path.resolve does not like wildcards
                // continue resolving the entries via glob
            }
            FileSystem fs = libDir.getFileSystem();
            final PathMatcher matcher = fs.getPathMatcher("glob:"+entry);
            SimpleFileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file,BasicFileAttributes attrs) {
                    Path p = libDir.relativize(file);
                    if(matcher.matches(p)) pathEntries.add(file);
                    return FileVisitResult.CONTINUE;
                }
            };
            try {
                Files.walkFileTree(libDir,visitor);
            } catch(IOException e) {
                throw new HookException(e);
            }
        }
        return pathEntries;
    }

    /**
     * Create the classloader
     *
     * @param classpath    Classpath entries
     * @param loader       Parent loader
     * @param shared       Do we want a shared classloader?
     * @param dependencies Dependencies to import when NOT using a shared classloader
     * @return Classloader
     * @throws MalformedURLException
     * @throws NotFoundException
     */
    private ClassLoader createClassLoader(String classpath,Loader loader,Boolean shared,ClassLoader... dependencies) throws MalformedURLException, NotFoundException {
        List<Path> pathEntries = getClassLoaderEntries(classpath);
        logger.info("Classpath: "+pathEntries.toString());
        if(shared) {
            final HookManager hookManager = HookManager.getInstance();
            for(Path path : pathEntries) {
                hookManager.appendSharedClassPath(path);
            }
            return loader;
        } else {
            List<URL> urls = new ArrayList<>();
            for(Path path : pathEntries) {
                urls.add(path.toUri().toURL());
            }
            ClassLoader parent = loader;
            if(dependencies!=null && dependencies.length>0) {
                parent = new JoinClassLoader(parent,dependencies);
            }
            return new URLClassLoader(urls.toArray(new URL[urls.size()]),parent);
        }
    }
}
