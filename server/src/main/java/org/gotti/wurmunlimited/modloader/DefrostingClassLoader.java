package org.gotti.wurmunlimited.modloader;

import java.io.Closeable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.Loader;
import javassist.NotFoundException;
import javassist.Translator;

/**
 * Classloader which defrosts loaded classes when the classloader is closed.
 * 
 * @author ago
 */
public class DefrostingClassLoader extends Loader implements Closeable {

	private static final Logger LOGGER = Logger.getLogger(DefrostingClassLoader.class.getName());

	private AddToListTranslator translator;
	private ClassPool classPool;

	public DefrostingClassLoader(ClassPool classPool) throws CannotCompileException, NotFoundException {
		super(classPool);

		// Only add classes to the defrost list which are unfrozen at load time
		translator = new AddToListTranslator(this::isUnFrozen);

		super.addTranslator(classPool, translator);
		this.classPool = classPool;
	}

	/**
	 * Test if the class is currently unfrozen
	 * 
	 * @return true if the class is unfrozen, false otherwise
	 */
	private boolean isUnFrozen(String className) {
		try {
			return !classPool.get(className).isFrozen();
		} catch (NotFoundException e) {
			return false;
		}
	}

	@Override
	public void addTranslator(ClassPool cp, Translator t) throws NotFoundException, CannotCompileException {
		throw new UnsupportedOperationException("A translator is already active");
	}

	@Override
	public void close() {
		for (String loadedClass : translator.getLoadedClasses()) {
			try {
				classPool.get(loadedClass).defrost();
			} catch (NotFoundException e) {
				LOGGER.log(Level.WARNING, e.getMessage());
			}
		}
	}
}
