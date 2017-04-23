package com.allstars.project1;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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

    public static EzServer fromJson(String json) {
        return fromJson(new JsonParser().parse(json).getAsJsonObject());
    }

    public static EzServer fromJson(JsonObject jsonObj) {
        if (!jsonObj.has("port") || !jsonObj.has("hostname")) {
            return null;
        } else if (jsonObj.get("port").getAsJsonPrimitive().isNumber() &&
                jsonObj.get("hostname").getAsJsonPrimitive().isString()) {
            return Static.GSON.fromJson(jsonObj, EzServer.class);
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return hostname + ":" + port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EzServer ezServer = (EzServer) o;

        if (port != ezServer.port) return false;
        return hostname != null ? hostname.equals(ezServer.hostname) : ezServer.hostname == null;
    }

    @Override
    public int hashCode() {
        int result = hostname != null ? hostname.hashCode() : 0;
        result = 31 * result + port;
        return result;
    }
}
