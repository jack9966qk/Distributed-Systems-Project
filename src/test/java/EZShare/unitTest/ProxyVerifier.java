package EZShare.unitTest;

import EZShare.Server;
import EZShare.Static;
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

    static class ExpectedSuccessResponse extends ExpectedJson {
        public ExpectedSuccessResponse() {
            super("{ \"response\" : \"success\" }");
        }
    }

    static class ExpectedAnyUTF implements Verifiable {
        @Override
        public void verifyAndForward(Socket from, Socket to) throws Exception {
            DataOutputStream out = new DataOutputStream(to.getOutputStream());
            DataInputStream in = new DataInputStream(from.getInputStream());
            String str = in.readUTF();
//            System.out.println(">>>>>> " + str);
            out.writeUTF(str);
        }
    }

    static class ExpectedResourcesWithSuccess extends ExpectedSuccessResponse {
        Set<String> resources = new HashSet<>();

        public ExpectedResourcesWithSuccess(Set<String> resources) {
            super();
            this.resources = resources;
        }

        @Override
        public void verifyAndForward(Socket from, Socket to) throws Exception {
            super.verifyAndForward(from, to);
            DataOutputStream out = new DataOutputStream(to.getOutputStream());
            List<String> strings = Verify.checkResources(from, resources);
            for (String str : strings) {
                Static.sendJsonUTF(out, str);
            }
        }
    }

    class ExpectedMessage {
        boolean toServer;
        Verifiable message;

        public ExpectedMessage(boolean toServer, Verifiable message) {
            this.toServer = toServer;
            this.message = message;
        }
    }

    Queue<ExpectedMessage> expectedMessages = new LinkedList<>();

    Socket clientSocket;
    Socket serverSocket;

    int port;
    int serverPort;
    int serverSecurePort;
    String serverHost;

    boolean secure;
    boolean successful;

    String status = "";

    public ProxyVerifier(int port, int serverPort, int serverSecurePort, String serverHost, boolean secure) {
        this.port = port;
        this.serverPort = serverPort;
        this.serverSecurePort = serverSecurePort;
        this.serverHost = serverHost;
        this.secure = secure;
    }

    void addExpectedRequest(Verifiable verifiable) {
        expectedMessages.add(new ExpectedMessage(true, verifiable));
    }

    void addExpectedResponse(Verifiable verifiable) {
        expectedMessages.add(new ExpectedMessage(false, verifiable));
    }

    void addExpectedRequestJson(String json) {
        addExpectedRequest(new ExpectedJson(json));
    }

    void addExpectedResponseJson(String json) {
        addExpectedResponse(new ExpectedJson(json));
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

            synchronized (this) {
                // tell waiting threads that server is ready
                status = "ready";
                notifyAll();
            }

            // establish connection with server
            if (secure) {
                serverSocket = SSLSocketFactory.getDefault().createSocket(serverHost, serverSecurePort);
            } else {
                serverSocket = new Socket(serverHost, serverPort);
            }
            serverSocket.setSoTimeout(timeout);
            System.out.println("verifier got connection to server");

            // establish connection with client
            clientSocket = selfServerSocket.accept();
            clientSocket.setSoTimeout(timeout);
            System.out.println("verifier got connection from client");

            while (!(expectedMessages.isEmpty())) {
                ExpectedMessage msg = expectedMessages.remove();
                if (msg.toServer) {
                    // receive request, forward to server
                    msg.message.verifyAndForward(clientSocket, serverSocket);
                } else {
                    // receive response, forward to client
                    msg.message.verifyAndForward(serverSocket, clientSocket);
                }
            }

            successful = true;
        } catch (Exception e) {
            e.printStackTrace();
            successful = false;
        } finally {
            try {
                clientSocket.close();
                serverSocket.close();
                if (selfServerSocket != null) {
                    selfServerSocket.close();
                }
                System.out.println("verifier finish");
                synchronized (this) {
                    // tell waiting threads that verification ended
                    status = "ended";
                    notifyAll();
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
        Server server = new Server();
        new Thread(() -> server.run("-debug -secret abcd".split(" "))).start();
        server.waitUntilReady();
        ProxyVerifier verifier = new ProxyVerifier(9999, 3780, 3781, "localhost", secure);
        verifier.addExpectedRequest(new ExpectedAnyUTF());
        verifier.addExpectedResponseJson(expectedResponseJson);
        verifier.setup();
        new DummyClient(requestJson, "localhost", 9999, secure).start();
        verifier.test();
        server.stop();
    }

    static void verifyServer(String requestJson, Set<String> expectedResources, boolean secure) throws InterruptedException {
        Server server = new Server();
        new Thread(() -> server.run("-debug -secret abcd".split(" "))).start();
        server.waitUntilReady();
        ProxyVerifier verifier = new ProxyVerifier(9999, 3780, 3781, "localhost", secure);
        verifier.addExpectedRequest(new ExpectedAnyUTF());
        verifier.addExpectedResponse(new ExpectedResourcesWithSuccess(expectedResources));
        verifier.setup();
        new DummyClient(requestJson, "localhost", 9999, secure).start();
        verifier.test();
        server.stop();
    }
}
