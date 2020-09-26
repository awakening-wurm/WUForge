package org.gotti.wurmunlimited.modloader;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.Loader;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DefrostingClassLoaderTest {

    private ClassPool classPool;

    @Before
    public void setUp() {
        classPool = new ClassPool(true);
    }

    /**
     * Test loading a system class. The defrosting class loader should try to defrost that class
     *
     * @throws Exception
     */
    @Test
    public void testDefrostSystemClass() throws Exception {
        try(DefrostingClassLoader loader = new DefrostingClassLoader(classPool)) {
            loader.loadClass("java.awt.Event");
        }
    }

    /**
     * Test loading a class pool class and verify freeze and defrosting
     *
     * @throws Exception
     */
    @Test
    public void testDefrost() throws Exception {
        CtClass ctClass = classPool.makeClass("test.package.Test");
        Assert.assertFalse(ctClass.isFrozen());
        try(DefrostingClassLoader loader = new DefrostingClassLoader(classPool)) {
            loader.loadClass("test.package.Test");
            Assert.assertTrue(ctClass.isFrozen());
        }
        Assert.assertFalse(ctClass.isFrozen());
    }

    /**
     * Test loading a frozen class pool class and verify it's not defrosted again
     *
     * @throws Exception
     */
    @Test
    public void testDefrostAlreadyFrozenClass() throws Exception {
        CtClass ctClass = classPool.makeClass("test.package.Test");
        new Loader(classPool).loadClass("test.package.Test");
        Assert.assertTrue(ctClass.isFrozen());
        try(DefrostingClassLoader loader = new DefrostingClassLoader(classPool)) {
            loader.loadClass("test.package.Test");
            Assert.assertTrue(ctClass.isFrozen());
        }
        Assert.assertTrue(ctClass.isFrozen());
    }

}
