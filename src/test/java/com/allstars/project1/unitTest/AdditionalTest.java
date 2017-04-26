package com.allstars.project1.unitTest;

import com.allstars.project1.Client;
import com.allstars.project1.Server;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

        private String getResponse(String host, int port, String request) throws IOException {
            Socket s = new Socket(host, port);
            new DataOutputStream(s.getOutputStream()).writeUTF(request);
            return new DataInputStream(s.getInputStream()).readUTF();
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

    class ServerVerifier extends Thread {

        String requestJson;
        String expectedResponseJson;
        boolean testWithSunrise = false;

        boolean successful = false;

        public ServerVerifier(String requestJson, String expectedResponseJson, boolean testWithSunrise) {
            this.requestJson = requestJson;
            this.expectedResponseJson = expectedResponseJson;
            this.testWithSunrise = testWithSunrise;
        }

        @Override
        public void run() {
            new ServerThread("-port 9999".split(" ")).start();
            try {
                Server.waitUntilReady();
                Socket socket = new Socket("localhost", 9999);
                new DataOutputStream(socket.getOutputStream()).writeUTF(requestJson);
                assertJsonEquivalent(new DataInputStream(socket.getInputStream()).readUTF(), expectedResponseJson);
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
            wait(1000 * 10);
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
            verifier.waitForDummyServerToBeReady(1000 * 3);
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

    private void assertJsonEquivalent(String actual, String expected) throws Exception {
        JsonParser parser = new JsonParser();
        JsonObject obj1 = parser.parse(actual).getAsJsonObject();
        JsonObject obj2 = parser.parse(expected).getAsJsonObject();
        removeAllNullFields(obj1);
        removeAllNullFields(obj2);
        if (!obj1.equals(obj2)) {
            System.err.println("ACTUAL>>>>>>>>>>>");
            System.err.println(obj1);
            System.err.println("=================");
            System.err.println(obj2);
            System.err.println("EXPECTED<<<<<<<<<");
            throw new Exception("assert failed");
        }
    }

    @BeforeAll
    static void setUp() {

    }

    @Test
    void testClientRequestJson() throws InterruptedException {
        // Everything specified
        testWith(
                "-port 3780 -debug".split(" "),
                "-host localhost -port 3780 -publish -name Leo -description \"bastard\" -owner Jack -channel LeosChannel -uri http://leo.com -tags leo,ntr -debug".split(" "),
                "{\n" +
                        "   \"command\":\"PUBLISH\",\n" +
                        "   \"resource\":{\n" +
                        "      \"name\":\"Leo\",\n" +
                        "      \"description\":\"bastard\",\n" +
                        "      \"tags\":[\n" +
                        "         \"leo\",\n" +
                        "         \"ntr\"\n" +
                        "      ],\n" +
                        "      \"owner\":\"Jack\",\n" +
                        "      \"uri\":\"http://leo.com\",\n" +
                        "      \"channel\":\"LeosChannel\",\n" +
                        "      \"ezserver\":null\n" +
                        "   }\n" +
                        "}",
                null,
                false
        );

        // Nothing about resource specified
        testWith(
                "-port 3780 -debug".split(" "),
                "-host localhost -port 3780 -publish -debug".split(" "),
                "{\n" +
                        "   \"command\":\"PUBLISH\",\n" +
                        "   \"resource\":{\n" +
                        "      \"name\":\"\",\n" +
                        "      \"description\":\"\",\n" +
                        "      \"tags\":[],\n" +
                        "      \"owner\":\"\",\n" +
                        "      \"uri\":\"\",\n" +
                        "      \"channel\":\"\",\n" +
                        "      \"ezserver\":null\n" +
                        "   }\n" +
                        "}",
                null,
                false
        );
    }

    @Test
    void testPublishRemove() throws InterruptedException {
        List<TestCase> testCases = new ArrayList<>();
        // add a resource
        testCases.add(new TestCase(
                "-host localhost -port 3780 -publish -name Leo -owner Jack -channel LeosChannel -uri http://leo.com -tags leo,ntr -debug".split(" "),
                "{\n" +
                        "   \"command\":\"PUBLISH\",\n" +
                        "   \"resource\":{\n" +
                        "      \"name\":\"Leo\",\n" +
                        "      \"tags\":[\n" +
                        "         \"leo\",\n" +
                        "         \"ntr\"\n" +
                        "      ],\n" +
                        "      \"owner\":\"Jack\",\n" +
                        "      \"description\":\"\",\n" +
                        "      \"uri\":\"http://leo.com\",\n" +
                        "      \"channel\":\"LeosChannel\",\n" +
                        "      \"ezserver\":null\n" +
                        "   }\n" +
                        "}",
                "{\"response\":\"success\"}"
        ));
        // add it one more time, should get the same result
        testCases.add(new TestCase(
                "-host localhost -port 3780 -publish -name Leo -owner Jack -channel LeosChannel -uri http://leo.com -tags leo,ntr -debug".split(" "),
                "{\n" +
                        "   \"command\":\"PUBLISH\",\n" +
                        "   \"resource\":{\n" +
                        "      \"name\":\"Leo\",\n" +
                        "      \"tags\":[\n" +
                        "         \"leo\",\n" +
                        "         \"ntr\"\n" +
                        "      ],\n" +
                        "      \"owner\":\"Jack\",\n" +
                        "      \"description\":\"\",\n" +
                        "      \"uri\":\"http://leo.com\",\n" +
                        "      \"channel\":\"LeosChannel\",\n" +
                        "      \"ezserver\":null\n" +
                        "   }\n" +
                        "}",
                "{\"response\":\"success\"}"
        ));
        // remove the resource added
        testCases.add(new TestCase(
                "-host localhost -port 3780 -remove -name Leo -owner Jack -channel LeosChannel -uri http://leo.com -tags leo,ntr -debug".split(" "),
                "{\n" +
                        "   \"command\":\"REMOVE\",\n" +
                        "   \"resource\":{\n" +
                        "      \"name\":\"Leo\",\n" +
                        "      \"tags\":[\n" +
                        "         \"leo\",\n" +
                        "         \"ntr\"\n" +
                        "      ],\n" +
                        "      \"owner\":\"Jack\",\n" +
                        "      \"description\":\"\",\n" +
                        "      \"uri\":\"http://leo.com\",\n" +
                        "      \"channel\":\"LeosChannel\",\n" +
                        "      \"ezserver\":null\n" +
                        "   }\n" +
                        "}",
                "{\"response\":\"success\"}"
        ));
        // try remove again, should fail since resource no longer exists
        testCases.add(new TestCase(
                "-host localhost -port 3780 -remove -name Leo -owner Jack -channel LeosChannel -uri http://leo.com -tags leo,ntr -debug".split(" "),
                "{\n" +
                        "   \"command\":\"REMOVE\",\n" +
                        "   \"resource\":{\n" +
                        "      \"name\":\"Leo\",\n" +
                        "      \"tags\":[\n" +
                        "         \"leo\",\n" +
                        "         \"ntr\"\n" +
                        "      ],\n" +
                        "      \"owner\":\"Jack\",\n" +
                        "      \"description\":\"\",\n" +
                        "      \"uri\":\"http://leo.com\",\n" +
                        "      \"channel\":\"LeosChannel\",\n" +
                        "      \"ezserver\":null\n" +
                        "   }\n" +
                        "}",
                "{ \"response\" : \"error\", \"errorMessage\" : \"cannot remove resource\" }"
        ));
        testMultipleWith("-port 3780 -debug".split(" "), testCases, false);

        // no tags specified
        testCases = new ArrayList<>();
        testCases.add(new TestCase(
                "-host localhost -port 3780 -publish -name Leo -owner Jack -channel LeosChannel -uri http://leo.com -debug".split(" "),
                "{\n" +
                        "   \"command\":\"PUBLISH\",\n" +
                        "   \"resource\":{\n" +
                        "      \"name\":\"Leo\",\n" +
                        "      \"tags\":[],\n" +
                        "      \"owner\":\"Jack\",\n" +
                        "      \"description\":\"\",\n" +
                        "      \"uri\":\"http://leo.com\",\n" +
                        "      \"channel\":\"LeosChannel\",\n" +
                        "      \"ezserver\":null\n" +
                        "   }\n" +
                        "}",
                "{\"response\":\"success\"}"
        ));
        testMultipleWith("-port 3780 -debug".split(" "), testCases, false);
    }

    @Test
    void testInvalidRequests() throws InterruptedException {
        new ServerVerifier("{\"command\": \"PUBLISH\"}",
                "{ \"response\" : \"error\", \"errorMessage\" : \"missing resource\" }",
                false).test();

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
                "{ \"response\" : \"error\", \"errorMessage\" : \"cannot fetch resource\" }",
                false).test();
    }

    @Test
    void testShare() {

    }

    @Test
    void testRemove() {

    }

    @Test
    void testQuery() {

    }

    @Test
    void testExchange() {

    }
}
