package com.allstars.project1.unitTest;

import com.allstars.project1.EzServer;
import com.allstars.project1.ResourceStorage;
import com.allstars.project1.ServiceThread;
import com.allstars.project1.Static;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by Zheping on 2017/4/25.
 */
class ServiceThreadTest {

    private static final String HOST = "localhost";
    private static final int PORT = 2333;
    public static ResourceStorage resourceStorage = new ResourceStorage();
    public static Set<EzServer> serverList = Collections.synchronizedSet(new HashSet<>());
    public static HashMap<SocketAddress, Date> lastConnectionTime = new HashMap<>();
    public static EzServer self;

    class DummyServer {
        ServerSocket serverSocket;

        DummyServer () {
            try {
                serverSocket = new ServerSocket(PORT);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        void DummyServerAccept() {

            try {
                Socket clientSocket = serverSocket.accept();
                String secret = "JackBigLeg";

                ServiceThread serviceThread = new ServiceThread(lastConnectionTime, clientSocket, secret, resourceStorage, serverList);

                serviceThread.start();
                Thread.sleep(5000);

            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    class DummyClient {

        Socket socket;
        DataInputStream in;
        DataOutputStream out;

        DummyClient () {

            try {
                socket = new Socket(HOST, PORT);
                in = new DataInputStream(socket.getInputStream());
                out = new DataOutputStream(socket.getOutputStream());

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        void sendRequest(String req) {
            try {
                // send a request
                out.writeUTF(req);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        boolean getResponse() {

            String response = null;
            try {
                response = Static.readJsonUTF(in);
            } catch (IOException e) {
                e.printStackTrace();
            }
            JsonObject resObj = new JsonParser().parse(response).getAsJsonObject();

            if (resObj.get("response").getAsString().equals("success")) {
                return true;
            } else {
                return false;
            }

        }
    }

    @BeforeEach
    void setUp() {
    }

    @Test
    void run() {

        DummyServer server = new DummyServer();

        ArrayList<String> commandSuccess = new ArrayList<String>();
        ArrayList<String> commandFail = new ArrayList<String>();

        // publish resource UOM
        commandSuccess.add("{'command': 'PUBLISH', " +
                "'resource': {" +
                "'name': 'UOM', " +
                "'tags': ['web', 'uom', 'jack', 'bigleg'], " +
                "'description': 'university of melbourne first time', " +
                "'uri': 'http:\\/\\/www.unimelb.edu.au', " +
                "'channel': 'Web', " +
                "'owner': 'Leo', " +
                "'ezserver': null}}");

        // publish UOM again with different NAME and DESCRIPTION but the same resourceKey
        commandSuccess.add("{'command': 'PUBLISH', " +
                "'resource': {" +
                "'name': 'UOM again', " +
                "'tags': ['web'], " +
                "'description': 'university of melbourne second time', " +
                "'uri': 'http:\\/\\/www.unimelb.edu.au', " +
                "'channel': 'Web', " +
                "'owner': 'Leo', " +
                "'ezserver': null}}");

        // publish resource with same CHANNEL and URI, BUT different OWENER
        commandFail.add("{'command': 'PUBLISH', " +
                "'resource': {" +
                "'name': 'UOM again and again', " +
                "'tags': ['web'], " +
                "'description': 'university of melbourne second time', " +
                "'uri': 'http:\\/\\/www.unimelb.edu.au', " +
                "'channel': 'Web', " +
                "'owner': 'Jack', " +
                "'ezserver': null}}");

        // publish resource with OWNER = '*'
        commandFail.add("{'command': 'PUBLISH', " +
                "'resource': {" +
                "'name': 'Leo', " +
                "'tags': ['double'], " +
                "'description': 'LeoGoodMan', " +
                "'uri': 'http:\\/\\/www.unimelb.edu.au', " +
                "'channel': 'Personal', " +
                "'owner': '*', " +
                "'ezserver': null}}");

        // query with an empty resourceTemplate
        commandFail.add("{'command': 'QUERY', " +
                "'relay': true, " +
                "'resourceTemplate': {" +
                "'name': '', " +
                "'tags': [], " +
                "'description': '', " +
                "'uri': '', " +
                "'channel': '', " +
                "'owner': '', " +
                "'ezserver': null}}");

        // query with a resourceTemplate with CHANNEL
        commandSuccess.add("{'command': 'QUERY', " +
                "'relay': true, " +
                "'resourceTemplate': {" +
                "'name': '', " +
                "'tags': [], " +
                "'description': '', " +
                "'uri': '', " +
                "'channel': 'Personal', " +
                "'owner': '', " +
                "'ezserver': null}}");

        // query with a resourceTemplate with CHANNEL and correct OWNER
        commandSuccess.add("{'command': 'QUERY', " +
                "'relay': true, " +
                "'resourceTemplate': {" +
                "'name': '', " +
                "'tags': [], " +
                "'description': '', " +
                "'uri': '', " +
                "'channel': 'Personal', " +
                "'owner': 'Leo', " +
                "'ezserver': null}}");

        // query with a resourceTemplate with CHANNEL and incorrect OWNER
        commandFail.add("{'command': 'QUERY', " +
                "'relay': true, " +
                "'resourceTemplate': {" +
                "'name': '', " +
                "'tags': [], " +
                "'description': '', " +
                "'uri': '', " +
                "'channel': 'Personal', " +
                "'owner': 'LEO', " +
                "'ezserver': null}}");

        // query with a resourceTemplate with CHANNEL and correct TAGS
        commandSuccess.add("{'command': 'QUERY', " +
                "'relay': true, " +
                "'resourceTemplate': {" +
                "'name': '', " +
                "'tags': ['WEB', 'JACK'], " +
                "'description': '', " +
                "'uri': '', " +
                "'channel': 'Personal', " +
                "'owner': '', " +
                "'ezserver': null}}");

        // query with a resourceTemplate with CHANNEL and incorrect TAGS
        commandFail.add("{'command': 'QUERY', " +
                "'relay': true, " +
                "'resourceTemplate': {" +
                "'name': '', " +
                "'tags': ['WEB', 'JACK', 'laji'], " +
                "'description': '', " +
                "'uri': '', " +
                "'channel': 'Personal', " +
                "'owner': '', " +
                "'ezserver': null}}");

        // query with a resourceTemplate with CHANNEL and correct URI
        commandSuccess.add("{'command': 'QUERY', " +
                "'relay': true, " +
                "'resourceTemplate': {" +
                "'name': '', " +
                "'tags': [], " +
                "'description': '', " +
                "'uri': 'http:\\/\\/www.unimelb.edu.au', " +
                "'channel': 'Personal', " +
                "'owner': '', " +
                "'ezserver': null}}");

        // query with a resourceTemplate with CHANNEL and incorrect URI
        commandFail.add("{'command': 'QUERY', " +
                "'relay': true, " +
                "'resourceTemplate': {" +
                "'name': '', " +
                "'tags': [], " +
                "'description': '', " +
                "'uri': 'http:\\/\\/www.uniuseless.edu.au', " +
                "'channel': 'Personal', " +
                "'owner': '', " +
                "'ezserver': null}}");

        // query with a resourceTemplate with CHANNEL and NAME SUBSTRING
        commandSuccess.add("{'command': 'QUERY', " +
                "'relay': true, " +
                "'resourceTemplate': {" +
                "'name': 'U', " +
                "'tags': [], " +
                "'description': '', " +
                "'uri': '', " +
                "'channel': 'Personal', " +
                "'owner': '', " +
                "'ezserver': null}}");

        // query with a resourceTemplate with CHANNEL and DESCRIPTION SUBSTRING
        commandSuccess.add("{'command': 'QUERY', " +
                "'relay': true, " +
                "'resourceTemplate': {" +
                "'name': 'U', " +
                "'tags': [], " +
                "'description': 'university of melbourne', " +
                "'uri': '', " +
                "'channel': 'Personal', " +
                "'owner': '', " +
                "'ezserver': null}}");

        // query command missing a resourceTemplate
        commandFail.add("{'command': 'QUERY'}");

//        // fetch the resource UOM with correct URI and CHANNEL
//        commandSuccess.add("{'command': 'FETCH', " +
//                "'resourceTemplate': {'name': '', " +
//                "'tags': [], " +
//                "'description': '', " +
//                "'uri': 'http:\\/\\/www.unimelb.edu.au', " +
//                "'channel': 'Personal', " +
//                "'owner': '', " +
//                "'ezserver': null}}");
//
//        // fetch the resource UOM with incorrect URI and correct CHANNEL
//        commandFail.add("{'command': 'FETCH', " +
//                "'resourceTemplate': {'name': '', " +
//                "'tags': [], " +
//                "'description': '', " +
//                "'uri': 'http:\\/\\/www.uniuseless.edu.au', " +
//                "'channel': 'Personal', " +
//                "'owner': '', " +
//                "'ezserver': null}}");
//
//        // fetch the resource UOM with correct URI and empty CHANNEL
//        commandFail.add("{'command': 'FETCH', " +
//                "'resourceTemplate': {'name': '', " +
//                "'tags': [], " +
//                "'description': '', " +
//                "'uri': 'http:\\/\\/www.unimelb.edu.au', " +
//                "'channel': '', " +
//                "'owner': '', " +
//                "'ezserver': null}}");
//
//        // fetch the resource UOM with no resourceTemplate
//        commandFail.add("{'command': 'FETCH'}");
//
//        // fetch the resource UOM with incorrect resourceTemplate missing DESCRIPTION and TAGS
//        commandFail.add("{'command': 'FETCH', " +
//                "'resourceTemplate': {'name': '', " +
//                "'uri': 'http:\\/\\/www.unimelb.edu.au', " +
//                "'channel': 'Personal', " +
//                "'owner': '', " +
//                "'ezserver': null}}");
//
//        // fetch the resource UOM with missing URI field
//        commandFail.add("{'command': 'FETCH', " +
//                "'resourceTemplate': {'name': '', " +
//                "'tags': [], " +
//                "'description': '', " +
//                "'channel': 'Personal', " +
//                "'owner': '', " +
//                "'ezserver': null}}");
//
//        // fetch the resource UOM with missing CHANNEL field
//        commandFail.add("{'command': 'FETCH', " +
//                "'resourceTemplate': {'name': '', " +
//                "'tags': [], " +
//                "'description': '', " +
//                "'uri': 'http:\\/\\/www.unimelb.edu.au', " +
//                "'owner': '', " +
//                "'ezserver': null}}");

        // exchange with a serverList
        commandSuccess.add("{'command': 'EXCHANGE', " +
                "'serverList': [" +
                "{" +
                "'hostname': '117.123.86.25', " +
                "'port': 2334" +
                "}," +
                "{" +
                "'hostname': '115.146.85.24', " +
                "'port': 2335" +
                "}" +
                "]}");

        // exchange with an empty serverList
        commandSuccess.add("{'command': 'EXCHANGE', " +
                "'serverList': [" +
                "]}");

        // exchange with no serverList specified in the JSON template
        commandFail.add("{'command': 'EXCHANGE'}");

        // remove resource which is not existed #1
        commandFail.add("{'command': 'REMOVE', " +
                "'resource': {" +
                "'name': '', " +
                "'tags': [], " +
                "'description': '', " +
                "'uri': 'http:\\/\\/www.unimelb.edu.au', " +
                "'channel': 'Web', " +
                "'owner': 'Leo', " +
                "'ezserver': null}}");

        // remove resource which is not existed #2
        commandFail.add("{'command': 'REMOVE', " +
                "'resource': {" +
                "'name': '', " +
                "'tags': [], " +
                "'description': '', " +
                "'uri': 'http:\\/\\/www.unimelb.edu.au', " +
                "'channel': 'Personal', " +
                "'owner': 'Jack', " +
                "'ezserver': null}}");

        // remove resource which is not existed #3
        commandFail.add("{'command': 'REMOVE', " +
                "'resource': {" +
                "'name': '', " +
                "'tags': [], " +
                "'description': '', " +
                "'uri': '', " +
                "'channel': 'Personal', " +
                "'owner': 'Leo', " +
                "'ezserver': null}}");

        // remove resource UOM again [the resourceKey is (Leo, Personal, http:\/\/www.unimelb.edu.au)]
        commandSuccess.add("{'command': 'REMOVE', " +
                "'resource': {" +
                "'name': '', " +
                "'tags': [], " +
                "'description': '', " +
                "'uri': 'http:\\/\\/www.unimelb.edu.au', " +
                "'channel': 'Personal', " +
                "'owner': 'Leo', " +
                "'ezserver': null}}");


        try {

            for (String s : commandSuccess) {
                DummyClient c = new DummyClient();
                server.DummyServerAccept();
                System.out.println(s);
                c.sendRequest(s);

                boolean success = c.getResponse();
                //assert the respond to be success
                Assertions.assertTrue(success);
            }

            for (String s : commandFail) {
                DummyClient c = new DummyClient();
                server.DummyServerAccept();
                System.out.println(s);
                c.sendRequest(s);

                boolean fail = c.getResponse();
                Assertions.assertFalse(fail);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}