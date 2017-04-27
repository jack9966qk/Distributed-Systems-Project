package com.allstars.project1;

import java.util.HashMap;
import java.util.Map;

/**
 * Server side exceptions to be sent back as JSON
 * Created by Jack on 24/3/2017.
 */
public class ServerException extends Exception {
    private String errorMessage;

    /**
     *
     * @param errorMessage
     */
    public ServerException(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public String getMessage() {
        return errorMessage;
    }
    
    /**
     *
     * @return
     */
    public String toJson() {
        Map<String, String> map = new HashMap<>();
        map.put("response", "error");
        map.put("errorMessage", errorMessage);
        return Static.GSON.toJson(map);
    }
}
