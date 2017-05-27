package EZShare;

import java.io.IOException;
import java.util.*;

/**
 * List storing EZShare servers
 * Created by Jack on 6/5/2017.
 */
public class ServerList {
    private SubscriptionManager subscriptionManager = null;

    public Set<EzServer> getServers() {
        return servers;
    }

    /**
     *
     * @param subscriptionManager
     */
    public ServerList(SubscriptionManager subscriptionManager) {
        this.subscriptionManager = subscriptionManager;
    }

    /**
     *
     */
    public ServerList() {}

    Set<EzServer> servers = Collections.synchronizedSet(new HashSet<>());

    /**
     *
     * @return
     */
    public int size() {
        return servers.size();
    }

    /**
     *
     * @param ezServer
     * @return
     * @throws IOException
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
     *
     * @param o
     * @return
     */
    public boolean remove(Object o) {
        return servers.remove(o);
    }
}
