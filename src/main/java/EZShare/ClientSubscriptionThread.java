package EZShare;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by Zheping on 2017/5/7.
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

    public String getSubId() {return id;}
    public boolean isRunning() {return running;}
    public Socket getSocket() {
        return server;
    }
    public EzServer getEzServer() {
        return new EzServer(host, port);
    }
    public Resource getTemplate() {
        return template;
    }

    public ClientSubscriptionThread(Socket server, String id, Resource template) {

        this.server = server;
        this.id = id;
        this.template = template;

        InetSocketAddress socketAddress = (InetSocketAddress) server.getRemoteSocketAddress();
        this.port = socketAddress.getPort();

        InetAddress inetAddress = socketAddress.getAddress();
        this.host = inetAddress.getHostAddress();
    }

    public void terminate() {
        running = false;
        this.interrupt();
    }

    @Override
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
                for (SubscriptionThread thread : Subscription.getSubscriptionThreads()) {
                    thread.onResourceArrived(resource);
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
