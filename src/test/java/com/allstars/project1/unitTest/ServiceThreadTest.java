package com.allstars.project1.unitTest;

import com.allstars.project1.*;
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

    private static final String HOST = "sunrise.cis.unimelb.edu.au";
    private static final int PORT = 3781;
    public static ResourceStorage resourceStorage = new ResourceStorage();
    public static Set<EzServer> serverList = Collections.synchronizedSet(new HashSet<>());
    public static HashMap<SocketAddress, Date> lastConnectionTime = new HashMap<>();


    class DummyServer {
        ServerSocket serverSocket;
        Thread mainThread;
        public EzServer self;

        DummyServer () {
            try {
                serverSocket = new ServerSocket(PORT);
                self = new EzServer(HOST, PORT);
                mainThread = Thread.currentThread();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        void dummyServerAccept() {

            try {
                Socket clientSocket = serverSocket.accept();
                String secret = "JackBigLeg";

                ServiceThread serviceThread = new ServiceThread(lastConnectionTime, clientSocket, secret, resourceStorage, serverList, self);

                serviceThread.start();
                Thread.sleep(5000);

            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        void dummyServerStop() {
            mainThread.interrupt();
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

        boolean getQueryResponse() {

            String response;

            try {
                JsonObject jsonObj = new JsonParser().parse(Static.readJsonUTF(in)).getAsJsonObject();

                if (jsonObj.get("response").getAsString().equals("success")) {

                    while (!jsonObj.has("resultSize")) {
                        jsonObj = new JsonParser().parse(Static.readJsonUTF(in)).getAsJsonObject();
                    }

                    if (jsonObj.get("resultSize").getAsInt() == 0) {
                        return false;
                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return true;
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

        ArrayList<String> queryCommandSuccess = new ArrayList<String>();
        ArrayList<String> queryCommandFail = new ArrayList<String>();

       
        queryCommandFail.add("{'command': 'QUERY', ");

//      
        try {

            System.out.println("Sucessed cases:");

            for (String s : commandSuccess) {
                DummyClient c = new DummyClient();
                server.dummyServerAccept();
                System.out.println(s);
                c.sendRequest(s);

                boolean success = c.getResponse();
                //assert the respond to be success
                Assertions.assertTrue(success);
            }

            System.out.println("Failed cases:");

            for (String s : commandFail) {
                DummyClient c = new DummyClient();
                server.dummyServerAccept();
                System.out.println(s);
                c.sendRequest(s);

                boolean fail = c.getResponse();
                Assertions.assertFalse(fail);
            }

            System.out.println("Query success cases:");

            for (String s : queryCommandSuccess) {
                DummyClient c = new DummyClient();
                server.dummyServerAccept();
                System.out.println(s);
                c.sendRequest(s);

                boolean success = c.getQueryResponse();
                //assert the respond to be success
                Assertions.assertTrue(success);
            }

            System.out.println("Query fail cases:");

            for (String s : queryCommandFail) {
                DummyClient c = new DummyClient();
                server.dummyServerAccept();
                System.out.println(s);
                c.sendRequest(s);

                boolean success = c.getQueryResponse();
                //assert the respond to be success
                Assertions.assertFalse(success);
            }

            String lastRequest = "{'command': 'REMOVE', " +
                "'resource': {" +
                "'name': '', " +
                "'tags': [], " +
                "'description': '', " +
                "'uri': 'http:\\/\\/www.unimelb.edu.au', " +
                "'channel': 'Web', " +
                "'owner': 'Leo', " +
                "'ezserver': null}}";

            DummyClient c = new DummyClient();
            server.dummyServerAccept();
            System.out.println(lastRequest);
            c.sendRequest(lastRequest);
            boolean success = c.getResponse();
            Assertions.assertTrue(success);


        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            server.dummyServerStop();
        }
    }

}