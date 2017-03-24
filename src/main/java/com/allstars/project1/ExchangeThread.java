package com.allstars.project1;

import java.util.List;

/**
 * Created by Jack on 23/3/2017.
 */
public class ExchangeThread extends Thread {
    private int interval;
    private List<EzServer> serverList;

    public ExchangeThread(int interval, List<EzServer> serverList) {
        this.interval = interval;
        this.serverList = serverList;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    private boolean running = false;

    public static void exchange() {
        // TODO
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

            exchange();
        }

    }
}
