package EzShare;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Server side handler thread for subscriptions, waits for new resources and sends matches to clients
 * Created by Jack on 6/5/2017.
 */
public class SubscriptionThread extends Thread {
    Socket client;
    DataOutputStream outputStream;
    Resource template;
    Queue<Resource> toSend = new LinkedList<>();

    public static void addThread(SubscriptionThread thread, String id) {
        Subscription.subscriptionThreads.put(id, thread);
        thread.start();
    }

    public static void removeThread(String id) {
        SubscriptionThread thread = Subscription.subscriptionThreads.get(id);
        thread.terminate();
        Subscription.subscriptionThreads.remove(id);
    }

    public SubscriptionThread(Socket client, Resource template) {
        this.client = client;
        this.template = template;
    }

    public Resource getTemplate() {
        return template;
    }

    boolean running;

    public boolean isRunning() {
        return running;
    }

    public void terminate() {
        this.running = false;
        this.interrupt();
    }

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
            if (running) {
                e.printStackTrace();
            }
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
