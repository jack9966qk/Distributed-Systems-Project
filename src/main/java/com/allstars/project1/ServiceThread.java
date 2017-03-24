package com.allstars.project1;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jack on 24/3/2017.
 */
public class ServiceThread extends Thread {
    Socket clientSocket;
    List<EzServer> serverList;

    public ServiceThread(Socket clientSocket, ResourceStorage resourceStorage, List<EzServer> serverList) {
        this.clientSocket = clientSocket;
        this.serverList = serverList;
    }

    private static void publish(Resource r) {

    }

    private static void remove(Resource r) {

    }

    private static void share(String secret, Resource r) {

    }

    private static ArrayList<Resource> query(Resource template, boolean relay) {
        return new ArrayList<>();
    }

    private static void fetch(Resource template) {

    }

    private static void exchange() {

    }

    private static void sendErrorResponse(String description) {

    }



    @Override
    public void run() {
        try {
            DataInputStream inputStream =
                    new DataInputStream(clientSocket.getInputStream());

            // TODO read json from socket
            String reqJson = inputStream.readUTF();
            JsonParser parser = new JsonParser();
            JsonObject obj = parser.parse(reqJson).getAsJsonObject();


            // TODO determine command type
            if (!obj.has("command")) {
                sendErrorResponse("invalid command");
                return;
            }

            String command = obj.get("command").getAsString();
            if (command.equals("PUBLISH")) {
                publish(Resource.fromJson(obj.get("resource").getAsString()));
            } else if (command.equals("REMOVE")) {
                remove(Resource.fromJson(obj.get("resource").getAsString()));
            } else if (command.equals("SHARE")) {
                String secret = obj.get("secret").getAsString();
                Resource resource =
                        Resource.fromJson(obj.get("resource").getAsString());
                share(secret, resource);
            } else if (command.equals("QUERY")) {
                boolean relay = obj.get("relay").getAsBoolean();
                query(Resource.fromJson(obj.get("resourceTemplate").getAsString()), relay);
            } else if (command.equals("FETCH")) {
                fetch(Resource.fromJson(obj.get("resourceTemplate").getAsString()));
            } else if (command.equals("EXCHANGE")) {

            } else {
                sendErrorResponse("invalid command");
                return;
            }

            // TODO call methods respectively

            // TODO send response to client


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
