package com.allstars.project1;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.org.apache.xerces.internal.util.URI;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.*;

/**
 * Created by Jack on 24/3/2017.
 */
public class ServiceThread extends Thread {
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private String secret;
    private ResourceStorage resourceStorage;
    private Set<EzServer> serverList;

    public ServiceThread(Socket clientSocket, String secret, ResourceStorage resourceStorage, Set<EzServer> serverList)
        throws IOException {
        this.inputStream = new DataInputStream(clientSocket.getInputStream());
        this.outputStream = new DataOutputStream(clientSocket.getOutputStream());
        this.secret = secret;
        this.resourceStorage = resourceStorage;
        this.serverList = serverList;
    }



    private void checkCommand(Resource resource) throws ServerException {
        // created a resource only with the primary keys
        Resource r = new Resource(null,null,null,resource.getUri(),
                resource.getChannel(),resource.getOwner(),null);

        if (resource.getOwner().length()==1&&resource.getOwner().toCharArray()[0]=='*') {// * owner
            throw new ServerException("invalid resource");
        }else if (resource.getUri().isEmpty()) {//uri is empty
            throw new ServerException("missing resource");
        }else if (!resource.getUri().matches("(.*):(.*)")) { //not an absolute uri
            throw new ServerException("missing resource");
        }else if (resourceStorage.getUriSet().contains(resource.getUri())) { // duplicate uri
            throw new ServerException("invalid resource");
        }
    }


    private void respondSuccess() throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("response", "success");
        outputStream.writeUTF(json.toString());
    }

    private void respondResource(Resource resource) throws IOException {
        outputStream.writeUTF(resource.toJson());
    }

    private void respondResultSize(int size) throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("resultSize", 2);
        outputStream.writeUTF(json.toString());
    }

    private void publish(Resource resource) throws ServerException, IOException {
        if (resourceStorage.containsKey(resource)){
            resourceStorage.remove(resource);
            resourceStorage.add(resource);
        }else{
            checkCommand(resource);
            resourceStorage.add(resource);
        }
        respondSuccess();
    }

    private void remove(Resource resource) throws ServerException, IOException {
        checkCommand(resource);
        resourceStorage.remove(resource);
        respondSuccess();
    }

    private void share(String secret, Resource resource) throws ServerException, IOException {
        if (!secret.equals(this.secret)) {
            throw new ServerException("incorrect secret");
        } else if (!URI.isWellFormedAddress(resource.getUri())) {
            throw new ServerException("invalid resource");
        } else {
            resourceStorage.add(resource);
        }
        respondSuccess();
    }

    private void query(Resource template, boolean relay) throws ServerException, IOException {
        // TODO relay
        Set<Resource> results = resourceStorage.searchWithTemplate(template);

        for (Resource r : results) {
            Debug.println(r.toJson());
        }

        respondSuccess();
        for (Resource resource : results) {
            respondResource(resource);
        }
        respondResultSize(results.size());
    }

    private void fetch(Resource template) throws ServerException {
        // TODO
        // http://stackoverflow.com/questions/9520911/java-sending-and-receiving-file-byte-over-sockets
    }

    private void exchange(EzServer[] servers) throws ServerException, IOException {
        this.serverList.addAll(Arrays.asList(servers));
        respondSuccess();
    }


    @Override
    public void run() {
        try {
            // read json from socket
            String reqJson = inputStream.readUTF();
            JsonParser parser = new JsonParser();
            JsonObject obj = parser.parse(reqJson).getAsJsonObject();

            // determine command type
            if (!obj.has("command")) {
                throw new ServerException("invalid command");
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
                System.out.println(reqJson);
                boolean relay = obj.get("relay").getAsBoolean();
                JsonElement resourceTemplate = obj.get("resourceTemplate");
                Resource r = new Gson().fromJson(resourceTemplate, Resource.class);
                query(r, relay);
            } else if (command.equals("FETCH")) {
                JsonElement resourceTemplate = obj.get("resourceTemplate");
                Resource r = new Gson().fromJson(resourceTemplate, Resource.class);
                fetch(r);
            } else if (command.equals("EXCHANGE")) {
                exchange(new Gson().fromJson(obj.get("serverList"), EzServer[].class));
            } else {
                throw new ServerException("invalid command");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ServerException e) {
            try {
                outputStream.writeUTF(e.toJson());
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
        }
    }
}
