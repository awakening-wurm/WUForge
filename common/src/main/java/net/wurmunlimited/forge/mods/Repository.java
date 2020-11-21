package net.wurmunlimited.forge.mods;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;
import net.wurmunlimited.forge.config.ForgeConfig;
import net.wurmunlimited.forge.interfaces.JsonStringBuilder;
import net.wurmunlimited.forge.util.FileUtil;

import java.util.*;

public class Repository implements JsonStringBuilder {
    public final String version;
    public final Date date;
    public final Set<ReleaseVersion> forgeReleases;
    public final Map<String,Mod> mods;

    Repository(VersionHandler versionHandler,JsonObject jo) throws ParseException {
        this.version = JsonStringBuilder.getJsonString(jo,"version",null);
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
            for(JsonObject.Member entry : joMods) {
                String name = entry.getName();
                Mod mod = new Mod(entry.getValue().asObject());
                if(versionHandler!=null) {
                    mod.installedMod = versionHandler.getInstalledMods().get(mod.name);
                }
                this.mods.put(name,mod);
            }
        }
    }

    public void save() {
        ForgeConfig config = ForgeConfig.getInstance();
        StringBuilder sb = new StringBuilder();
        toJsonString(sb,"");
        sb.append("\n");
        FileUtil.writeTextFile(config.getRepositoryFile(),sb.toString());
    }

    @Override
    public void toJsonString(StringBuilder sb,String ind) {
        String ind2 = ind+JsonStringBuilder.INDENT;
        String ind3 = ind2+JsonStringBuilder.INDENT;
        sb.append("{\n")
          .append(ind2).append("\"version\":").append(JsonStringBuilder.toJsonString(version)).append(",\n")
          .append(ind2).append("\"timestamp\":").append(date!=null? (date.getTime()/1000L) : "null").append(",\n")
          .append(ind2).append("\"forge\":{\n")
          .append(ind3).append("\"releases\":");
        ReleaseVersion.toJsonString(sb,forgeReleases,ind3);
        sb.append("\n")
          .append(ind2).append("},\n")
          .append(ind2).append("\"mods\":");
        Mod.toJsonString(sb,mods,ind2);
        sb.append("\n")
          .append(ind).append("}");
    }
}
