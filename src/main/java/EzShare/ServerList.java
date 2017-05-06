package EzShare;

import java.util.*;

/**
 * Created by Jack on 6/5/2017.
 */
public class ServerList {
    List<ServerListListener> listeners = new ArrayList<>();

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

    public boolean add(EzServer ezServer) {
        for (ServerListListener listener: listeners) {
            listener.onEzServerAdded(ezServer);
        }
        return servers.add(ezServer);
    }

    public boolean remove(Object o) {
        return servers.remove(o);
    }
}
