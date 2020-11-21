package net.wurmunlimited.forge.util;

import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class FileUtil {

    private static final Logger logger = Logger.getLogger(FileUtil.class.getName());

    private static String OS = System.getProperty("os.name").toLowerCase();

    public static boolean isWindows() {
        return OS.contains("win");
    }

    public static boolean isMacintosh() {
        return OS.contains("mac");
    }

    public static boolean isLinux() {
        return OS.contains("nix") || OS.contains("nux") || OS.contains("aix");
    }

    public static String readTextFile(Path file) {
        try {
            String text = new String(Files.readAllBytes(file),StandardCharsets.UTF_8);
            return text;
        } catch(IOException e) {}
        return null;
    }

    /** Write text in UTF-8 format to a file.
     *
     * @param file
     * @param text
     */
    public static void writeTextFile(Path file,String text) {
        writeFile(file,text.getBytes(StandardCharsets.UTF_8));
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
            return null;
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
        return findFiles(dir,true,true);
    }

    public static List<Path> findFiles(Path dir,boolean recurse,boolean includeDirectories) {
        if(!Files.exists(dir) || !Files.isDirectory(dir)) return null;
        List<Path> files = new ArrayList<>();
        try {
            if(includeDirectories) files.add(dir);
            if(recurse) {
                LinkedList<Path> dirs = new LinkedList<>();
                dirs.push(dir);
                while(!dirs.isEmpty()) {
                    Path d = dirs.pollFirst();
                    try(DirectoryStream<Path> ds = Files.newDirectoryStream(d)) {
                        for(Path f : ds) {
                            if(Files.isRegularFile(f)) files.add(f);
                            else if(Files.isDirectory(f)) {
                                dirs.push(f);
                                if(includeDirectories) files.add(f);
                            }
                        }
                    }
                }
            } else {
                try(DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
                    for(Path f : ds) {
                        if(Files.isRegularFile(f)) files.add(f);
                        else if(Files.isDirectory(f)) {
                            if(includeDirectories) files.add(f);
                        }
                    }
                }
            }
        } catch(IOException e) {}
        return files;
    }

    public static List<Path> findDirectories(Path dir) {
        return findDirectories(dir,true,true);
    }

    public static List<Path> findDirectories(Path dir,boolean recurse,boolean includeRoot) {
        if(!Files.exists(dir) || !Files.isDirectory(dir)) return null;
        List<Path> files = new ArrayList<>();
        try {
            if(includeRoot) files.add(dir);
            if(recurse) {
                LinkedList<Path> dirs = new LinkedList<>();
                dirs.push(dir);
                while(!dirs.isEmpty()) {
                    Path d = dirs.pollFirst();
                    try(DirectoryStream<Path> ds = Files.newDirectoryStream(d)) {
                        for(Path f : ds) {
                            if(Files.isDirectory(f)) {
                                dirs.push(f);
                                files.add(f);
                            }
                        }
                    }
                }
            } else {
                try(DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
                    for(Path f : ds) {
                        if(Files.isDirectory(f)) {
                            files.add(f);
                        }
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

    public static int copyDirectory(Path sourceDir,Path destDir,boolean overwrite) throws IOException {
        if(!Files.exists(sourceDir) || !Files.isDirectory(sourceDir) ||
           !Files.exists(destDir) || !Files.isDirectory(destDir)) return -1;
        int files = 0;
        try {
            LinkedList<Path> dirs = new LinkedList<>();
            dirs.push(sourceDir);
            while(!dirs.isEmpty()) {
                Path d = dirs.pollFirst();
                Path r = sourceDir.relativize(d);
                Path c = destDir.resolve(r);
                if(!Files.exists(c)) createDirectory(c);
                try(DirectoryStream<Path> ds = Files.newDirectoryStream(d)) {
                    for(Path f : ds) {
                        if(Files.isDirectory(f)) dirs.push(f);
                        else if(Files.isRegularFile(f)) {
                            Path t = c.resolve(f.getFileName());
                            if(!Files.exists(t) || overwrite) {
                                Files.copy(f,t,REPLACE_EXISTING,COPY_ATTRIBUTES);
                                ++files;
                            }
                        }
                    }
                }
            }
        } catch(IOException e) {}
        return files;
    }

    public static Properties loadProperties(Path file) {
        Properties properties = new Properties();
        if(!Files.exists(file)) {
            logger.warning("The config file seems to be missing.");
            return properties;
        }
        InputStream stream = null;
        try {
            logger.info("Opening the config file.");
            stream = Files.newInputStream(file);
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

    public static void unzip(Path zipFile,Path destDir) throws IOException {
        if(!Files.exists(destDir) || !Files.isDirectory(destDir)) {
            throw new IOException("Destination directory doesn't exist or isn't a directory.");
        }
        ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFile.toFile()));
        ZipEntry entry = zipIn.getNextEntry();
        // iterates over entries in the zip file
        while(entry!=null) {
            Path filePath = destDir.resolve(entry.getName());
            if(entry.isDirectory()) {
                // if the entry is a directory, make the directory
                FileUtil.createDirectory(filePath);
            } else {
                // if the entry is a file, extracts it
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath.toFile()));
                byte[] bytesIn = new byte[1024];
                int read = 0;
                while((read = zipIn.read(bytesIn))!=-1) {
                    bos.write(bytesIn,0,read);
                }
                bos.close();
            }
            zipIn.closeEntry();
            entry = zipIn.getNextEntry();
        }
        zipIn.close();
    }

    public static String getSha1Sum(Path path) {
        if(Files.exists(path)) {
            try(final InputStream is = Files.newInputStream(path)) {
                return getSha1Sum(is);
            } catch(IOException e) {}
        }
        return null;
    }

    static String getSha1Sum(InputStream is) {
        try {
            final MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
            messageDigest.reset();
            int n = 0;
            final byte[] buffer = new byte[8192];
            while(n!=-1) {
                n = is.read(buffer);
                if(n>0) messageDigest.update(buffer,0,n);
            }
            final byte[] digest = messageDigest.digest();
            return DatatypeConverter.printHexBinary(digest);
        } catch(IOException|NoSuchAlgorithmException e) {}
        return null;
    }
}
