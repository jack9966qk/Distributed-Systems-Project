package com.allstars.project1.unitTest;

import com.allstars.project1.Client;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by Zheping on 2017/3/26.
 */
class ClientTest {

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

                if (inText.equals("quit()")) {
                    System.out.println("Server quit.");
                }

                outputStream.writeUTF("Receive from client:" + inText);

            } catch (IOException e) {
                e.printStackTrace();
            }

        }


    }

    @BeforeEach
    void setUp() {
        // set up the dummy server and create a Client instance
        try {
            ServerSocket socket = new ServerSocket(2333);
            System.out.println("Server listen to connections.");

            int i=0;
            Socket clientSocket = socket.accept();
            System.out.println("Received connection: " + ++i);

            ServiceThread service = new ServiceThread(clientSocket);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void share() {
        String[] share;
    }

    @Test
    void query() {
        String[] query;
    }

    @Test
    void fetch() {
        String[] fetch;
    }

    @Test
    void exchange() {
        String[] exchange;
    }

    @Test
    void getOptions() {
        String[] testCmd = new String[] {};
    }

    @Test
    void makeResourceFromCmd() {
    }

    @Test
    void connectToServer() {
        String[] connection = new String[] {"-host", "localhost", "-port", "2333"};
        new Client().main(connection);
    }

//    @Test
//    void main() {
//    }

}