package EzShare;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * EzShare Service Thread Implementation.
 */
public class ServiceThread extends Thread {
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private String secret;
    private ResourceStorage resourceStorage;
    private ServerList serverList;
    private Socket socket;
    private Map<SocketAddress, Date> lastConnectionTime;
    private EzServer server;

    /**
     * Constructor for ServiceThread
     *
     * @param lastConnectionTime the HashMap contains the latest connection time for all clients that have connected before
     * @param clientSocket       the client socket connects to this thread
     * @param secret             the secret password from the Server
     * @param resourceStorage    the resource storage for the Server
     * @param serverList         a list of Servers that this Server has acknowledges
     * @param server             an instance of the server itself
     * @throws IOException Network connection exception
     */
    public ServiceThread(Map<SocketAddress, Date> lastConnectionTime, Socket clientSocket, String secret, ResourceStorage resourceStorage, ServerList serverList, EzServer server)
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

    /**
     * Check whether the context in a JSON field is String
     *
     * @param obj   the JSON object contains the target field
     * @param field the target field needs to be verified
     * @return true if it can be converted to String, otherwise return false
     */
    private boolean hasFieldAsString(JsonObject obj, String field) {
        if (obj.has(field)) {
            try {
                obj.get(field).getAsString();
                return true;
            } catch (Exception e) {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * Parse any resource (JSON object) that contains non-String resource field to null
     *
     * @param obj the JSON form resource needs to be parsed
     * @return null if any resource field is non-String
     */
    private Resource parseResourceObj(JsonObject obj) {
        final String[] fields = {"owner", "channel", "name", "uri", "description"};
        for (String field : fields) {
            if (!hasFieldAsString(obj, field)) {
                return null;
            }
        }

        return Resource.parseAndNormalise(obj);
    }

    /**
     * Parse the resource field in the JSON requests send by clients
     *
     * @param obj the JSON form request needs to be parsed
     * @return the resource after parsed
     * @throws ServerException any error in the resource
     */
    private Resource parseResource(JsonObject obj) throws ServerException {

        // check if the request has resource field, otherwise throw a "missing resource" exception
        if (!obj.has("resource")) {
            throw new ServerException("missing resource");
            // check if the resource field is in JSON format, otherwise throw a "missing resource" exception
        } else if (!(obj.get("resource").isJsonObject())) {
            throw new ServerException("missing resource");
        }

        // retrieve and parse the resource field as a JSON object
        Resource resource = parseResourceObj(obj.get("resource").getAsJsonObject());

        // check if the resource is valid, otherwise throw a "invalid resource" exception
        if (resource == null) {
            throw new ServerException("invalid resource");
        }

        return resource;
    }

    /**
     * Parse any resource template field in the JSON requests send by clients
     *
     * @param obj the JSON form request needs to be parsed
     * @return the resource template after parsed
     * @throws ServerException any error in the resource template
     */
    private Resource parseTemplate(JsonObject obj) throws ServerException {

        // check if the request has resource template field, otherwise throw a "missing resourceTemplate" exception
        if (!obj.has("resourceTemplate")) {
            throw new ServerException("missing resourceTemplate");
            // check if the resource template field is in JSON format, otherwise throw a "missing resourceTemplate exception
        } else if (!(obj.get("resourceTemplate").isJsonObject())) {
            throw new ServerException("missing resourceTemplate");
        }

        // retrieve and parse the resource template field as a JSON object
        Resource resource = parseResourceObj(obj.get("resourceTemplate").getAsJsonObject());

        // check if the resource template is valid, otherwise throw a "invalid resourceTemplate" exception
        if (resource == null) {
            throw new ServerException("invalid resourceTemplate");
        }

        return resource;
    }

    /**
     * Parse the secret field in the JSON requests send by clients
     *
     * @param obj the JSON form request needs to be parsed
     * @return the secret after parsed
     * @throws ServerException any error in the resource or secret field
     */
    private String parseSecret(JsonObject obj) throws ServerException {
        if (!hasFieldAsString(obj, "secret")) {
            throw new ServerException("missing resource and/or secret");
        } else {
            return obj.get("secret").getAsString();
        }
    }

    /**
     * Parse the relay field in the JSON requests send by clients
     *
     * @param obj the JOSN form request needs to be parsed
     * @return true if the relay in request is true
     * @throws ServerException any error in the resource template and relay field
     */
    private Boolean parseRelay(JsonObject obj) throws ServerException {
        try {
            // check if the request has relay field, otherwise throw a "missing resourceTemplate" exception
            if (!obj.has("relay")) {
                throw new ServerException("missing resourceTemplate");
            } else {
                return obj.get("relay").getAsBoolean();
            }

        } catch (IllegalStateException e) {
            // throw a "missing resourceTemplate" exception when relay field cannot be get as boolean
            throw new ServerException("missing resourceTemplate");
        }
    }

    /**
     * Check if the resource is valid
     *
     * @param resource the resource needs to be checked
     * @return true if all conditions are satisfied, otherwise return false
     */
    private boolean checkResource(Resource resource) {

        // retrieve the resource with same uri and channel in resource storage
        Resource r = resourceStorage.findWith(resource.getChannel(), resource.getUri());

        if (resource.getUri() == null || resource.getUri().isEmpty()) {
            // check uri is null or not given
            return false;
        } else if (resource.getUri().equals("*")) {
            // owner is "*"
            return false;
        } else if (!URI.create(resource.getUri()).isAbsolute()) {
            // not an absolute uri
            return false;
        } else if (r != null && !r.getOwner().equals(resource.getOwner())) {
            // duplicate uri in a given channel with different owners
            return false;
        }
        return true;
    }

    /**
     * Check whether a given uri is a file
     *
     * @param uri the uri needs to be checked
     * @return true if the uri is a file, otherwise return false
     */
    private boolean isFile(String uri) {

        URI u = URI.create(uri);
        String scheme = u.getScheme();
        try {
            return scheme.equalsIgnoreCase("file");
        } catch (NullPointerException e) {
            // No scheme is found
            return false;
        }
    }

    /**
     * Send a success response as JSON format to the client
     *
     * @throws IOException Network connection exception
     */
    private void respondSuccess() throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("response", "success");
        Static.sendJsonUTF(outputStream, json.toString());
    }

    /**
     * Send the required resource as JSON format to the client
     *
     * @param resource the resource that is required by the client
     * @throws IOException Network connection exception
     */
    private void respondResource(Resource resource) throws IOException {
        Static.sendJsonUTF(outputStream, resource.toJson());
    }

    /**
     * Send the required resource and its size
     *
     * @param resource the resource that is required by the client
     * @param size     the size of the resource
     * @throws IOException Network connection exception
     */
    private void respondResource(Resource resource, long size) throws IOException {
        Static.sendJsonUTF(outputStream, resource.sizeAdded(size).toJson());
    }

    /**
     * Send the result size as JSON format to the client
     *
     * @param size the result size
     * @throws IOException Network connection exception
     */
    private void respondResultSize(int size) throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("resultSize", size);
        Static.sendJsonUTF(outputStream, json.toString());
    }

    /**
     * Publish a given resource on the server
     *
     * @param resource the given resource needs to be published
     * @throws ServerException any error in the given resource
     * @throws IOException     Network connection exception
     */
    private void publish(Resource resource) throws ServerException, IOException {

        if (!checkResource(resource)) {
            throw new ServerException("invalid resource");
        }

        // check if the uri of the given resource is a file, if so, throw a "cannot publish resource" exception
        if (isFile(resource.getUri())) {
            throw new ServerException("cannot publish resource");
        }
        // check if there is resource with same primary key on the Server
        if (resourceStorage.containsKey(resource)) {
            // remove the previous resource existed and add the new one to the Server
            resourceStorage.remove(resource);
            resourceStorage.add(resource);
        } else {
            resourceStorage.add(resource);
        }
        // respond success to the client if with no exception
        respondSuccess();
    }

    /**
     * Remove a given resource on the Server
     *
     * @param resource the given resource
     * @throws ServerException any error in the given resource or no required resource exists
     * @throws IOException     Network connection exception
     */
    private void remove(Resource resource) throws ServerException, IOException {
        if (!checkResource(resource)) {
            throw new ServerException("invalid resource");
        }

        // check if the given resource exists on the Server, otherwise throw a "cannot remove resource" exception
        if (resourceStorage.containsKey(resource)) {
            resourceStorage.remove(resource);
            respondSuccess();
        } else {
            // the resource did not exist
            throw new ServerException("cannot remove resource");
        }
    }

    /**
     * Share a given resource (a file) on the Server
     *
     * @param secret   the secret specified by the Server
     * @param resource the given resource
     * @throws ServerException any error in resource or secret
     * @throws IOException     Network connection exception
     */
    private void share(String secret, Resource resource) throws ServerException, IOException {

        if (!checkResource(resource)) {
            throw new ServerException("invalid resource");
        }

        if (secret == null || resource == null) {
            throw new ServerException("missing resource and/or secret");
        }

        if (!isFile(resource.getUri())) {
            throw new ServerException("invalid resource");
        }

        if (!secret.equals(this.secret)) {
            throw new ServerException("incorrect secret");
        } else {
            File f = null;
            try {
                f = new File(new URI(resource.getUri()));
            } catch (URISyntaxException e) {
                throw new ServerException("cannot share resource");
            }
            if (f.isFile()) {
                if (!f.exists()) {
                    throw new ServerException("cannot share resource");
                }
                resourceStorage.add(resource);
                respondSuccess();
            } else {
                throw new ServerException("cannot share resource");
            }
        }
    }

    /**
     * Query a list of resources that matches the given resource template
     *
     * @param template the given resource template
     * @param relay    whether forwarding the query command to other servers known by this Server
     * @throws ServerException any error in resource template
     * @throws IOException     Network connection exception
     */
    private void query(Resource template, boolean relay) throws ServerException, IOException {
        // search for results
        Set<Resource> results = resourceStorage.searchWithTemplate(template).stream().map(
                r -> r.ezServerAdded(server) // add EzServer info for all result from itself
        ).collect(Collectors.toSet());

        // make relay queries
        if (relay) {
            for (EzServer server : serverList.getServers()) {
                try {
                    Socket socket = Client.connectToServer(server.hostname, server.port, Static.DEFAULT_TIMEOUT);
                    results.addAll(Client.query(socket, false, template));
                } catch (Exception e) {
                    Logging.logInfo("Error making query to server " + server + ". Skip to next server");
                }
            }
        }

        Logging.logInfo("Query results:");
        for (Resource r : results) {
            Logging.logFine(r.toJson());
        }

        respondSuccess();

        for (Resource resource : results) {
            // hide owner information
            respondResource(resource.ownerHidden());
        }

        respondResultSize(results.size());
    }

    /**
     * Fetch a specific resource (file) that matches the given template
     *
     * @param template the given template
     * @throws ServerException any error in resource template
     * @throws IOException     Network connection exception
     */
    private void fetch(Resource template) throws ServerException, IOException {

        if (!isFile(template.getUri())) {
            throw new ServerException("cannot fetch resource");
        }

        // search for resource
        Resource resource = Server.resourceStorage.findWith(template.getChannel(), template.getUri());

        if (resource == null) {
            // found 0 result
            respondSuccess();
            respondResultSize(0);
            return;
        }

        try {
            // get file
            java.net.URI uri = new java.net.URI(template.getUri());
            String path = uri.getPath();
            File file = new File(path);

            // send resource
            respondSuccess();
            respondResource(resource, file.length());

            // send bytes of file
            FileInputStream fileInputStream = new FileInputStream(file);
            Logging.logFine("start sending file bytes");
            byte[] bytes = new byte[Static.FILE_READ_WRITE_CHUNK_SIZE];
            int count;
            while ((count = fileInputStream.read(bytes)) > 0) {
                outputStream.write(bytes, 0, count);
            }
            Logging.logInfo("ended sending file bytes");
            respondResultSize(1);
        } catch (FileNotFoundException | URISyntaxException e) {
            // behaviour undefined in spec, chose to report an error here
            throw new ServerException("cannot fetch resource");
        }
    }

    /**
     * Exchange information between this server and other servers in the given list
     *
     * @param servers the given list of servers
     * @throws ServerException any error in server list
     * @throws IOException     Network connection exception
     */
    private void exchange(EzServer[] servers) throws ServerException, IOException {
        Logging.logInfo("handle exchange request");
        Logging.logInfo("request server list: " + Arrays.toString(servers));
        if (servers == null) {
            // no servers in the given list
            throw new ServerException("missing server list");
        } else {
            for (EzServer server : servers) {
                Logging.logFine(server);
                if (this.server != server) {
                    // if the server in the list is not this server
                    this.serverList.add(server);
                }
            }
            Logging.logInfo("updated server list: " + this.serverList);
            respondSuccess();
        }
    }

    private void subscribe(Resource resourceTemplate) {
        // TODO
    }

    private void unsubscribe() {
        // TODO
    }

    /**
     * Parse the serverList field in the JSON requests send by clients
     *
     * @param obj the JSON form request needs to be parsed
     * @return an array of EzServer
     * @throws ServerException any error in server list field
     */
    private EzServer[] parseExchange(JsonObject obj) throws ServerException {
        // check if contains serverList
        if (!obj.has("serverList")) {
            throw new ServerException("missing or invalid server list");
        }

        List<EzServer> servers = new ArrayList<>();
        JsonArray elems;
        // parse servers
        try {
            elems = obj.getAsJsonArray("serverList");
        } catch (ClassCastException e) {
            // not JSON array
            throw new ServerException("missing or invalid server list");
        }
        // check if all servers are valid
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

    /**
     * Running the Service Thread to catch any request from the client
     */
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

            // parse json to JsonObject
            JsonParser parser = new JsonParser();
            JsonObject obj;

            try {
                obj = parser.parse(reqJson).getAsJsonObject();
            } catch (Exception e) {
                // cannot parse request as json
                throw new ServerException("missing or incorrect type for command");
            }

            // check command exist as a string
            if (!hasFieldAsString(obj, "command")) {
                throw new ServerException("missing or incorrect type for command");
            }
            String command = obj.get("command").getAsString();

            // determine command type and handle each case
            if (command.equals("PUBLISH")) {
                publish(parseResource(obj));
            } else if (command.equals("REMOVE")) {
                remove(parseResource(obj));
            } else if (command.equals("SHARE")) {
                share(parseSecret(obj), parseResource(obj));
            } else if (command.equals("QUERY")) {
                query(parseTemplate(obj), parseRelay(obj));
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
