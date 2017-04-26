package com.allstars.project1;

import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Jack on 23/3/2017.
 */
public class ExchangeThread extends Thread {
    private int interval;
    private Set<EzServer> serverList;

    public ExchangeThread(int interval, Set<EzServer> serverList) {
        this.interval = interval;
        this.serverList = serverList;
    }

    /**
     * Return true if thread is running, false otherwise
     * @return true if thread is running, false otherwise
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Terminate ExchangeThread
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
                socket = Client.connectToServer(server.hostname, server.port, Static.DEFAULT_TIMEOUT);
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

    @Override
    public void run() {
        running = true;

        while (running) {
            try {
                sleep(interval);
            } catch (InterruptedException e) {
                if (!this.running) {
                    return;
                }
                e.printStackTrace();
            }

            exchange();
        }

        Logging.logInfo("exchange thread terminated");
    }
}
