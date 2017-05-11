package EzShare.unitTest;

import EzShare.Static;

import javax.net.ssl.SSLSocketFactory;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

/**
 * Created by Jack on 10/5/2017.
 */
public class DummyClient extends Thread {
    String request;
    String host;
    int port;
    boolean secure = false;

    public DummyClient(String request, String host, int port, boolean secure) {
        this.request = request;
        this.host = host;
        this.port = port;
        this.secure = secure;
    }

    @Override
    public void run() {
        Socket socket = null;
        try {
            if (secure) {
                socket = SSLSocketFactory.getDefault().createSocket(host, port);
            } else {
                socket = new Socket(host, port);
            }
            socket.setSoTimeout(1000 * 30);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            Static.sendJsonUTF(out, request);
            DataInputStream in = new DataInputStream(socket.getInputStream());
            Static.readJsonUTF(in);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
