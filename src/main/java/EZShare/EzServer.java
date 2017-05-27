package EZShare;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * EzServer object, represnt an EZShare server
 * Created by Jack on 24/3/2017.
 */
public class EzServer {
    String hostname;
    int port;

    /**
     * Construct an EzServer object
     *
     * @param hostname hostname of server
     * @param port     port of server
     */
    public EzServer(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }

    /**
     * Construct EzServer object from string
     *
     * @param string string in "host:port" format
     * @return EzServer constructed
     */
    static EzServer fromString(String string) {
        String[] parts = string.split(":");
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);
        return new EzServer(host, port);
    }

    /**
     * Construct EzServer object from json
     *
     * @param json json string
     * @return EzServer constructed
     */
    public static EzServer fromJson(String json) {
        return fromJson(new JsonParser().parse(json).getAsJsonObject());
    }

    /**
     * Construct EzServer object from json
     *
     * @param jsonObj json object
     * @return EzServer constructed
     */
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

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
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
