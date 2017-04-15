package com.allstars.project1;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.cli.*;

public class Client {
    private static Gson gson = new Gson();

    private static boolean handleResponse(String response) {
        JsonObject resObj = new JsonParser().parse(response).getAsJsonObject();
        if (resObj.get("response").getAsString().equals("success")) {
            System.out.println("success");
            return true;
        } else {
            System.out.println(response);
            System.out.println("error: " + resObj.get("errorMessage").getAsString());
            return false;
        }
    }

    private static String makeJsonFrom(String command, Resource resource) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("command", command);
        jsonObject.add("resource", new Gson().toJsonTree(resource));
        return jsonObject.toString();
    }

    private static String makeJsonFrom(String command, Resource resource, boolean isTemplate) {
        if (isTemplate) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("command", command);
            jsonObject.add("resourceTemplate", new Gson().toJsonTree(resource));
            return jsonObject.toString();
        } else {
            return makeJsonFrom(command, resource);
        }
    }

    private static String makeJsonFrom(String command, String secret, Resource resource) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("command", command);
        jsonObject.addProperty("secret", secret);
        jsonObject.add("resource", new Gson().toJsonTree(resource));
        return jsonObject.toString();
    }

    private static String makeJsonFrom(String command, boolean relay, Resource resource) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("command", command);
        jsonObject.addProperty("relay", relay);
        jsonObject.add("resourceTemplate", new Gson().toJsonTree(resource));
        return jsonObject.toString();
    }

    private static String makeJsonFrom(String command, EzServer[] serverList) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("command", command);
        jsonObject.add("serverList", new Gson().toJsonTree(serverList));
        return jsonObject.toString();
    }


    private static void publish(Socket socket, Resource resource) throws IOException {
        DataInputStream in = new DataInputStream(socket.getInputStream());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        // send request
        out.writeUTF(makeJsonFrom("PUBLISH", resource));

        // wait for response
        String response = in.readUTF();
        handleResponse(response);
    }

    private static void remove(Socket socket, Resource resource) throws IOException {
        DataInputStream in = new DataInputStream(socket.getInputStream());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        // send request
        out.writeUTF(makeJsonFrom("REMOVE", resource));

        // wait for response
        String response = in.readUTF();
        handleResponse(response);
    }

    public static void share(Socket socket, String secret, Resource resource) throws IOException {
        DataInputStream in = new DataInputStream(socket.getInputStream());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        // send request
        out.writeUTF(makeJsonFrom("SHARE", secret, resource));

        // wait for response
        String response = in.readUTF();
        handleResponse(response);
    }

    public static Set<Resource> query(Socket socket, boolean relay, Resource template) throws IOException {
        DataInputStream in = new DataInputStream(socket.getInputStream());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        // send request
        out.writeUTF(makeJsonFrom("QUERY", relay, template));

        Set<Resource> resources = new HashSet<>();

        // wait for response
        String response = in.readUTF();
        boolean success = handleResponse(response);
        if (success) {
            JsonObject jsonObj = new JsonParser().parse(in.readUTF()).getAsJsonObject();
            while (!jsonObj.has("resultSize")) {
                System.out.println(jsonObj);
                resources.add(Constants.GSON.fromJson(jsonObj, Resource.class));
                jsonObj = new JsonParser().parse(in.readUTF()).getAsJsonObject();
            }
            System.out.println(jsonObj);
            if (jsonObj.get("resultSize").getAsInt() != resources.size()) {
                // TODO
                // something wrong happened
            }
        } else {
            // TODO
        }

        return resources;
    }

    public static void fetch(Socket socket, Resource template) throws IOException, URISyntaxException {
        // http://stackoverflow.com/questions/9520911/java-sending-and-receiving-file-byte-over-sockets

        DataInputStream in = new DataInputStream(socket.getInputStream());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        // send request
        out.writeUTF(makeJsonFrom("FETCH", template, true));

        // wait for response
        String response = in.readUTF();
        boolean success = handleResponse(response);
        System.out.println("read resource");
        Resource resource = Resource.fromJson(in.readUTF());
        System.out.println(resource.getName());
        System.out.println("read file");
        long totalSize = resource.getResourceSize();
        System.out.println(totalSize);
        long sizeRead = 0;
        if (success) {
            String path = resource.getUri();
            String filename = path.substring(path.lastIndexOf('/') + 1);
            java.net.URI uri = new java.net.URI("./" + filename);
            FileOutputStream fileOutputStream = new FileOutputStream(new File(uri.getPath()));

            byte[] bytes = new byte[16*1024];

            int toRead = (int) Math.min(totalSize - sizeRead, 16 * 1024);

            int count;
            while (toRead > 0 && (count = in.read(bytes, 0, toRead)) > 0) {
                sizeRead += count;
                fileOutputStream.write(bytes, 0, count);
                toRead = (int) Math.min(totalSize - sizeRead, 16 * 1024);
            }
            System.out.println("read file complete");
            fileOutputStream.close();
        }
        String sizeResponse = in.readUTF();
        // TODO
    }

    public static void exchange(Socket socket, EzServer[] servers) throws IOException {
        DataInputStream in = new DataInputStream(socket.getInputStream());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        Debug.infoPrintln(makeJsonFrom("EXCHANGE", servers));

        // send request
        out.writeUTF(makeJsonFrom("EXCHANGE", servers));

        // wait for response
        String response = in.readUTF();
        handleResponse(response);
    }

    public static CommandLine getOptions(String[] args) {
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
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e);
            System.out.println("parse exception");
        } finally {
            return cmd;
        }
    }

    static Resource makeResourceFromCmd(CommandLine cmd) {
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


    public static Socket connectToServer(String host, int port) {
        // TODO connect to server
        Socket socket = null;
        try {
            socket = new Socket(host, port);

            System.out.println("Connection Established");
            return socket;
//            DataInputStream in = new DataInputStream(socket.getInputStream());
//            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
//            System.out.println("Sending data");
//            out.writeUTF(args[0]);     // UTF is a string encoding see Sn. 4.4
//            String data = in.readUTF();   // read a line of data from the stream
//            System.out.println("Received: "+ data) ;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    public static void main(String[] args) {
        CommandLine cmd = getOptions(args);
        String host = cmd.getOptionValue("host");
        int port = Integer.parseInt(cmd.getOptionValue("port"));


        Resource resource = makeResourceFromCmd(cmd);

        // TODO print error if no command
        // ...

        Socket socket = connectToServer(host, port);

        try {
            if (cmd.hasOption("publish")) {
                publish(socket, resource);
            } else if (cmd.hasOption("remove")) {
                remove(socket, resource);
            } else if (cmd.hasOption("share")) {
                String secret = cmd.getOptionValue("secret");
                share(socket, secret, resource);
            } else if (cmd.hasOption("query")) {
                query(socket, true, new Resource(null, null, null, null, null, null, null, null));
//                query(socket, resource);
            } else if (cmd.hasOption("fetch")) {
                fetch(socket, resource);
            } else if (cmd.hasOption("exchange")) {
                EzServer[] servers = Arrays.stream(cmd.getOptionValue("servers").split(","))
                        .map(EzServer::fromString).toArray(EzServer[]::new);
                Debug.infoPrintln(Arrays.toString(servers));
                exchange(socket, servers);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
}