package EZShare.unitTest;

import EZShare.Client;
import EZShare.Server;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Jack on 19/5/2017.
 */
public class IntegrationTest {
    Server serverA;
    Server serverB;
    Server serverC;

    static String leo1Json = "{\"name\":\"Leo\",\"description\":\"\",\"tags\":[],\"uri\":\"http://leo.com\",\"channel\":\"\",\"owner\":\"*\",\"ezserver\":\"localhost:3780\"}";
    static String leo1SecureJson = "{\"name\":\"Leo\",\"description\":\"\",\"tags\":[],\"uri\":\"http://leo.com\",\"channel\":\"\",\"owner\":\"*\",\"ezserver\":\"localhost:3781\"}";
    static String leo2Json = "{\"name\":\"Leo2\",\"description\":\"\",\"tags\":[],\"uri\":\"http://leo2.com\",\"channel\":\"\",\"owner\":\"*\",\"ezserver\":\"localhost:4780\"}";
    static String leo3Json = "{\"name\":\"Leo3\",\"description\":\"\",\"tags\":[],\"uri\":\"http://leo3.com\",\"channel\":\"\",\"owner\":\"*\",\"ezserver\":\"localhost:5781\"}";;

    @BeforeEach
    void beforeEach() throws InterruptedException {
        // start servers
        serverA = new Server("A");
        serverB = new Server("B");
        serverC = new Server("C");
        new Thread(() -> serverA.run("-advertisedhostname localhost -port 3780 -sport 3781 -debug -secret abcd".split(" "))).start();
        new Thread(() -> serverB.run("-advertisedhostname localhost -port 4780 -sport 4781 -debug -secret abcd".split(" "))).start();
        new Thread(() -> serverC.run("-advertisedhostname localhost -port 5780 -sport 5781 -debug -secret abcd".split(" "))).start();
        serverA.waitUntilReady();
        serverB.waitUntilReady();
        serverC.waitUntilReady();

        System.out.println("server initialisation complete");
    }

    @AfterEach
    void afterEach() {
        // stop servers
        serverA.stop();
        serverB.stop();
        serverC.stop();
    }

    void setupLinks() throws InterruptedException {
        // client tell A about insecure server B
        ProxyVerifier verifier = new ProxyVerifier(9999, 3780, 3781, "localhost", false);;
        verifier.addExpectedRequest(new ProxyVerifier.ExpectedAnyUTF());
        verifier.addExpectedResponse(new ProxyVerifier.ExpectedSuccessResponse());
        verifier.setup();
        Client.main(("-host localhost -port 9999 -exchange -servers localhost:4780 -debug").split(" "));
        verifier.test();

        System.out.println("================================================");

        // client tell A about secure server C
        verifier = new ProxyVerifier(9999, 3780, 3781, "localhost", true);;
        verifier.addExpectedRequest(new ProxyVerifier.ExpectedAnyUTF());
        verifier.addExpectedResponse(new ProxyVerifier.ExpectedSuccessResponse());
        verifier.setup();
        Client.main(("-host localhost -port 9999 -exchange -servers localhost:5781 -debug -secure").split(" "));
        verifier.test();

        System.out.println("================================================");
    }

    void publishLeoToAInsecure() throws InterruptedException {
        ProxyVerifier verifier = new ProxyVerifier(9999, 3780, 3781, "localhost", false);
        verifier.addExpectedRequest(new ProxyVerifier.ExpectedAnyUTF());
        verifier.addExpectedResponse(new ProxyVerifier.ExpectedSuccessResponse());
        verifier.setup();
        Client.main(("-host localhost -port 9999 -publish -name Leo -uri http://leo.com -debug").split(" "));
        verifier.test();

        System.out.println("================================================");
    }

    void publishLeo2ToBInsecure() throws InterruptedException {
        ProxyVerifier verifier = new ProxyVerifier(9999, 4780, 4781, "localhost", false);
        verifier.addExpectedRequest(new ProxyVerifier.ExpectedAnyUTF());
        verifier.addExpectedResponse(new ProxyVerifier.ExpectedSuccessResponse());
        verifier.setup();
        Client.main(("-host localhost -port 9999 -publish -name Leo2 -uri http://leo2.com -debug").split(" "));
        verifier.test();

    }

    void publishLeo3ToCSecure() throws InterruptedException {
        ProxyVerifier verifier = new ProxyVerifier(9999, 5780, 5781,  "localhost", true);
        verifier.addExpectedRequest(new ProxyVerifier.ExpectedAnyUTF());
        verifier.addExpectedResponse(new ProxyVerifier.ExpectedSuccessResponse());
        verifier.setup();
        Client.main(("-host localhost -port 9999 -publish -name Leo3 -uri http://leo3.com -debug -secure").split(" "));
        verifier.test();
    }

    @Test
    void testQuery() throws InterruptedException {
        setupLinks();

        publishLeoToAInsecure();
        publishLeo2ToBInsecure();
        publishLeo3ToCSecure();

        ProxyVerifier verifier;

        // query A about any resource insecurely, should get results from A and B
        verifier = new ProxyVerifier(9999, 3780, 3781,  "localhost", false);
        verifier.addExpectedRequest(new ProxyVerifier.ExpectedAnyUTF());
        Set<String> expected = new HashSet<>();
        expected.add(leo1Json);
        expected.add(leo2Json);
        verifier.addExpectedResponse(new ProxyVerifier.ExpectedResourcesWithSuccess(expected));
        verifier.setup();
        Client.main(("-host localhost -port 9999 -query").split(" "));
        verifier.test();

        // query A about any resource securely, should get results from A and C
        verifier = new ProxyVerifier(9999, 3780, 3781,  "localhost", true);
        verifier.addExpectedRequest(new ProxyVerifier.ExpectedAnyUTF());
        expected = new HashSet<>();
        expected.add(leo1SecureJson);
        expected.add(leo3Json);
        verifier.addExpectedResponse(new ProxyVerifier.ExpectedResourcesWithSuccess(expected));
        verifier.setup();
        Client.main(("-host localhost -port 9999 -query -secure").split(" "));
        verifier.test();
    }

    @Test
    void testSubscribe() throws InterruptedException {
        setupLinks();

        ProxyVerifier verifier;

        // client subscribe to A, by doing this, A should have relay to B but not C
        ProxyVerifier subscription = new ProxyVerifier(2333, 3780, 3781, "localhost", false);;
        subscription.addExpectedRequest(new ProxyVerifier.ExpectedAnyUTF());
        subscription.addExpectedResponseJson("{\"response\": \"success\", \"id\": \"0\"}");
        // client is going to receive following resources in order
        subscription.addExpectedResponseJson(leo1Json);
        subscription.addExpectedResponseJson(leo2Json);
//        subscription.addExpectedResponseJson(leo3Json);
        subscription.setup();
        new Thread(() -> Client.main(("-host localhost -port 2333 -subscribe -debug").split(" "))).start();
        Thread.sleep(5000);

        System.out.println("================================================");

        // another client publish Leo to A
        verifier = new ProxyVerifier(9999, 3780, 3781, "localhost", false);
        verifier.addExpectedRequest(new ProxyVerifier.ExpectedAnyUTF());
        verifier.addExpectedResponse(new ProxyVerifier.ExpectedSuccessResponse());
        verifier.setup();
        Client.main(("-host localhost -port 9999 -publish -name Leo -uri http://leo.com -debug").split(" "));
        verifier.test();

        System.out.println("================================================");

        // publish Leo2 to B
        verifier = new ProxyVerifier(9999, 4780, 4781, "localhost", false);
        verifier.addExpectedRequest(new ProxyVerifier.ExpectedAnyUTF());
        verifier.addExpectedResponse(new ProxyVerifier.ExpectedSuccessResponse());
        verifier.setup();
        Client.main(("-host localhost -port 9999 -publish -name Leo2 -uri http://leo2.com -debug").split(" "));
        verifier.test();

        System.out.println("================================================");

        // publish Leo3 securely to C, should not be forwarded to subscription client
        verifier = new ProxyVerifier(9999, 5780, 5781,  "localhost", true);
        verifier.addExpectedRequest(new ProxyVerifier.ExpectedAnyUTF());
        verifier.addExpectedResponse(new ProxyVerifier.ExpectedSuccessResponse());
        verifier.setup();
        Client.main(("-host localhost -port 9999 -publish -name Leo3 -uri http://leo3.com -debug -secure").split(" "));
        verifier.test();

        System.out.println("================================================");

        // unsubscribe
        verifier = new ProxyVerifier(9999, 3780, 3781,  "localhost", false);
        verifier.addExpectedRequest(new ProxyVerifier.ExpectedAnyUTF());
        verifier.addExpectedResponseJson("{\"resultSize\": 2}");
        verifier.setup();
        new DummyClient("{\"command\": \"UNSUBSCRIBE\", \"id\": \"0\"}", "localhost", 9999, false).start();
        verifier.test();

        System.out.println("================================================");

        // verify subscription is successful
        subscription.test();
    }
}
