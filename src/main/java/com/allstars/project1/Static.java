package com.allstars.project1;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Class for globally available static variables, functions and constants
 * Created by Jack on 25/3/2017.
 */
public class Static {
    public static final Gson GSON = new Gson();
    public static Gson GSON_WITH_NULL = new GsonBuilder().serializeNulls().create();
    public static final int DEFAULT_EXCHANGE_INTERVAL = 1000 * 60 * 10;
    public static final int DEFAULT_CONNECTION_INTERVAL = 1000;
    public static final int DEFAULT_TIMEOUT = 1000 * 10;

    /**
     *
     * @param str
     * @return
     */
    private static String addEscChar(String str) {
        return str.replace("/", "\\/");
    }

    /**
     *
     * @param str
     * @return
     */
    private static String removeEscChar(String str) {
        return str.replace("\\/", "/");
    }

    /**
     *
     * @param out
     * @param str
     * @throws IOException
     */
    public static void sendJsonUTF(DataOutputStream out, String str) throws IOException {
        String escaped = addEscChar(str);
        out.writeUTF(escaped);
        Logging.logFine("SENT: " + escaped);
    }

    /**
     * 
     * @param in
     * @return
     * @throws IOException
     */
    public static String readJsonUTF(DataInputStream in) throws IOException {
        String str = in.readUTF();
        Logging.logFine("RECEIVED: " + str);
        String removed = removeEscChar(str);
        return removeEscChar(removed);
    }
}
