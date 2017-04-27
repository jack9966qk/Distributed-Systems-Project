package com.allstars.project1;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Thread safe, storage class for resources
 * Created by Jack on 23/3/2017.
 */
public class ResourceStorage {

    Map<ResourceKey,Resource> resources = Collections.synchronizedMap(new HashMap<>());

    /**
     *
     * @param template
     * @return
     */
    public synchronized HashSet<Resource> searchWithTemplate(Resource template) {
        return new HashSet<>(resources.values().stream().filter(
                (Resource r) -> r.matchesTemplate(template)
        ).collect(Collectors.toSet()));
    }

    /**
     *
     * @return
     */
    public synchronized Set<String> getUriSet() {
        return new HashSet<>(resources.values().stream().map(Resource::getUri).collect(Collectors.toSet()));
    }

    /**
     *
     * @param resource
     */
    public synchronized void add(Resource resource) {
        resources.put(resource.getKey(), resource);
        Logging.logFine("Added new resource");
        Logging.logFine(this);
    }

    /**
     *
     * @param resource
     */
    public synchronized void remove(Resource resource) {
        resources.remove(resource.getKey());
        Logging.logFine("Removed resource");
        Logging.logFine(this);
    }

    /**
     * determine if a resource exists with channel and uri
     * @param channel channel of resource
     * @param uri uri of resource
     * @return true if resource exist, false otherwise
     */
    public boolean hasResourceWith(String channel, String uri) {
        for (Resource r : resources.values()) {
            if (r.getChannel().equals(channel) && r.getUri().equals(uri)) {
                return true;
            }
        }
        return false;
    }

    /**
     *
     * @param r
     * @return
     */
    public synchronized boolean containsKey(Resource r){
        return resources.containsKey(r.getKey());
    }

    // for debug usage
    public synchronized String toString() {
        return Static.GSON.toJson(resources.values().toArray(new Resource[this.resources.size()]));
    }
}
