package com.allstars.project1.unitTest;

import com.allstars.project1.Client;
import static org.junit.jupiter.api.Assertions.fail;

import org.apache.commons.cli.CommandLine;
import org.junit.jupiter.api.*;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by Zheping on 2017/3/26.
 */
class ClientTest {

    static ServerSocket serviceSocket = null;

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
        String connectionTest = "connectToServer succeed";
        String host = "localhost";
        int port = 2334;

        Socket clientSocket = Client.connectToServer(host, port);

        serverAccept();
    }

    @Test
    void getOptions() {
        String[] testCmd = new String[] {"-host","localhost", "-port", "2334","-publish"};
        CommandLine cmd = Client.getOptions(testCmd);
        System.out.println(cmd.toString());
    }

    @Disabled
    @Test
    void share() {
        String[] share;
    }

    @Disabled
    @Test
    void query() {
        String[] query;
        fail("Not fully implemented yet.");
    }

    @Disabled
    @Test
    void fetch() {
        String[] fetch;
        fail("Not fully implemented yet.");
    }

    @Disabled
    @Test
    void exchange() {
        String[] exchange;
        fail("Not fully implemented yet.");
    }


    @Disabled
    @Test
    void makeResourceFromCmd() {
        fail("Not fully implemented yet.");
    }

}