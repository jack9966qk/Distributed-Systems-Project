package EZShare;

import com.google.gson.JsonElement;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Resource in EZShare, can point to a remote uri or a local file.
 */
public class Resource {
    private String name = "";
    private String description = "";
    private String[] tags = new String[0];
    private String uri = "";
    private String channel = "";
    private String owner = "";
    private String ezserver = null;
    private Long resourceSize = null;

    /**
     * Constructor for Resource with all fields
     *
     * @param name         the name of the resource
     * @param description  the description of the resource
     * @param tags         the tags of the resource
     * @param uri          the uri of the resource
     * @param channel      the channel of the resource
     * @param owner        the owner of the resource
     * @param ezserver     the ezserver of the resource
     * @param resourceSize the resource size of the resource
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
     * Constructor for Resource without resource size
     *
     * @param name        the name of the resource
     * @param description the description of the resource
     * @param tags        the tags of the resource
     * @param uri         the uri of the resource
     * @param channel     the channel of the resource
     * @param owner       the owner of the resource
     * @param ezserver    the ezserver of the resource
     */
    public Resource(String name, String description, String[] tags, String uri, String channel, String owner, String ezserver) {
        this.name = name;
        this.description = description;
        this.tags = tags != null ? tags : new String[0];
        this.uri = uri;
        this.channel = channel;
        this.owner = owner;
        this.ezserver = ezserver;
    }

    /**
     * Change all char in the given String to lowercase letter
     *
     * @param strings the given String
     * @return new String with all lowercase letters
     */
    static List<String> stringsToLower(String[] strings) {
        return Arrays.stream(strings).map(String::toLowerCase).collect(Collectors.toList());
    }

    /**
     * Creating a Resource from the given JSON
     *
     * @param json the given JSON string
     * @return Resource converted from the given JSON
     */
    public static Resource fromJson(String json) {
        return Static.GSON.fromJson(json, Resource.class);
    }

    /**
     * Creating a Resource from the given JSON element
     *
     * @param elem the given JSON element
     * @return Resource converted from the given JSON element
     */
    public static Resource fromJsonElem(JsonElement elem) {
        return Static.GSON.fromJson(elem, Resource.class);
    }

    /**
     * Parse the resource in the given JSON and normalize it
     *
     * @param json the given JSON
     * @return the parsed and normalized resource
     */
    public static Resource parseAndNormalise(String json) {
        Resource r = fromJson(json);
        return r.normalised();
    }

    /**
     * Parse the resource in the given JSON element and normalize it
     *
     * @param elem the given JSON element
     * @return the parsed and normalized resource
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
     * Remove null characters, leading and trailing whitespaces of string
     *
     * @param s string
     * @return normalised string
     */
    static String normaliseStr(String s) {
        return s.replace("\0", "").trim();
    }

    /**
     * Get the name
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the description
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get the tags
     *
     * @return the tags
     */
    public String[] getTags() {
        return tags;
    }

    /**
     * Get the uri
     *
     * @return the uri
     */
    public String getUri() {
        return uri;
    }

    /**
     * Get the channel
     *
     * @return the channel
     */
    public String getChannel() {
        return channel;
    }

    /**
     * Get the owner
     *
     * @return the owner
     */
    public String getOwner() {
        return owner;
    }

    /**
     * Get the EzServer
     *
     * @return the EzServer
     */
    public String getEzserver() {
        return ezserver;
    }

    /**
     * Get the resource size
     *
     * @return the resource size
     */
    public Long getResourceSize() {
        return resourceSize;
    }

    /**
     * Creating a new Resource instance with ezServer added, other fields remain the same
     *
     * @param ezServer the ezServer specified
     * @return new Resource instance with ezServer added, other fields remain the same
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
     * Creating a new Resource instance with resource size added, other fields remain the same
     *
     * @param size the resource size specified
     * @return new Resource instance with resource size added, other fields remain the same
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
     * Creating a new Resource instance with owner field hidden, other fields remain the same
     *
     * @return new Resource instance with owner field hidden, other fields remain the same
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
     * Check if this resource matches the given resource tempalte
     *
     * @param template the given resource template
     * @return true if it matches the given template, otherwise return false
     */
    public boolean matchesTemplate(Resource template) {
        if (!this.channel.equals(template.channel)) {
            return false;
        } else if (!template.getOwner().equals("") && !this.owner.equals(template.owner)) {
            return false;
        } else if (template.getTags() != null &&
                !stringsToLower(this.tags).containsAll(stringsToLower(template.tags))) {
            return false;
        } else if (template.getUri() != null &&
                !template.getUri().equals("") &&
                !this.uri.equals(template.getUri())) {
            return false;
        } else {
            if (this.name.contains(template.getName())) {
                // if candidate name contains the template name asa substring (for non "" template name), OR
                return true;
            } else if (this.description.contains(template.getDescription())) {
                // The candidate description contains the template description as a substring
                // (for non "" template descriptions) OR
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
     * Creating a resource primary key with owner, channel and uri of this resource
     *
     * @return resource primary key
     */
    public ResourceKey getKey() {
        return new ResourceKey(owner, channel, uri);
    }

    /**
     * Creating a JSON object of this resource
     *
     * @return JSON object of this resource
     */
    public String toJson() {
        return Static.GSON.toJson(this);
    }

    /**
     * Creating a JSON element of this resource
     *
     * @return JSON element of this resource
     */
    public JsonElement toJsonElement() {
        return Static.GSON.toJsonTree(this);
    }

    /**
     * Fill missing fields with empty string and array,
     * remove "\0" and whitespaces at the start/end
     *
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