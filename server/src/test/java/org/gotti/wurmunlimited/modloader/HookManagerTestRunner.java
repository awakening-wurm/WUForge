package org.gotti.wurmunlimited.modloader;

import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

import javassist.Loader;

public class HookManagerTestRunner extends BlockJUnit4ClassRunner {
	
	private static final Loader loader = initLoader();
	
	private static Loader initLoader() {
		final Loader loader = HookManager.getInstance().getLoader();
		loader.delegateLoadingOf("org.junit.");
		loader.delegateLoadingOf("org.gotti.wurmunlimited.modloader.classhooks.");
		loader.delegateLoadingOf("javassist.");
		return loader;
	}

	public HookManagerTestRunner(Class<?> klass) throws InitializationError {
		super(load(klass));
	}

	private static Class<?> load(Class<?> clazz) throws InitializationError {
		try {

			return Class.forName(clazz.getName(), true, loader);
		} catch (ClassNotFoundException e) {
			throw new InitializationError(e);
		}
	}
}
