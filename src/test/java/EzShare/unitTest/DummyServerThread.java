package EzShare.unitTest;

import com.google.gson.JsonObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Created by Zheping on 2017/4/22.
 */
public class DummyServerThread extends Thread {

    private DataInputStream inputStream;
    private DataOutputStream outputStream;

    public DummyServerThread(Socket socket) {

        try {
            this.inputStream = new DataInputStream(socket.getInputStream());
            this.outputStream = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {

        try {
            String request = inputStream.readUTF();
            outputStream.writeUTF("Got request" + request);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void respondSuccess() throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("response", "success");
        outputStream.writeUTF(json.toString());
    }
}
