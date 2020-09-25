package org.gotti.wurmunlimited.modloader;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.NotFoundException;
import javassist.Translator;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Translator which adds each loaded class to a list. Only classes handled directly by the class pool are added.
 *
 * @author ago
 */
public class AddToListTranslator implements Translator {

    private List<String> loaded = new LinkedList<>();
    private Predicate<String> predicate;

    /**
     * Create translator with a default predicate which adds all loaded classes to the list.
     */
    public AddToListTranslator() {
        this(p -> true);
    }

    /**
     * Create translator with a predicate to select which classes to add to the list.
     *
     * @param predicate Predicate. The class is only added to the list if the predicate returns true for the class name
     */
    public AddToListTranslator(Predicate<String> predicate) {
        this.predicate = predicate;
    }

    @Override
    public void start(ClassPool paramClassPool) throws NotFoundException, CannotCompileException {
    }

    @Override
    public void onLoad(ClassPool paramClassPool,String paramString) throws NotFoundException, CannotCompileException {
        if(predicate.test(paramString)) {
            loaded.add(paramString);
        }
    }

    /**
     * Get the list of loaded classes.
     *
     * @return list of loaded classes
     */
    public List<String> getLoadedClasses() {
        return loaded;
    }
}
