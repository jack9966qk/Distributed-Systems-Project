package com.allstars.project1;
import com.google.gson.Gson;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Jack on 23/3/2017.
 */
public class ResourceStorage {
    Set<Resource> resources = Collections.synchronizedSet(new HashSet<>());

    public synchronized Set<Resource> searchWithTemplate(Resource template) {
        return new HashSet<Resource>(resources.stream().filter(
                (Resource r) -> r.matchesTemplate(template)
        ).collect(Collectors.toSet()));
    }

    public synchronized void add(Resource resource) {
        resources.add(resource);
        Debug.println("Added new resource");
        Debug.println(this.toJson());
    }

    public synchronized void remove(Resource resource) {
        resources.remove(resource);
        Debug.println("Removed resource");
        Debug.println(this.toJson());
    }

    // for debug usage
    public synchronized String toJson() {
        return new Gson().toJson(resources.toArray(new Resource[this.resources.size()]));
    }
}
