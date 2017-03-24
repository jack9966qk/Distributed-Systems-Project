package com.allstars.project1;

/**
 * Created by Jack on 24/3/2017.
 */
public class EzServer {
    String hostname;
    int port;

    public EzServer(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }

    static EzServer fromString(String string) {
        String[] parts = string.split(":");
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);
        return new EzServer(host, port);
    }

    @Override
    public String toString() {
        return hostname + ":" + port;
    }
}
