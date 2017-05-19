package EZShare;

import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
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

    public ListenerThread(int connectionIntervalLimit, int exchangeInterval, String secret,
                          String host, int port, boolean secure, ServerList serverList, ResourceStorage resourceStorage) {
        this.connectionIntervalLimit = connectionIntervalLimit;
        this.exchangeInterval = exchangeInterval;
        this.secret = secret;
        this.host = host;
        this.port = port;
        this.secure = secure;
        this.serverList = serverList;
        this.resourceStorage = resourceStorage;
    }

    /**
     * Terminate server activity (for testing)
     */
    public void terminate() {
        this.running = false;
        this.interrupt();
    }


    @Override
    public void run() {
        self = new EzServer(host, port);
        ServerSocket listenSocket = null;
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
                //TODO relay and exchange
                // wait for new client
                Logging.logInfo("Port " + port + " listening for new connection");
                Socket clientSocket = listenSocket.accept();
//                SocketAddress clientAddress = clientSocket.getRemoteSocketAddress();
                i++;
                Logging.logInfo("Port " + port + " received connection " + i);
                // start a new thread handling the client
                // TODO limitation on total number of threads
                ServiceThread c = new ServiceThread(lastConnectionTime, clientSocket,
                        secret, resourceStorage, serverList, self, secure);
                c.start();
                sleep(connectionIntervalLimit);
            }
        } catch (InterruptedException e) {
            if (running) {
                e.printStackTrace();
            } else {
                try {
                    listenSocket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                Logging.logInfo("Server shutting down...");
            }
        } catch (BindException e) {
            Logging.logInfo("Port already taken, exiting...");
        } catch (Exception e) {
            Logging.logInfo("Unknown Exception in Listener Thread, exiting...");
        }
    }
}
