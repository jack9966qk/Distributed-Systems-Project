package EzShare;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Static class for managing subscriptions on server side
 * Created by Jack on 7/5/2017.
 */
public class Subscription {
    // count result size for each client
    public static Map<SocketAddress, Integer> resultSizes = new HashMap<>();

    // threads for connection with all clients for subscription, id is only unique from the same client
    public static Map<SocketAddress, Map<String, SubscriptionThread>> subscriptionThreads = new HashMap<>();

    // threads for all server side subscription requests (relay)
    public static Map<Resource, ClientSubscriptionThread> relayThreads = new HashMap<>();

    public static void addSubscriptionThread(Socket client, Resource template, String id) {
        SocketAddress address = client.getRemoteSocketAddress();
        if (!subscriptionThreads.containsKey(address)) {
            subscriptionThreads.put(address, new HashMap<>());
        }
        SubscriptionThread thread = new SubscriptionThread(client, template);
        subscriptionThreads.get(address).put(id, thread);
        if (!resultSizes.containsKey(address)) {
            resultSizes.put(address, 0);
        }
        thread.start();
    }

    public static Integer removeSubscriptionThread(Socket client, String id) throws ServerException {
        SocketAddress address = client.getRemoteSocketAddress();
        if (!subscriptionThreads.containsKey(address)) {
            throw new ServerException("Subscription does not exist");
        }
        Map<String, SubscriptionThread> threads = subscriptionThreads.get(address);
        if (!threads.containsKey(id) ||threads.size() == 0) {
            throw new ServerException("Subscription does not exist");
        }
        SubscriptionThread thread = threads.get(id);
        Resource template = thread.getTemplate();
        thread.terminate();
        threads.remove(id);

        // remove relay if not required by other subscription threads
        boolean relayRequired = false;
        for (Map.Entry<SocketAddress, Map<String, SubscriptionThread>> entry :
                subscriptionThreads.entrySet()) {
            for (Map.Entry<String, SubscriptionThread> threadEntry :
                    entry.getValue().entrySet()) {
                if (threadEntry.getValue().getTemplate() == template) {
                    // another subscription needs this resource template
                    relayRequired = true;
                    break;
                }
            }
        }

        if (!relayRequired) {
            // relay for this specific template can be stopped
            removeRelaySubscriptionThread(template);
        }

        if (threads.size() == 0) {
            Integer resultSize = resultSizes.get(address);
            return resultSize;
        } else {
            return null;
        }
    }

    public static void addRelaySubscriptionThread(Resource template, Socket socket) {
        if (!relayThreads.containsKey(template)) {
            ClientSubscriptionThread relayThread = new ClientSubscriptionThread(socket, "123");
            relayThreads.put(template, relayThread);
        }
    }

    public static void removeRelaySubscriptionThread(Resource template) {
        ClientSubscriptionThread thread = relayThreads.get(template);
        try {
            Client.unsubscribe(thread.getServer(), thread.getSubId());
        } catch (IOException e) {
            // TODO
            e.printStackTrace();
        }
    }

    public static Set<Resource> getSubscriptionTemplates() {
        Set<Resource> templates = new HashSet<>();
        for (SubscriptionThread thread : getSubscriptionThreads()) {
            templates.add(thread.getTemplate());
        }
        return templates;
    }

    public static Set<SubscriptionThread> getSubscriptionThreads() {
        Set<SubscriptionThread> threads = new HashSet<>();
        for (Map.Entry<SocketAddress, Map<String, SubscriptionThread>> entry :
                subscriptionThreads.entrySet()) {
            for (Map.Entry<String, SubscriptionThread> threadEntry :
                    entry.getValue().entrySet()) {
                threads.add(threadEntry.getValue());
            }
        }
        return threads;
    }
}
