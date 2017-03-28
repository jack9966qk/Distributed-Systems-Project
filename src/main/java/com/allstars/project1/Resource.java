package com.allstars.project1;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

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

    public Long getResourceSize() {
        return resourceSize;
    }

    private String name = "";
    private String description = "";
    private String[] tags = new String[0];
    private String uri = "";
    private String channel = "";
    private String owner = "";
    private String ezServer = null;
    private Long resourceSize = null;

    public Resource(String name, String description, String[] tags, String uri, String channel, String owner, String ezServer, Long resourceSize) {
        this.name = name;
        this.description = description;
        this.tags = tags;
        this.uri = uri;
        this.channel = channel;
        this.owner = owner;
        this.ezServer = ezServer;
        this.resourceSize = resourceSize;
    }

    public Resource(String name, String description, String[] tags, String uri, String channel, String owner, String ezServer) {
        // TODO normalise strings
        this.name = name;
        this.description = description;
        this.tags = tags;
        this.uri = uri;
        this.channel = channel;
        this.owner = owner;
        this.ezServer = ezServer;
    }

    public boolean matchesTemplate(Resource template) {
        if (!this.channel.equals(template.channel)) {
            return false;
        } else if (!template.getOwner().equals("") && !this.owner.equals(template.owner)) {
            return false;
        } else if (template.getTags() != null && !Arrays.asList(this.tags).containsAll(Arrays.asList(template.tags))) { // TODO case insensitive
            return false;
        } else if (template.getUri() != null && !template.getUri().equals("") && !this.uri.equals(template.getUri())) {
            return false;
        } else {
            return true; // TODO if candidate name contains the template name as a substring (for non "" template name), OR
//            The candidate description contains the template description as a substring (for non "" template descriptions)
//            OR
//            The template description and name are both ""))
        }
    }

    public String toJson() {
        return new Gson().toJson(this);
    }

    public String toJsonWithSize(long size) {
        return new Resource(this.name, this.description, this.tags, this.uri, this.channel, this.owner, this.ezServer, size).toJson();
    }

    public static Resource fromJson(String json) {
        return new Gson().fromJson(json, Resource.class);
    }

    public static Resource fromJsonElem(JsonElement elem) {
        return new Gson().fromJson(elem, Resource.class);
    }


    // =========EQUALS AND HASHCODE AUTOMATICALLY IMPLEMENTED BY INTELLIJ IDEA=======
    // may need to be changed in the future

    @Override
    public boolean equals(Object o) {

        // TODO match only primary keys

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
  //      int result = name != null ? name.hashCode() : 0;
  //      result = 31 * result + (description != null ? description.hashCode() : 0);
 //       result = 31 * result + Arrays.hashCode(tags);
        int   result = uri != null ? uri.hashCode() : 0;
        result = 31 * result + (channel != null ? channel.hashCode() : 0);
        result = 31 * result + (owner != null ? owner.hashCode() : 0);
//        result = 31 * result + (ezServer != null ? ezServer.hashCode() : 0);
        return result;
    }
}