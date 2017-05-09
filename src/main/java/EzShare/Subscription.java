package EzShare;

import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Static class for managing subscriptions on server side
 * Created by Jack on 7/5/2017.
 */
public class Subscription {
    public static Map<String, SubscriptionThread> subscriptionThreads = new HashMap<>();
    public static Map<Resource, ClientSubscriptionThread> relaySubscriptionThreads = new HashMap<>();

    public static void addRelaySubscriptionThread(Resource template, Socket socket) {
        if (!relaySubscriptionThreads.containsKey(template)) {
            ClientSubscriptionThread relayThread = new ClientSubscriptionThread(socket, "123");
            relaySubscriptionThreads.put(template, relayThread);
        }
    }

    public static Set<Resource> getSubscriptionTemplates() {
        Set<Resource> templates = new HashSet<>();
        for (SubscriptionThread thread : subscriptionThreads.values()) {
            templates.add(thread.getTemplate());
        }
        return templates;
    }
}
