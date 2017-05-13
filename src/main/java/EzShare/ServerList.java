package EzShare;

import java.io.IOException;
import java.net.Socket;
import java.util.*;

/**
 * Created by Jack on 6/5/2017.
 */
public class ServerList {
    public Set<EzServer> getServers() {
        return servers;
    }

    Set<EzServer> servers = Collections.synchronizedSet(new HashSet<>());

    public boolean addAll(Collection<? extends EzServer> c) {
        return servers.addAll(c);
    }

    public int size() {
        return servers.size();
    }

    public boolean add(EzServer ezServer) throws IOException {
        if (!servers.contains(ezServer)) {
            for (Resource template : Subscription.getSubscriptionTemplates()) {
                Subscription.addRelaySubscriptionThread(template, ezServer.getHostname(), ezServer.getPort());
            }
        }
        return servers.add(ezServer);
    }

    public boolean remove(Object o) {
        return servers.remove(o);
    }
}
