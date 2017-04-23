package com.allstars.project1;

import java.io.*;
import java.net.*;
import java.util.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.cli.*;

public class Client {
    private static boolean handleResponse(String response) {
        JsonObject resObj = new JsonParser().parse(response).getAsJsonObject();
        if (resObj.get("response").getAsString().equals("success")) {
            Logging.logInfo("success");
            return true;
        } else {
            Logging.logFine("RECEIVED: " + response);
            Logging.logInfo("error: " + resObj.get("errorMessage").getAsString());
            return false;
        }
    }

    private static String makeJsonFrom(String command, Resource resource) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("command", command);
        jsonObject.add("resource", Static.GSON.toJsonTree(resource));
        return jsonObject.toString();
    }

    private static String makeJsonFrom(String command, Resource resource, boolean isTemplate) {
        if (isTemplate) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("command", command);
            jsonObject.add("resourceTemplate", Static.GSON.toJsonTree(resource));
            return jsonObject.toString();
        } else {
            return makeJsonFrom(command, resource);
        }
    }

    private static String makeJsonFrom(String command, String secret, Resource resource) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("command", command);
        jsonObject.addProperty("secret", secret);
        jsonObject.add("resource", Static.GSON.toJsonTree(resource));
        return jsonObject.toString();
    }

    private static String makeJsonFrom(String command, boolean relay, Resource resource) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("command", command);
        jsonObject.addProperty("relay", relay);
        jsonObject.add("resourceTemplate", Static.GSON.toJsonTree(resource));
        return jsonObject.toString();
    }

    private static String makeJsonFrom(String command, EzServer[] serverList) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("command", command);
        jsonObject.add("serverList", Static.GSON.toJsonTree(serverList));
        return jsonObject.toString();
    }


    public static void publish(Socket socket, Resource resource) throws IOException {
        DataInputStream in = new DataInputStream(socket.getInputStream());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        // send request
        Static.sendJsonUTF(out, makeJsonFrom("PUBLISH", resource));

        // wait for response
        String response = Static.readJsonUTF(in);
        handleResponse(response);
    }

    protected static void remove(Socket socket, Resource resource) throws IOException {
        DataInputStream in = new DataInputStream(socket.getInputStream());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        // send request
        Static.sendJsonUTF(out, makeJsonFrom("REMOVE", resource));

        // wait for response
        String response = Static.readJsonUTF(in);
        handleResponse(response);
    }

    protected static void share(Socket socket, String secret, Resource resource) throws IOException {
        DataInputStream in = new DataInputStream(socket.getInputStream());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        // send request
        Static.sendJsonUTF(out, makeJsonFrom("SHARE", secret, resource));

        // wait for response
        String response = Static.readJsonUTF(in);
        handleResponse(response);
    }

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
            JsonObject jsonObj = new JsonParser().parse(Static.readJsonUTF(in)).getAsJsonObject();
            while (!jsonObj.has("resultSize")) {
                Logging.logFine(jsonObj);
                resources.add(Static.GSON.fromJson(jsonObj, Resource.class));
                jsonObj = new JsonParser().parse(Static.readJsonUTF(in)).getAsJsonObject();
            }
            Logging.logFine(jsonObj);
            if (jsonObj.get("resultSize").getAsInt() != resources.size()) {
                // TODO
                // something wrong happened
            }
        } else {
            // TODO
        }

        return resources;
    }

    protected static void fetch(Socket socket, Resource template) throws IOException {
        // http://stackoverflow.com/questions/9520911/java-sending-and-receiving-file-byte-over-sockets

        DataInputStream in = new DataInputStream(socket.getInputStream());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        // send request
        Static.sendJsonUTF(out, makeJsonFrom("FETCH", template, true));

        // wait for response
        String response = Static.readJsonUTF(in);
        boolean success = handleResponse(response);
        Logging.logFine("read resource");
        Resource resource = Resource.fromJson(Static.readJsonUTF(in));
        Logging.logFine(resource.getName());
        Logging.logFine("read file");
        long totalSize = resource.getResourceSize();
        Logging.logFine(totalSize);
        long sizeRead = 0;
        if (success) {
            String path = resource.getUri();
            String filename = path.substring(path.lastIndexOf('/') + 1);
            URI uri = null;
            try {
                uri = new URI("./" + filename);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            FileOutputStream fileOutputStream = new FileOutputStream(new File(uri.getPath()));

            byte[] bytes = new byte[16*1024];

            int toRead = (int) Math.min(totalSize - sizeRead, 16 * 1024);

            int count;
            while (toRead > 0 && (count = in.read(bytes, 0, toRead)) > 0) {
                sizeRead += count;
                fileOutputStream.write(bytes, 0, count);
                toRead = (int) Math.min(totalSize - sizeRead, 16 * 1024);
            }
            Logging.logFine("read file complete");
            fileOutputStream.close();
        }
        String sizeResponse = Static.readJsonUTF(in);
        // TODO check size
    }

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

    public static Resource makeResourceFromCmd(CommandLine cmd) {
        String name = cmd.getOptionValue("name", "");
        String description = cmd.getOptionValue("description", "");
        String[] tags = null;
        if (cmd.hasOption("tag")) {
            tags = cmd.getOptionValue("tag").split(",");
        }
        String uri = cmd.getOptionValue("uri", "");
        String channel = cmd.getOptionValue("channel", "");
        String owner = cmd.getOptionValue("owner", "");
        String[] servers = cmd.getOptionValue("server", "").split(",");

        return new Resource(name, description, tags, uri, channel, owner, servers[0], null);
    }

    public static Socket connectToServer(String host, int port, int timeout) throws IOException {
        // TODO connect to server
        Socket socket;
        socket = new Socket(host, port);
        socket.setSoTimeout(timeout);
        Logging.logFine("Connection Established");
        return socket;
    }

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
        Logging.logInfo("setting debug on");

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
                EzServer[] servers = Arrays.stream(cmd.getOptionValue("servers")
                        .split(","))
                        .map(EzServer::fromString).toArray(EzServer[]::new);
                Logging.logInfo(Arrays.toString(servers));
                exchange(socket, servers);
            }
        } catch (SocketTimeoutException e) {
            Logging.logInfo("Timeout communicating with server, please check connections and try again.");
        } catch (IOException e) {
            Logging.logInfo("Unknown connection error, please check connections and try again.");
        }
    }
}