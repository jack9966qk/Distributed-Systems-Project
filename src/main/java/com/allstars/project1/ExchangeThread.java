package com.allstars.project1;

/**
 * Created by Jack on 23/3/2017.
 */
public class ExchangeThread extends Thread {
    private int interval;

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

    public ExchangeThread(int interval) {
        this.interval = interval;
    }
}
