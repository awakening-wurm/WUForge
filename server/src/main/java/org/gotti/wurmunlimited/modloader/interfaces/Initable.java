package org.gotti.wurmunlimited.modloader.interfaces;

/**
 * Add init phase to the mod.
 * <p>
 * {@link #init()} is called after {@link PreInitable#preInit()}.
 * Hooks can be added in this phase but no classes should be loaded here which causes the class to be frozen.
 * <p>
 * Bytecode editing in this phase may fail because other mods may have already registered hooks which renames methods.
 * 
 * @author ago
 */

public interface Initable {
	/**
	 * Intialize hooks.
	 */
	void init();
}
