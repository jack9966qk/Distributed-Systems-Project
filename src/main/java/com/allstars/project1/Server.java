package com.allstars.project1;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.cli.*;

public class Server {
    public static ResourceStorage resourceStorage = new ResourceStorage();
    public static List<EzServer> serverList = Collections.synchronizedList(new ArrayList<>());

    public static void startServer(int exchangeInterval, String host, int port) {
        try {
            // for sending exchange request to other servers
            ExchangeThread exchangeThread = new ExchangeThread(exchangeInterval, serverList);
            exchangeThread.start();

            ServerSocket listenSocket = new ServerSocket(port);
            int i = 0;
            while (true) {
                // wait for new client
                System.out.println("Server listening for a connection");
                Socket clientSocket = listenSocket.accept();
                i++;
                System.out.println("Received connection " + i );
                // start a new thread handling the client
                // TODO limitation on total number of threads
                ServiceThread c = new ServiceThread(clientSocket, resourceStorage, serverList);
                c.start();
            }
        } catch(IOException e) {
            System.out.println("Listen socket:"+e.getMessage());
        }
    }

    public static CommandLine getOptions(String[] args) {
        Options options = new Options();

        options.addOption(Option.builder("advertisedhostname").desc("advertised hostname")
                .required().hasArg().type(String.class).build());
        options.addOption(Option.builder("connectionintervallimit").desc("connection interval limit in seconds")
                .required().hasArg().type(Integer.class).build());
        options.addOption(Option.builder("exchangeinterval").desc("exchange interval in seconds")
                .required().hasArg().type(Integer.class).build());
        options.addOption(Option.builder("port").desc("server port, an integer")
                .required().hasArg().type(Integer.class).build());
        options.addOption(Option.builder("secret").desc("secret")
                .hasArg().type(String.class).build());
        options.addOption(Option.builder("debug").desc("print debug information").build());

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println("parse exception");
        } finally {
            return cmd;
        }
    }

    public static void main(String[] args) {
        CommandLine cmd = getOptions(args);

        // set debug
        Debug.setEnablePrint(cmd.hasOption("debug"));

        // detemine secret
        String secret = null;
        if (!cmd.hasOption("secret")) {
            // TODO generate random secret
        } else {
            secret = cmd.getOptionValue("secret");
        }

        // determine host and port
        InetAddress address = null;
        try {
            address = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        String host = cmd.getOptionValue("advertisedhostname", address.getHostName());
        int port = Integer.parseInt(cmd.getOptionValue("port"));

        int connectionIntervalLimit = Integer.parseInt(cmd.getOptionValue("connectionintervallimit"));
        int exchangeInterval = Integer.parseInt(cmd.getOptionValue("exchangeinterval"));

        // start the server
        startServer(exchangeInterval, host, port);
    }
}