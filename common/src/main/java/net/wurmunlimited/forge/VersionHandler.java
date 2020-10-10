package net.wurmunlimited.forge;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonObject.Member;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;
import net.wurmunlimited.forge.config.ForgeConfig;
import net.wurmunlimited.forge.util.ClientChecksum;
import net.wurmunlimited.forge.util.FileUtil;
import net.wurmunlimited.forge.util.HttpClient;

import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static net.wurmunlimited.forge.interfaces.ForgeConstants.FORGE_BASE_URL;

public class VersionHandler {

    private static final Logger logger = Logger.getLogger(VersionHandler.class.getName());

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd",Locale.ENGLISH);
    private static final String INDENT = "  ";

    private static String getSha1Sum(Path path) {
        if(Files.exists(path)) {
            try(final InputStream is = Files.newInputStream(path)) {
                return getSha1Sum(is);
            } catch(IOException e) {}
        }
        return null;
    }

    private static String getSha1Sum(InputStream is) {
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

    private static String toJsonString(String str) {
        return str!=null? "\""+str+"\"" : "null";
    }

    private static String toJsonString(Date date) {
        return date!=null? "\""+DATE_FORMAT.format(date)+"\"" : "null";
    }


    private static String getJsonString(JsonObject jo,String name,String defaultValue) {
        JsonValue value = jo.get(name);
        return value!=null && !value.isNull()? value.asString() : defaultValue;
    }

    public static class ReleaseVersion {

        private static Set<ReleaseVersion> getReleaseVersions(Mod mod,JsonObject jo) {
            JsonObject joReleases = jo.get("releases").asObject();
            Set<ReleaseVersion> releases = new HashSet<>();
            for(Member entry : joReleases) {
                Date date;
                try {
                    date = DATE_FORMAT.parse(entry.getName());
                } catch(java.text.ParseException e) {
                    date = null;
                }
                JsonObject value = entry.getValue().asObject();
                releases.add(new ReleaseVersion(mod,date,value));
            }
            return releases;
        }

        private static String toJsonString(Set<ReleaseVersion> releaseVersions,String ind) {
            StringBuilder sb = new StringBuilder();
            if(releaseVersions!=null && !releaseVersions.isEmpty()) {
                int i = 0;
                String ind2 = ind+INDENT;
                sb.append("{");
                for(ReleaseVersion rv : releaseVersions) {
                    if(rv.date==null) continue;
                    if(i>0) sb.append(",");
                    sb.append("\n").append(ind2);
                    sb.append("\"").append(DATE_FORMAT.format(rv.date)).append("\":").append(rv.toJsonString(ind2));
                    ++i;
                }
                if(i>0) sb.append("\n").append(ind);
                sb.append("}");
            } else {
                sb.append("null");
            }
            return sb.toString();
        }

        private final Mod mod;
        public final Date date;
        public final String tag;
        public final String file;
        public final String url;
        public final boolean local;
        public final String md5;

        ReleaseVersion(Date date,JsonObject jo) {
            this(null,date,jo);
        }

        ReleaseVersion(Mod mod,Date date,JsonObject jo) {
            this.mod = mod;
            this.date = date==null? new Date(0L) : date;
            this.tag = getJsonString(jo,"tag",null);
            this.file = getJsonString(jo,"file",null);
            this.url = getJsonString(jo,"url",null);
            this.local = jo.getBoolean("local",false);
            this.md5 = getJsonString(jo,"md5",null);
        }

        public String getZipUrl() {
            if(local) return FORGE_BASE_URL+"mods/"+file;
            if(url.startsWith("https://github.com"))
                return url+"/releases/download/"+tag+"/"+file;
            return file;
        }

        public boolean isInstalled() {
            return mod!=null && mod.installedMod!=null && mod.installedMod.md5.equalsIgnoreCase(md5);
        }

        public String toJsonString(String ind) {
            return "{"+
                   "\"tag\":"+VersionHandler.toJsonString(tag)+
                   (file!=null? ",\"file\":"+VersionHandler.toJsonString(file) : "")+
                   (url!=null? ",\"url\":"+VersionHandler.toJsonString(url) : "")+
                   (local? ",\"local\":true" : "")+
                   (md5!=null? ",\"md5\":"+VersionHandler.toJsonString(md5) : "")+
                   "}";
        }

        @Override
        public int hashCode() {
            return date.hashCode();
        }
    }

    public static class Mod {

        private static String toJsonString(Map<String,Mod> mods,String ind) {
            StringBuilder sb = new StringBuilder();
            if(mods!=null && !mods.isEmpty()) {
                int i = 0;
                String ind2 = ind+INDENT;
                sb.append("{");
                for(Entry<String,Mod> entry : mods.entrySet()) {
                    if(i>0) sb.append(",");
                    sb.append("\n").append(ind2);
                    String name = entry.getKey();
                    Mod mod = entry.getValue();
                    sb.append("\"").append(name).append("\":").append(mod.toJsonString(ind2));
                    ++i;
                }
                if(i>0) sb.append("\n").append(ind);
                sb.append("}");
            } else {
                sb.append("null");
            }
            return sb.toString();
        }

        public final String author;
        public final String name;
        public final String url;
        public final Set<ReleaseVersion> releases;
        public final ReleaseVersion latest;
        public ModInfo installedMod;

        Mod(JsonObject jo) {
            this.author = getJsonString(jo,"author","unknown");
            this.name = getJsonString(jo,"name",null);
            this.url = getJsonString(jo,"url",null);
            this.releases = ReleaseVersion.getReleaseVersions(this,jo);
            this.latest = this.releases.stream().max(Comparator.comparing(r -> r.date)).orElse(null);
            this.installedMod = null;
        }

        public String toJsonString(String ind) {
            String ind2 = ind+INDENT;
            return "{\n"+
                   ind2+"\"author\":"+VersionHandler.toJsonString(author)+",\n"+
                   ind2+"\"name\":"+VersionHandler.toJsonString(name)+",\n"+
                   ind2+"\"url\":"+VersionHandler.toJsonString(url)+",\n"+
                   ind2+"\"releases\":"+ReleaseVersion.toJsonString(releases,ind2)+"\n"+
                   ind+"}";
        }
    }

    public static class Repository {
        public final String version;
        public final Date date;
        public final Set<ReleaseVersion> forgeReleases;
        public final Map<String,Mod> mods;

        Repository(VersionHandler versionHandler,JsonObject jo) throws ParseException {
            this.version = getJsonString(jo,"version",null);
            this.date = new Date(jo.getLong("timestamp",0L)*1000L);
            this.forgeReleases = new HashSet<>();
            this.mods = new HashMap<>();
            JsonValue jvForge = jo.get("forge");
            if(jvForge!=null) {
                JsonObject joForge = jvForge.asObject();
                if(joForge!=null) {
                    this.forgeReleases.addAll(ReleaseVersion.getReleaseVersions(null,joForge));
                }
            }
            JsonValue jvMods = jo.get("mods");
            if(jvMods!=null) {
                JsonObject joMods = jvMods.asObject();
                for(Member entry : joMods) {
                    String name = entry.getName();
                    Mod mod = new Mod(entry.getValue().asObject());
                    if(versionHandler!=null) {
                        mod.installedMod = versionHandler.installedMods.get(mod.name);
                    }
                    this.mods.put(name,mod);
                }
            }
        }

        public String toJsonString(String ind) {
            String ind2 = ind+INDENT;
            String ind3 = ind2+INDENT;
            return "{\n"+
                   ind2+"\"version\":"+VersionHandler.toJsonString(version)+",\n"+
                   ind2+"\"timestamp\":"+(date!=null? (date.getTime()/1000L) : "null")+",\n"+
                   ind2+"\"forge\":{\n"+
                   ind3+"\"releases\":"+ReleaseVersion.toJsonString(forgeReleases,ind3)+"\n"+
                   ind2+"},\n"+
                   ind2+"\"mods\":"+Mod.toJsonString(mods,ind2)+"\n" +
                   ind+"}";
        }
    }

    public static class ModInfo {

        private static String toJsonString(Map<String,ModInfo> installedMods,String ind) {
            StringBuilder sb = new StringBuilder();
            String ind2 = ind+INDENT;
            String ind3 = ind2+INDENT;
            sb.append("{\n")
              .append(ind2).append("\"timestamp\":").append(System.currentTimeMillis()/1000L).append(",\n")
              .append(ind2).append("\"mods\":");
            if(installedMods!=null && !installedMods.isEmpty()) {
                int i = 0;
                sb.append("{");
                for(ModInfo modInfo : installedMods.values()) {
                    if(i>0) sb.append(",");
                    sb.append("\n").append(ind3);
                    sb.append("\"").append(modInfo.name).append("\":").append(modInfo.toJsonString(ind3));
                    ++i;
                }
                if(i>0) sb.append("\n").append(ind2);
                sb.append("}\n");
            } else {
                sb.append("null\n");
            }
            sb.append(ind).append("}");
            return sb.toString();
        }

        private Date date;
        private String name;
        private String tag;
        private Path jar;
        private String hash;
        private String md5;

        ModInfo(String date,String name,String tag,String file) {
            set(date,name,tag,file);
        }

        ModInfo(Date date,String name,String tag,String file) {
            set(date,name,tag,file);
        }

        ModInfo(JsonObject jo) {
            String date = getJsonString(jo,"date",null);
            String name = getJsonString(jo,"name",null);
            String tag = getJsonString(jo,"tag",null);
            String file = getJsonString(jo,"file",null);
            set(date,name,tag,file);
        }

        private void set(String date,String name,String tag,String file) {
            Date d = null;
            try {
                d = DATE_FORMAT.parse(date);
            } catch(java.text.ParseException e) {}
            set(d,name,tag,file);
        }

        private void set(Date date,String name,String tag,String file) {
            ForgeConfig config = ForgeConfig.getInstance();
            this.date = date;
            this.name = name;
            this.tag = tag;
            this.jar = file!=null? config.getModsLibDir().resolve(file) : null;
            if(this.jar!=null && Files.exists(this.jar)) {
                this.hash = getSha1Sum(this.jar);
                this.md5 = ClientChecksum.getMD5(this.jar);
            } else {
                this.hash = null;
                this.md5 = null;
            }
        }

        public String toJsonString(String ind) {
            String ind2 = ind+INDENT;
            return "{\n"+
                   ind2+"\"date\":"+VersionHandler.toJsonString(date)+",\n"+
                   ind2+"\"name\":"+VersionHandler.toJsonString(name)+",\n"+
                   ind2+"\"tag\":"+VersionHandler.toJsonString(tag)+",\n"+
                   ind2+"\"file\":"+(jar!=null? "\""+jar.getFileName()+"\"" : "null")+",\n"+
                   ind2+"\"md5\":"+VersionHandler.toJsonString(md5)+"\n"+
                   ind+"}";
        }

        public Date getDate() {
            return date;
        }

        public String getName() {
            return name;
        }

        public String getTag() {
            return tag;
        }

        public Path getJar() {
            return jar;
        }

        public String getHash() {
            return hash;
        }

        public String getMd5() {
            return md5;
        }
    }

    private static VersionHandler instance = null;

    public static VersionHandler getInstance() {
        if(instance==null) instance = new VersionHandler();
        return instance;
    }

    private Repository repository;
    private Map<String,ModInfo> installedMods;
    private Map<ModInfo,ReleaseVersion> updates;

    private VersionHandler() {
        repository = null;
        installedMods = null;
        updates = null;
    }

    public Repository getRepository() {
        return repository;
    }

    public Map<String,ModInfo> getInstalledMods() {
        if(installedMods==null) {
            ForgeConfig config = ForgeConfig.getInstance();
            Path installedJsonFile = config.getModsDir().resolve("installed.json");
            String installedJson = FileUtil.readTextFile(installedJsonFile);
            installedMods = new HashMap<>();
            if(installedJson!=null && !installedJson.isEmpty()) {
                try {
                    JsonObject jo = Json.parse(installedJson).asObject();
                    Date lastUpdate = new Date(jo.getLong("timestamp",0L)*1000L);
                    JsonObject joMods = jo.get("mods").asObject();
                    for(Member entry : joMods) {
                        ModInfo modInfo = new ModInfo(entry.getValue().asObject());
                        if(modInfo.jar!=null && modInfo.hash!=null && modInfo.md5!=null) {
                            installedMods.put(modInfo.name,modInfo);
                        }
                    }
                } catch(ParseException e) {}
            } else {
                try(DirectoryStream<Path> ds = Files.newDirectoryStream(config.getModsLibDir())) {
                    for(Path f : ds) {
                        String fileName = f.getFileName().toString();
                        if(Files.isRegularFile(f) && fileName.endsWith(".jar")) {
                            Date date = new Date(Files.getLastModifiedTime(f).toMillis());
                            String name = fileName.substring(0,fileName.length()-4);
                            String tag = null;
                            installedMods.put(name,new ModInfo(date,name,tag,fileName));
                        }
                    }
                    FileUtil.writeTextFile(installedJsonFile,ModInfo.toJsonString(installedMods,"")+"\n");
                } catch(IOException e) {}
            }
        }
        return installedMods;
    }

    public void loadNewVersions() {
        ForgeConfig config = ForgeConfig.getInstance();

        Path repositoryFile = config.getModsDir().resolve("repository.json");
        String oldJsonData = FileUtil.readTextFile(repositoryFile);
        Repository oldMods = null;
        if(oldJsonData!=null) {
            try {
                JsonObject jo = Json.parse(oldJsonData).asObject();
                oldMods = new Repository(this,jo);
            } catch(ParseException e) {
                logger.log(Level.WARNING,"getAvailableMods: "+e.getMessage(),e);
            }
        }

        HttpClient http = new HttpClient();
        http.request(FORGE_BASE_URL+"mods/list"+(oldMods!=null? "?from="+DATE_FORMAT.format(oldMods.date) : ""));
        if(http.getStatus()!=200) return;
        String newJsonData = http.getResponse();
        if(newJsonData.isEmpty()) return;
        Repository newMods = null;
        if(newJsonData!=null) {
            try {
                JsonObject jo = Json.parse(newJsonData).asObject();
                newMods = new Repository(this,jo);
            } catch(ParseException e) {
                logger.log(Level.WARNING,"getAvailableMods: "+e.getMessage(),e);
            }
        }

        repository = oldMods;
        if(newMods!=null && (oldMods==null || !newMods.date.equals(oldMods.date))) {
            repository = newMods;
            if(oldMods!=null) {
                for(ReleaseVersion rv : oldMods.forgeReleases)
                    if(!repository.forgeReleases.contains(rv))
                        repository.forgeReleases.add(rv);
                for(Entry<String,Mod> entry : oldMods.mods.entrySet()) {
                    String name = entry.getKey();
                    Mod oldMod = entry.getValue();
                    if(!repository.mods.containsKey(name)) {
                        repository.mods.put(name,oldMod);
                    } else {
                        Mod newMod = repository.mods.get(name);
                        for(ReleaseVersion rv : oldMod.releases)
                            if(!newMod.releases.contains(rv))
                                newMod.releases.add(rv);

                    }
                }
            }
            FileUtil.writeTextFile(repositoryFile,repository.toJsonString("")+"\n");
        }
    }

    public Map<ModInfo,ReleaseVersion> getUpdates() {
        if(updates==null) {
            updates = new HashMap<>();
            for(ModInfo modInfo : installedMods.values()) {
                Mod mod = repository.mods.get(modInfo.name);
                if(mod!=null && mod.releases!=null && !mod.releases.isEmpty()) {
                    ReleaseVersion update = null;
                    for(ReleaseVersion rv : mod.releases)
                        if(update==null || update.date==null || (rv.date!=null && rv.date.after(update.date))) {
                            update = rv;
                        }
                    if(update!=null) updates.put(modInfo,update);
                }
            }
        }
        return updates;
    }

    public boolean install() {
        getInstalledMods();
        if(repository==null) {
            loadNewVersions();
        }
        getUpdates();
        if(!updates.isEmpty()) {
            Iterator<Entry<ModInfo,ReleaseVersion>> it = updates.entrySet().iterator();
            int num = 0;
            while(it.hasNext()) {
                Entry<ModInfo,ReleaseVersion> entry = it.next();
                ModInfo modInfo = entry.getKey();
                ReleaseVersion update = entry.getValue();
                if(installMod(modInfo,update)) {
                    ++num;
                }
            }
            logger.info("Client mods have been updated, client needs to be restarted...");
            return num>0;
        }
        return false;
    }

    public boolean installMod(ModInfo modInfo,ReleaseVersion update) {
        ForgeConfig config = ForgeConfig.getInstance();
        String zipUrl = update.getZipUrl();
        Path zipFile = config.getCacheDir().resolve(update.file);
        logger.info("Download client mod: "+zipUrl+" => "+zipFile.toAbsolutePath().toString());
        if(Files.exists(zipFile) || HttpClient.download(zipUrl,zipFile)) {
            try {
                return extractMod(modInfo.name,zipFile);
            } catch(IOException e) {
                logger.log(Level.SEVERE,e.getMessage(),e);
            }
        }
        return false;
    }

    public boolean extractMod(String modName,Path zipFile) throws IOException {
        ForgeConfig config = ForgeConfig.getInstance();
        Path jarPath = config.getModsLibDir().resolve(modName);
        try(ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFile.toFile()))) {
            ZipEntry entry;
            while((entry = zipIn.getNextEntry())!=null) {
                String fileName = entry.getName();
                int i1 = fileName.lastIndexOf('/');
                int i2 = fileName.lastIndexOf('\\');
                if(i1!=-1 || i2!=-1) fileName = fileName.substring((i1>i2? i1 : i2)+1);
                logger.info("extractMod: "+entry.getName()+" ["+fileName+"]");
                if(!entry.isDirectory()) {
                    Path filePath;
                    if(fileName.endsWith(".properties") || fileName.endsWith(".config")) {
                        filePath = config.getModsDefaultDir().resolve(fileName);
                    } else if(fileName.equals(modName+".jar")) {
                        filePath = config.getModsLibDir().resolve(fileName);
                    } else {
                        if(!Files.exists(jarPath)) FileUtil.createDirectory(jarPath);
                        filePath = jarPath.resolve(fileName);
                    }
                    // if the entry is a file, extracts it
                    try(BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath.toFile()))) {
                        byte[] bytesIn = new byte[2048];
                        int read = 0;
                        while((read = zipIn.read(bytesIn))!=-1) {
                            bos.write(bytesIn,0,read);
                        }
                    }
                    Files.setLastModifiedTime(filePath,FileTime.fromMillis(entry.getTime()));
                }
                zipIn.closeEntry();
            }
        }
        return true;
    }
}
