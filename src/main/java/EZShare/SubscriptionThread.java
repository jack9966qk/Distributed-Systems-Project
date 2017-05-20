package EZShare;

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
    private SubscriptionManager subscriptionManager;
    private EzServer self;

    public SubscriptionThread(Socket client, Resource template, SubscriptionManager subscriptionManager, EzServer self) {
        this.client = client;
        this.template = template;
        this.subscriptionManager = subscriptionManager;
        this.self = self;
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
        System.out.println("RESOURCE ARRIVED");
        synchronized (this) {
            if (resource.getOwner() == null || resource.getOwner().equals("")) {
                resource = resource.ownerHidden();
            }
            if (resource.getEzserver() == null || resource.getEzserver().equals("")) {
                resource = resource.ezServerAdded(self);
            }
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
                System.out.println("WAIT FOR RESOURCE");
                synchronized (this) {
                    while (toSend.isEmpty()) {
                        wait();
                    }

                    System.out.println("RESOURCE REMOVED");
                    resource = toSend.remove();
                }

                if (resource.matchesTemplate(template)) {
                    Static.sendJsonUTF(outputStream, resource.toJson());
                    subscriptionManager.incrementCount(client);
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
