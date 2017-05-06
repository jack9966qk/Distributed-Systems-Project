package EzShare;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by Jack on 6/5/2017.
 */
public class SubscriptionThread extends Thread implements ResourceStorageListener, SubscriptionListener {
    Socket client;
    DataOutputStream outputStream;
    Resource template;
    Queue<Resource> toSend = new LinkedList<>();

    public SubscriptionThread(Socket client, Resource template) {
        this.client = client;
        this.template = template;
    }

    boolean running;

    public boolean isRunning() {
        return running;
    }

    public void terminate() {
        this.running = false;
        this.interrupt();
    }

    @Override
    public void onResourceAdded(Resource resource) {
        synchronized (this) {
            toSend.add(resource);
            notifyAll();
        }
    }

    @Override
    public void onResourceArrived(Resource resource) {
        synchronized (this) {
            toSend.add(resource);
            notifyAll();
        }
    }

    @Override
    public void run() {
        try {
            this.outputStream = new DataOutputStream(client.getOutputStream());
            while (running) {
                Resource resource;
                synchronized (this) {
                    while (toSend.isEmpty()) {
                        wait();
                    }

                    resource = toSend.remove();
                }

                Static.sendJsonUTF(outputStream, resource.toJson());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
