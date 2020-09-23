package org.gotti.wurmunlimited.modloader.callbacks;

import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.gotti.wurmunlimited.modloader.classhooks.HookManager;

/**
 * Mark a method for callback instrumentation.
 * 
 * The method will be available as a callback method when using {@link HookManager#addCallback(javassist.CtClass, String, Object)}
 * 
 * @author ago
 */
@Retention(CLASS)
@Target(value = { ElementType.METHOD })
public @interface CallbackApi {
}
