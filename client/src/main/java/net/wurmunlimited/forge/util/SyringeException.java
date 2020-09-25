package net.wurmunlimited.forge.util;

public class SyringeException extends RuntimeException {
    private static final long serialVersionUID = -8955817806357327378L;

    public SyringeException(Throwable e) {
        super(e);
    }

    public SyringeException(String message) {
        super(message);
    }
}
