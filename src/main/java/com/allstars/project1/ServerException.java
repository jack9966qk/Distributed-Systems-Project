package com.allstars.project1;

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
        map.put("response", "error");
        map.put("errorMessage", errorMessage);
        return Static.GSON.toJson(map);
    }
}
