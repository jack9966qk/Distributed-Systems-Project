package EzShare.unitTest;

import EzShare.Server;
import EzShare.Static;
import org.junit.jupiter.api.Assertions;

import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;
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

    static class ExpectedJson implements Verifiable {
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

    static class ExpectedResourcesWithSuccess implements Verifiable {
        Set<String> resources = new HashSet<>();

        public ExpectedResourcesWithSuccess(Set<String> resources) {
            this.resources = resources;
        }

        @Override
        public void verifyAndForward(Socket from, Socket to) throws Exception {
            DataOutputStream out = new DataOutputStream(to.getOutputStream());
            String actual = Static.readJsonUTF(new DataInputStream(from.getInputStream()));
            Verify.assertJsonEquivalent(actual, "{ \"response\" : \"success\" }");
            Static.sendJsonUTF(out, actual);
            List<String> strings = Verify.checkResources(from, resources);
            for (String str : strings) {
                Static.sendJsonUTF(out, str);
            }
        }
    }

    Queue<Verifiable> expectedRequests = new LinkedList<>();
    Queue<Verifiable> expectedResponses = new LinkedList<>();

    Socket clientSocket;
    Socket serverSocket;

    int port;
    int serverPort;
    String serverHost;

    boolean secure;
    boolean successful;

    String status = "";

    public ProxyVerifier(int port, int serverPort, String serverHost, boolean secure) {
        this.port = port;
        this.serverPort = serverPort;
        this.serverHost = serverHost;
        this.secure = secure;
    }

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
            while (!status.equals("ready")) {
                wait(timeout);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    synchronized void waitUntilFinish() {
        try {
            while (!status.equals("ended")) {
                wait(timeout);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        ServerSocket selfServerSocket = null;
        try {
            // setup proxy
            if (secure) {
                selfServerSocket = SSLServerSocketFactory.getDefault().createServerSocket(port);
            } else {
                selfServerSocket = new ServerSocket(port);
            }

            selfServerSocket.setSoTimeout(timeout);

            synchronized (this) {
                // tell waiting threads that server is ready
                status = "ready";
                notifyAll();
            }

            // establish connection with client
            clientSocket = selfServerSocket.accept();
            clientSocket.setSoTimeout(timeout);

            // establish connection with server
            if (secure) {
                serverSocket = SSLSocketFactory.getDefault().createSocket(serverHost, serverPort);
            } else {
                serverSocket = new Socket(serverHost, serverPort);
            }
            serverSocket.setSoTimeout(timeout);

            while (!(expectedRequests.isEmpty() && expectedResponses.isEmpty())) {
                // assume every request has a corresponding response
                // receive request, forward to server
                handleExpected(expectedRequests, clientSocket, serverSocket);
                // receive response, forward to client
                handleExpected(expectedResponses, serverSocket, clientSocket);
            }

            successful = true;
        } catch (Exception e) {
            e.printStackTrace();
            successful = false;
        } finally {
            synchronized (this) {
                // tell waiting threads that verification ended
                status = "ended";
                notifyAll();
            }

            try {
                clientSocket.close();
                serverSocket.close();
                if (selfServerSocket != null) {
                    selfServerSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                successful = false;
            }
        }
    }

    void setup() throws InterruptedException {
        this.start();
        this.waitUntilReady();
    }


    void test() {
        this.waitUntilFinish();
        Assertions.assertTrue(this.successful);
        Server.stop();
    }

    static void verifyServerBothSecureAndInsecure(String requestJson, String expectedResponseJson) throws InterruptedException {
        verifyServer(requestJson, expectedResponseJson, false);
        verifyServer(requestJson, expectedResponseJson, true);
    }

    static void verifyServerBothSecureAndInsecure(String requestJson, Set<String> expectedResources) throws InterruptedException {
        verifyServer(requestJson, expectedResources, false);
        verifyServer(requestJson, expectedResources, true);
    }

    static void verifyServer(String requestJson, String expectedResponseJson, boolean secure) throws InterruptedException {
        int port = 3780;
        if (secure) {
            new ServerThread("-sport 9999 -debug -secret abcd".split(" ")).start();
        } else {
            new ServerThread("-port 9999 -debug -secret abcd".split(" ")).start();
        }
        Server.waitUntilReady();
        ProxyVerifier verifier = new ProxyVerifier(port, 9999, "localhost", secure);
        verifier.addExpectedResponseJson(expectedResponseJson);
        verifier.setup();
        new DummyClient(requestJson, "localhost", port, secure).start();
        verifier.test();
    }

    static void verifyServer(String requestJson, Set<String> expectedResources, boolean secure) throws InterruptedException {
        int port = 3780;
        if (secure) {
            new ServerThread("-sport 9999 -debug -secret abcd".split(" ")).start();
        } else {
            new ServerThread("-port 9999 -debug -secret abcd".split(" ")).start();
        }
        Server.waitUntilReady();
        ProxyVerifier verifier = new ProxyVerifier(port, 9999, "localhost", secure);
        Verifiable expected = new ExpectedResourcesWithSuccess(expectedResources);
        verifier.addExpectedResponse(expected);
        verifier.setup();
        new DummyClient(requestJson, "localhost", port, secure).start();
        verifier.test();
    }
}
