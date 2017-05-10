package EzShare.unitTest;

import EzShare.Server;
import org.junit.jupiter.api.Assertions;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.*;

/**
 * Created by Jack on 5/5/2017.
 */
public class ServerVerifier extends Thread {
    static int waitTime = 1000 * 30;

    String requestJson;
    String expectedResponseJson;
    Set<String> expectedResourcesJson;
    boolean testWithSunrise = false;

    boolean successful = false;

    private ServerVerifier(String requestJson, String expectedResponseJson, boolean testWithSunrise) {
        this.requestJson = requestJson;
        this.expectedResponseJson = expectedResponseJson;
        this.testWithSunrise = testWithSunrise;
    }

    private ServerVerifier(String requestJson, String expectedResponseJson, Set<String> expectedResourcesJson, boolean testWithSunrise) {
        this.requestJson = requestJson;
        this.expectedResponseJson = expectedResponseJson;
        this.testWithSunrise = testWithSunrise;
        this.expectedResourcesJson = expectedResourcesJson;
    }

    public static ServerVerifier verifyServer(String requestJson, String expectedResponseJson, boolean testWithSunrise) {
        return new ServerVerifier(requestJson, expectedResponseJson, testWithSunrise);
    }

    public static ServerVerifier verifyServer(String requestJson, String expectedResponseJson, Set<String> expectedResourcesJson, boolean testWithSunrise) {
        return new ServerVerifier(requestJson, expectedResponseJson, expectedResourcesJson, testWithSunrise);
    }

    @Override
    public void run() {
        new ServerThread("-port 9999 -secret abcd".split(" ")).start();
        try {
            Server.waitUntilReady();
            Socket socket = new Socket("localhost", 9999);
            new DataOutputStream(socket.getOutputStream()).writeUTF(requestJson);
            Verify.assertJsonEquivalent(new DataInputStream(socket.getInputStream()).readUTF(), expectedResponseJson);

            // check resources
            if (expectedResourcesJson != null) {
                Verify.checkResources(socket, expectedResourcesJson);
            }

            // check with Sunrise
            if (testWithSunrise) {
                String sunriseResponse = getResponse("sunrise.cis.unimelb.edu.au", 3780, requestJson);
                Verify.assertJsonEquivalent(expectedResponseJson, sunriseResponse);
            }

            successful = true;
        } catch (Exception e) {
            successful = false;
            e.printStackTrace();
        } finally {
            if (Server.isRunning()) {
                Server.stop();
            }
            synchronized (this) {
                notifyAll();
            }
        }
    }

    public synchronized void test() throws InterruptedException {
        this.start();
        wait(waitTime);
        Assertions.assertTrue(this.successful);
    }

    String getResponse(String host, int port, String request) throws IOException {
        Socket s = new Socket(host, port);
        new DataOutputStream(s.getOutputStream()).writeUTF(request);
        return new DataInputStream(s.getInputStream()).readUTF();
    }
}

