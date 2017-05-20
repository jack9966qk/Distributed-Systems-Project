package EZShare;

import java.math.BigInteger;
import java.net.*;
import java.security.SecureRandom;

import org.apache.commons.cli.*;

/**
 * EZShare server implementation, has a main method to be used through command line
 */
public class Server {
    SubscriptionManager subscriptionManager = new SubscriptionManager();
    public ResourceStorage resourceStorage = new ResourceStorage(subscriptionManager);
    public ServerList secureServerList = new ServerList(subscriptionManager);
    public ServerList insecureServerList = new ServerList(subscriptionManager);
    public ListenerThread insecureListener;
    public ListenerThread secureListener;
    private String name;

    public Server() {
        this("Server");
    }

    public Server(String name) {
        this.name = name;
    }

    public boolean isRunning() {
        boolean insecureRunning = insecureListener != null && insecureListener.running;
        boolean secureRunning = secureListener != null && secureListener.running;
        return insecureRunning && secureRunning;
    }

    /**
     * Terminate server activity (for testing)
     */
    public void stop() {
        if (insecureListener != null) {
            insecureListener.terminate();
        }
        if (secureListener != null) {
            secureListener.terminate();
        }
    }

    /**
     * Block current thread until server is ready for connection (for testing)
     *
     * @throws InterruptedException Interrupted during sleep
     */
    public void waitUntilReady() throws InterruptedException {
        while (!isRunning()) { // busy waiting
            Thread.sleep(1000);
        }
    }

    /**
     * Get command line options
     *
     * @param args command line arguments
     * @return
     * @throws ParseException
     */
    public static CommandLine getOptions(String[] args) throws ParseException {
        Options options = new Options();

        options.addOption(Option.builder("advertisedhostname").desc("advertised hostname")
                .hasArg().type(String.class).build());
        options.addOption(Option.builder("connectionintervallimit").desc("connection interval limit in seconds")
                .hasArg().type(Integer.class).build());
        options.addOption(Option.builder("exchangeinterval").desc("exchange interval in seconds")
                .hasArg().type(Integer.class).build());
        options.addOption(Option.builder("sport").desc("secure server port, an integer")
                .hasArg().type(Integer.class).build());
        options.addOption(Option.builder("port").desc("server port, an integer")
                .hasArg().type(Integer.class).build());
        options.addOption(Option.builder("secret").desc("secret")
                .hasArg().type(String.class).build());
        options.addOption(Option.builder("debug").desc("print debug information").build());

        CommandLineParser parser = new DefaultParser();
        return parser.parse(options, args);
    }

    public void run(String[] args) {
        CommandLine cmd = null;
        Static.configSecurity("keystore/server.jks");
        try {
            cmd = getOptions(args);
        } catch (ParseException e) {
            Logging.logInfo("Command line arguments missing or invalid, please try again");
            return;
        }

        // set debug
        Logging.setEnablePrint(cmd.hasOption("debug"));
        if (cmd.hasOption("debug")) {
            Logging.logInfo("setting debug on");
        }

        // determine secret
        String secret = null;
        if (!cmd.hasOption("secret")) {
            // generate random secret
            // from http://stackoverflow.com/questions/41107/how-to-generate-a-random-alpha-numeric-string
            SecureRandom random = new SecureRandom();
            secret = new BigInteger(130, random).toString(32);
        } else {
            secret = cmd.getOptionValue("secret");
        }

        Logging.logInfo("Server secret: " + secret);

        // determine host
        InetAddress address = null;
        try {
            address = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            System.out.println("Cannot resolve local host, exiting...");
            return;
        }
        String host = cmd.getOptionValue("advertisedhostname", address.getHostName());

        // set intervals
        int connectionIntervalLimit;
        if (cmd.hasOption("connectionintervallimit")) {
            connectionIntervalLimit = Integer.parseInt(cmd.getOptionValue("connectionintervallimit"));
        } else {
            connectionIntervalLimit = Static.DEFAULT_CONNECTION_INTERVAL;
        }
        int exchangeInterval;
        if (cmd.hasOption("exchangeinterval")) {
            exchangeInterval = Integer.parseInt(cmd.getOptionValue("exchangeinterval"));
        } else {
            exchangeInterval = Static.DEFAULT_EXCHANGE_INTERVAL;
        }

        // set ports
        int port;
        if (cmd.hasOption("port")) {
            port = Integer.parseInt((cmd.getOptionValue("port")));
        } else {
            port = Static.DEFAULT_PORT;
        }

        int sport;
        if (cmd.hasOption("sport")) {
            sport = Integer.parseInt((cmd.getOptionValue("sport")));
        } else {
            sport = Static.DEFAULT_SPORT;
        }

        secureListener = new ListenerThread(
                connectionIntervalLimit,
                exchangeInterval,
                secret,
                host,
                sport,
                true,
                secureServerList,
                resourceStorage,
                subscriptionManager);
        secureListener.start();
        insecureListener = new ListenerThread(
                connectionIntervalLimit,
                exchangeInterval,
                secret,
                host,
                port,
                false,
                insecureServerList,
                resourceStorage,
                subscriptionManager);
        insecureListener.start();
    }

    /**
     * Main function of Server, used through command line
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        new Server().run(args);
    }
}