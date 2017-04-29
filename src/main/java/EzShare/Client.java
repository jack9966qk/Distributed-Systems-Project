package EzShare;

import java.io.*;
import java.net.*;
import java.util.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.cli.*;

/**
 * EzShare client implementation, has a main method to be used through command line
 */
public class Client {
    /**
     * Handle response from server
     *
     * @param response response from server
     * @return true if success, false otherwise
     */
    private static boolean handleResponse(String response) {
        JsonObject resObj = null;
        try {
            resObj = new JsonParser().parse(response).getAsJsonObject();
        } catch (Exception e) {
            Logging.logInfo("cannot parse result as json");
            return false;
        }
        if (resObj.get("response").getAsString().equals("success")) {
            Logging.logInfo("command successful");
            return true;
        } else {
            Logging.logInfo("error: " + resObj.get("errorMessage").getAsString());
            return false;
        }
    }

    /**
     * Handle resultSize response from server
     *
     * @param responseObj  response from server as JSONObject
     * @param expectedSize expected number of resources received
     * @return true if reported number matches expected, false otherwise
     */
    private static boolean handleSizeResponse(JsonObject responseObj, int expectedSize) {
        int size = responseObj.get("resultSize").getAsInt();
        Logging.logInfo("number of results received: " + expectedSize);
        if (size != expectedSize) {
            Logging.logInfo("WARNING: number of results received different to reported number from server");
            return false;
        } else {
            return true;
        }
    }

    /**
     * Handle resultSize response from server
     *
     * @param response     response from server
     * @param expectedSize expected number of resources received
     * @return true if reported number matches expected, false otherwise
     */
    private static boolean handleSizeResponse(String response, int expectedSize) {
        JsonObject resObj = null;
        try {
            resObj = new JsonParser().parse(response).getAsJsonObject();
        } catch (Exception e) {
            Logging.logInfo("cannot parse result as json");
            return false;
        }
        return handleSizeResponse(resObj, expectedSize);
    }

    /**
     * Make json from arguments
     *
     * @param command  the command
     * @param resource the resource
     * @return json with arguments as fields
     */
    private static String makeJsonFrom(String command, Resource resource) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("command", command);
        jsonObject.add("resource", resource.toJsonElement());
        return jsonObject.toString();
    }


    /**
     * Make json from arguments
     *
     * @param command    the command
     * @param resource   the resource
     * @param isTemplate true if resource is template, false otherwise
     * @return json with arguments as fields
     */
    private static String makeJsonFrom(String command, Resource resource, boolean isTemplate) {
        if (isTemplate) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("command", command);
            jsonObject.add("resourceTemplate", resource.toJsonElement());
            return jsonObject.toString();
        } else {
            return makeJsonFrom(command, resource);
        }
    }

    /**
     * Make json from arguments
     *
     * @param command  the command
     * @param secret   the secret
     * @param resource the resource
     * @return json with arguments as fields
     */
    private static String makeJsonFrom(String command, String secret, Resource resource) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("command", command);
        jsonObject.addProperty("secret", secret);
        jsonObject.add("resource", resource.toJsonElement());
        return jsonObject.toString();
    }

    /**
     * Make json from arguments
     *
     * @param command  the command
     * @param relay    true if need relay, false otherwise
     * @param resource the resource
     * @return json with arguments as fields
     */
    private static String makeJsonFrom(String command, boolean relay, Resource resource) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("command", command);
        jsonObject.addProperty("relay", relay);
        jsonObject.add("resourceTemplate", resource.toJsonElement());
        return jsonObject.toString();
    }

    /**
     * Make json from arguments
     *
     * @param command    the command
     * @param serverList an array of EzShare to send in exchange
     * @return json with arguments as fields
     */
    private static String makeJsonFrom(String command, EzServer[] serverList) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("command", command);
        jsonObject.add("serverList", Static.GSON.toJsonTree(serverList));
        return jsonObject.toString();
    }

    /**
     * Make publish request
     *
     * @param socket   socket used to communicate with server
     * @param resource resource to be published
     * @throws IOException any network error
     */
    public static void publish(Socket socket, Resource resource) throws IOException {
        DataInputStream in = new DataInputStream(socket.getInputStream());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        // send request
        Static.sendJsonUTF(out, makeJsonFrom("PUBLISH", resource));

        // wait for response
        String response = Static.readJsonUTF(in);
        handleResponse(response);
    }

    /**
     * Make remove request
     *
     * @param socket   socket used to communicate with server
     * @param resource resource to be removed
     * @throws IOException any network error
     */
    protected static void remove(Socket socket, Resource resource) throws IOException {
        DataInputStream in = new DataInputStream(socket.getInputStream());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        // send request
        Static.sendJsonUTF(out, makeJsonFrom("REMOVE", resource));

        // wait for response
        String response = Static.readJsonUTF(in);
        handleResponse(response);
    }

    /**
     * Make share request
     *
     * @param socket   socket used to communicate with server
     * @param secret   secret of server
     * @param resource resource to be published
     * @throws IOException any network error
     */
    protected static void share(Socket socket, String secret, Resource resource) throws IOException {
        DataInputStream in = new DataInputStream(socket.getInputStream());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        // send request
        Static.sendJsonUTF(out, makeJsonFrom("SHARE", secret, resource));

        // wait for response
        String response = Static.readJsonUTF(in);
        handleResponse(response);
    }

    /**
     * Make query request
     *
     * @param socket   socket used to communicate with server
     * @param relay    true if need relay, false otherwise
     * @param template resource template used for searching
     * @return query result as set of resources
     * @throws IOException any network error
     */
    protected static Set<Resource> query(Socket socket, boolean relay, Resource template) throws IOException {
        DataInputStream in = new DataInputStream(socket.getInputStream());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        // send request
        Static.sendJsonUTF(out, makeJsonFrom("QUERY", relay, template));

        Set<Resource> resources = new HashSet<>();

        // wait for response
        String response = Static.readJsonUTF(in);
        boolean success = handleResponse(response);
        if (success) {
            Logging.logInfo("Results:");
            JsonObject jsonObj = new JsonParser().parse(Static.readJsonUTF(in)).getAsJsonObject();
            int numReceived = 0;
            while (!jsonObj.has("resultSize")) {
                numReceived += 1;
                Logging.logFine(jsonObj);
                resources.add(Static.GSON.fromJson(jsonObj, Resource.class));
                jsonObj = new JsonParser().parse(Static.readJsonUTF(in)).getAsJsonObject();
            }
            Logging.logFine(jsonObj);
            handleSizeResponse(jsonObj, numReceived);
        } else {
            Logging.logInfo("query failed, client exiting...");
        }

        return resources;
    }

    /**
     * Make fetch request
     *
     * @param socket   socket used to communicate with server
     * @param template resource template used for searching
     * @throws IOException any network error
     */
    protected static void fetch(Socket socket, Resource template) throws IOException {
        // http://stackoverflow.com/questions/9520911/java-sending-and-receiving-file-byte-over-sockets

        DataInputStream in = new DataInputStream(socket.getInputStream());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        // send request
        Static.sendJsonUTF(out, makeJsonFrom("FETCH", template, true));

        // wait for response
        String response = Static.readJsonUTF(in);
        boolean success = handleResponse(response);
        if (!success) {
            return;
        }

        // if resultSize gets reported early
        String nextResponse = Static.readJsonUTF(in);
        JsonParser parser = new JsonParser();
        JsonObject jsonObject = parser.parse(nextResponse).getAsJsonObject();
        if (jsonObject.has("resultSize")) {
            handleSizeResponse(jsonObject, 1);
            return;
        }

        // read resource info
        Logging.logFine("read resource");
        Resource resource = Resource.fromJson(nextResponse);
        Logging.logFine(resource.getName());

        // create file
        String path = resource.getUri();
        String filename = path.substring(path.lastIndexOf('/') + 1);
        URI uri = null;
        try {
            uri = new URI("./" + filename);
        } catch (URISyntaxException e) {
            Logging.logInfo("Error creating local file: " + filename);
            return;
        }
        FileOutputStream fileOutputStream = new FileOutputStream(new File(uri.getPath()));


        // start reading bytes of file
        Logging.logFine("downloading file...");
        long totalSize = resource.getResourceSize();
        long sizeRead = 0;
        byte[] bytes = new byte[Static.FILE_READ_WRITE_CHUNK_SIZE];
        int toRead = (int) Math.min(totalSize - sizeRead, Static.FILE_READ_WRITE_CHUNK_SIZE);

        int count;
        while (toRead > 0 && (count = in.read(bytes, 0, toRead)) > 0) {
            sizeRead += count;
            fileOutputStream.write(bytes, 0, count);
            toRead = (int) Math.min(totalSize - sizeRead, Static.FILE_READ_WRITE_CHUNK_SIZE);
        }
        Logging.logFine("download complete");
        fileOutputStream.close();

        // check number of resources to be 1 (as specified)
        String sizeResponse = Static.readJsonUTF(in);
        handleSizeResponse(sizeResponse, 1);
    }

    /**
     * Make exchange request
     *
     * @param socket  socket used to communicate with server
     * @param servers array of known EzServers to be sent
     * @throws IOException any network error
     */
    protected static void exchange(Socket socket, EzServer[] servers) throws IOException {
        DataInputStream in = new DataInputStream(socket.getInputStream());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        String message = makeJsonFrom("EXCHANGE", servers);

        // send request
        Static.sendJsonUTF(out, message);
        Logging.logInfo("SENT: " + message);

        // wait for response
        String response = Static.readJsonUTF(in);
        handleResponse(response);
    }

    /**
     * Parse command line arguments
     *
     * @param args arguments passed from main function
     * @return CommandLine object as parse result
     * @throws ParseException error in parsing
     */
    public static CommandLine getOptions(String[] args) throws ParseException {
        Options options = new Options();

        options.addOption(Option.builder("channel").hasArg().type(Integer.class).build());
        options.addOption(Option.builder("debug").build());
        options.addOption(Option.builder("description").hasArg().type(String.class).build());
        options.addOption(Option.builder("exchange").build());
        options.addOption(Option.builder("fetch").build());
        options.addOption(Option.builder("host").required().hasArg().type(String.class).build());
        options.addOption(Option.builder("name").hasArg().type(String.class).build());
        options.addOption(Option.builder("owner").hasArg().type(String.class).build());
        options.addOption(Option.builder("port").required().hasArg().type(Integer.class).build());
        options.addOption(Option.builder("publish").build());
        options.addOption(Option.builder("query").build());
        options.addOption(Option.builder("remove").build());
        options.addOption(Option.builder("secret").hasArg().type(String.class).build());
        options.addOption(Option.builder("servers").hasArg().type(String.class).build());
        options.addOption(Option.builder("share").build());
        options.addOption(Option.builder("tags").hasArg().type(String.class).build());
        options.addOption(Option.builder("uri").hasArg().type(String.class).build());

        CommandLineParser parser = new DefaultParser();
        return parser.parse(options, args);
    }

    /**
     * Construct a resource object from command line arguments
     *
     * @param cmd CommandLine object containing arguments
     * @return a new resource object from arguments
     */
    public static Resource makeResourceFromCmd(CommandLine cmd) {
        String name = cmd.getOptionValue("name", "");
        String description = cmd.getOptionValue("description", "");
        String[] tags = new String[0];
        if (cmd.hasOption("tags")) {
            tags = cmd.getOptionValue("tags").split(",");
        }
        String uri = cmd.getOptionValue("uri", "");
        String channel = cmd.getOptionValue("channel", "");
        String owner = cmd.getOptionValue("owner", "");
        return new Resource(name, description, tags, uri, channel, owner, null);
    }

    /**
     * Establish connection to a server
     *
     * @param host    host of server
     * @param port    port of server
     * @param timeout timeout in milliseconds for all operations with the server
     * @return Socket of the server
     * @throws IOException any network error
     */
    public static Socket connectToServer(String host, int port, int timeout) throws IOException {
        Socket socket;
        socket = new Socket(host, port);
        socket.setSoTimeout(timeout);
        Logging.logFine("Connection Established");
        return socket;
    }

    /**
     * The main function for client
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {

        // command line arguments parsing
        CommandLine cmd = null;
        try {
            cmd = getOptions(args);
        } catch (ParseException e) {
            Logging.logInfo("Command line parsing failed, please check if arguments are missing or incorrect.");
            return;
        }
        String host = cmd.getOptionValue("host");
        int port = Integer.parseInt(cmd.getOptionValue("port"));
        Resource resource = makeResourceFromCmd(cmd);


        if (!cmd.hasOption("publish") &&
                !cmd.hasOption("remove") &&
                !cmd.hasOption("share") &&
                !cmd.hasOption("query") &&
                !cmd.hasOption("fetch") &&
                !cmd.hasOption("remove")) {
            Logging.logInfo("No command found, please check your input and try again.");
        }

        // set debug
        Logging.setEnablePrint(cmd.hasOption("debug"));
        if (cmd.hasOption("debug")) {
            Logging.logInfo("setting debug on");
        }

        // connect to server
        Socket socket = null;
        try {
            socket = connectToServer(host, port, Static.DEFAULT_TIMEOUT);
        } catch (IOException e) {
            Logging.logInfo("Failed to connect to server, please check server availability and internet connection.");
            return;
        }

        // figure out command and handle each case
        try {
            if (cmd.hasOption("publish")) {
                publish(socket, resource);
            } else if (cmd.hasOption("remove")) {
                remove(socket, resource);
            } else if (cmd.hasOption("share")) {
                String secret = cmd.getOptionValue("secret");
                share(socket, secret, resource);
            } else if (cmd.hasOption("query")) {
                query(socket, true, resource);
            } else if (cmd.hasOption("fetch")) {
                fetch(socket, resource);
            } else if (cmd.hasOption("exchange")) {
                EzServer[] servers = Arrays.stream(cmd.getOptionValue("servers").split(","))
                        .map(EzServer::fromString).toArray(EzServer[]::new);
                Logging.logInfo(Arrays.toString(servers));
                exchange(socket, servers);
            }
        } catch (SocketTimeoutException e) {
            Logging.logInfo("Timeout communicating with server, please check connections and try again.");
        } catch (IOException e) {
            Logging.logInfo("Unknown connection error, please check connections and try again.");
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                Logging.logInfo("Network error closing socket to server");
            }
        }
    }
}