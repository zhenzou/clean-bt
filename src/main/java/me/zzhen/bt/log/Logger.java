package me.zzhen.bt.log;

import java.util.logging.Level;

/**
 * Project:CleanBlog
 *
 * @author zzhen zzzhen1994@gmail.com
 *         Create Time: 2016/8/29.
 *         Version :
 *         Description:
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

    public void warn(Object msg) {
        if (proxy.isLoggable(Level.WARNING)) {
            proxy.log(Level.WARNING, msg.toString());
        }
    }

    public void info(Object msg) {
        if (proxy.isLoggable(Level.INFO)) {
            proxy.log(Level.INFO, msg.toString());
        }
    }

    public void error(Object msg) {
        if (proxy.isLoggable(Level.SEVERE)) {
            proxy.log(Level.SEVERE, msg.toString());
        }
    }

    public void debug(Object msg) {
        if (proxy.isLoggable(Level.CONFIG)) {
            proxy.log(Level.CONFIG, msg.toString());
        }
    }
}
