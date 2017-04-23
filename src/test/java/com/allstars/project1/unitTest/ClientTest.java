package com.allstars.project1.unitTest;

import com.allstars.project1.Client;
import com.allstars.project1.Resource;

import org.apache.commons.cli.CommandLine;
import org.junit.jupiter.api.*;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by Zheping on 2017/3/26.
 */
class ClientTest {

    private static ServerSocket serviceSocket = null;
    private Socket clientSocket = null;
    private Resource resource;

    class ServiceThread extends Thread {
        private DataInputStream inputStream;
        private DataOutputStream outputStream;

        public ServiceThread(Socket socket) {

            try {
                this.inputStream = new DataInputStream(socket.getInputStream());
                this.outputStream = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {

            try {
                String inText = inputStream.readUTF();

                outputStream.writeUTF("Receive from client:" + inText);
                outputStream.flush();

            } catch (IOException e) {
                e.printStackTrace();
            }

        }


    }

    void serverAccept() {

        Socket clientSocket = null;
        try {
            clientSocket = serviceSocket.accept();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Received connection: " + 1);

        ServiceThread service = new ServiceThread(clientSocket);
    }

    @BeforeAll
    static void setUp() {
        // set up the dummy server and create a Client instance
        try {
            serviceSocket = new ServerSocket(2334);
            System.out.println("Server listening to connections.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void connectToServer() {
        String host = "localhost";
        int port = 2334;

        try {
            clientSocket = Client.connectToServer(host, port, 1000);
        } catch (Exception e) {
            e.printStackTrace();
        }

        serverAccept();
    }

    @Test
    void getOptions() {
        String[] testCmd = new String[] {"-host","localhost", "-port", "2334","-publish"};
        CommandLine cmd = Client.getOptions(testCmd);
        System.out.println(cmd.toString());
    }

    @Test
    void publish() {
        String[] publish = new String[] {"-host","localhost", "-port", "2334","-publish","-name","''",
                "-description","''","-uri","",
                "-tags",""};
        CommandLine cmd = Client.getOptions(publish);
        resource = Client.makeResourceFromCmd(cmd);

        try {
            Client.publish(clientSocket, resource);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Disabled
    @Test
    void share() {
        String share;
    }

    @Disabled
    @Test
    void query() {
//        String query = "{'command': 'QUERY', " +
//                "'relay': true, " +
//                "'resourceTemplate': {" +
//                "'name': '', " +
//                "'tags': [], " +
//                "'description': '', " +
//                "'uri': '', " +
//                "'channel': '', " +
//                "'owner': '', " +
//                "'ezserver': null}}";
        // give a resource template
        // Client.query(clientSocket, false, resource);
    }

    @Disabled
    @Test
    void fetch() {
//        String fetch = "{'command': 'FETCH', " +
//                "'resourceTemplate': {'name': '', " +
//                "'tags': [], " +
//                "'description': '', " +
//                "'uri': 'file:\\/\\/\\/\\/home\\/aaron\\/EZShare\\/ezshare.jar', " +
//                "'channel': '', " +
//                "'owner': '', " +
//                "'ezserver': null}}";
    }

    @Disabled
    @Test
    void exchange() {
//        String exchange = "{'command': 'EXCHANGE', " +
//                "'serverList': [" +
//                "{" +
//                "'hostname': 'localhost', " +
//                "'port': 2333" +
//                "}," +
//                "{" +
//                "'hostname': '115.146.85.24', " +
//                "'port': 2333" +
//                "}" +
//                "]}";
    }


    @Test
    void makeResourceFromCmd() {
        String[] publish = new String[] {"-host","localhost", "-port", "2334","-publish","-name","",
                                        "-description","","-uri","",
                                        "-tags",""};
        CommandLine cmd = Client.getOptions(publish);

        resource = Client.makeResourceFromCmd(cmd);
        System.out.println(resource.getName());
        Assertions.assertTrue(resource.getName().equals(""));
    }

}