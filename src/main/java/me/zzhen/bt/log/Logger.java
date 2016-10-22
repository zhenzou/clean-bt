package me.zzhen.bt.log;

import java.util.logging.Level;

/**
 * Project:CleanBlog
 * Author zhen
 * Create Time: 2016/8/29.
 * Version :
 * Description:
 */
public class Logger {

    private final java.util.logging.Logger proxy;

    public static Logger getLogger(String name) {
        return new Logger(name);
    }

    public static Logger getLogger(Class clazz) {
        return new Logger(clazz.getName());
    }

    private Logger(String name) {
        proxy = java.util.logging.Logger.getLogger(name);
    }

    public void warn(String msg) {
        if (proxy.isLoggable(Level.WARNING)) {
            proxy.log(Level.WARNING, msg);
        }
    }

    public void info(String msg) {
        if (proxy.isLoggable(Level.INFO)) {
            proxy.log(Level.INFO, msg);
        }
    }

    public void error(String msg) {
        if (proxy.isLoggable(Level.SEVERE)) {
            proxy.log(Level.SEVERE, msg);
        }
    }

    public void debug(String msg) {

        if (proxy.isLoggable(Level.CONFIG)) {
            proxy.log(Level.CONFIG, msg);
        }
    }
}
