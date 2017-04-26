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

    class Verifier extends Thread {
        class ServerThread extends Thread {
            String[] args;

            public ServerThread(String[] args) {
                this.args = args;
            }

            public void run() {
                Server.main(args);
            }
        }

        List<TestCase> testCases;
        ServerThread serverThread;
        boolean testWithSunrise;


        public boolean isSuccessful() {
            return successful;
        }

        boolean successful = true;

        public Verifier(String[] serverArgs, List<TestCase> testCases, boolean testWithSunrise) {
            this.testCases = testCases;
            this.testWithSunrise = testWithSunrise;
            this.serverThread = new ServerThread(serverArgs);
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
            // create a dummy server to check request from client
            ServerSocket dummyServerSocket;
            synchronized (this) {
                dummyServerSocket = new ServerSocket(3780);
                notifyAll();
            }
            Socket client = dummyServerSocket.accept();
            DataInputStream stream = new DataInputStream(client.getInputStream());
            String request = stream.readUTF();
            assertJsonEquivalent(request, expectedRequestJson);
            dummyServerSocket.close();

            this.serverThread.start();
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
        }

        public void run() {
            try {
                for (TestCase testCase: testCases) {
                    testOne(testCase.getExpectedRequestJson(), testCase.getExpectedResponseJson());
                }
                Server.stop();
            } catch (Exception e) {
                if (!e.getMessage().equals("assert failed")) {
                    e.printStackTrace();
                }
                successful = false;
                if (Server.isRunning()) {
                    Server.stop();
                }
            }
        }
    }

    private void testWith(String[] serverArgs, List<TestCase> testCases, boolean testWithSunrise) throws InterruptedException {
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
    void testPublish() throws InterruptedException {
        List<TestCase> testCases = new ArrayList<>();
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
        testWith("-port 3780 -debug".split(" "), testCases, false);


//        testCases = new ArrayList<>();
//        testCases.add(new TestCase(
//                "-host localhost -port 3780 -publish -name Leo -owner Jack -channel LeosChannel -uri http://leo.com -tags -debug".split(" "),
//                "{\n" +
//                        "   \"command\":\"PUBLISH\",\n" +
//                        "   \"resource\":{\n" +
//                        "      \"name\":\"Leo\",\n" +
//                        "      \"tags\":[],\n" +
//                        "      \"owner\":\"Jack\",\n" +
//                        "      \"description\":\"\",\n" +
//                        "      \"uri\":\"http://leo.com\",\n" +
//                        "      \"channel\":\"LeosChannel\",\n" +
//                        "      \"ezserver\":null\n" +
//                        "   }\n" +
//                        "}",
//                "{\"response\":\"success\"}"
//        ));
//        testWith("-port 3780 -debug".split(" "), testCases, false);
//
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
        testWith("-port 3780 -debug".split(" "), testCases, false);
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
