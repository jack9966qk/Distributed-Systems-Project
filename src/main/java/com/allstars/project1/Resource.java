package com.allstars.project1;

import com.google.gson.Gson;

import java.util.Arrays;

public class Resource {
    public String getName() {
        return name;
    }



    public String getDescription() {
        return description;
    }

    public String[] getTags() {
        return tags;
    }

    public String getUri() {
        return uri;
    }

    public String getChannel() {
        return channel;
    }

    public String getOwner() {
        return owner;
    }

    public String getEzServer() {
        return ezServer;
    }

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
        if (template.name != null && !this.name.equals(template.name)) {
            return false;
        } else if (template.channel != null && !this.channel.equals(template.channel)) {
            return false;
        } else if (template.description != null && !this.description.equals(template.description)) {
            return false;
        } else if (template.tags != null && !Arrays.asList(this.tags).containsAll(Arrays.asList(template.tags))) {
            return false;
        } else if (template.uri != null && !this.uri.equals(template.uri)) {
            return false;
        } else if (template.owner != null && !this.owner.equals(template.owner)) {
            return false;
        } else if (template.ezServer != null && !this.ezServer.equals(template.ezServer)) {
            return false;
        } else {
            return true;
        }
    }

    public String toJson() {
        return new Gson().toJson(this);
    }

    public static Resource fromJson(String json) {
        return new Gson().fromJson(json, Resource.class);
    }


    // =========EQUALS AND HASHCODE AUTOMATICALLY IMPLEMENTED BY INTELLIJ IDEA=======
    // may need to be changed in the future

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Resource resource = (Resource) o;

        if (name != null ? !name.equals(resource.name) : resource.name != null)
            return false;
        if (description != null ? !description.equals(resource.description) : resource.description != null)
            return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(tags, resource.tags)) return false;
        if (uri != null ? !uri.equals(resource.uri) : resource.uri != null)
            return false;
        if (channel != null ? !channel.equals(resource.channel) : resource.channel != null)
            return false;
        if (owner != null ? !owner.equals(resource.owner) : resource.owner != null)
            return false;
        return ezServer != null ? ezServer.equals(resource.ezServer) : resource.ezServer == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(tags);
        result = 31 * result + (uri != null ? uri.hashCode() : 0);
        result = 31 * result + (channel != null ? channel.hashCode() : 0);
        result = 31 * result + (owner != null ? owner.hashCode() : 0);
        result = 31 * result + (ezServer != null ? ezServer.hashCode() : 0);
        return result;
    }
}