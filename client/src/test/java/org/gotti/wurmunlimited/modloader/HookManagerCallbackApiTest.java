package org.gotti.wurmunlimited.modloader;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.NotFoundException;
import net.spirangle.wuforge.Config;
import org.assertj.core.api.Assertions;
import org.gotti.wurmunlimited.modloader.callbacks.CallbackApi;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Properties;

@RunWith(HookManagerTestRunner.class)
public class HookManagerCallbackApiTest {

    public static class CallbackApiMod {

        public CallbackApiMod() throws NotFoundException, CannotCompileException {

            HookManager hookManager = HookManager.getInstance();
            CtClass ctClass = hookManager.getClassPool().get(HookManagerCallbackApiTest.class.getName()+"$CallbackApiTarget");

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

    @Test
    public void testShared() throws Exception {
        Config.modsDir = "mods";

        ModInstanceBuilder<Object> instanceBuilder = new ModInstanceBuilder<>(Object.class);

        String modName = "test";

        Properties properties = new Properties();
        properties.setProperty("sharedClassLoader","true");
        properties.setProperty("classname",CallbackApiMod.class.getName());
        properties.setProperty("classpath","test.jar,files");
        ModInfo modEntry = new ModInfo(properties,modName);

        instanceBuilder.createModInstance(modEntry);

        Assertions.assertThat(new CallbackApiTarget().test()).isTrue();
    }

}
