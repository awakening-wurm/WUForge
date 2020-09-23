package org.gotti.wurmunlimited.modloader.interfaces;

import org.gotti.wurmunlimited.modloader.classhooks.HookManager;

/**
 * Add preInit phase to the mod.
 * <p>
 * {@link #preInit()} is called after {@link Configurable#configure(java.util.Properties)} and before {@link Initable#init()}.
 * Byte code editing should happen in this phase but no classes should be loaded here which causes the class to be frozen.
 * <p>
 * {@link HookManager#registerHook(String, String, String, org.gotti.wurmunlimited.modloader.classhooks.InvocationHandlerFactory)}
 * should NOT be called in this phase. Adding hooks renames methods and my prevent other mods from editing the byte code of that
 * method.
 * 
 * @author ago
 */
public interface PreInitable {
	/**
	 * Perform byte code editing
	 */
	void preInit();
}
