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
public class SubscriptionHandler extends Thread {
    private Socket client;
    private DataOutputStream outputStream;
    private Resource template;
    private Queue<Resource> toSend = new LinkedList<>();
    private boolean running;
    private SubscriptionManager subscriptionManager;
    private EzServer self;

    /**
     * Constructor for SubscriptionHandler
     *
     * @param client              the client waiting for subscription
     * @param template            the given resource template
     * @param subscriptionManager the subscription manager
     * @param self                the server giving subscription info
     */
    public SubscriptionHandler(Socket client, Resource template, SubscriptionManager subscriptionManager, EzServer self) {
        this.client = client;
        this.template = template;
        this.subscriptionManager = subscriptionManager;
        this.self = self;
    }

    /**
     * Get the resource template
     *
     * @return the resource template
     */
    public Resource getTemplate() {
        return template;
    }

    /**
     * Get if the thread is running
     *
     * @return a boolean indicates whether the thread is running or not
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * terminate the thread
     */
    public void terminate() {
        this.running = false;
        this.interrupt();
    }

    /**
     * notify the client that new resource is ready
     *
     * @param resource the resource for the client
     */
    public void onResourceArrived(Resource resource) {
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
    /**
     * Run the SubscriptionHandler
     */
    public void run() {
        try {
            this.running = true;
            this.outputStream = new DataOutputStream(client.getOutputStream());
            this.running = true;
            while (running) {
                Resource resource;

                // wait until there is resource to send
                synchronized (this) {
                    while (toSend.isEmpty()) {
                        wait();
                    }

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
            Logging.logInfo("Closing subscription connection with client "
                    + client.getRemoteSocketAddress());
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
