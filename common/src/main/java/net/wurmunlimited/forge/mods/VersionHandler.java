package net.wurmunlimited.forge.mods;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonObject.Member;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;
import net.wurmunlimited.forge.config.ForgeConfig;
import net.wurmunlimited.forge.interfaces.ForgeUpdater;
import net.wurmunlimited.forge.interfaces.JsonStringBuilder;
import net.wurmunlimited.forge.util.FileUtil;
import net.wurmunlimited.forge.util.HttpClient;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class VersionHandler {

    private static final Logger logger = Logger.getLogger(VersionHandler.class.getName());

    private static VersionHandler instance = null;

    public static VersionHandler getInstance() {
        if(instance==null) instance = new VersionHandler();
        return instance;
    }

    private Repository repository;
    private String forgeTag;
    private String forgeMd5;
    private ReleaseVersion forgeVersion;
    private Map<String,InstalledMod> installedMods;
    private boolean saveInstalledModsData;
    private boolean saveRepositoryData;

    private VersionHandler() {
        repository = null;
        forgeTag = null;
        forgeMd5 = null;
        forgeVersion = null;
        installedMods = null;
        saveInstalledModsData = false;
        saveRepositoryData = false;
    }

    public void load(boolean apiRequest) {
        loadInstalledMods();
        loadRepository(apiRequest);
        findForgeVersion();
        findInstalledModsVersions();
        save();
    }

    public void save() {
        if(saveInstalledModsData) {
            saveInstalledMods();
            saveInstalledModsData = false;
        }
        if(saveRepositoryData) {
            repository.save();
            saveRepositoryData = false;
        }
    }

    public Repository getRepository() {
        return repository;
    }

    public Mod getMod(String name) {
        if(repository==null || repository.mods.isEmpty()) return null;
        return repository.mods.get(name);
    }

    public ReleaseVersion getLatestReleaseVersion(String name) {
        Mod mod = getMod(name);
        if(mod==null) return null;
        return mod.latest;
    }

    public InstalledMod getInstalledMod(String name) {
        if(installedMods==null || installedMods.isEmpty()) return null;
        return installedMods.get(name);
    }

    private void findForgeVersion() {
        if(forgeVersion==null && (forgeTag!=null || forgeMd5!=null)) {
            if(repository.forgeReleases!=null && !repository.forgeReleases.isEmpty()) {
                for(ReleaseVersion rv : repository.forgeReleases)
                    if((rv.tag!=null && rv.tag.equalsIgnoreCase(forgeTag)) || (rv.md5!=null && rv.md5.equalsIgnoreCase(forgeMd5))) {
                        forgeTag = rv.tag;
                        forgeMd5 = rv.md5;
                        forgeVersion = rv;
                    }
            }
        }
    }

    public ReleaseVersion getForgeVersion() {
        return forgeVersion;
    }

    public void updateForgeVersion(ReleaseVersion releaseVersion) {
        if(releaseVersion==null) return;
        forgeTag = releaseVersion.tag;
        forgeMd5 = releaseVersion.md5;
        forgeVersion = releaseVersion;
        saveInstalledModsData = true;
    }

    public Map<String,InstalledMod> getInstalledMods() {
        if(installedMods==null) loadInstalledMods();
        return installedMods;
    }

    private void loadInstalledMods() {
        if(installedMods!=null) return;
        ForgeConfig config = ForgeConfig.getInstance();
        forgeTag = null;
        forgeMd5 = null;
        forgeVersion = null;
        installedMods = new HashMap<>();
        String installedJson = FileUtil.readTextFile(config.getInstalledFile());
        if(installedJson!=null && !installedJson.isEmpty()) {
            try {
                JsonObject jo = Json.parse(installedJson).asObject();
                Date lastUpdate = new Date(jo.getLong("timestamp",0L)*1000L);
                JsonValue jvForge = jo.get("forge");
                if(jvForge!=null) {
                    JsonObject joForge = jvForge.asObject();
                    forgeTag = JsonStringBuilder.getJsonString(joForge,"tag",null);
                    forgeMd5 = JsonStringBuilder.getJsonString(joForge,"hash",null);
                }
                JsonValue jvMods = jo.get("mods");
                if(jvMods!=null) {
                    JsonObject joMods = jvMods.asObject();
                    for(Member entry : joMods) {
                        InstalledMod installedMod = new InstalledMod(entry.getValue().asObject());
                        if(installedMod.jar!=null && installedMod.hash!=null && installedMod.md5!=null) {
                            installedMods.put(installedMod.name,installedMod);
                        }
                    }
                }
            } catch(ParseException e) {}
        } else {
            try(DirectoryStream<Path> ds = Files.newDirectoryStream(config.getModsLibDir())) {
                for(Path f : ds) addInstalledMod(f);
                saveInstalledModsData = true;
            } catch(IOException e) {}
        }
    }

    public void addInstalledMod(Path jarFile) {
        String fileName = jarFile.getFileName().toString();
        if(Files.isRegularFile(jarFile) && fileName.endsWith(".jar")) {
            Date date = null;
            try {
                date = new Date(Files.getLastModifiedTime(jarFile).toMillis());
            } catch(IOException e) {}
            String name = fileName.substring(0,fileName.length()-4);
            String tag = null;
            installedMods.put(name,new InstalledMod(date,name,tag,fileName));
            saveInstalledModsData = true;
        }
    }

    private void saveInstalledMods() {
        ForgeConfig config = ForgeConfig.getInstance();
        String ind = JsonStringBuilder.INDENT;
        String ind2 = ind+JsonStringBuilder.INDENT;
        StringBuilder sb = new StringBuilder();
        sb.append("{\n")
          .append(ind).append("\"timestamp\":").append(System.currentTimeMillis()/1000L).append(",\n")
          .append(ind).append("\"forge\":{\n")
          .append(ind2).append("\"tag\":").append(JsonStringBuilder.toJsonString(forgeTag)).append(",\n")
          .append(ind2).append("\"hash\":").append(JsonStringBuilder.toJsonString(forgeMd5)).append("\n")
          .append(ind).append("},\n")
          .append(ind).append("\"mods\":");
        InstalledMod.toJsonString(sb,installedMods,ind);
        sb.append("\n}\n");
        FileUtil.writeTextFile(config.getInstalledFile(),sb.toString());
        logger.info("Saved installed mods.");
    }

    private void findInstalledModsVersions() {
        for(InstalledMod installedMod : installedMods.values()) {
            if(installedMod.tag==null) {
                Mod mod = repository.mods.get(installedMod.name);
                ReleaseVersion rv = mod.findReleaseVersion(installedMod.md5);
                if(rv!=null) {
                    installedMod.tag = rv.tag;
                }
            }
        }
    }

    private void loadRepository(boolean apiRequest) {
        ForgeConfig config = ForgeConfig.getInstance();
        String oldJsonData = FileUtil.readTextFile(config.getRepositoryFile());
        Repository oldMods = null;
        if(oldJsonData!=null) {
            try {
                JsonObject jo = Json.parse(oldJsonData).asObject();
                oldMods = new Repository(this,jo);
            } catch(ParseException e) {
                logger.log(Level.WARNING,"loadRepository: "+e.getMessage(),e);
            }
        }
        Repository newMods = null;
        if(apiRequest) {
            HttpClient http = new HttpClient();
            http.request(config.getModsUrl()+(oldMods!=null? "?from="+JsonStringBuilder.DATE_FORMAT.format(oldMods.date) : ""));
            if(http.getStatus()!=200) return;
            String newJsonData = http.getResponse();
            if(newJsonData.isEmpty()) return;
            if(newJsonData!=null) {
                try {
                    JsonObject jo = Json.parse(newJsonData).asObject();
                    newMods = new Repository(this,jo);
                } catch(ParseException e) {
                    logger.log(Level.WARNING,"loadRepository: "+e.getMessage(),e);
                }
            }
        }
        repository = oldMods;
        if(newMods!=null && (oldMods==null || newMods.date.after(oldMods.date))) {
            repository = newMods;
            saveRepositoryData = true;
            if(oldMods!=null) {
                for(ReleaseVersion rv : oldMods.forgeReleases)
                    if(!repository.forgeReleases.contains(rv))
                        repository.forgeReleases.add(rv);
                for(Entry<String,Mod> entry : oldMods.mods.entrySet()) {
                    String name = entry.getKey();
                    Mod oldMod = entry.getValue();
                    Mod newMod = repository.mods.get(name);
                    if(newMod==null) {
                        repository.mods.put(name,oldMod);
                    } else {
                        for(ReleaseVersion rv : oldMod.releases)
                            if(!newMod.releases.contains(rv))
                                newMod.releases.add(rv);

                    }
                }
            }
        }
    }

    public ReleaseVersion findForgeUpdate() {
        ReleaseVersion update = null;
        ReleaseVersion latest = null;
        for(ReleaseVersion rv : repository.forgeReleases)
            if(latest==null || rv.date.after(latest.date)) latest = rv;
        if(latest!=null && latest.md5!=null && (forgeVersion==null || !latest.md5.equalsIgnoreCase(forgeVersion.md5))) {
            update = latest;
        }
        return update;
    }

    public Map<InstalledMod,ReleaseVersion> findModUpdates() {
        Map<InstalledMod,ReleaseVersion> updates = new HashMap<>();
        for(InstalledMod installedMod : installedMods.values()) {
            Mod mod = repository.mods.get(installedMod.name);
            if(mod!=null && mod.latest!=null && !mod.latest.md5.equalsIgnoreCase(installedMod.md5))
                updates.put(installedMod,mod.latest);
        }
        return updates;
    }

    public boolean installUpdates(ForgeUpdater updater) {
        int num = 0;
        load(false);
        ReleaseVersion forgeUpdate = findForgeUpdate();
        if(forgeUpdate!=null && updater.updateForge(forgeUpdate)) ++num;
        Map<InstalledMod,ReleaseVersion> updates = findModUpdates();
        if(!updates.isEmpty()) {
            Iterator<Entry<InstalledMod,ReleaseVersion>> it = updates.entrySet().iterator();
            while(it.hasNext()) {
                Entry<InstalledMod,ReleaseVersion> entry = it.next();
                InstalledMod installedMod = entry.getKey();
                ReleaseVersion update = entry.getValue();
                if(installMod(installedMod.name,update)) {
                    ++num;
                }
            }
            logger.info(num+" installed mods have been updated to newer versions.");
        }
        if(num>0) {
            saveInstalledModsData = true;
            save();
        }
        return num>0;
    }

    private boolean installMod(String modName,ReleaseVersion update) {
        ForgeConfig config = ForgeConfig.getInstance();
        String zipUrl = update.getZipUrl();
        Path zipFile = config.getCacheDir().resolve(update.file);
        logger.info("Download mod: "+zipUrl+" => "+zipFile.toAbsolutePath().toString());
        if(Files.exists(zipFile) || HttpClient.download(zipUrl,zipFile)) {
            try {
                return extractMod(modName,zipFile);
            } catch(IOException e) {
                logger.log(Level.SEVERE,e.getMessage(),e);
            }
        }
        return false;
    }

    private boolean extractMod(String modName,Path zipFile) throws IOException {
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
        addInstalledMod(jarPath);
        return true;
    }
}
