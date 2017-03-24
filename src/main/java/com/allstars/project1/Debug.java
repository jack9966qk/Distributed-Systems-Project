package com.allstars.project1;

/**
 * Created by Jack on 23/3/2017.
 */
public class Debug {
    private static boolean enablePrint = false;

    public static void setEnablePrint(boolean enablePrint) {
        Debug.enablePrint = enablePrint;
    }

    public static void println(String str) {
        System.out.println(str);
    }
}
