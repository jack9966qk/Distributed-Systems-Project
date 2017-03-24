package com.allstars.project1;

import com.google.gson.Gson;

public class Resource {
    private String name = "";
    private String description = "";
    private String[] tags = new String[0];
    private String uri = "";
    private String channel = "";
    private String owner = "";
    private String ezServer = null;

    public Resource(String name, String description, String[] tags, String uri, String channel, String owner, String ezServer) {
        this.name = name;
        this.description = description;
        this.tags = tags;
        this.uri = uri;
        this.channel = channel;
        this.owner = owner;
        this.ezServer = ezServer;
    }

    public boolean matchesTemplate(Resource template) {
        // TODO
        return true;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }

    public static Resource fromJson(String json) {
        return new Gson().fromJson(json, Resource.class);
    }
}