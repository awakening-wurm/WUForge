package org.gotti.wurmunlimited.modloader.interfaces;

import java.util.Properties;

/**
 * Mod entry.
 *
 * @param <T>
 *            Actual mod entry type
 */
public interface ModEntry<T> {

	/**
	 * Get the mod name
	 * 
	 * @return mod name
	 */
	String getName();

	/**
	 * Get the properties of the mod
	 * 
	 * @return mod properties
	 */
	Properties getProperties();

	/**
	 * Get the actual mod
	 * 
	 * @return the actual mod
	 */
	T getWurmMod();

}
