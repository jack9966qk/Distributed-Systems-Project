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
    private EzServer self;

    public ExchangeThread(int interval, Set<EzServer> serverList, String host, int port) {
        this.interval = interval;
        this.serverList = serverList;
        self = new EzServer(host, port);
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    private boolean running = false;

    private void exchange() throws IOException {
        Set<EzServer> allServers = new HashSet<>();
        allServers.addAll(serverList);
        allServers.add(self);
        for (EzServer server: serverList) {
            Socket socket = Client.connectToServer(server.hostname, server.port);
            Client.exchange(socket, allServers.toArray(new EzServer[serverList.size()]));
        }
    }

    @Override
    public void run() {
        running = true;

        while (running) {
            try {
                sleep(interval);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            try {
                exchange();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
