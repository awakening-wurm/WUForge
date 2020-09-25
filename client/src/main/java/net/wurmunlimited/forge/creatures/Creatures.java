package net.wurmunlimited.forge.creatures;

import com.wurmonline.client.renderer.CreatureData;
import com.wurmonline.client.renderer.cell.CreatureCellRenderable;
import net.wurmunlimited.forge.Config;


public class Creatures {

    public static String creatureCellRenderableName(final CreatureCellRenderable ccr) {
        final CreatureData creature = ccr.getCreatureData();
        String name = creature.getName();
        String desc = getDescription(creature);
        desc = desc==null? "" : " ("+desc+")";
        if(!ccr.isItem() && ccr.getPercentHealth()<100.0f)
            desc = desc+" ("+getHurtingDesc(ccr.getPercentHealth())+")";
        return name+desc;
    }

    private static final String getHurtingDesc(final float phealth) {
        if(phealth >= 85.0f) return "Healthy";
        if(phealth >= 65.0f) return "Hurt";
        if(phealth >= 35.0f) return "Wounded";
        if(phealth >= 15.0f) return "Bleeding";
        return "Dying";
    }

    public static String getDescription(final CreatureData creature) {
        String model = creature.getModelName().toString();
        if(Config.showExtraTooltips) {
            String gender = null, col = null;
            if(model.contains(".male")) gender = "male";
            else if(model.contains(".female")) gender = "female";
            if(model.contains("horse") || model.contains("foal")) {
                if(model.contains(".hell")) {
                    if(model.contains(".cinder")) col = "cinder";
                    else if(model.contains(".envious")) col = "envious";
                    else if(model.contains(".shadow")) col = "shadow";
                    else if(model.contains(".pestilential")) col = "pestilential";
                    else if(model.contains(".nightshade")) col = "nightshade";
                    else if(model.contains(".incandescent")) col = "incandescent";
                    else if(model.contains(".molten")) col = "molten";
                    else col = "Ash";
                } else {
                    if(model.contains(".brown")) col = "brown";
                    else if(model.contains(".skewbaldpinto")) col = "skewbald pinto";
                    else if(model.contains(".goldbuckskin")) col = "bold buckskin";
                    else if(model.contains(".blacksilver")) col = "black silver";
                    else if(model.contains(".appaloosa")) col = "appaloosa";
                    else if(model.contains("horse.chestnut") || model.contains("foal.chestnut")) col = "chestnut";
                    else if(model.contains(".gold")) col = "gold";
                    else if(model.contains(".black")) col = "black";
                    else if(model.contains(".white")) col = "white";
                    else if(model.contains(".piebaldpinto")) col = "piebald pinto";
                    else if(model.contains(".bloodbay")) col = "blood bay";
                    else if(model.contains(".ebonyblack")) col = "ebony black";
                    else col = "Gray";
                }
            }
            if(gender!=null && col!=null) return gender+", "+col;
            if(gender!=null) return gender;
            if(col!=null) return col;
        }
        return null;
    }
}
