package EzShare.unitTest;

import EzShare.Server;
import EzShare.Static;

import javax.net.ssl.SSLServerSocketFactory;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

/**
 * Created by Jack on 10/5/2017.
 */
public class ProxyVerifier extends Thread {
    static int timeout = 1000 * 30;

    interface Verifiable {
        public void verifyAndForward(Socket from, Socket to) throws Exception;
    }

    class ExpectedJson implements Verifiable {
        String json;

        public ExpectedJson(String json) {
            this.json = json;
        }

        @Override
        public void verifyAndForward(Socket from, Socket to) throws Exception {
            String actual = Static.readJsonUTF(new DataInputStream(from.getInputStream()));
            Verify.assertJsonEquivalent(actual, json);
            Static.sendJsonUTF(new DataOutputStream(to.getOutputStream()), actual);
        }
    }

    class ExpectedResources implements Verifiable {
        Set<String> resources = new HashSet<>();

        void add(String resourceJson) {
            resources.add(resourceJson);
        }

        @Override
        public void verifyAndForward(Socket from, Socket to) throws Exception {
            DataOutputStream out = new DataOutputStream(to.getOutputStream());
            List<String> strings = Verify.checkResources(from, resources);
            for (String str : strings) {
                Static.sendJsonUTF(out, str);
            }
        }
    }

    Queue<Verifiable> expectedRequests = new LinkedList<>();
    List<Verifiable> expectedResponses = new LinkedList<>();

    Socket clientSocket;
    Socket serverSocket;

    int port;
    int serverPort;
    String serverHost;

    boolean secure;
    boolean successful;

    void addExpectedRequest(Verifiable verifiable) {
        expectedRequests.add(verifiable);
    }

    void addExpectedResponse(Verifiable verifiable) {
        expectedResponses.add(verifiable);
    }

    void addExpectedRequestJson(String json) {
        addExpectedRequest(new ExpectedJson(json));
    }

    void addExpectedResponseJson(String json) {
        addExpectedResponse(new ExpectedJson(json));
    }

    void handleExpected(Queue<Verifiable> expected, Socket from, Socket to) throws Exception {
        if (expected.isEmpty()) {
            String str = new DataInputStream(from.getInputStream()).readUTF();
            new DataOutputStream(to.getOutputStream()).writeUTF(str);
        } else {
            Verifiable next = expected.remove();
            next.verifyAndForward(from, to);
        }
    }

    synchronized void waitUntilReady() {
        try {
            wait(timeout);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            ServerSocket selfServerSocket;
            // setup proxy
            if (secure) {
                selfServerSocket = SSLServerSocketFactory.getDefault().createServerSocket(port);
            } else {
                selfServerSocket = new ServerSocket(port);
            }

            selfServerSocket.setSoTimeout(timeout);

            synchronized (this) {
                // tell waiting threads that server is ready
                notifyAll();
            }

            // establish connection with client
            clientSocket = selfServerSocket.accept();
            clientSocket.setSoTimeout(timeout);

            // establish connection with server
            serverSocket = new Socket(serverHost, serverPort);
            serverSocket.setSoTimeout(timeout);

            while (!(expectedRequests.isEmpty() && expectedResponses.isEmpty())) {
                // assume every request has a corresponding response
                // receive request, forward to server
                handleExpected(expectedRequests, clientSocket, serverSocket);
                // receive response, forward to client
                handleExpected(expectedRequests, clientSocket, serverSocket);
            }

            successful = true;
        } catch (Exception e) {
            e.printStackTrace();
            successful = false;
        } finally {
            try {
                clientSocket.close();
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
                successful = false;
            }
        }
    }

    void setup() throws InterruptedException {
        new ServerThread("-port 9999 -secret abcd".split(" ")).start();
        Server.waitUntilReady();
        this.start();
        this.waitUntilReady();
    }
}
