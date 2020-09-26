package org.gotti.wurmunlimited.modloader;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.NotFoundException;
import javassist.Translator;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;

import java.io.Closeable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Helper for checking and logging if a mod loaded and froze a WurmUnlimited class early.
 */
interface EarlyLoadingChecker extends Closeable {
    public static final Logger LOGGER = Logger.getLogger(EarlyLoadingChecker.class.getName());

    @Override
    public void close();

    public static EarlyLoadingChecker init(String modname,String phase) {

        final HookManager hookManager = HookManager.getInstance();
        final AddToListTranslator translator = new AddToListTranslator(paramString -> paramString.startsWith("com.wurmonline.") && !paramString.endsWith("Exception"));

        try {
            hookManager.getLoader().addTranslator(hookManager.getClassPool(),translator);

            return new EarlyLoadingChecker() {

                @Override
                public void close() {

                    for(String classname : translator.getLoadedClasses()) {
                        LOGGER.log(Level.WARNING,String.format("Mod %1$s loaded server class %3$s during phase %2$s",modname,phase,classname));
                    }

                    try {
                        hookManager.getLoader().addTranslator(hookManager.getClassPool(),NOOP_TRANSLATOR);
                    } catch(CannotCompileException|NotFoundException e) {
                    }
                }
            };
        } catch(CannotCompileException|NotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public final static Translator NOOP_TRANSLATOR = new Translator() {
        @Override
        public void start(ClassPool paramClassPool) throws NotFoundException, CannotCompileException {
        }

        @Override
        public void onLoad(ClassPool paramClassPool,String paramString) throws NotFoundException, CannotCompileException {
        }
    };
}
