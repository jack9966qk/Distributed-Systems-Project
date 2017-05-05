package EzShare.unitTest;

import EzShare.Client;
import EzShare.Resource;

import EzShare.Static;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

/**
 * Created by Zheping on 2017/3/26.
 */
class ClientTest {

    private static ServerSocket serviceSocket = null;
    private Socket socket = null;
    private Resource resource;
    private static ArrayList<Resource> resourceSet = new ArrayList<Resource>();
    private static final String HOST = "localhost";
    private static final int PORT = 2334;

    void serverAccept() {

        Socket clientSocket = null;
        try {
            clientSocket = serviceSocket.accept();
            System.out.println("Received connection: " + 1);

            DummyServerThread service = new DummyServerThread(clientSocket);
            service.start();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @BeforeAll
    static void setUp() {
        // set up the dummy server and create a Client instance
        try {
            serviceSocket = new ServerSocket(PORT);
            System.out.println("Server listening to connections.");

            resourceSet.add(new Resource("","", new String[0], "", "", "", ""));
            resourceSet.add(new Resource("Leo", "Leoo", new String[0], "leo1", "Private", "BigO", ""));
            resourceSet.add(new Resource("Jack", "Zaku", new String[0], "jack", "Private", "JackD", ""));
            resourceSet.add(new Resource("Leo", "Leoooooo", new String[0], "leo1", "Private", "BigO", ""));
            resourceSet.add(new Resource("Leo1", "Leoo", new String[0], "leo1", "Private", "SmallO", ""));
            resourceSet.add(new Resource("Jack", "Zaku", new String[0], "jack", "Private", "*", ""));


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void connectToServer() {

        try {
            socket = Client.connectToServer(HOST, PORT, Static.DEFAULT_TIMEOUT);
            serverAccept();
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            out.writeUTF("Connected.");
            System.out.println(in.readUTF());

        } catch (Exception e) {
            e.printStackTrace();
        }



    }

    @Test
    void getOptions() {

        String[] testCmd = new String[] {"-host", HOST, "-port", "2334","-publish"};
        CommandLine cmd = null;
        try {
            cmd = Client.getOptions(testCmd);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        System.out.println(cmd.toString());
    }

    @Test
    void publish() {

        try {
            for (Resource r : resourceSet) {
                socket = Client.connectToServer(HOST, PORT, Static.DEFAULT_TIMEOUT);
                serverAccept();
                Client.publish(socket, r);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Disabled
    @Test
    void share() {
        String secret = "IHaveNoIdeaWhatTheHellIsThis";
        try {
            for (Resource r : resourceSet) {
                socket = Client.connectToServer(HOST, PORT, Static.DEFAULT_TIMEOUT);
                serverAccept();
                //Client.share(socket, secret, r);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Test
    void makeResourceFromCmd() {
        String[] publish = new String[] {"-host","localhost", "-port", "2334","-publish","-name","Leo",
                                        "-description","LeoBigO","-uri","www.leo.com",
                                        "-tags", "ntr,double", "-owner", "Leooo", "-channel", "Personal"};
        CommandLine cmd = null;

        try {
            cmd = Client.getOptions(publish);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        resource = Client.makeResourceFromCmd(cmd);

        // Assert names
        Assertions.assertTrue(resource.getName().equals("Leo"));
        // Assert tags
        String[] tags = {"ntr", "double"};
        Assertions.assertTrue(Arrays.equals(tags, resource.getTags()));
        // Assert uri
        Assertions.assertTrue(resource.getUri().equals("www.leo.com"));
        // Assert description
        Assertions.assertTrue(resource.getDescription().equals("LeoBigO"));
        // Assert owner
        Assertions.assertTrue(resource.getOwner().equals("Leooo"));
        // Assert channel
        Assertions.assertTrue(resource.getChannel().equals("Personal"));
    }

    @Test
    void main() {
        // put all protected function test here
        
    }
}