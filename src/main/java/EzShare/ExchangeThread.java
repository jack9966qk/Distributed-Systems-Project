package EzShare;

import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Server thread for sending automatic exchange requests in fixed time interval
 * Created by Jack on 23/3/2017.
 */
public class ExchangeThread extends Thread {
    private int interval;
    private Set<EzServer> serverList;
    private Date lastExchangeTime;
    private boolean secure;

    /**
     * Create a new exchange thread
     *  @param interval   time interval between exchange being performed
     * @param serverList a reference of list of servers to send exchange
     * @param secure
     */
    public ExchangeThread(int interval, Set<EzServer> serverList, boolean secure) {
        this.interval = interval;
        this.serverList = serverList;
        this.secure = secure;
        lastExchangeTime = new Date();
    }

    /**
     * Return true if thread is running, false otherwise (mainly for testing)
     *
     * @return true if thread is running, false otherwise
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Terminate ExchangeThread (mainly for testing)
     */
    public void terminate() {
        this.running = false;
        this.interrupt();
    }

    private boolean running = false;

    /**
     * Perform an exchange request to all servers in serverList
     */
    private void exchange() {
        Logging.logInfo("send exchange request to servers: " + serverList);
        Set<EzServer> allServers = new HashSet<>();
        allServers.addAll(serverList);
        allServers.add(Server.self);
        // manually synchronize serverList since it is not thread safe for iteration
        // make a copy to avoid deadlock when requesting self
        // (ExchangeThread wait for ServiceThread response, ServiceThread wait for serverList object lock)
        // http://stackoverflow.com/questions/1775717/explain-synchronization-of-collections-when-iterators-are-used
        HashSet<EzServer> servers = new HashSet<>();
        synchronized (serverList) {
            servers.addAll(serverList);
        }
        for (EzServer server : servers) {
            Logging.logInfo("sending to " + server);
            Socket socket = null;
            try {
                socket = Client.connectToServer(server.hostname, server.port, Static.DEFAULT_TIMEOUT, secure);
                Client.exchange(socket, allServers.toArray(new EzServer[serverList.size()]));
                socket.close();
            } catch (ConnectException e) {
                Logging.logInfo("Failed to connect to " + server + ". Remove server from exchange list");
                serverList.remove(server);
            } catch (IOException e) {
                Logging.logInfo("Unknown error communicating with " + server);
            }
        }
    }

    /**
     * Run the exchange thread
     */
    @Override
    public void run() {
        running = true;

        while (running) {
            if (lastExchangeTime != null) {
                long timeSinceLastExchange = new Date().getTime() - lastExchangeTime.getTime();
                long timeToWait = Math.max(0, interval - timeSinceLastExchange);
                try {
                    sleep(timeToWait);
                } catch (InterruptedException e) {
                    if (running) {
                        Logging.logInfo("Exchange thread sleep interrupted");
                    } else {
                        break;
                    }
                }
            }

            lastExchangeTime = new Date();
            exchange();
        }

        Logging.logInfo("exchange thread terminated");
    }
}
