package com.allstars.project1;
import com.google.gson.Gson;
import com.sun.org.apache.regexp.internal.RE;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Jack on 23/3/2017.
 */
public class ResourceStorage {

    Map<String,Resource> resources = Collections.synchronizedMap(new HashMap<>());
/*
    public synchronized String searchWithTemplate(Resource template) {
        return new HashSet<Resource>(resources.keySet().stream().filter(
                (Resource r) -> r.matchesTemplate(template)
        ).collect(Collectors.toSet()));

    }*/

    public synchronized Set<String> getUriSet() {
        return new HashSet<String>(resources.values().stream().map(Resource::getUri).collect(Collectors.toSet()));
    }

    public synchronized void add(Resource resource) {
        resources.put(Integer.toString(resource.hashCode()),resource);
        System.out.println("Added new resource");
        System.out.println(this.toJson());
    }

    public synchronized void remove(Resource resource) {
        resources.remove(Integer.toString(resource.hashCode()));
        System.out.println("Removed resource");
        System.out.println(this.toJson());
    }

    // for debug usage
    public synchronized String toJson() {
        // please fix this
        return new Gson().toJson(resources.toArray(new Resource[this.resources.size()]));
    }
}
