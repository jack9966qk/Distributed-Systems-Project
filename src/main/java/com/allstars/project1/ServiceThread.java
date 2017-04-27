package com.allstars.project1;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.*;

import java.io.*;
import java.net.*;
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
    private Socket socket;
    private Map<SocketAddress, Date> lastConnectionTime;
    private EzServer server;

    public ServiceThread(Map<SocketAddress, Date> lastConnectionTime, Socket clientSocket, String secret, ResourceStorage resourceStorage, Set<EzServer> serverList, EzServer server)
            throws IOException {
        this.socket = clientSocket;
        this.lastConnectionTime = lastConnectionTime;
        this.inputStream = new DataInputStream(clientSocket.getInputStream());
        this.outputStream = new DataOutputStream(clientSocket.getOutputStream());
        this.secret = secret;
        this.resourceStorage = resourceStorage;
        this.serverList = serverList;
        this.server = server;
    }



    private Resource parseResource(JsonObject obj) throws ServerException {
         if(!obj.has("resource")){
        	 throw new ServerException("missing resource");
         }else if(!(obj.get("resource").isJsonObject())){
        	 throw new ServerException("missing resource");
         }
        return Resource.fromJsonElem(obj.get("resource"));
        
    }
    private Resource parseTemplate(JsonObject obj) throws ServerException {
        if(!obj.has("resourceTemplate")){
       	 throw new ServerException("missing resourceTemplate");
        } else if(!(obj.get("resourceTemplate").isJsonObject())){
       	 throw new ServerException("missing resourceTemplate");
        }  
        
        return Resource.fromJsonElem(obj.get("resourceTemplate"));
      
   }
    private String parsesecrect(JsonObject obj) throws ServerException {
   try{ 
	   if(!obj.has("secerct")){
       	 throw new ServerException("missing resource or secret");
         } else{ return obj.get("secret").getAsString();
         }
       }catch(Exception e){
    	   throw new ServerException("missing resouorce or secert");
       }  
   }
    private Boolean parserelay(JsonObject obj) throws ServerException {
    	   try{ 
    		   if(!obj.has("relay")){
    	       	 throw new ServerException("missing resourceTemplate");
    	         } else{ return obj.get("relay").getAsBoolean();
    	         }
    	       }catch(Exception e){
    	    	   throw new ServerException("missing resourceTemplate");
    	       } 
    	   }
    
    private boolean checkCommand(Resource resource) throws ServerException {
        // created a resource only with the primary keys
        Resource r = new Resource(null, null, null, resource.getUri(),
                resource.getChannel(), resource.getOwner(), null);

        if (resource.getOwner().length() == 1 && resource.getOwner().toCharArray()[0] == '*') {// * owner
            return false;
        } else if (resource.getUri().isEmpty()||resource.getUri()==null) {//uri is empty or not given
            return false;
        } else if (!URI.create(resource.getUri()).isAbsolute()) { //not an absolute uri
            return false;
        } else if (resourceStorage.hasResourceWith(resource.getChannel(),resource.getUri())) { // duplicate uri in a given channel
            return false;
        }
        return true;
    }
    
    
    private boolean isFile(String uri) {
        URI u = URI.create(uri);
        String scheme = u.getScheme();
        try {
            return scheme.equalsIgnoreCase("file");
        } catch (NullPointerException e) {
            return false; // No scheme was found
        }
    }
    
    private void respondSuccess() throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("response", "success");
        Static.sendJsonUTF(outputStream, json.toString());
    }
    private void respondResource(Resource resource) throws IOException {
        Static.sendJsonUTF(outputStream, resource.toJson());
    }

    private void respondResource(Resource resource, long size) throws IOException {
        Static.sendJsonUTF(outputStream, resource.sizeAdded(size).toJson());
    }

    private void respondResultSize(int size) throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("resultSize", size);
        Static.sendJsonUTF(outputStream, json.toString());
    }

    private void publish(Resource resource) throws ServerException, IOException {
        // checkResource(resource);
        // check whether the uri is a file scheme

        if (isFile(resource.getUri())) {
            throw new ServerException("cannot publish resource");
        }
        if (resourceStorage.containsKey(resource)) {// check whether it is an update
            resourceStorage.remove(resource);
            resourceStorage.add(resource);
        } else {
            if (checkCommand(resource)){
                resourceStorage.add(resource);
            } else {
                throw new ServerException("cannot publish resource");
            }
        }
        respondSuccess();
    }

    private void remove(Resource resource) throws ServerException, IOException {
        // checkResource(resource);
        if (resourceStorage.containsKey(resource)) {
            resourceStorage.remove(resource);
            respondSuccess();
        } else {
            throw new ServerException("cannot remove resource"); // the resource did not exist
        }
    }

    private void share(String secret, Resource resource) throws ServerException, IOException {

        if (secret == null||resource==null) {
            throw new ServerException("missing resource and/or secret");
        }
        if (!secret.equals(this.secret)) {
            throw new ServerException("incorrect secret");
        } else {
            File f;
            f = new File(resource.getUri());
            if (f.isFile()) {
                if (!f.exists()) {
                    throw new ServerException("cannot share resource");
                }
                if (checkCommand(resource)) {
                    throw new ServerException("cannot share resource");
                }
                Logging.logFine("add to resource");
                resourceStorage.add(resource);
                respondSuccess();
            } else {
                throw new ServerException("cannot share resource");
            }
        }
    }
    
  
    
    private void query(Resource template, boolean relay) throws ServerException, IOException {
        Set<Resource> results = resourceStorage.searchWithTemplate(template).stream().map(
                r -> r.ezServerAdded(server) // add EzServer info for all result from itself
        ).collect(Collectors.toSet());

        if (relay) {
            for (EzServer server : serverList) {
                Socket socket = Client.connectToServer(server.hostname, server.port, Static.DEFAULT_TIMEOUT);
                results.addAll(Client.query(socket, false, template));
            }
        }

        for (Resource r : results) {
            Logging.logFine(r.toJson());
        }

        respondSuccess();
        for (Resource resource : results) {
            respondResource(resource.ownerHidden()); // hide owner information
        }
        respondResultSize(results.size());
    }

    private void fetch(Resource template) throws ServerException, IOException {
        if(!isFile(template.getUri())){
        	 throw new ServerException("cannot fetch resource");
        }
        List<Resource> results = new ArrayList<>(Server.resourceStorage.searchWithTemplate(template));
        if (results.size() == 0) {
            // 0 result
            respondSuccess();
            respondResultSize(0);
            return;
        }
        Resource resource = results.get(0);

        try {
            java.net.URI uri = new java.net.URI(template.getUri());
            String path = uri.getPath();
            File file = new File(path);
            FileInputStream fileInputStream = new FileInputStream(file);
            Logging.logFine("send success");
            respondSuccess();
            Logging.logFine("send resource");
            respondResource(resource, file.length());
            Logging.logFine("send file");
            byte[] bytes = new byte[16 * 1024];
            int count;
            while ((count = fileInputStream.read(bytes)) > 0) {
                outputStream.write(bytes, 0, count);
            }
            respondResultSize(1);

        } catch (FileNotFoundException|URISyntaxException e) {
            // behaviour undefined in spec, chose to report an error here
            throw new ServerException("cannot fetch resource");
        }
    }

    private void exchange(EzServer[] servers) throws ServerException, IOException {
        Logging.logInfo("handle exchange request");
        Logging.logInfo("request server list: " + Arrays.toString(servers));
        if (servers == null) {
            throw new ServerException("missing server list");
        } else {
            for (EzServer server : servers) {
                Logging.logFine(server);
                if (Server.self != server) {
                    this.serverList.add(server);
                }
            }
            Logging.logInfo("updated server list: " + this.serverList);
            respondSuccess();
        }
    }

    private EzServer[] parseExchange(JsonObject obj) throws ServerException {
        // check if contains serverList
        if (!obj.has("serverList")) {
            throw new ServerException("missing or invalid server list");
        }

        // parse servers, check if all servers are valid
        List<EzServer> servers = new ArrayList<>();

        JsonArray elems;
        try {
            elems = obj.getAsJsonArray("serverList");
        } catch (ClassCastException e) {
            // not JSON array
            throw new ServerException("missing or invalid server list");
        }

        for (JsonElement elem : elems) {
            EzServer server = EzServer.fromJson(elem.getAsJsonObject());
            if (server == null) {
                // invalid server record
                throw new ServerException("missing or invalid server list");
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

            // set timeout
            socket.setSoTimeout(Static.DEFAULT_TIMEOUT);

            // read json from socket

            String reqJson = Static.readJsonUTF(inputStream);
            Logging.logFine("RECEIVED: " + reqJson);

            JsonParser parser = new JsonParser();

            // parse json to JsonObject
            JsonObject obj;
            try {
                obj = parser.parse(reqJson).getAsJsonObject();
            } catch (Exception e) {
                // cannot parse request as json
                throw new ServerException("missing or incorrect type for command");
            }

            // check command exist as a string
            if (!obj.has("command")) {
                throw new ServerException("missing or incorrect type for command");
            }

            String command;
            try {
                command = obj.get("command").getAsString();
            } catch (IllegalStateException e) {
                // not a string
                throw new ServerException("missing or incorrect type for command");
            }


            // determine command type and handle each case
            if (command.equals("PUBLISH")) {
                publish(parseResource(obj));
            } else if (command.equals("REMOVE")) {
                remove(parseResource(obj));
            } else if (command.equals("SHARE")) {
                share(parsesecrect(obj), parseResource(obj));
            } else if (command.equals("QUERY")) {
                query(parseTemplate(obj), parserelay(obj));
            } else if (command.equals("FETCH")) {
                fetch(parseTemplate(obj));
            } else if (command.equals("EXCHANGE")) {
                exchange(parseExchange(obj));
            } else {
                throw new ServerException("invalid command");
            }
        } catch (SocketTimeoutException e) {
            Logging.logInfo("Timeout communicating with client, disconnecting...");
        } catch (IOException e) {
            Logging.logInfo("Unknown network error with client, disconnecting...");
        } catch (ServerException e) {
            try {
                Logging.logInfo("Invalid client request: " + e.getErrorMessage());
                Logging.logInfo("Disconnecting from client...");
                Static.sendJsonUTF(outputStream, e.toJson());
            } catch (IOException e1) {
                Logging.logInfo("Unknown network error with client, disconnecting...");
            }
        } catch (InterruptedException e) {
            Logging.logInfo("InterruptedException waiting for connection interval to pass, disconnecting...");
        } catch (Exception e) {
            e.printStackTrace();
            Logging.logInfo("Unknown exception in ServiceThread, disconnecting...");
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                Logging.logInfo("Network error closing connection with client");
            }
        }
    }
}
