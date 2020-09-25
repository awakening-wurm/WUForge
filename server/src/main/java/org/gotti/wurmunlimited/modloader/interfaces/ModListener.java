package org.gotti.wurmunlimited.modloader.interfaces;

/**
 * {@link ModListener#modInitialized(ModEntry)} is called for each activated mod after they have been intialized.<
 * <p>
 * The interface can be used on a mod if it needs to know which mods are available.
 *
 * @author ago
 */
public interface ModListener {

    /**
     * Called once for each activated mod after it has been initalized.
     *
     * @param entry Mod entry
     */
    void modInitialized(ModEntry<?> entry);

}
