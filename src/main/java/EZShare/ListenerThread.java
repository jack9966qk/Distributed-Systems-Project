package EZShare;

import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.net.*;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by Jack on 18/5/2017.
 */
public class ListenerThread extends Thread {
    private final int connectionIntervalLimit;
    private final int exchangeInterval;
    private final String secret;
    private final String host;
    private final int port;
    private final boolean secure;
    private final ServerList serverList;
    private ResourceStorage resourceStorage;
    EzServer self;
    public HashMap<SocketAddress, Date> lastConnectionTime = new HashMap<>();
    boolean running = false;
    private ServerSocket listenSocket;
    private SubscriptionManager manager;

    /**
     *
     * @param connectionIntervalLimit the maximum connection time
     * @param exchangeInterval the interval of exchange
     * @param secret the secret specified by the Server
     * @param host listening host
     * @param port listening port
     * @param secure secure flag to determine whether the socket shall be secure
     * @param serverList a list of Servers that this Server has acknowledge
     * @param resourceStorage the resource storage for the Server
     * @param manager subscription manager
     */
    public ListenerThread(int connectionIntervalLimit, int exchangeInterval, String secret,
                          String host, int port, boolean secure, ServerList serverList, ResourceStorage resourceStorage, SubscriptionManager manager) {
        this.connectionIntervalLimit = connectionIntervalLimit;
        this.exchangeInterval = exchangeInterval;
        this.secret = secret;
        this.host = host;
        this.port = port;
        this.secure = secure;
        this.serverList = serverList;
        this.resourceStorage = resourceStorage;
        this.manager = manager;
    }

    /**
     * Terminate server activity (for testing)
     */
    public void terminate() {
        Logging.logInfo("Shutting down port " + port);
        this.running = false;
        this.interrupt();
        try {
            ServerSocket socket = listenSocket;
            listenSocket = null;
            socket.close();
        } catch (IOException e) {
            Logging.logInfo("IOException closing socket");
        }
    }


    @Override
    public void run() {
        self = new EzServer(host, port);
        listenSocket = null;
        try {
            // for sending exchange request to other servers
            ExchangeThread exchangeThread = new ExchangeThread(exchangeInterval, serverList, secure, self);
            exchangeThread.start();

            if (secure) {
                listenSocket = SSLServerSocketFactory.getDefault().createServerSocket(port);
            } else {
                listenSocket = new ServerSocket(port);
            }
            int i = 0;
            Logging.logInfo( (secure ? "Secure " : "") + "Port " + port + " now available");
            running = true;
            while (running) {
                // wait for new client
                Logging.logInfo("Port " + port + " listening for new connection");
                Socket clientSocket = listenSocket.accept();
                i++;
                Logging.logInfo("Port " + port + " received connection " + i);
                // start a new thread handling the client
                // TODO limitation on total number of threads
                ServiceThread c = new ServiceThread(lastConnectionTime, clientSocket,
                        secret, resourceStorage, serverList, self, secure, manager);
                c.start();
                sleep(connectionIntervalLimit);
            }
        } catch (BindException e) {
            Logging.logInfo("Port already taken, exiting...");
        } catch (InterruptedException | SocketException e) {
            if (running) {
                e.printStackTrace();
            } else {
                try {
                    if (listenSocket != null) {
                        listenSocket.close();
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                Logging.logInfo("Port " + port  + " closed.");
            }
        } catch (Exception e) {
            Logging.logInfo("Unknown Exception in Listener Thread, exiting...");
        }
    }
}
