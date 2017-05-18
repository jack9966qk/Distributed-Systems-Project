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
            this.running = true;
            this.outputStream = new DataOutputStream(client.getOutputStream());
            this.running = true;
            while (running) {
                Resource resource;
                synchronized (this) {
                    while (toSend.isEmpty()) {
                        wait();
                    }

                    resource = toSend.remove();
                }

                if (resource.matchesTemplate(template)) {
                    Static.sendJsonUTF(outputStream, resource.toJson());
                    Subscription.incrementCount(client);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            if (running) {
                e.printStackTrace();
            }
        } finally {
            Logging.logInfo("Closing subscription connection with client " + client.getRemoteSocketAddress());
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
