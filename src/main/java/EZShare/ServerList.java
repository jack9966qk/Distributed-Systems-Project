package EZShare;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * List storing EZShare servers
 * Created by Jack on 6/5/2017.
 */
public class ServerList {
    Set<EzServer> servers = Collections.synchronizedSet(new HashSet<>());
    private SubscriptionManager subscriptionManager = null;

    /**
     * Create a new server list
     *
     * @param subscriptionManager SubscriptionManager that will use the list
     */
    public ServerList(SubscriptionManager subscriptionManager) {
        this.subscriptionManager = subscriptionManager;
    }

    public Set<EzServer> getServers() {
        return servers;
    }

    /**
     * Get size of server list
     *
     * @return number of servers in list
     */
    public int size() {
        return servers.size();
    }

    /**
     * Add a server to the list
     *
     * @param ezServer server to be added
     * @return true if operation is successful, false otherwise
     * @throws IOException any network error
     */
    public boolean add(EzServer ezServer) throws IOException {
        if (!servers.contains(ezServer) && subscriptionManager != null) {
            for (Resource template : subscriptionManager.getSubscriptionTemplates()) {
                subscriptionManager.addRelaySubscription(ezServer, template);
            }
        }
        return servers.add(ezServer);
    }

    /**
     * Remove a server from list
     *
     * @param o server to be removed
     * @return true if operation is successful, false otherwise
     */
    public boolean remove(Object o) {
        return servers.remove(o);
    }
}
