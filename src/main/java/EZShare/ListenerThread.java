package EZShare;

import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;

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
    boolean running = false;

    public ListenerThread(int connectionIntervalLimit, int exchangeInterval, String secret,
                          String host, int port, boolean secure, ServerList serverList) {
        this.connectionIntervalLimit = connectionIntervalLimit;
        this.exchangeInterval = exchangeInterval;
        this.secret = secret;
        this.host = host;
        this.port = port;
        this.secure = secure;
        this.serverList = serverList;
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
        Server.self = new EzServer(host, port);
        ServerSocket listenSocket = null;
        try {
            // for sending exchange request to other servers
            ExchangeThread exchangeThread = new ExchangeThread(exchangeInterval, Server.insecureServerList, secure);
            exchangeThread.start();

            if (secure) {
                System.out.println("creating secure socket at port " + port);
                listenSocket = SSLServerSocketFactory.getDefault().createServerSocket(port);
            } else {
                System.out.println("creating insecure socket at port " + port);
                listenSocket = new ServerSocket(port);
            }
            int i = 0;
            Logging.logInfo("Server initialisation complete");
            running = true;
            while (running) {
                //TODO relay and exchange
                // wait for new client
                Logging.logInfo("Server listening for a connection");
                Socket clientSocket = listenSocket.accept();
//                SocketAddress clientAddress = clientSocket.getRemoteSocketAddress();
                i++;
                Logging.logInfo("Received connection " + i);
                // start a new thread handling the client
                // TODO limitation on total number of threads
                ServiceThread c = new ServiceThread(Server.lastConnectionTime, clientSocket,
                        secret, Server.resourceStorage, serverList, Server.self, secure);
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
