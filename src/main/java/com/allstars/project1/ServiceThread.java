package com.allstars.project1;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.jndi.toolkit.url.Uri;
import com.google.gson.*;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Jack on 24/3/2017.
 */
public class ServiceThread extends Thread {
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private String secret;
    private ResourceStorage resourceStorage;
    private Set<EzServer> serverList;
    Socket socket;
    Map<SocketAddress, Date> lastConnectionTime;

    public ServiceThread(Map<SocketAddress, Date> lastConnectionTime, Socket clientSocket, String secret, ResourceStorage resourceStorage, Set<EzServer> serverList)
        throws IOException {
        this.socket = clientSocket;
        this.lastConnectionTime = lastConnectionTime;
        this.inputStream = new DataInputStream(clientSocket.getInputStream());
        this.outputStream = new DataOutputStream(clientSocket.getOutputStream());
        this.secret = secret;
        this.resourceStorage = resourceStorage;
        this.serverList = serverList;
    }

    private boolean isFile(String uri){
        URI u = URI.create(uri);
        String scheme = u.getScheme();
        try {
            return scheme.equalsIgnoreCase("file");
        } catch (NullPointerException e) {
            return false; // No scheme was found
        }

    }

    private void checkCommand(Resource resource) throws ServerException {
        // created a resource only with the primary keys
        Resource r = new Resource(null,null,null,resource.getUri(),
                resource.getChannel(),resource.getOwner(),null);

        if (resource.getOwner().length()==1&&resource.getOwner().toCharArray()[0]=='*') {// * owner
            throw new ServerException("invalid resource");
        }else if (resource.getUri().isEmpty()) {//uri is empty
            throw new ServerException("missing resource");
        }
        else if (!URI.create(resource.getUri()).isAbsolute()) { //not an absolute uri
            throw new ServerException("missing resource");
        }else if (resourceStorage.getUriSet().contains(resource.getUri())) { // duplicate uri
            throw new ServerException("invalid resource");
        }
    }
       private void checkTemplate(Resource template) throws ServerException{
            Resource r = new Resource(null,null,null,template.getUri(),
                template.getChannel(),null,null);
    	 if (r.getUri().isEmpty()) {
            throw new ServerException("missing resourceTemplate");
        }else if (!Paths.get(template.getUri()).toUri().isAbsolute()) { 
             throw new ServerException("missing resourceTemplate");
         }else if(!resourceStorage.containsKey(r)){
          throw new ServerException("missing resourceTemplate");}
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
        outputStream.writeUTF(resource.sizeAdded(size).toJson());
    }

    private void respondResultSize(int size) throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("resultSize", size);
        outputStream.writeUTF(json.toString());
    }

    private void publish(Resource resource) throws ServerException, IOException {
        // check whether the uri is a file scheme
        if (isFile(resource.getUri())){
            throw new ServerException("cannot publish resource");
        }
        if (resourceStorage.containsKey(resource)){// check whether it is an update
            resourceStorage.remove(resource);
            resourceStorage.add(resource);
        }else {
            checkCommand(resource); // check whether it is valid
            resourceStorage.add(resource);
        }
        respondSuccess();
    }

    private void remove(Resource resource) throws ServerException, IOException {
        if (resourceStorage.containsKey(resource)){
            resourceStorage.remove(resource);
            respondSuccess();
        }else{
            checkCommand(resource); // check whether it is valid
            throw new ServerException("cannot remove resource"); // the resource did not exist
        }
    }

    private void share(String secret, Resource resource) throws ServerException, IOException {
        if (!secret.equals(this.secret)) {
            throw new ServerException("incorrect secret");
        } else {
            File f;
            f = new File(resource.getUri());
            if (f.isFile()){
                if (!f.exists()) {
                    throw new ServerException("invalid resource");
                }
                checkCommand(resource);
                Debug.println("add to resource");
                resourceStorage.add(resource);
                respondSuccess();
            }
            else {
                throw new ServerException("cannot share resource");
            }
        }
    }

    private void query(Resource template, boolean relay) throws ServerException, IOException {
        Set<Resource> results = resourceStorage.searchWithTemplate(template).stream().map(
                r -> r.ezServerAdded(Server.self) // add EzServer info for all result from itself
        ).collect(Collectors.toSet());

        if (relay) {
            for (EzServer server : serverList) {
                Socket socket = Client.connectToServer(server.hostname, server.port, Constants.DEFAULT_TIMEOUT);
                results.addAll(Client.query(socket, false, template));
            }
        }

        for (Resource r : results) {
            Debug.println(r.toJson());
        }

        respondSuccess();
        for (Resource resource : results) {
            respondResource(resource.ownerHidden()); // hide owner information
        }
        respondResultSize(results.size());
    }

    private void fetch(Resource template) throws ServerException, IOException {
        checkTemplate(template);// TODO check valid...

        Resource resource = new ArrayList<>(Server.resourceStorage.searchWithTemplate(template)).get(0);

        try {
            java.net.URI uri = new java.net.URI(template.getUri());
            String path = uri.getPath();
            File file = new File(path);
            FileInputStream fileInputStream = new FileInputStream(file);
            Debug.println("send success");
            respondSuccess();
            Debug.println("send resource");
            respondResource(resource, file.length());
            Debug.println("send file");
            byte[] bytes = new byte[16 * 1024];
            int count;
            while ((count = fileInputStream.read(bytes)) > 0) {
                outputStream.write(bytes, 0, count);
            }
            respondResultSize(1);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            // TODO throw exception
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private void exchange(EzServer[] servers) throws ServerException, IOException {
        Debug.infoPrintln("handle exchange request");
        Debug.infoPrintln("request server list: " + Arrays.toString(servers));
                if(!((servers) instanceof EzServer[])){
        	throw new ServerException("missing server list");
        }else{
        for (EzServer server : servers) {
            System.out.println(server);
            if (Server.self != server) {
                this.serverList.add(server);
            }
        }
        Debug.infoPrintln("updated server list: " + this.serverList);
        respondSuccess();
    }
}

    private EzServer[] parseExchange(JsonObject obj) throws ServerException {
        // check if contains serverList
        if (!obj.has("serverList")) {
            throw new ServerException("missing server list");
        }

        // parse servers, check if all servers are valid
        List<EzServer> servers = new ArrayList<>();
        JsonArray elems = obj.getAsJsonArray("serverList");
        for (JsonElement elem : elems) {
            EzServer server = EzServer.fromJson(elem.getAsJsonObject());
            if (server == null) {
                throw new ServerException("invalid server record");
            } else {
                servers.add(server);
            }
        }
        return servers.toArray(new EzServer[servers.size()]);
    }

    @Override
    public void run() {
        try {
            // wait for rest of connection interval
            long waitTime = 0;
            SocketAddress clientAddress = socket.getRemoteSocketAddress();
            if (lastConnectionTime.containsKey(clientAddress)) {
                Date lastTime = lastConnectionTime.get(clientAddress);
                Date now = new Date();
                waitTime = now.getTime() - lastTime.getTime();
                if (waitTime < 0) {
                    waitTime = 0;
                }
            }
            sleep(waitTime);

            // record this connection time
            lastConnectionTime.put(clientAddress, new Date());

            // read json from socket
            String reqJson = inputStream.readUTF();
            Debug.println("RECEIVED: " + reqJson);
            JsonParser parser = new JsonParser();
            JsonObject obj = parser.parse(reqJson).getAsJsonObject();

            // determine command type
            if (!obj.has("command")) {
                throw new ServerException("invalid command");
            }

            String command = obj.get("command").getAsString();
            if (command.equals("PUBLISH")) {
                Resource resource = Resource.parseAndNormalise(obj.get("resource"));
                publish(resource);
            } else if (command.equals("REMOVE")) {
                Resource resource = Resource.parseAndNormalise(obj.get("resource"));
                remove(resource);
            } else if (command.equals("SHARE")) {
                String secret = obj.get("secret").getAsString();
                Debug.println(obj);
                Resource resource = Resource.parseAndNormalise(obj.get("resource"));
                share(secret, resource);
            } else if (command.equals("QUERY")) {
                Debug.println(reqJson);
                boolean relay = obj.get("relay").getAsBoolean();
                Resource resourceTemplate = Resource.fromJsonElem(obj.get("resourceTemplate"));
                query(resourceTemplate, relay);
            } else if (command.equals("FETCH")) {
                Debug.println(obj.get("resourceTemplate"));
                Resource resourceTemplate = Resource.fromJsonElem(obj.get("resourceTemplate"));
                fetch(resourceTemplate);
            } else if (command.equals("EXCHANGE")) {
                Debug.println(obj);
                exchange(parseExchange(obj));
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
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
