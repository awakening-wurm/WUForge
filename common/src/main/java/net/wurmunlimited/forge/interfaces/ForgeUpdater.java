package net.wurmunlimited.forge.interfaces;

import net.wurmunlimited.forge.mods.ReleaseVersion;

public interface ForgeUpdater {

    void updateProgress(double from,double to,String status);

    boolean updateForge(ReleaseVersion releaseVersion);
}
