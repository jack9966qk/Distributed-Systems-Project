package com.allstars.project1;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Jack on 24/3/2017.
 */
public class ServerException extends Exception {
    private String errorMessage;

    public ServerException(String errorMessage) {
        this.errorMessage = errorMessage;
    }


    public String toJson() {
        Map<String, String> map = new HashMap<>();
        map.put("response", "ERROR");
        map.put("errorMessage", errorMessage);
        return new Gson().toJson(map);
    }
}
