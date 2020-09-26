package org.gotti.wurmunlimited.modloader;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.NotFoundException;
import net.wurmunlimited.forge.config.ForgeConfig;
import org.assertj.core.api.Assertions;
import org.gotti.wurmunlimited.modloader.callbacks.CallbackApi;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Properties;

@RunWith(HookManagerTestRunner.class)
public class HookManagerCallbackApiTest {

    public static class CallbackApiMod {

        public CallbackApiMod() throws NotFoundException, CannotCompileException {

            final HookManager hookManager = HookManager.getInstance();
            final CtClass ctClass = hookManager.getClassPool().get(HookManagerCallbackApiTest.class.getName()+"$CallbackApiTarget");

            hookManager.addCallback(ctClass,"callbackTest",this);

            ctClass.getMethod("test","()Z").insertAfter("return callbackTest.testApi();");
        }

        @CallbackApi
        public boolean testApi() {
            return true;
        }
    }

    public static class CallbackApiTarget {
        public boolean test() {
            return false;
        }
    }

    @BeforeClass
    public static void setupClass() {
        Properties properties = new Properties();
        properties.setProperty("forgeDir","test");
        ForgeConfig.init(properties);
    }

    @AfterClass
    public static void closeClass() {
        ForgeConfig.close();
    }

    @Test
    public void testShared() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("sharedClassLoader","true");
        properties.setProperty("classname",CallbackApiMod.class.getName());
        properties.setProperty("classpath","test.jar,files");
        ModInstanceBuilder<Object> instanceBuilder = new ModInstanceBuilder<>(Object.class);
        final String modName = "test";
        ModInfo modEntry = new ModInfo(properties,modName);
        instanceBuilder.createModInstance(modEntry);
        Assertions.assertThat(new CallbackApiTarget().test()).isTrue();
    }
}
