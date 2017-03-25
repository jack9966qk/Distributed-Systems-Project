package com.allstars.project1;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.org.apache.xerces.internal.util.URI;

import java.io.*;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.URL;
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

    private void respondSuccess() throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("response", "success");
        outputStream.writeUTF(json.toString());
    }

    private void respondResource(Resource resource) throws IOException {
        outputStream.writeUTF(resource.toJson());
    }

    private void respondResource(Resource resource, long size) throws IOException {
        outputStream.writeUTF(resource.toJsonWithSize(size));
    }

    private void respondResultSize(int size) throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("resultSize", size);
        outputStream.writeUTF(json.toString());
    }

    private void publish(Resource resource) throws ServerException, IOException {
        resourceStorage.add(resource);
        respondSuccess();
    }

    private void remove(Resource resource) throws ServerException, IOException {
        resourceStorage.remove(resource);
        respondSuccess();
    }

    private void share(String secret, Resource resource) throws ServerException, IOException {
        if (!secret.equals(this.secret)) {
            throw new ServerException("incorrect secret");
        } else {
            File f = null;
            try {
                f = new File(new URL(resource.getUri()).toURI());
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            if (!f.exists()) {
                throw new ServerException("invalid resource");
            } else {
                System.out.println("add to resource");
                resourceStorage.add(resource);
                respondSuccess();
            }
        }
    }

    private void query(Resource template, boolean relay) throws ServerException, IOException {
        Set<Resource> results = resourceStorage.searchWithTemplate(template);

        if (relay) {
            for (EzServer server : serverList) {
                Socket socket = Client.connectToServer(server.hostname, server.port);
                results.addAll(Client.query(socket, false, template));
            }
        }

        for (Resource r : results) {
            Debug.println(r.toJson());
        }

        respondSuccess();
        for (Resource resource : results) {
            respondResource(resource);
        }
        respondResultSize(results.size());
    }

    private void fetch(Resource template) throws ServerException, IOException {
        // TODO check valid...

        Resource resource = new ArrayList<>(Server.resourceStorage.searchWithTemplate(template)).get(0);

        try {
            File file = new File(resource.getUri());
            FileInputStream fileInputStream = new FileInputStream(file);
            System.out.println("send success");
            respondSuccess();
            System.out.println("send resource");
            respondResource(resource, file.length());
            System.out.println("send file");
            byte[] bytes = new byte[16 * 1024];
            int count;
            while ((count = fileInputStream.read(bytes)) > 0) {
                outputStream.write(bytes, 0, count);
            }
            respondResultSize(1);

        } catch (FileNotFoundException e) {
            // TODO throw exception
        }
    }

    private void exchange(EzServer[] servers) throws ServerException, IOException {
        this.serverList.addAll(Arrays.asList(servers));
        System.out.println(this.serverList);
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
                Resource resource = Resource.fromJsonElem(obj.get("resource"));
                publish(resource);
            } else if (command.equals("REMOVE")) {
                Resource resource = Resource.fromJsonElem(obj.get("resource"));
                remove(resource);
            } else if (command.equals("SHARE")) {
                String secret = obj.get("secret").getAsString();
                System.out.println(obj);
                Resource resource = Resource.fromJsonElem(obj.get("resource"));
                share(secret, resource);
            } else if (command.equals("QUERY")) {
                System.out.println(reqJson);
                boolean relay = obj.get("relay").getAsBoolean();
                Resource resourceTemplate = Resource.fromJsonElem(obj.get("resourceTemplate"));
                query(resourceTemplate, relay);
            } else if (command.equals("FETCH")) {
                System.out.println(obj.get("resourceTemplate"));
                Resource resourceTemplate = Resource.fromJsonElem(obj.get("resourceTemplate"));
                fetch(resourceTemplate);
            } else if (command.equals("EXCHANGE")) {
                System.out.println(obj);
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
        }
    }
}
