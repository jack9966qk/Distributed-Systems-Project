package EZShare;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Thread safe, storage class for resources
 * Created by Jack on 23/3/2017.
 */
public class ResourceStorage {
    Map<ResourceKey, Resource> resources = Collections.synchronizedMap(new HashMap<>());
    private SubscriptionManager subscriptionManager = null;

    public ResourceStorage(SubscriptionManager subscriptionManager) {
        this.subscriptionManager = subscriptionManager;
    }

    /**
     * Search for resources using a resource template
     *
     * @param template the resource template
     * @return search results as a set of resources
     */
    public synchronized HashSet<Resource> searchWithTemplate(Resource template) {
        return new HashSet<>(resources.values().stream().filter(
                (Resource r) -> r.matchesTemplate(template)
        ).collect(Collectors.toSet()));
    }

    /**
     * Search for the first resource in the storage with channel name and uri
     *
     * @param channel channel of resource
     * @param uri     uri of resource
     * @return the first resource that matches channel and uri exactly
     */
    public synchronized Resource findWith(String channel, String uri) {
        for (Resource r : resources.values()) {
            if (r.getUri().equals(uri) && r.getChannel().equals(channel)) {
                return r;
            }
        }
        return null;
    }

    /**
     * Add a resource to storage
     *
     * @param resource resource to add
     */
    public synchronized void add(Resource resource) {
        // check if new resource is different to existing
        if (!resource.equals(resources.get(resource.getKey()))) {
            if (subscriptionManager != null) {
                for (SubscriptionHandler listener : subscriptionManager.getSubscriptionThreads()) {
                    listener.onResourceArrived(resource);
                }
            }
        }

        resources.put(resource.getKey(), resource);

        Logging.logFine("Added new resource");
        Logging.logFine(this);
    }

    /**
     * Remove a resource from storage
     *
     * @param resource
     */
    public synchronized void remove(Resource resource) {
        resources.remove(resource.getKey());
        Logging.logFine("Removed resource");
        Logging.logFine(this);
    }

    /**
     * Determine if a resource exists with channel and uri
     *
     * @param channel channel of resource
     * @param uri     uri of resource
     * @return true if resource exist, false otherwise
     */
    public synchronized boolean hasResourceWith(String channel, String uri) {
        for (Resource r : resources.values()) {
            if (r.getChannel().equals(channel) && r.getUri().equals(uri)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determine if storage has a resource with primary key
     *
     * @param r resource key
     * @return true if such resource exists, false otherwise
     */
    public synchronized boolean containsKey(Resource r) {
        return resources.containsKey(r.getKey());
    }

    // for debug usage
    public synchronized String toString() {
        return Static.GSON.toJson(resources.values().toArray(new Resource[this.resources.size()]));
    }
}
