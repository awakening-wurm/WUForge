package net.wurmunlimited.forge.mods;

import com.eclipsesource.json.JsonObject;
import net.wurmunlimited.forge.config.ForgeConfig;
import net.wurmunlimited.forge.interfaces.JsonStringBuilder;
import net.wurmunlimited.forge.util.FileUtil;
import net.wurmunlimited.forge.util.HashUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.Map;

public class InstalledMod implements JsonStringBuilder {

    static void toJsonString(StringBuilder sb,Map<String,InstalledMod> installedMods,String ind) {
        String ind2 = ind+JsonStringBuilder.INDENT;
        if(installedMods!=null && !installedMods.isEmpty()) {
            int i = 0;
            sb.append("{");
            for(InstalledMod installedMod : installedMods.values()) {
                if(i>0) sb.append(",");
                sb.append("\n").append(ind2);
                sb.append("\"").append(installedMod.name).append("\":");
                installedMod.toJsonString(sb,ind2);
                ++i;
            }
            if(i>0) sb.append("\n").append(ind);
            sb.append("}");
        } else {
            sb.append("null");
        }
    }

    Date date;
    String name;
    String tag;
    Path jar;
    String hash;
    String md5;

    InstalledMod(String date,String name,String tag,String file) {
        set(date,name,tag,file);
    }

    InstalledMod(Date date,String name,String tag,String file) {
        set(date,name,tag,file);
    }

    InstalledMod(JsonObject jo) {
        String date = JsonStringBuilder.getJsonString(jo,"date",null);
        String name = JsonStringBuilder.getJsonString(jo,"name",null);
        String tag = JsonStringBuilder.getJsonString(jo,"tag",null);
        String file = JsonStringBuilder.getJsonString(jo,"file",null);
        set(date,name,tag,file);
    }

    private void set(String date,String name,String tag,String file) {
        Date d = null;
        try {
            d = JsonStringBuilder.DATE_FORMAT.parse(date);
        } catch(java.text.ParseException e) {}
        set(d,name,tag,file);
    }

    private void set(Date date,String name,String tag,String file) {
        ForgeConfig config = ForgeConfig.getInstance();
        this.date = date==null? new Date(0L) : date;
        this.name = name;
        this.tag = tag;
        this.jar = file!=null? config.getModsLibDir().resolve(file) : null;
        if(this.jar!=null && Files.exists(this.jar)) {
            this.hash = FileUtil.getSha1Sum(this.jar);
            this.md5 = HashUtil.getMD5(this.jar);
        } else {
            this.hash = null;
            this.md5 = null;
        }
    }

    @Override
    public void toJsonString(StringBuilder sb,String ind) {
        String ind2 = ind+JsonStringBuilder.INDENT;
        sb.append("{\n")
          .append(ind2).append("\"date\":").append(JsonStringBuilder.toJsonString(date)).append(",\n")
          .append(ind2).append("\"name\":").append(JsonStringBuilder.toJsonString(name)).append(",\n")
          .append(ind2).append("\"tag\":").append(JsonStringBuilder.toJsonString(tag)).append(",\n")
          .append(ind2).append("\"file\":").append(jar!=null? "\""+jar.getFileName()+"\"" : "null").append(",\n")
          .append(ind2).append("\"md5\":").append(JsonStringBuilder.toJsonString(md5)).append("\n")
          .append(ind).append("}");
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
