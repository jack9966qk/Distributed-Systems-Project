package com.allstars.project1;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Jack on 23/3/2017.
 */
public class Debug {
    private static boolean enablePrint = false;

    public static void setEnablePrint(boolean enablePrint) {
        Debug.enablePrint = enablePrint;
    }

    private static String getPrefix() {
        SimpleDateFormat dt = new SimpleDateFormat("dd/mm/yyyy hh:mm:ss.SSS");
        String date = dt.format(new Date());

        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        StackTraceElement elem = trace[trace.length - 1];
        String pos = elem.toString()
                .replaceAll("com\\.allstars\\.project1\\.", "")
                .replaceAll("\\(.*\\)", "");

        return date + " " + pos + " ";
    }

    public static void infoPrintln(Object obj) {
        System.out.println(getPrefix() + "[INFO] " + obj.toString());
    }

    public static void println(Object obj) {
        if (enablePrint) {
            System.out.println(getPrefix() + "[FINE] " + obj.toString());
        }
    }
}
