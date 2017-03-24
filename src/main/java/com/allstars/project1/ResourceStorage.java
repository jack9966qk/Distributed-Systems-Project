package com.allstars.project1;


import java.util.ArrayList;

/**
 * Created by Jack on 23/3/2017.
 */
public class ResourceStorage {
    ArrayList<Resource> resources = new ArrayList<Resource>();

    public ArrayList<Resource> searchWithTemplate(Resource template) {
        // TODO
        return null;
    }

    public void add(Resource resource) {
        resources.add(resource);
    }

    public void remove(Resource resource) {
        resources.remove(resource);
    }
}
