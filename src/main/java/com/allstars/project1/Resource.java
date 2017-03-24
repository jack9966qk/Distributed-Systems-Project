package com.allstars.project1;

public class Resource {
    public String name = "";
    public String description = "";
    public String[] tags = new String[0];
    public String uri = "";
    public String channel = "";
    public String owner = "";
    public String ezServer = "";

    public Resource(String name, String description, String[] tags, String uri, String channel, String owner, String ezServer) {
        this.name = name;
        this.description = description;
        this.tags = tags;
        this.uri = uri;
        this.channel = channel;
        this.owner = owner;
        this.ezServer = ezServer;
    }

    public String toJson() {
        // TODO
        return "";
    }

    public static Resource fromJson(String json) {
        // TODO
        return null;
    }
}