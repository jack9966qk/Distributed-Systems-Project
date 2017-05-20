package EZShare;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class for managing subscriptions on server side
 * Created by Jack on 7/5/2017.
 */
public class SubscriptionManager {
    // count result size for each client
    public Map<String, Integer> resultSizes = new HashMap<>();

    // threads for connection with all clients for subscription, id is only unique from the same client
    public Map<String, Map<String, SubscriptionThread>> subscriptionThreads = new HashMap<>();

    // threads for all server side subscription requests (relay)
    public Set<ClientSubscriptionThread> relayThreads = new HashSet<>();

    public void addSubscriptionThread(Socket client, Resource template, String id, EzServer self) {
        InetSocketAddress address = (InetSocketAddress) client.getRemoteSocketAddress();
        if (!subscriptionThreads.containsKey(address.getHostName())) {
            subscriptionThreads.put(address.getHostName(), new HashMap<>());
        }
        SubscriptionThread thread = new SubscriptionThread(client, template, this, self);
        subscriptionThreads.get(address.getHostName()).put(id, thread);
        if (!resultSizes.containsKey(address.getHostName())) {
            resultSizes.put(address.getHostName(), 0);
        }
        thread.start();
    }

    public void incrementCount(Socket client) {
        InetSocketAddress address = (InetSocketAddress) client.getRemoteSocketAddress();
        Integer size = resultSizes.get(address.getHostName());
        resultSizes.put(address.getHostName(), size + 1);
    }

    public Integer removeSubscriptionThread(Socket client, String id) throws ServerException, IOException {
        InetSocketAddress address = (InetSocketAddress) client.getRemoteSocketAddress();
        if (!subscriptionThreads.containsKey(address.getHostName())) {
            throw new ServerException("SubscriptionManager does not exist");
        }
        Map<String, SubscriptionThread> threads = subscriptionThreads.get(address.getHostName());
        if (!threads.containsKey(id)) {
            throw new ServerException("SubscriptionManager does not exist");
        }
        SubscriptionThread thread = threads.get(id);
        Resource template = thread.getTemplate();
        thread.terminate();
        threads.remove(id);

        // remove relay if not required by other subscription threads
        boolean relayRequired = false;
        for (Map.Entry<String, Map<String, SubscriptionThread>> entry :
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
            removeRelaySubscriptions(template);
        }

        if (threads.size() == 0) {
            return resultSizes.get(address.getHostName());
        } else {
            return null;
        }
    }

    public void addRelaySubscription(EzServer ezServer, Resource template) throws IOException {
        if (relayThreads.stream().filter(
            t -> t.getEzServer().equals(ezServer) && t.getTemplate().equals(template)
        ).count() == 0) {
            // if not exist, create new relay thread
            Socket socket = new Socket(ezServer.getHostname(), ezServer.getPort());
            String id = IdGenerator.getIdGeneartor().generateId();
            ClientSubscriptionThread thread = Client.makeClientSubscriptionThread(socket, false, id, template, this);
            if (thread != null) {
                thread.start();
                relayThreads.add(thread);
            }
        }
    }

    public void removeRelaySubscriptions(Resource template) throws IOException {
        List<ClientSubscriptionThread> toRemove = relayThreads.stream().filter(
                t -> t.getTemplate().equals(template)
        ).collect(Collectors.toList());
        for (ClientSubscriptionThread thread : toRemove) {
            Socket socket = Client.connectToServer(
                    thread.getEzServer().getHostname(), thread.getEzServer().getPort(), Static.DEFAULT_TIMEOUT);
            Client.unsubscribe(thread, socket, Static.DEFAULT_TIMEOUT);
        }
        relayThreads.removeAll(toRemove);
    }

    public Set<Resource> getSubscriptionTemplates() {
        Set<Resource> templates = new HashSet<>();
        for (SubscriptionThread thread : getSubscriptionThreads()) {
            templates.add(thread.getTemplate());
        }
        return templates;
    }

    public Set<SubscriptionThread> getSubscriptionThreads() {
        Set<SubscriptionThread> threads = new HashSet<>();
        for (Map.Entry<String, Map<String, SubscriptionThread>> entry :
                subscriptionThreads.entrySet()) {
            for (Map.Entry<String, SubscriptionThread> threadEntry :
                    entry.getValue().entrySet()) {
                threads.add(threadEntry.getValue());
            }
        }
        return threads;
    }
}
