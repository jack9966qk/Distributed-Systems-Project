package com.allstars.project1;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Resource in EzShare, can point to a remote uri or a local file.
 */
public class Resource {
    /**
     *
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     *
     * @return
     */
    public String getDescription() {
        return description;
    }

    /**
     *
     * @return
     */
    public String[] getTags() {
        return tags;
    }

    /**
     *
     * @return
     */
    public String getUri() {
        return uri;
    }

    /**
     *
     * @return
     */
    public String getChannel() {
        return channel;
    }

    /**
     *
     * @return
     */
    public String getOwner() {
        return owner;
    }

    /**
     *
     * @return
     */
    public String getEzserver() {
        return ezserver;
    }

    /**
     *
     * @return
     */
    public Long getResourceSize() {
        return resourceSize;
    }

    private String name = "";
    private String description = "";
    private String[] tags = new String[0];
    private String uri = "";
    private String channel = "";
    private String owner = "";
    private String ezserver = null;
    private Long resourceSize = null;

    /**
     *
     * @param name
     * @param description
     * @param tags
     * @param uri
     * @param channel
     * @param owner
     * @param ezserver
     * @param resourceSize
     */
    public Resource(String name, String description, String[] tags, String uri, String channel, String owner, String ezserver, Long resourceSize) {
        this.name = name;
        this.description = description;
        this.tags = tags;
        this.uri = uri;
        this.channel = channel;
        this.owner = owner;
        this.ezserver = ezserver;
        this.resourceSize = resourceSize;
    }

    /**
     *
     * @param name
     * @param description
     * @param tags
     * @param uri
     * @param channel
     * @param owner
     * @param ezserver
     */
    public Resource(String name, String description, String[] tags, String uri, String channel, String owner, String ezserver) {
        this.name = name;
        this.description = description;
        this.tags = tags != null ? tags: new String[0];
        this.uri = uri;
        this.channel = channel;
        this.owner = owner;
        this.ezserver = ezserver;
    }

    /**
     *
     * @param ezServer
     * @return
     */
    public Resource ezServerAdded(EzServer ezServer) {
        return new Resource(
                this.name,
                this.description,
                this.tags,
                this.uri,
                this.channel,
                this.owner,
                ezServer.toString(),
                this.resourceSize
        );
    }

    /**
     *
     * @param size
     * @return
     */
    public Resource sizeAdded(long size) {
        return new Resource(
                this.name,
                this.description,
                this.tags,
                this.uri,
                this.channel,
                this.owner,
                this.ezserver,
                size
        );
    }

    /**
     *
     * @return
     */
    public Resource ownerHidden() {
        return new Resource(
                this.name,
                this.description,
                this.tags,
                this.uri,
                this.channel,
                "*",
                this.ezserver,
                this.resourceSize
        );
    }

    /**
     *
     * @param strings
     * @return
     */
    static List<String> stringsToLower(String[] strings) {
        return Arrays.stream(strings).map(String::toLowerCase).collect(Collectors.toList());
    }

    /**
     *
     * @param template
     * @return
     */
    public boolean matchesTemplate(Resource template) {
        if (!this.channel.equals(template.channel)) {
            return false;
        } else if (!template.getOwner().equals("") && !this.owner.equals(template.owner)) {
            return false;
        } else if (template.getTags() != null &&
                !stringsToLower(this.tags).containsAll(stringsToLower(template.tags))) {
            return false;
        } else if (template.getUri() != null && !template.getUri().equals("") && !this.uri.equals(template.getUri())) {
            return false;
        } else {
            if (this.name.contains(template.getName())) {
                // if candidate name contains the template name as a substring (for non "" template name), OR
                return true;
            } else if (this.description.contains(template.getDescription())) {
                // The candidate description contains the template description as a substring (for non "" template descriptions) OR
                return true;
            } else if (template.getDescription().equals("") && template.getName().equals("")) {
                // The template description and name are both ""
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     *
     * @return
     */
    public ResourceKey getKey() {
        return new ResourceKey(owner, channel, uri);
    }

    /**
     *
     * @return
     */
    public String toJson() {
        return Static.GSON.toJson(this);
    }

    /**
     *
     * @return
     */
    public JsonElement toJsonElement() {
        return Static.GSON.toJsonTree(this);
    }

    /**
     *
     * @param json
     * @return
     */
    public static Resource fromJson(String json) {
        return Static.GSON.fromJson(json, Resource.class);
    }

    /**
     *
     * @param elem
     * @return
     */
    public static Resource fromJsonElem(JsonElement elem) {
        return Static.GSON.fromJson(elem, Resource.class);
    }

    /**
     *
     * @param json
     * @return
     */
    public static Resource parseAndNormalise(String json) {
        Resource r = fromJson(json);
        return r.normalised();
    }

    /**
     *
     * @param elem
     * @return
     */
    public static Resource parseAndNormalise(JsonElement elem) {
        Resource r = fromJsonElem(elem);
        if (r == null) {
            return null;
        } else {
            Resource normalised = r.normalised();
            if (normalised.getUri() != null &&
                    normalised.getChannel() != null &&
                    normalised.getDescription() != null &&
                    normalised.getName() != null &&
                    normalised.getOwner() != null) {
                return normalised;
            } else {
                return null;
            }
        }
    }

    /**
     *
     * @param s
     * @return
     */
    static String normaliseStr(String s) {
        return s.replace("\0", "").trim();
    }

    /**
     * Fill missing fields with empty string and array,
     * remove "\0" and whitespaces at the start/end
     * @return normalised resource
     */
    public Resource normalised() {
        String name = (this.name != null) ? normaliseStr(this.name) : "";
        String description = (this.description != null) ? normaliseStr(this.description) : "";
        String[] tags;
        if (this.tags != null) {
            tags = Arrays.stream(this.tags).map(Resource::normaliseStr).toArray(String[]::new);
        } else {
            tags = new String[0];
        }
        String uri = (this.uri != null) ? normaliseStr(this.uri) : "";
        String channel = (this.channel != null) ? normaliseStr(this.channel) : "";
        String owner = (this.owner != null) ? normaliseStr(this.owner) : "";
        String ezServer = (this.ezserver != null) ? normaliseStr(this.ezserver) : "";
        return new Resource(name, description, tags, uri, channel, owner, ezServer);
    }

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
        return ezserver != null ? ezserver.equals(resource.ezserver) : resource.ezserver == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(tags);
        result = 31 * result + (uri != null ? uri.hashCode() : 0);
        result = 31 * result + (channel != null ? channel.hashCode() : 0);
        result = 31 * result + (owner != null ? owner.hashCode() : 0);
        result = 31 * result + (ezserver != null ? ezserver.hashCode() : 0);
        result = 31 * result + (resourceSize != null ? resourceSize.hashCode() : 0);
        return result;
    }
}