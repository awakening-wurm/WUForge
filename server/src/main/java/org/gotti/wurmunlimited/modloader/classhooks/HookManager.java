package org.gotti.wurmunlimited.modloader.classhooks;

import javassist.*;
import org.gotti.wurmunlimited.modloader.callbacks.Callbacks;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HookManager {

    // Javassist class pool
    private ClassPool classPool;

    // Javassist class loader
    private Loader loader;

    // classloaders for retrieving resources from mods with sharedClassLoader=true
    private Map<URL,URLClassLoader> resourceClassLoaders;

    // Invocation targets
    private Map<String,InvocationTarget> invocationTargets = new HashMap<>();

    // Instance
    private static HookManager instance;

    // Callbacks
    private Callbacks callbacks;

    private static final Logger LOG = Logger.getLogger(HookManager.class.getName());

    private HookManager() {
        classPool = ClassPool.getDefault();
        resourceClassLoaders = new LinkedHashMap<>();
        loader = new Loader(classPool) {

            @Override
            public URL getResource(String name) {
                URL url = getParent().getResource(name);
                if(url==null) {
                    url = findResource(name);
                }
                return url;
            }

            @Override
            protected URL findResource(String name) {
                for(URLClassLoader urlClassLoader : resourceClassLoaders.values()) {
                    URL url = urlClassLoader.findResource(name);
                    if(url!=null) {
                        return url;
                    }
                }
                return super.findResource(name);
            }

            @Override
            protected Enumeration<URL> findResources(String name) throws IOException {
                List<Enumeration<URL>> enumerations = new ArrayList<>();
                enumerations.add(super.findResources(name));
                for(URLClassLoader urlClassLoader : resourceClassLoaders.values()) {
                    enumerations.add(urlClassLoader.findResources(name));
                }
                return new CompoundEnumeration<>(enumerations);
            }

            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                int index = name.lastIndexOf(".");
                if(index!=-1) {
                    try {
                        Package pkg = getPackage(name.substring(0,index));
                        if(pkg==null) {
                            CtClass ctClass = classPool.get(name);
                            String url = ctClass.getURL().toString();
                            if(url.startsWith("jar:") && url.lastIndexOf("!")!=-1) {
                                URL manifestURL = new URL(url.substring(0,url.lastIndexOf("!")+1)+"/META-INF/MANIFEST.MF");
                                try(InputStream is = manifestURL.openStream()) {
                                    Manifest man = new Manifest(is);

                                    String specTitle = null, specVersion = null, specVendor = null;
                                    String implTitle = null, implVersion = null, implVendor = null;

                                    String path = name.replace('.','/').concat("/");

                                    Attributes attr = man.getAttributes(path);
                                    if(attr!=null) {
                                        specTitle = attr.getValue(Name.SPECIFICATION_TITLE);
                                        specVersion = attr.getValue(Name.SPECIFICATION_VERSION);
                                        specVendor = attr.getValue(Name.SPECIFICATION_VENDOR);
                                        implTitle = attr.getValue(Name.IMPLEMENTATION_TITLE);
                                        implVersion = attr.getValue(Name.IMPLEMENTATION_VERSION);
                                        implVendor = attr.getValue(Name.IMPLEMENTATION_VENDOR);
                                    }
                                    attr = man.getMainAttributes();
                                    if(attr!=null) {
                                        if(specTitle==null) {
                                            specTitle = attr.getValue(Name.SPECIFICATION_TITLE);
                                        }
                                        if(specVersion==null) {
                                            specVersion = attr.getValue(Name.SPECIFICATION_VERSION);
                                        }
                                        if(specVendor==null) {
                                            specVendor = attr.getValue(Name.SPECIFICATION_VENDOR);
                                        }
                                        if(implTitle==null) {
                                            implTitle = attr.getValue(Name.IMPLEMENTATION_TITLE);
                                        }
                                        if(implVersion==null) {
                                            implVersion = attr.getValue(Name.IMPLEMENTATION_VERSION);
                                        }
                                        if(implVendor==null) {
                                            implVendor = attr.getValue(Name.IMPLEMENTATION_VENDOR);
                                        }
                                    }

                                    definePackage(ctClass.getPackageName(),specTitle,specVersion,specVendor,implTitle,implVersion,implVendor,null);

                                } catch(IOException e) {
                                }
                            }
                        }
                    } catch(NotFoundException|MalformedURLException e) {
                    }
                }

                return super.findClass(name);
            }

            //
            // Some javax.* packages are not part of the JDK and must be loaded from the modloader classpath.
            // We look for classes in the parent classpath first. If we found the class from a javax
            // package we store the package name in a set of system provided javax packages. All classes
            // from those packages are always resolved from the parent classpath.
            // if a class from a javax. package is not found null is returned instead of a ClassNotFound
            // exception to continue searching for the class in the modloader classpath.
            //
            // The special treatment does only apply to mods with sharedClassLoader. The javax packages
            // are resolved without problems if the mod has its own classloader.
            //
            private final Set<String> delegateJavaXPackages = Collections.synchronizedSet(new HashSet<>());
            private final Pattern javaxPackagePattern = Pattern.compile("^(?<pkg>javax\\.[^.]+)\\..*$");

            protected Class<?> delegateToParent(String classname) throws ClassNotFoundException {
                Matcher matcher = javaxPackagePattern.matcher(classname);
                if(!matcher.matches()) {
                    return super.delegateToParent(classname);
                }

                String javaxPackage = matcher.group("pkg");
                if(delegateJavaXPackages.contains(javaxPackage)) {
                    return super.delegateToParent(classname);
                }

                try {
                    Class<?> c = super.delegateToParent(classname);
                    delegateJavaXPackages.add(javaxPackage);
                    return c;
                } catch(ClassNotFoundException e) {
                    return null;
                }
            }
        };
        callbacks = new Callbacks(loader,classPool);
    }

    public static synchronized HookManager getInstance() {
        if(instance==null) {
            instance = new HookManager();
        }
        return instance;
    }

    public ClassPool getClassPool() {
        return classPool;
    }

    public Loader getLoader() {
        return loader;
    }

    /**
     * Create a unique method name in the class. The name is generated from the baseName + "$" + number
     *
     * @param ctClass  Class
     * @param baseName method base name
     * @return unique name
     */
    private static String getUniqueMethodName(CtClass ctClass,String baseName) {
        Set<String> usedNames = new HashSet<>();
        for(CtMethod method : ctClass.getDeclaredMethods()) {
            usedNames.add(method.getName());
        }

        int i = 1;
        do {
            String methodName = String.format("%s$%d",baseName,i++);
            if(!usedNames.contains(methodName)) {
                return methodName;
            }
        } while(true);
    }

    private InvocationTarget createHook(CtClass ctClass,ClassHook classHook) throws NotFoundException, CannotCompileException {
        CtMethod origMethod;

        if(classHook.getMethodType()!=null) {
            origMethod = ctClass.getMethod(classHook.getMethodName(),classHook.getMethodType());
        } else {
            origMethod = ctClass.getDeclaredMethod(classHook.getMethodName());
        }

        if(Modifier.isNative(origMethod.getModifiers())) {
            throw new CannotCompileException("native methods can not be hooked");
        }


        String callee;
        boolean isStatic = Modifier.isStatic(origMethod.getModifiers());
        if(isStatic) {
            callee = String.format("%s.class",ctClass.getName());
        } else {
            callee = "this";
        }

        origMethod.setName(getUniqueMethodName(ctClass,classHook.getMethodName()));

        CtMethod newMethod = CtNewMethod.copy(origMethod,classHook.getMethodName(),ctClass,null);

        CtClass[] exceptionTypes = origMethod.getExceptionTypes();
        Class<?>[] exceptionClasses = new Class<?>[exceptionTypes.length];
        for(int i = 0; i<exceptionTypes.length; i++) {
            try {
                exceptionClasses[i] = loader.loadClass(exceptionTypes[i].getName());
            } catch(ClassNotFoundException e) {
                throw new CannotCompileException(e);
            }
        }

        InvocationTarget invocationTarget = new InvocationTarget(classHook.getInvocationHandlerFactory(),isStatic,origMethod.getName(),origMethod.getLongName(),exceptionClasses);

        CtClass type = newMethod.getReturnType();
        String typeName = type.getName();
        boolean voidType = "void".equals(typeName);

        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        if(!voidType) {
            builder.append("Object result = ");
        }
        builder.append(String.format("%s#getInstance().invoke(%s,\"%s\",$args);\n",HookManager.class.getName(),callee,origMethod.getLongName()));
        if(!voidType) {
            if(!type.isPrimitive()) {
                builder.append(String.format("return (%s)result;\n",typeName));
            } else if(type==CtClass.booleanType) {
                builder.append(String.format("return ((java.lang.Boolean)result).booleanValue();\n",typeName));
            } else if(type==CtClass.byteType) {
                builder.append(String.format("return ((java.lang.Number)result).byteValue();\n",typeName));
            } else if(type==CtClass.shortType) {
                builder.append(String.format("return ((java.lang.Number)result).shortValue();\n",typeName));
            } else if(type==CtClass.intType) {
                builder.append(String.format("return ((java.lang.Number)result).intValue();\n",typeName));
            } else if(type==CtClass.longType) {
                builder.append(String.format("return ((java.lang.Number)result).longValue();\n",typeName));
            } else if(type==CtClass.floatType) {
                builder.append(String.format("return ((java.lang.Number)result).floatValue();\n",typeName));
            } else if(type==CtClass.doubleType) {
                builder.append(String.format("return ((java.lang.Number)result).doubleValue();\n",typeName));
            } else if(type==CtClass.charType) {
                builder.append(String.format("return ((java.lang.Character)result).charValue();\n",typeName));
            }
        }
        builder.append("\n}");

        String body = builder.toString();
        LOG.fine(body);
        newMethod.setBody(body);
        ctClass.addMethod(newMethod);

        return invocationTarget;
    }

    /**
     * Register a hook.
     *
     * @param className                Class name to hook
     * @param methodName               Method to hook
     * @param methodType               Method signature to hook
     * @param invocationHandlerFactory Factory to create the InvocationHandler to call
     */
    public void registerHook(String className,String methodName,String methodType,InvocationHandlerFactory invocationHandlerFactory) {
        ClassHook classHook = new ClassHook(methodName,methodType,invocationHandlerFactory);
        try {
            CtClass ctClass = classPool.get(className);
            InvocationTarget target = createHook(ctClass,classHook);
            invocationTargets.put(target.getIdentifier(),target);
        } catch(NotFoundException|CannotCompileException e) {
            throw new HookException(e);
        }
    }

    /**
     * Register a hook.
     *
     * @param className         Class name to hook
     * @param methodName        Method to hook
     * @param methodType        Method signature to hook
     * @param invocationHandler InvocationHandler to call
     */
    @Deprecated
    public void registerHook(String className,String methodName,String methodType,InvocationHandler invocationHandler) {
        registerHook(className,methodName,methodType,new InvocationHandlerFactory() {
            public InvocationHandler createInvocationHandler() {
                return invocationHandler;
            }

            ;
        });
    }

    /**
     * Invoke the InvocationHandler for a class hook
     *
     * @param object        Hooked class object
     * @param wrappedMethod Hooked method
     * @param args          Call arguments
     * @return Call result
     * @throws Throwable Throwables
     */
    public Object invoke(Object object,String wrappedMethod,Object[] args) throws Throwable {
        // Get the invocation target
        InvocationTarget invocationTarget = invocationTargets.get(wrappedMethod);
        if(invocationTarget==null) {
            throw new HookException("Uninstrumented method "+wrappedMethod);
        }

        try {
            // Get the called method
            Method method = invocationTarget.resolveMethod(invocationTarget.isStaticMethod()? (Class<?>)object : object.getClass());

            boolean accessible = method.isAccessible();
            method.setAccessible(true);
            try {
                // Call the invocation handler
                return invocationTarget.resolveInvocationHandler().invoke(object,method,args);
            } finally {
                method.setAccessible(accessible);
            }
        } catch(Throwable e) {
            for(Class<?> exceptionType : invocationTarget.getExceptionTypes()) {
                if(exceptionType.isInstance(e)) {
                    throw e;
                }
            }
            throw new HookException(e);
        }
    }

    public static <T> T getCallback(String callbackId) {
        return getInstance().callbacks.getCallback(callbackId);
    }

    public void addCallback(CtClass targetClass,String callbackName,Object callbackTarget) {
        callbacks.addCallback(targetClass,callbackName,callbackTarget);
    }

    public void initCallbacks() {
        callbacks.init();
    }

    public void appendSharedClassPath(Path path) throws NotFoundException, MalformedURLException {
        classPool.appendClassPath(path.toString());
        final URL url = path.toUri().toURL();
        resourceClassLoaders.putIfAbsent(url,new URLClassLoader(new URL[]{url},this.getClass().getClassLoader()));
    }
}
