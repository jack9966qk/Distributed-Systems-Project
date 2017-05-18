package EzShare;

import java.io.IOException;
import java.math.BigInteger;
import java.net.*;
import java.security.SecureRandom;
import java.util.*;

import org.apache.commons.cli.*;

/**
 * EzShare server implementation, has a main method to be used through command line
 */
public class Server {
    public static ResourceStorage resourceStorage = new ResourceStorage();
    public static Set<EzServer> secureserverList = Collections.synchronizedSet(new HashSet<>());
    public static Set<EzServer> insecureserverList = Collections.synchronizedSet(new HashSet<>());
    public static HashMap<SocketAddress, Date> lastConnectionTime = new HashMap<>();
    public static EzServer self;
    public static ListenerThread insecureListener;
    public static ListenerThread secureListener;

    private static Thread mainThread;

    public static boolean isRunning() {
        if (insecureListener != null) {
            if (insecureListener.running) {
                return true;
            }
        }
        if (secureListener != null) {
            return secureListener.running;
        }
        return false;
    }

    /**
     * Terminate server activity (for testing)
     */
    public static void stop() {
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
    public static void waitUntilReady() throws InterruptedException {
        while (!Server.isRunning()) { // busy waiting
            Thread.sleep(100);
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

    /**
     * Main function of Server, used through command line
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        CommandLine cmd = null;
        Static.configSecurity("keystore/server.jks");
        try {
            cmd = getOptions(args);
        } catch (ParseException e) {
            Logging.logInfo("Command line arguments missing or invalid, please try again");
            return;
        }

        mainThread = Thread.currentThread();

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

        try {
            // determine host
            InetAddress address = InetAddress.getLocalHost();
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
                    secureserverList);
            secureListener.start();
            insecureListener = new ListenerThread(
                    connectionIntervalLimit,
                    exchangeInterval,
                    secret,
                    host,
                    port,
                    false,
                    insecureserverList);
            insecureListener.start();
        } catch (IOException e) {
            Logging.logInfo("Unknown IOException in Server main thread, exiting...");
        } catch (Exception e) {
            Logging.logInfo("Unknown Exception in Server main thread, exiting...");
        }
    }
}