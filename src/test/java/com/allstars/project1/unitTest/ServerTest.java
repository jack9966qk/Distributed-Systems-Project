package com.allstars.project1.unitTest;

import com.allstars.project1.Server;
import org.junit.jupiter.api.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import java.util.ArrayList;

/**
 * Created by Zheping on 2017/3/25.
 */
class ServerTest {

    ServiceThread service = null;

    class ServiceThread extends Thread {

        @Override
        public void run() {
            String[] args = new String[] {"-port", "2333", "-secret", "JackBigLeg"};
            Server.main(args);
        }
    }

    class Client {
        Socket s = null;
        DataInputStream in = null;
        DataOutputStream out = null;

        public void sendRequest(String request) {
            try {
                s = new Socket("localhost", 2333);
                in = new DataInputStream(s.getInputStream());
                out = new DataOutputStream(s.getOutputStream());
                // send a publish request
                out.writeUTF(request);
                String response = in.readUTF();
                System.out.println(response);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public Client() throws IOException {
        }
    }

    @BeforeEach
    void setUp() {
        service = new ServiceThread();
        service.start();
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void main() {
        Client c = null;

        String publish = "{'command': 'PUBLISH', " +
                "'resource': {" +
                "'name': 'UOM', " +
                "'tags': ['web'], " +
                "'description': 'university of melbourne', " +
                "'uri': 'http:\\/\\/www.unimelb.edu.au', " +
                "'channel': '', " +
                "'owner': '', " +
                "'ezserver': null}}";

        String remove = "{'command': 'REMOVE', " +
                "'resource': {" +
                "'name': 'UOM', " +
                "'tags': ['web'], " +
                "'description': 'university of melbourne', " +
                "'uri': 'http:\\/\\/www.unimelb.edu.au', " +
                "'channel': '', " +
                "'owner': '', " +
                "'ezserver': null}}";

        String query = "{'command': 'QUERY', " +
                "'relay': true, " +
                "'resourceTemplate': {" +
                "'name': '', " +
                "'tags': [], " +
                "'description': '', " +
                "'uri': '', " +
                "'channel': '', " +
                "'owner': '', " +
                "'ezserver': null}}";

        String fetch = "{'command': 'FETCH', " +
                "'resourceTemplate': {'name': '', " +
                "'tags': [], " +
                "'description': '', " +
                "'uri': 'file:\\/\\/\\/\\/home\\/aaron\\/EZShare\\/ezshare.jar', " +
                "'channel': '', " +
                "'owner': '', " +
                "'ezserver': null}}";

        String exchange = "{'command': 'EXCHANGE', " +
                "'serverList': [" +
                "{" +
                "'hostname': 'localhost', " +
                "'port': 2333" +
                "}," +
                "{" +
                "'hostname': '115.146.85.24', " +
                "'port': 2333" +
                "}" +
                "]}";

        ArrayList<String> commands = new ArrayList<String>();
        commands.add(publish);
        commands.add(remove);
        commands.add(query);
        commands.add(exchange);
        commands.add(fetch);

        try {
            c = new Client();
            for (String s : commands) {
                System.out.println(s);
                c.sendRequest(s);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}