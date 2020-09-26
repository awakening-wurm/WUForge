package net.wurmunlimited.forge.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class FileUtil {

    private static final Logger logger = Logger.getLogger(FileUtil.class.getName());

    private static String OS = System.getProperty("os.name").toLowerCase();

    /** Write text in UTF-8 format to a file.
     *
     * @param file
     * @param text
     */
    public static void writeTextFile(Path file,String text) {
        writeFile(file,text.getBytes(Charset.forName("UTF-8")));
    }

    /** Write byte data to a file.
     *
     * @param file
     * @param bytes
     */
    public static void writeFile(Path file,byte[] bytes) {
        OutputStream os = null;
        try {
            if(!Files.exists(file)) Files.createFile(file);
            os = Files.newOutputStream(file);
            os.write(bytes);
            os.flush();
            System.out.println("File Written Successfully: "+file.toAbsolutePath());
        } catch(IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(os!=null) os.close();
            } catch(IOException e) {
                System.out.println("Error in closing the file.");
            }
        }
    }

    /** Extract a resource from a jar-file to the output file, will overwrite existing files if overwrite is set.
     * Resource path is the internal path to the resource
     *
     * @param cl
     * @param outputFile
     * @param resource
     * @param overwrite
     * @return
     */
    public static Path extractFile(Class cl,Path outputFile,String resource,boolean overwrite) {
        InputStream link = null;
        try {
            if(!Files.exists(outputFile) || overwrite) {
                link = cl.getResourceAsStream(resource);
                Files.copy(link,outputFile.toAbsolutePath(),REPLACE_EXISTING);
            }
        } catch(IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(link!=null) link.close();
            } catch(IOException e) {
                System.out.println("Error in closing the stream.");
            }
        }
        return outputFile;
    }

    public static List<Path> findFiles(Path dir) {
        if(!Files.exists(dir) || !Files.isDirectory(dir)) return null;
        List<Path> files = new ArrayList<>();
        try {
            LinkedList<Path> dirs = new LinkedList<>();
            dirs.push(dir);
            while(!dirs.isEmpty()) {
                Path d = dirs.pop();
                try(DirectoryStream<Path> ds = Files.newDirectoryStream(d)) {
                    for(Path f : ds) {
                        if(Files.isDirectory(f)) dirs.push(f);
                        else if(Files.isRegularFile(f)) files.add(f);
                    }
                }
            }
        } catch(IOException e) {}
        return files;
    }

    public static boolean deleteFiles(Path[] files) {
        if(files!=null && files.length>0) {
            try {
                for(int i=files.length-1; i>=0; --i) {
                    if(files[i]==null) continue;
                    Files.deleteIfExists(files[i]);
                    files[i] = null;
                }
            } catch(IOException e) {
                return false;
            }
        }
        return true;
    }

    public static boolean deleteFiles(List<Path> files) {
        if(files!=null && !files.isEmpty()) {
            try {
                for(int i=files.size()-1; i>=0; --i) {
                    Files.deleteIfExists(files.get(i));
                }
            } catch(IOException e) {
                return false;
            }
        }
        return true;
    }

    public static boolean createDirectory(Path dir) throws IOException {
        if(!Files.exists(dir)) {
            try {
                Files.createDirectory(dir);
                return true;
            } catch(IOException e) {}
        } else if(!Files.isDirectory(dir)) {
            throw new IOException("File "+dir.toAbsolutePath()+" is not a directory!");
        }
        return false;
    }

    public static boolean createDirectories(Path[] dirs) throws IOException {
        boolean ret = true;
        for(int i=0; i<dirs.length; ++i) {
            Path dir = dirs[i];
            if(!createDirectory(dir)) ret = false;
            else dirs[i] = null;
        }
        return true;
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
}