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

    public synchronized Set<String> getUriSet() {
        return new HashSet<String>(resources.stream().map(Resource::getUri).collect(Collectors.toSet()));
    }

    public synchronized boolean containsKey(Resource r) {
        return resources.containsKey(Integer.toString(r.hashCode()));
    }

    public synchronized void add(Resource resource) {
<<<<<<< Updated upstream
        resources.add(resource);
        Debug.println("Added new resource");
        Debug.println(this.toJson());
    }

    public synchronized void remove(Resource resource) {
        resources.remove(resource);
        Debug.println("Removed resource");
        Debug.println(this.toJson());
=======
        resources.put(Integer.toString(resource.hashCode()),resource);
        System.out.println("Added new resource");
 //       System.out.println(this.toJson());
    }

    public synchronized void remove(Resource resource) {
        resources.remove(Integer.toString(resource.hashCode()));
        System.out.println("Removed resource");
//        System.out.println(this.toJson());
>>>>>>> Stashed changes
    }
/*
    // for debug usage
    public synchronized String toJson() {
        return new Gson().toJson(resources.toArray(new Resource[this.resources.size()]));
    }*/
}
