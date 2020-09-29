package org.gotti.wurmunlimited.clientlauncher;

import com.wurmonline.client.console.ConsoleListenerClass;

import java.util.logging.*;

public class ClientLogger {

    private static final String CONSOLE_LOGGER = "com.wurmonline.console";
    private static ThreadLocal<Boolean> inLogHandler = new ThreadLocal<>();


    public static void initLogger() {
        Formatter formatter = new OneLineLogMessageFormatter();

        Handler handler = new StreamHandler(System.out,new SimpleFormatter()) {
            @Override
            public void publish(LogRecord record) {

                if(!CONSOLE_LOGGER.equals(record.getLoggerName())) {
                    try {
                        inLogHandler.set(true);
                        System.out.println(formatter.format(record));
                    } finally {
                        inLogHandler.remove();
                    }
                }
            }
        };
        Logger.getLogger("").addHandler(handler);
    }

    public static ConsoleListenerClass createConsoleListener() {
        return new ConsoleListenerClass() {

            @Override
            public void consoleOutput(String message) {
                // Avoid logging log messages as console messages again
                Boolean b = inLogHandler.get();
                if(b==null || !b) {
                    Logger.getLogger(CONSOLE_LOGGER).info(message);
                }
            }

            @Override
            public void consoleClosed() {
            }
        };
    }
}
