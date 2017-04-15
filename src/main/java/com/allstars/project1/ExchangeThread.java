package com.allstars.project1;

import java.io.IOException;
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

    public boolean isRunning() {
        return running;
    }

    public void terminate() {
        this.running = false;
        this.interrupt();
    }

    private boolean running = false;

    private void exchange() throws IOException {
        Debug.infoPrintln("send exchange request to servers: " + serverList);
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
            Debug.infoPrintln("sending to " + server);
            Socket socket = Client.connectToServer(server.hostname, server.port);
            Client.exchange(socket, allServers.toArray(new EzServer[serverList.size()]));
            socket.close();
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

            try {
                exchange();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Debug.infoPrintln("exchange thread terminated");
    }
}
