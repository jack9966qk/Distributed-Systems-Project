package EzShare.unitTest;

import EzShare.Server;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Assertions;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.*;

/**
 * Created by Jack on 5/5/2017.
 */
public class ProxyVerifier extends Thread {
    static int waitTime = 1000 * 30;

    String requestJson;
    String expectedResponseJson;
    Set<String> expectedResourcesJson;
    boolean testWithSunrise = false;

    boolean successful = false;

    private ProxyVerifier(String requestJson, String expectedResponseJson, boolean testWithSunrise) {
        this.requestJson = requestJson;
        this.expectedResponseJson = expectedResponseJson;
        this.testWithSunrise = testWithSunrise;
    }

    private ProxyVerifier(String requestJson, String expectedResponseJson, Set<String> expectedResourcesJson, boolean testWithSunrise) {
        this.requestJson = requestJson;
        this.expectedResponseJson = expectedResponseJson;
        this.testWithSunrise = testWithSunrise;
        this.expectedResourcesJson = expectedResourcesJson;
    }

    public static ProxyVerifier verifyServer(String requestJson, String expectedResponseJson, boolean testWithSunrise) {
        return new ProxyVerifier(requestJson, expectedResponseJson, testWithSunrise);
    }

    public static ProxyVerifier verifyServer(String requestJson, String expectedResponseJson, Set<String> expectedResourcesJson, boolean testWithSunrise) {
        return new ProxyVerifier(requestJson, expectedResponseJson, expectedResourcesJson, testWithSunrise);
    }

    @Override
    public void run() {
        new ServerThread("-port 9999 -secret abcd".split(" ")).start();
        try {
            Server.waitUntilReady();
            Socket socket = new Socket("localhost", 9999);
            new DataOutputStream(socket.getOutputStream()).writeUTF(requestJson);
            assertJsonEquivalent(new DataInputStream(socket.getInputStream()).readUTF(), expectedResponseJson);

            // check resources
            if (expectedResourcesJson != null) {
                checkResources(socket);
            }

            // check with Sunrise
            if (testWithSunrise) {
                String sunriseResponse = getResponse("sunrise.cis.unimelb.edu.au", 3780, requestJson);
                assertJsonEquivalent(expectedResponseJson, sunriseResponse);
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

    void checkResources(Socket socket) throws Exception {
        List<String> receivedResources = new ArrayList<>();
        String receivedResultSize = null;
        DataInputStream inStream = new DataInputStream(socket.getInputStream());
        while (true) {
            String str = inStream.readUTF();
            if (str.contains("resultSize")) {
                receivedResultSize = str;
                break;
            } else {
                receivedResources.add(str);
            }
        }

        JsonObject resultSizeObj = new JsonParser().parse(receivedResultSize).getAsJsonObject();

        if (resultSizeObj.get("resultSize").getAsInt() != receivedResources.size()) {
            throw new Exception("size does not match");
        }

        for (String receivedResource : receivedResources) {
            boolean ok = false;
            for (String expectedResource : expectedResourcesJson) {
                if (areEquivalentJson(receivedResource, expectedResource)) {
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                throw new Exception("resources do not match");
            }
        }
    }

    public synchronized void test() throws InterruptedException {
        this.start();
        wait(waitTime);
        Assertions.assertTrue(this.successful);
    }

    private void removeAllNullFields(JsonObject jsonObject) {
        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            String key = entry.getKey();
            if (jsonObject.get(key).isJsonNull()) {
                jsonObject.remove(key);
            } else if (jsonObject.get(key).isJsonObject()) {
                removeAllNullFields(jsonObject.getAsJsonObject(key));
            }
        }
    }

    private boolean areEquivalentJson(String json1, String json2) {
        JsonParser parser = new JsonParser();
        JsonObject obj1 = parser.parse(json1).getAsJsonObject();
        JsonObject obj2 = parser.parse(json2).getAsJsonObject();
        removeAllNullFields(obj1);
        removeAllNullFields(obj2);
        return obj1.equals(obj2);
    }

    private void assertJsonEquivalent(String actual, String expected) throws Exception {
        if (!areEquivalentJson(actual, expected)) {
            System.err.println("ACTUAL>>>>>>>>>>>");
            System.err.println(actual);
            System.err.println("=================");
            System.err.println(expected);
            System.err.println("EXPECTED<<<<<<<<<");
            throw new Exception("assert failed");
        }
    }

    String getResponse(String host, int port, String request) throws IOException {
        Socket s = new Socket(host, port);
        new DataOutputStream(s.getOutputStream()).writeUTF(request);
        return new DataInputStream(s.getInputStream()).readUTF();
    }
}

