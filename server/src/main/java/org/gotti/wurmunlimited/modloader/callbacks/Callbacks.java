package org.gotti.wurmunlimited.modloader.callbacks;

import javassist.*;
import javassist.CtField.Initializer;
import javassist.bytecode.Descriptor;
import org.gotti.wurmunlimited.modloader.classhooks.HookException;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Callbacks {

    private static final Logger LOG = Logger.getLogger(Callbacks.class.getName());
    private static final String PROXY_CLASSNAME_PATTERN = "__cb_Proxy_%s_%d"; // callbackname, total number of callbacks
    private static final String CALLBACK_ID_PATTERN = "%s#%s"; // classname, callbackname
    private static final String METHOD_FIELD = "__cb_%s_%d"; // callbackname, method number
    private static final String INSTANCE_FIELD = "__cb_target";

    private static class CallbackInfo {
        private Object callback;
        private Supplier<Object> callbackBuilder;

        private CallbackInfo(Supplier<Object> callbackBuilder) {
            this.callbackBuilder = callbackBuilder;
        }

        public synchronized Object get() {
            if(callback==null && callbackBuilder!=null) {
                callback = callbackBuilder.get();
                callbackBuilder = null;
            }
            return callback;
        }
    }

    private final Map<String,CallbackInfo> callbackMap = new ConcurrentHashMap<>();
    private final ClassPool classPool;
    private final Loader loader;

    public Callbacks(Loader loader,ClassPool classPool) {
        this.loader = loader;
        this.classPool = classPool;
    }

    @SuppressWarnings("unchecked")
    public <T> T getCallback(String callbackId) {
        CallbackInfo callbackInfo = callbackMap.get(callbackId);
        if(callbackInfo==null || callbackInfo.get()==null) {
            throw new HookException("Callback "+callbackId+" was not found");
        }
        return (T)callbackInfo.get();
    }

    /**
     * Initialize the method on object references in the proxy.
     *
     * @param proxy          Proxy ctClass
     * @param targetField    Proxy target instance ctField
     * @param methods        Target method ctMethods
     * @param callbackTarget Target instance
     * @return Initialized proxy
     */
    private Object initializeProxy(CtClass proxy,CtField targetField,Map<String,CtMethod> methods,Object callbackTarget) {
        try {
            Class<?> proxyClass = toClass(proxy);
            Object proxyInstance = proxyClass.newInstance();

            for(Map.Entry<String,CtMethod> entry : methods.entrySet()) {
                final CtMethod ctMethod = entry.getValue();
                final Class<?> paramTypes[] = Arrays.stream(ctMethod.getParameterTypes()).map(this::toClass).toArray(Class[]::new);
                final Method method = callbackTarget.getClass().getMethod(ctMethod.getName(),paramTypes);
                method.setAccessible(true);
                proxyClass.getField(entry.getKey()).set(proxyInstance,method);
            }
            proxyClass.getField(targetField.getName()).set(proxyInstance,callbackTarget);
            return proxyInstance;
        } catch(Exception e) {
            throw new HookException(e);
        }
    }

    /**
     * Get a CtClass for a class using an appropriate classpool for the class.
     *
     * @param clazz Class
     * @return CtClass
     */
    private static CtClass getCtClass(Class<?> clazz) {
        try {
            final ClassPool classPool = new ClassPool();
            classPool.appendClassPath(new ClassClassPath(clazz));
            final CtClass ctClass = classPool.get(clazz.getName());
            return ctClass;
        } catch(NotFoundException e) {
            throw new HookException(e);
        }
    }

    /**
     * Get a CtClass for a class using the main classpool.
     *
     * @param clazz Class
     * @return CtClass
     */
    private CtClass toCtClass(Class<?> clazz) {
        try {
            return classPool.get(clazz.getName());
        } catch(NotFoundException e) {
            throw new HookException(e);
        }
    }

    /**
     * Get a CtClass for a CtClass using the main classpool.
     *
     * @param ctClass CtClass
     * @return CtClass from the main classpool
     */
    private CtClass toCtClass(CtClass ctClass) {
        try {
            return classPool.get(ctClass.getName());
        } catch(NotFoundException e) {
            throw new HookException(e);
        }
    }

    /**
     * Create a class from a CtClass using the main loader.
     *
     * @param ctClass CtClass
     * @return Class
     */
    private Class<?> toClass(CtClass ctClass) {
        try {
            if(ctClass.isPrimitive()) {
                final CtPrimitiveType ctPrimitiveType = (CtPrimitiveType)ctClass;
                switch(ctPrimitiveType.getDescriptor()) {
                    case 'Z':
                        return boolean.class;
                    case 'C':
                        return char.class;
                    case 'B':
                        return byte.class;
                    case 'S':
                        return short.class;
                    case 'I':
                        return int.class;
                    case 'J':
                        return long.class;
                    case 'F':
                        return float.class;
                    case 'D':
                        return double.class;
                    case 'V':
                        return void.class;
                    default:
                        throw new HookException("Invalid type "+ctClass.getName());
                }
            } else if(ctClass.isArray()) {
                return Class.forName(Descriptor.toJavaName(Descriptor.of(ctClass)));
            } else {
                return loader.loadClass(ctClass.getName());
            }
        } catch(ClassNotFoundException e) {
            throw new HookException(e);
        }
    }

    /**
     * Add a callback with name callbackName to class targetClass. The callback will be a proxy for callbackTarget.
     *
     * @param targetClass    Class to add the proxy to
     * @param callbackName   Field name of the callback in targetClass
     * @param callbackTarget Target of proxy
     */
    public void addCallback(CtClass targetClass,String callbackName,Object callbackTarget) {
        try {
            final CtClass proxy = classPool.makeClass(String.format(PROXY_CLASSNAME_PATTERN,callbackName,callbackMap.size()));
            final String callbackId = String.format(CALLBACK_ID_PATTERN,targetClass.getName(),callbackName);

            final CtField targetField = new CtField(classPool.get(Object.class.getName()),INSTANCE_FIELD,proxy);
            targetField.setModifiers(Modifier.PUBLIC);
            proxy.addField(targetField);

            Map<String,CtMethod> methodFieldValues = new HashMap<>();
            CtMethod[] methods = getCtClass(callbackTarget.getClass()).getMethods();

            // Check if any method has the CallbackApi annotation. This will make the annotation required for other methods too
            boolean requireAnnotation = Arrays.stream(methods).anyMatch(method -> method.hasAnnotation(CallbackApi.class));

            // Create the proxy methods
            for(CtMethod method : methods) {
                // Object methods are ignored. If they are required then overload them
                if(method.getDeclaringClass().getName().equals(Object.class.getName())) {
                    continue;
                }
                // If any method has the annotation then all methods require it to be pick up
                if(requireAnnotation && !method.hasAnnotation(CallbackApi.class)) {
                    continue;
                }

                // Create a field where the original Method is stored
                final CtField methodField = new CtField(toCtClass(Method.class),String.format(METHOD_FIELD,method.getName(),methodFieldValues.size()),proxy);
                methodField.setModifiers(Modifier.PUBLIC);
                proxy.addField(methodField);

                // Create the new method prototype
                final CtClass returnType = toCtClass(method.getReturnType());
                final CtClass[] paramTypes = Arrays.stream(method.getParameterTypes()).map(this::toCtClass).toArray(CtClass[]::new);
                final CtMethod ctMethod = new CtMethod(returnType,method.getName(),paramTypes,proxy);

                // Set the method body. Simply reflection invoke the saved method on the target 
                if(returnType==CtClass.voidType) {
                    final String code = String.format("%s.invoke(%s, $args);",methodField.getName(),INSTANCE_FIELD);
                    ctMethod.setBody(code);
                } else {
                    final String code = String.format("return ($r) %s.invoke(%s, $args);",methodField.getName(),INSTANCE_FIELD);
                    ctMethod.setBody(code);
                }
                proxy.addMethod(ctMethod);

                // Save the field name and the method prototype for later initialisation
                methodFieldValues.put(methodField.getName(),ctMethod);
            }

            // Create a static field for the callback on the target class
            final CtField callbackField = new CtField(proxy,callbackName,targetClass);
            callbackField.setModifiers(Modifier.STATIC|Modifier.PRIVATE);

            //Initializer initializer =  CtField.Initializer.byCall(classPool.get(HookManager.class.getName()), "getCallback", new String[]{ callbackId });
            final String expr = String.format("(%s) %s.getCallback(\"%s\")",proxy.getName(),HookManager.class.getName(),callbackId);
            final Initializer initializer = CtField.Initializer.byExpr(expr);
            targetClass.addField(callbackField,initializer);

            // Put the callback in the map
            final CallbackInfo callbackInfo = new CallbackInfo(() -> initializeProxy(proxy,targetField,methodFieldValues,callbackTarget));
            callbackMap.put(callbackId,callbackInfo);

            final List<String> methodNames = methodFieldValues.values().stream().map(CtMethod::getLongName).collect(Collectors.toList());
            LOG.info(String.format("Adding callback %s to class %s for  %s with methods %s",callbackName,targetClass.getName(),callbackTarget.getClass().getName(),methodNames));
        } catch(CannotCompileException|NotFoundException|IllegalArgumentException|SecurityException e) {
            throw new HookException(e);
        }
    }

    public void init() {
        for(CallbackInfo callbackInfo : callbackMap.values()) {
            callbackInfo.get();
        }
    }
}
