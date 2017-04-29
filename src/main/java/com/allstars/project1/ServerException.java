package com.allstars.project1;

import java.util.HashMap;
import java.util.Map;

/**
 * Server side exceptions to be sent back as JSON
 * Created by Jack on 24/3/2017.
 */
public class ServerException extends Exception {
    /**
     * Get the error message of exception
     *
     * @return error message of exception
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    private String errorMessage;

    /**
     * Create a new server exception
     *
     * @param errorMessage error message of the exception
     */
    public ServerException(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Get a JSON representation of the exception
     *
     * @return JSON representation of the exception
     */
    public String toJson() {
        Map<String, String> map = new HashMap<>();
        map.put("response", "error");
        map.put("errorMessage", errorMessage);
        return Static.GSON.toJson(map);
    }
}
