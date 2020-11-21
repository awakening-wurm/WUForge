package net.wurmunlimited.forge.profiles;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import net.wurmunlimited.forge.interfaces.JsonStringBuilder;
import net.wurmunlimited.forge.mods.Mod;
import net.wurmunlimited.forge.mods.VersionHandler;

import java.util.HashMap;
import java.util.Map;

public class ModsProfile implements JsonStringBuilder {

    public static class ModsProperties {

    }

    public static class ModValues {

    }

    public final String name;
    Map<String,Mod> mods;

    public ModsProfile(String name) {
        this.name = name;
        this.mods = new HashMap<>();
    }

    public ModsProfile(String name,JsonObject jo) {
        this(name);
        JsonValue jvMods = jo.get("mods");
        if(jvMods!=null) {
            JsonObject joMods = jvMods.asObject();
            if(joMods!=null) {
                for(JsonObject.Member entry : joMods) {
                    String modName = entry.getName();
                    Mod mod = VersionHandler.getInstance().getMod(modName);
                    this.mods.put(modName,mod);
                }
            }
        }
    }

    @Override
    public void toJsonString(StringBuilder sb,String ind) {
        String ind2 = ind+JsonStringBuilder.INDENT;
        String ind3 = ind2+JsonStringBuilder.INDENT;
        sb.append("{\n")
          .append(ind2).append("\"name\":").append(JsonStringBuilder.toJsonString(name)).append(",\n")
          .append(ind2).append("\"mods\":{");
        int i = 0;
        for(String mod : mods.keySet()) {
            if(i>0) sb.append(",");
            sb.append("\n");
            sb.append(ind3).append(JsonStringBuilder.toJsonString(mod)).append(":\"\"");
            ++i;
        }
        if(i>0) sb.append("\n");
        sb.append(ind2).append("}\n")
          .append(ind).append("}");
    }
}
