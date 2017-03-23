public class Resource {
    public String name = "";
    public String description = "";
    public String[] tags = [];
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
}