package com.allstars.project1;

/**
 * Created by Jack on 28/3/2017.
 */
public class ResourceKey {
    private String owner;
    private String channel;
    private String uri;

    public ResourceKey(String owner, String channel, String uri) {
        this.owner = owner;
        this.channel = channel;
        this.uri = uri;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ResourceKey that = (ResourceKey) o;

        if (!owner.equals(that.owner)) return false;
        if (!channel.equals(that.channel)) return false;
        return uri.equals(that.uri);
    }

    @Override
    public int hashCode() {
        int result = owner.hashCode();
        result = 31 * result + channel.hashCode();
        result = 31 * result + uri.hashCode();
        return result;
    }
}
