package org.gotti.wurmunlimited.modloader.interfaces;

import java.util.Properties;

/**
 * Add configure phase to the mod.
 * <p>
 * {@link #configure(Properties)} is called first in the mod lifecycle and should be used to prepare mod settings. No code modifications should happen at this stage.
 */
public interface Configurable {

	/**
	 * Configure the module from configuration settings.
	 * 
	 * @param properties
	 *            Properties from modname.properties and modname.config
	 */
	void configure(Properties properties);

}
