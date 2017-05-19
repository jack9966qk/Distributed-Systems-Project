package EZShare.unitTest;

import EZShare.Client;
import EZShare.Server;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Created by Jack on 19/5/2017.
 */
public class IntegrationTest {
    Server serverA;
    Server serverB;
    Server serverC;

    @BeforeEach
    void beforeEach() throws InterruptedException {
        // start servers
        serverA = new Server("A");
        serverB = new Server("B");
        serverC = new Server("C");
        new Thread(() -> serverA.run("-port 3780 -sport 3781 -debug -secret abcd".split(" "))).start();
        new Thread(() -> serverB.run("-port 4780 -sport 4781 -debug -secret abcd".split(" "))).start();
        new Thread(() -> serverC.run("-port 5780 -sport 5781 -debug -secret abcd".split(" "))).start();
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

    @Test
    void test() throws InterruptedException {
        setupLinks();

        ProxyVerifier verifier;

        // client subscribe to A, by doing this, A should have relay to B but not C
        ProxyVerifier subscription = new ProxyVerifier(2333, 3780, 3781, "localhost", false);;
        subscription.addExpectedRequest(new ProxyVerifier.ExpectedAnyUTF());
        subscription.addExpectedResponse(new ProxyVerifier.ExpectedSuccessResponse());
        // client is going to receive following resources in order
        subscription.addExpectedResponseJson("{\"name\":\"Leo\", \"uri\":\"http://leo.com\"}");
        subscription.addExpectedResponseJson("{\"name\":\"Leo2\", \"uri\":\"http://leo2.com\"}");
//        subscription.addExpectedResponseJson("{\"name\":\"Leo3\", \"uri\":\"http://leo3.com\"}");
        subscription.addExpectedResponseJson("{\"resultSize\": 2}");
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
        verifier.addExpectedResponse(new ProxyVerifier.ExpectedSuccessResponse());
        verifier.setup();
        Client.main(("-host localhost -port 9999 -publish -name Leo3 -uri http://leo3.com -debug -secure").split(" "));
        verifier.test();

        System.out.println("================================================");

        // verify subscription is successful
        subscription.test();
    }
}
