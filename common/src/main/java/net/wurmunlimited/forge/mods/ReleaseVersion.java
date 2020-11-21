package net.wurmunlimited.forge.mods;

import com.eclipsesource.json.JsonObject;
import net.wurmunlimited.forge.config.ForgeConfig;
import net.wurmunlimited.forge.interfaces.JsonStringBuilder;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class ReleaseVersion implements JsonStringBuilder {

    static Set<ReleaseVersion> getReleaseVersions(Mod mod,JsonObject jo) {
        JsonObject joReleases = jo.get("releases").asObject();
        Set<ReleaseVersion> releases = new HashSet<>();
        for(JsonObject.Member entry : joReleases) {
            Date date;
            try {
                date = JsonStringBuilder.DATE_FORMAT.parse(entry.getName());
            } catch(java.text.ParseException e) {
                date = null;
            }
            JsonObject value = entry.getValue().asObject();
            releases.add(new ReleaseVersion(mod,date,value));
        }
        return releases;
    }

    static void toJsonString(StringBuilder sb,Set<ReleaseVersion> releaseVersions,String ind) {
        if(releaseVersions!=null && !releaseVersions.isEmpty()) {
            int i = 0;
            String ind2 = ind+JsonStringBuilder.INDENT;
            sb.append("{");
            for(ReleaseVersion rv : releaseVersions) {
                if(rv.date==null) continue;
                if(i>0) sb.append(",");
                sb.append("\n").append(ind2);
                sb.append("\"").append(JsonStringBuilder.DATE_FORMAT.format(rv.date)).append("\":");
                rv.toJsonString(sb,ind2);
                ++i;
            }
            if(i>0) sb.append("\n").append(ind);
            sb.append("}");
        } else {
            sb.append("null");
        }
    }

    final Mod mod;
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
        this.tag = JsonStringBuilder.getJsonString(jo,"tag",null);
        this.file = JsonStringBuilder.getJsonString(jo,"file",null);
        this.url = JsonStringBuilder.getJsonString(jo,"url",null);
        this.local = jo.getBoolean("local",false);
        this.md5 = JsonStringBuilder.getJsonString(jo,"md5",null);
    }

    public String getZipUrl() {
        if(local || url==null || url.isEmpty()) {
            ForgeConfig config = ForgeConfig.getInstance();
            return config.getRepositoryUrl()+"/"+file;
        }
        if(url.startsWith("https://github.com"))
            return url+"/releases/download/"+tag+"/"+file;
        return null;
    }

    public boolean isInstalled() {
        return mod!=null && mod.installedMod!=null && mod.installedMod.md5.equalsIgnoreCase(md5);
    }

    @Override
    public void toJsonString(StringBuilder sb,String ind) {
        sb.append("{\"tag\":").append(JsonStringBuilder.toJsonString(tag));
        if(file!=null) sb.append(",\"file\":").append(JsonStringBuilder.toJsonString(file));
        if(url!=null) sb.append(",\"url\":").append(JsonStringBuilder.toJsonString(url));
        if(local) sb.append(",\"local\":true");
        if(md5!=null) sb.append(",\"md5\":").append(JsonStringBuilder.toJsonString(md5));
        sb.append("}");
    }

    @Override
    public int hashCode() {
        return date.hashCode();
    }
}
