package EzShare.unitTest;

import EzShare.Client;
import EzShare.Server;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

/**
 * Created by Jack on 25/4/2017.
 */
public class AdditionalTest {
    static int waitTime = 1000 * 30;

    class TestCase {
        public String[] getClientArgs() {
            return clientArgs;
        }

        public String getExpectedRequestJson() {
            return expectedRequestJson;
        }

        public String getExpectedResponseJson() {
            return expectedResponseJson;
        }

        String[] clientArgs;
        String expectedRequestJson;
        String expectedResponseJson;

        public TestCase(String[] clientArgs, String expectedRequestJson, String expectedResponseJson) {
            this.clientArgs = clientArgs;
            this.expectedRequestJson = expectedRequestJson;
            this.expectedResponseJson = expectedResponseJson;
        }
    }

    class ServerThread extends Thread {
        String[] args;

        public ServerThread(String[] args) {
            this.args = args;
        }

        public void run() {
            Server.main(args);
        }
    }

    class Verifier extends Thread {
        List<TestCase> testCases;
        boolean testWithSunrise;
        String[] serverArgs;

        public boolean isSuccessful() {
            return successful;
        }

        boolean successful = true;

        public Verifier(String[] serverArgs, List<TestCase> testCases, boolean testWithSunrise) {
            this.serverArgs = serverArgs;
            this.testCases = testCases;
            this.testWithSunrise = testWithSunrise;
        }



        public synchronized void waitForDummyServerToBeReady(int timeout) throws InterruptedException {
            wait(timeout);
        }

        private void testOne(String expectedRequestJson, String expectedResponseJson) throws Exception {
            Socket client = null;
            ServerSocket dummyServerSocket = null;
            try {
                // create a dummy server to check request from client
                synchronized (this) {
                    while (true) {
                        try {
                            dummyServerSocket = new ServerSocket(3780);
                            break;
                        } catch (BindException e) {}
                    }
                    notifyAll();
                }
                client = dummyServerSocket.accept();
                DataInputStream stream = new DataInputStream(client.getInputStream());
                String request = stream.readUTF();
                assertJsonEquivalent(request, expectedRequestJson);
                dummyServerSocket.close();

                if (expectedResponseJson == null) {
                    // no need to test server response
                    System.out.println("skip response check");
                    new DataOutputStream(client.getOutputStream()).writeUTF("{ \"response\": \"success\" }");
                    client.close();
                    return;
                }

                new ServerThread(serverArgs).start();
                Server.waitUntilReady();

                // get response from our server
                String serverRes = getResponse("localhost", 3780, request);
                assertJsonEquivalent(serverRes, expectedResponseJson);
                new DataOutputStream(client.getOutputStream()).writeUTF(serverRes);

                if (testWithSunrise) {
                    // get response from sunrise
                    String sunriseRes = getResponse("sunrise.cis.unimelb.edu.au", 3780, request);
                    System.err.println(sunriseRes);
                    assertJsonEquivalent(sunriseRes, expectedResponseJson);
                }
            } catch (Exception e) {
                throw e;
            } finally {
                if (client != null) {
                    client.close();
                }
                if (dummyServerSocket != null) {
                    dummyServerSocket.close();
                }
                if (Server.isRunning()) {
                    Server.stop();
                }
            }
        }

        public void run() {
            try {
                for (TestCase testCase: testCases) {
                    testOne(testCase.getExpectedRequestJson(), testCase.getExpectedResponseJson());
                }
            } catch (Exception e) {
                e.printStackTrace();
                successful = false;
                if (Server.isRunning()) {
                    Server.stop();
                }
            }
        }
    }

    String getResponse(String host, int port, String request) throws IOException {
        Socket s = new Socket(host, port);
        new DataOutputStream(s.getOutputStream()).writeUTF(request);
        return new DataInputStream(s.getInputStream()).readUTF();
    }

    class ServerVerifier extends Thread {

        String requestJson;
        String expectedResponseJson;
        Set<String> expectedResourcesJson;
        boolean testWithSunrise = false;

        boolean successful = false;

        public ServerVerifier(String requestJson, String expectedResponseJson, boolean testWithSunrise) {
            this.requestJson = requestJson;
            this.expectedResponseJson = expectedResponseJson;
            this.testWithSunrise = testWithSunrise;
        }

        public ServerVerifier(String requestJson, String expectedResponseJson, Set<String> expectedResourcesJson, boolean testWithSunrise) {
            this.requestJson = requestJson;
            this.expectedResponseJson = expectedResponseJson;
            this.testWithSunrise = testWithSunrise;
            this.expectedResourcesJson = expectedResourcesJson;
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

        public synchronized void test() throws InterruptedException {
            this.start();
            wait(waitTime);
            Assertions.assertTrue(this.successful);
        }
    }


    private void testWith(String[] serverArgs, String[] clientArgs, String expectedRequestJson, String expectedResponseJson, boolean testWithSunrise) throws InterruptedException {
        List<TestCase> testCases = new ArrayList<>();
        testCases.add(new TestCase(clientArgs, expectedRequestJson, expectedResponseJson));
        testMultipleWith(serverArgs, testCases, testWithSunrise);
    }

    private void testMultipleWith(String[] serverArgs, List<TestCase> testCases, boolean testWithSunrise) throws InterruptedException {
        Verifier verifier = new Verifier(serverArgs, testCases, testWithSunrise);
        System.out.println("verifier initialised");
        verifier.start();

        for (TestCase testCase : testCases){
            verifier.waitForDummyServerToBeReady(waitTime);
            Client.main(testCase.getClientArgs());
        }
        verifier.join(waitTime);
        Assertions.assertTrue(verifier.isSuccessful());
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

    @Test
    void testFetch() throws InterruptedException {
        // fetch something that does not exist, expect success with resultSize 0
        new ServerVerifier("{\n" +
                "    \"command\": \"FETCH\",\n" +
                "    \"resourceTemplate\": {\n" +
                "        \"name\": \"\",\n" +
                "        \"tags\": [],\n" +
                "        \"description\": \"\",\n" +
                "        \"uri\": \"file:\\/\\/\\/\\/home\\/aaron\\/EZShare\\/ezshare.jar\",\n" +
                "        \"channel\": \"my_private_channel\",\n" +
                "        \"owner\": \"\",\n" +
                "        \"ezserver\": null\n" +
                "    }\n" +
                "}",
                "{ \"response\" : \"success\" }",
                new HashSet<String>(),
                false).test();
    }

    @Test
    void testInvalidRequestsShare() throws InterruptedException {
        // cannot share resource
        // uri not present
        new ServerVerifier("{\n" +
                "    \"command\": \"SHARE\",\n" +
                "    \"secret\": \"abcd\",\n" +
                "    \"resource\": {\n" +
                "        \"name\": \"EZShare JAR\",\n" +
                "        \"tags\": [\n" +
                "            \"jar\"\n" +
                "        ],\n" +
                "        \"description\": \"The jar file for EZShare. Use with caution.\",\n" +
                "        \"channel\": \"my_private_channel\",\n" +
                "        \"owner\": \"aaron010\",\n" +
                "        \"ezserver\": null\n" +
                "    }\n" +
                "}",
                "{ \"response\" : \"error\", \"errorMessage\" : \"invalid resource\" }",
                false).test();

        // uri not file scheme
        new ServerVerifier("{\n" +
                "    \"command\": \"SHARE\",\n" +
                "    \"secret\": \"abcd\",\n" +
                "    \"resource\": {\n" +
                "        \"name\": \"EZShare JAR\",\n" +
                "        \"tags\": [\n" +
                "            \"jar\"\n" +
                "        ],\n" +
                "        \"description\": \"The jar file for EZShare. Use with caution.\"," +
                "        \"uri\": \"ftp:\\/\\/\\/home\\/aaron\\/EZShare\\/ezshare.jar\"," +
                "        \"channel\": \"my_private_channel\",\n" +
                "        \"owner\": \"aaron010\",\n" +
                "        \"ezserver\": null\n" +
                "    }\n" +
                "}",
                "{ \"response\" : \"error\", \"errorMessage\" : \"invalid resource\" }",
                false).test();

        // file does not exist
        new ServerVerifier("{\n" +
                "    \"command\": \"SHARE\",\n" +
                "    \"secret\": \"abcd\",\n" +
                "    \"resource\": {\n" +
                "        \"name\": \"EZShare JAR\",\n" +
                "        \"tags\": [\n" +
                "            \"jar\"\n" +
                "        ],\n" +
                "        \"description\": \"The jar file for EZShare. Use with caution.\"," +
                "        \"uri\": \"file:\\/\\/\\/\\/home\\/aaron\\/EZShare\\/ezshare.jar\"," +
                "        \"channel\": \"my_private_channel\",\n" +
                "        \"owner\": \"aaron010\",\n" +
                "        \"ezserver\": null\n" +
                "    }\n" +
                "}",
                "{ \"response\" : \"error\", \"errorMessage\" : \"cannot share resource\" }",
                false).test();

        // resource contained incorrect information that could not be recovered from
        // name field type
        new ServerVerifier("{\n" +
                "    \"command\": \"SHARE\",\n" +
                "    \"secret\": \"abcd\",\n" +
                "    \"resource\": {\n" +
                "        \"name\": [],\n" +
                "        \"tags\": [\n" +
                "            \"jar\"\n" +
                "        ],\n" +
                "        \"description\": \"The jar file for EZShare. Use with caution.\",\n" +
                "        \"uri\": \"file:\\/\\/\\/\\/home\\/aaron\\/EZShare\\/ezshare.jar\",\n" +
                "        \"channel\": \"my_private_channel\",\n" +
                "        \"owner\": \"aaron010\",\n" +
                "        \"ezserver\": null\n" +
                "    }\n" +
                "}",
                "{ \"response\" : \"error\", \"errorMessage\" : \"invalid resource\" }",
                false).test();

        // incorrect secret
        new ServerVerifier("{\n" +
                "    \"command\": \"SHARE\",\n" +
                "    \"secret\": \"aaaaaaaaaaaaaa\",\n" +
                "    \"resource\": {\n" +
                "        \"name\": \"EZShare JAR\",\n" +
                "        \"tags\": [\n" +
                "            \"jar\"\n" +
                "        ],\n" +
                "        \"description\": \"The jar file for EZShare. Use with caution.\",\n" +
                "        \"uri\": \"file:\\/\\/\\/\\/home\\/aaron\\/EZShare\\/ezshare.jar\",\n" +
                "        \"channel\": \"my_private_channel\",\n" +
                "        \"owner\": \"aaron010\",\n" +
                "        \"ezserver\": null\n" +
                "    }\n" +
                "}",
                "{ \"response\" : \"error\", \"errorMessage\" : \"incorrect secret\" }",
                false).test();

        // resource field not given or not the correct type
        // no resource
        new ServerVerifier("{\"command\": \"REMOVE\"}",
                "{ \"response\" : \"error\", \"errorMessage\" : \"missing resource\" }",
                false).test();

        // incorrect type
        new ServerVerifier("{\"command\": \"REMOVE\", \"resource\": []}",
                "{ \"response\" : \"error\", \"errorMessage\" : \"missing resource\" }",
                false).test();
    }

    @Test
    void testInvalidRequestsRemove() throws InterruptedException {
        // resource did not exist: tested in testPublishRemove()

        // resource contained incorrect information that could not be recovered from
        // TODO

        // resource field not given or not the correct type
        // no resource
        new ServerVerifier("{\"command\": \"REMOVE\"}",
                "{ \"response\" : \"error\", \"errorMessage\" : \"missing resource\" }",
                false).test();

        // incorrect type
        new ServerVerifier("{\"command\": \"REMOVE\", \"resource\": []}",
                "{ \"response\" : \"error\", \"errorMessage\" : \"missing resource\" }",
                false).test();
    }

    @Test
    void testInvalidRequestsQuery() throws InterruptedException {
        // resourceTemplate field not given or not the correct type
        // no resourceTemplate
        new ServerVerifier("{\"command\": \"QUERY\"}",
                "{ \"response\" : \"error\", \"errorMessage\" : \"missing resourceTemplate\" }",
                false).test();

        // incorrect type
        new ServerVerifier("{\"command\": \"QUERY\", \"resourceTemplate\": []}",
                "{ \"response\" : \"error\", \"errorMessage\" : \"missing resourceTemplate\" }",
                false).test();
    }

    @Test
    void testInvalidRequestsFetch() throws InterruptedException {
        // resourceTemplate field not given or not the correct type
        // no resourceTemplate
        new ServerVerifier("{\"command\": \"FETCH\"}",
                "{ \"response\" : \"error\", \"errorMessage\" : \"missing resourceTemplate\" }",
                false).test();

        // incorrect type
        new ServerVerifier("{\"command\": \"FETCH\", \"resourceTemplate\": []}",
                "{ \"response\" : \"error\", \"errorMessage\" : \"missing resourceTemplate\" }",
                false).test();
    }

    @Test
    void testInvalidRequestsExchange() throws InterruptedException {
        // missing serverList
        new ServerVerifier("{\"command\": \"EXCHANGE\"}",
                "{ \"response\" : \"error\", \"errorMessage\" : \"missing or invalid server list\" }",
                false).test();

        // incorrect type
        new ServerVerifier("{\"command\": \"EXCHANGE\", \"serverList\": {}}",
                "{ \"response\" : \"error\", \"errorMessage\" : \"missing or invalid server list\" }",
                false).test();
    }
}
