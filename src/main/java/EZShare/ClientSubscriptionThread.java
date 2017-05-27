package EZShare;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * EZServer client subscription thread implementation.
 */
public class ClientSubscriptionThread extends Thread {

    private Socket server;
    private String id;
    private String host;
    private int port;
    private Resource template;
    private DataOutputStream out;
    private DataInputStream in;
    private boolean running;
    private SubscriptionManager manager;

    /**
     * Get the subscription id
     *
     * @return the subscription id
     */
    public String getSubId() {
        return id;
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
     * Get the socket for server
     *
     * @return the server socket
     */
    public Socket getServer() {
        return server;
    }

    /**
     * Get the EzServer
     *
     * @return a new EzServer with host and port stored in this thread
     */
    public EzServer getEzServer() {
        return new EzServer(host, port);
    }

    /**
     * Get the resource template for subscription
     *
     * @return the resource template for subscription
     */
    public Resource getTemplate() {
        return template;
    }

    /**
     * Constructor for ClientSubscriptionThread
     *
     * @param server   socket for server
     * @param id       subscription id
     * @param template resource template
     * @param manager  subscription manager
     */
    public ClientSubscriptionThread(Socket server, String id, Resource template, SubscriptionManager manager) {

        this.server = server;
        this.id = id;
        this.template = template;
        this.manager = manager;

        // get the port from the socket of server
        InetSocketAddress socketAddress = (InetSocketAddress) server.getRemoteSocketAddress();
        this.port = socketAddress.getPort();

        // get the host from the socket of server
        InetAddress inetAddress = socketAddress.getAddress();
        this.host = inetAddress.getHostAddress();
    }

    /**
     * Terminate current ClientSubscriptionThread
     */
    public void terminate() {
        running = false;
        this.interrupt();
    }

    @Override
    /**
     * Run the ClientSubscriptionThread
     */
    public void run() {
        Logging.logInfo("Subscribe thread started");

        try {
            server.setSoTimeout(0); // disable timeout waiting for resources
            out = new DataOutputStream(server.getOutputStream());
            in = new DataInputStream(server.getInputStream());
            running = true;

            while (running) {
                String response = Static.readJsonUTF(in);
                Logging.logInfo(response);
                Resource resource = Resource.fromJson(response);

                // for server doing relay: send potential match to each subscription threads
                if (manager != null) {
                    for (SubscriptionThread thread : manager.getSubscriptionThreads()) {
                        thread.onResourceArrived(resource);
                    }
                }
            }

        } catch (IOException e) {
            if (running) {
                e.printStackTrace();
            }
        } finally {
            Logging.logInfo("Closing subscription connection with server " + server.getRemoteSocketAddress());
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

}
