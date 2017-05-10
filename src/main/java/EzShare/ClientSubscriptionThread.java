package EzShare;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Created by Zheping on 2017/5/7.
 */
public class ClientSubscriptionThread extends Thread {

    private Socket server;
    private String id;

    private DataOutputStream out;
    private DataInputStream in;
    private boolean running;

    public String getSubId() {return id;}
    public boolean isRunning() {return running;}

    public Socket getServer() {
        return server;
    }

    public ClientSubscriptionThread(Socket server, String id) {
        this.server = server;
        this.id = id;
    }

    public void terminate() {
        running = false;
        this.interrupt();
    }

    @Override
    public void run() {

        try {
            out = new DataOutputStream(server.getOutputStream());
            in = new DataInputStream(server.getInputStream());
            running = true;

            while (running) {
                String response = Static.readJsonUTF(in);
                System.out.println(response);
                Resource resource = Resource.fromJson(response);

                // for server doing relay: send potential match to each subscription threads
                for (SubscriptionThread thread : Subscription.getSubscriptionThreads()) {
                    thread.onResourceArrived(resource);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

}
