package net.wurmunlimited.forge.mods;

import com.eclipsesource.json.JsonObject;
import net.wurmunlimited.forge.interfaces.JsonStringBuilder;

import java.util.Map;
import java.util.Set;

public class Mod implements JsonStringBuilder {

    static void toJsonString(StringBuilder sb,Map<String,Mod> mods,String ind) {
        if(mods!=null && !mods.isEmpty()) {
            int i = 0;
            String ind2 = ind+JsonStringBuilder.INDENT;
            sb.append("{");
            for(Map.Entry<String,Mod> entry : mods.entrySet()) {
                if(i>0) sb.append(",");
                sb.append("\n").append(ind2);
                String name = entry.getKey();
                Mod mod = entry.getValue();
                sb.append("\"").append(name).append("\":");
                mod.toJsonString(sb,ind2);
                ++i;
            }
            if(i>0) sb.append("\n").append(ind);
            sb.append("}");
        } else {
            sb.append("null");
        }
    }

    public final String author;
    public final String name;
    public final String url;
    public final Set<ReleaseVersion> releases;
    public final ReleaseVersion latest;
    InstalledMod installedMod;

    Mod(JsonObject jo) {
        this.author = JsonStringBuilder.getJsonString(jo,"author","unknown");
        this.name = JsonStringBuilder.getJsonString(jo,"name",null);
        this.url = JsonStringBuilder.getJsonString(jo,"url",null);
        this.releases = ReleaseVersion.getReleaseVersions(this,jo);
        ReleaseVersion latest = null;
        for(ReleaseVersion rv : this.releases)
            if(latest==null || rv.date.after(latest.date)) latest = rv;
        this.latest = latest;
        this.installedMod = null;
    }

    @Override
    public void toJsonString(StringBuilder sb,String ind) {
        String ind2 = ind+JsonStringBuilder.INDENT;
        sb.append("{\n")
          .append(ind2).append("\"author\":").append(JsonStringBuilder.toJsonString(author)).append(",\n")
          .append(ind2).append("\"name\":").append(JsonStringBuilder.toJsonString(name)).append(",\n")
          .append(ind2).append("\"url\":").append(JsonStringBuilder.toJsonString(url)).append(",\n")
          .append(ind2).append("\"releases\":");
        ReleaseVersion.toJsonString(sb,releases,ind2);
        sb.append("\n")
          .append(ind).append("}");
    }

    public ReleaseVersion findReleaseVersion(String md5) {
        if(latest!=null && latest.md5!=null && latest.md5.equalsIgnoreCase(md5)) return latest;
        for(ReleaseVersion rv : releases)
            if(rv!=latest && rv.md5!=null && rv.md5.equalsIgnoreCase(md5)) return rv;
        return null;
    }
}
