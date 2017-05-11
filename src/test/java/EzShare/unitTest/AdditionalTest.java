package EzShare.unitTest;

import org.junit.jupiter.api.*;
import java.util.*;

/**
 * Created by Jack on 25/4/2017.
 */
public class AdditionalTest {
    @Test
    void testFetch() throws InterruptedException {
        // fetch something that does not exist, expect success with resultSize 0
        ProxyVerifier.verifyServer("{\n" +
                "    \"command\": \"FETCH\",\n" +
                "    \"resourceTemplate\": {\n" +
                "        \"name\": \"\",\n" +
                "        \"tags\": [],\n" +
                "        \"description\": \"\",\n" +
                "        \"uri\": \"file:\\/\\/\\/\\/home\\/aaron\\/EZShare\\/ezshare.jar\",\n" +
                "        \"channel\": \"my_private_channel\",\n" +
                "        \"owner\": \"\",\n" +
                "        \"ezserver\": null\n" +
                "    }\n" +
                "}",
                new HashSet<String>(),
                false);
    }

    @Test
    void testInvalidRequestsShare() throws InterruptedException {
        // cannot share resource
        // uri not present
        ProxyVerifier.verifyServer("{\n" +
                "    \"command\": \"SHARE\",\n" +
                "    \"secret\": \"abcd\",\n" +
                "    \"resource\": {\n" +
                "        \"name\": \"EZShare JAR\",\n" +
                "        \"tags\": [\n" +
                "            \"jar\"\n" +
                "        ],\n" +
                "        \"description\": \"The jar file for EZShare. Use with caution.\",\n" +
                "        \"channel\": \"my_private_channel\",\n" +
                "        \"owner\": \"aaron010\",\n" +
                "        \"ezserver\": null\n" +
                "    }\n" +
                "}",
                "{ \"response\" : \"error\", \"errorMessage\" : \"invalid resource\" }",
                false);

        // uri not file scheme
        ProxyVerifier.verifyServer("{\n" +
                "    \"command\": \"SHARE\",\n" +
                "    \"secret\": \"abcd\",\n" +
                "    \"resource\": {\n" +
                "        \"name\": \"EZShare JAR\",\n" +
                "        \"tags\": [\n" +
                "            \"jar\"\n" +
                "        ],\n" +
                "        \"description\": \"The jar file for EZShare. Use with caution.\"," +
                "        \"uri\": \"ftp:\\/\\/\\/home\\/aaron\\/EZShare\\/ezshare.jar\"," +
                "        \"channel\": \"my_private_channel\",\n" +
                "        \"owner\": \"aaron010\",\n" +
                "        \"ezserver\": null\n" +
                "    }\n" +
                "}",
                "{ \"response\" : \"error\", \"errorMessage\" : \"invalid resource\" }",
                false);

        // file does not exist
        ProxyVerifier.verifyServer("{\n" +
                "    \"command\": \"SHARE\",\n" +
                "    \"secret\": \"abcd\",\n" +
                "    \"resource\": {\n" +
                "        \"name\": \"EZShare JAR\",\n" +
                "        \"tags\": [\n" +
                "            \"jar\"\n" +
                "        ],\n" +
                "        \"description\": \"The jar file for EZShare. Use with caution.\"," +
                "        \"uri\": \"file:\\/\\/\\/\\/home\\/aaron\\/EZShare\\/ezshare.jar\"," +
                "        \"channel\": \"my_private_channel\",\n" +
                "        \"owner\": \"aaron010\",\n" +
                "        \"ezserver\": null\n" +
                "    }\n" +
                "}",
                "{ \"response\" : \"error\", \"errorMessage\" : \"cannot share resource\" }",
                false);

        // resource contained incorrect information that could not be recovered from
        // name field type
        ProxyVerifier.verifyServer("{\n" +
                "    \"command\": \"SHARE\",\n" +
                "    \"secret\": \"abcd\",\n" +
                "    \"resource\": {\n" +
                "        \"name\": [],\n" +
                "        \"tags\": [\n" +
                "            \"jar\"\n" +
                "        ],\n" +
                "        \"description\": \"The jar file for EZShare. Use with caution.\",\n" +
                "        \"uri\": \"file:\\/\\/\\/\\/home\\/aaron\\/EZShare\\/ezshare.jar\",\n" +
                "        \"channel\": \"my_private_channel\",\n" +
                "        \"owner\": \"aaron010\",\n" +
                "        \"ezserver\": null\n" +
                "    }\n" +
                "}",
                "{ \"response\" : \"error\", \"errorMessage\" : \"invalid resource\" }",
                false);

        // incorrect secret
        ProxyVerifier.verifyServer("{\n" +
                "    \"command\": \"SHARE\",\n" +
                "    \"secret\": \"aaaaaaaaaaaaaa\",\n" +
                "    \"resource\": {\n" +
                "        \"name\": \"EZShare JAR\",\n" +
                "        \"tags\": [\n" +
                "            \"jar\"\n" +
                "        ],\n" +
                "        \"description\": \"The jar file for EZShare. Use with caution.\",\n" +
                "        \"uri\": \"file:\\/\\/\\/\\/home\\/aaron\\/EZShare\\/ezshare.jar\",\n" +
                "        \"channel\": \"my_private_channel\",\n" +
                "        \"owner\": \"aaron010\",\n" +
                "        \"ezserver\": null\n" +
                "    }\n" +
                "}",
                "{ \"response\" : \"error\", \"errorMessage\" : \"incorrect secret\" }",
                false);

        // resource field not given or not the correct type
        // no resource
        ProxyVerifier.verifyServer("{\"command\": \"REMOVE\"}",
                "{ \"response\" : \"error\", \"errorMessage\" : \"missing resource\" }",
                false);

        // incorrect type
        ProxyVerifier.verifyServer("{\"command\": \"REMOVE\", \"resource\": []}",
                "{ \"response\" : \"error\", \"errorMessage\" : \"missing resource\" }",
                false);
    }

    @Test
    void testInvalidRequestsRemove() throws InterruptedException {
        // resource did not exist: tested in testPublishRemove()

        // resource contained incorrect information that could not be recovered from
        // TODO

        // resource field not given or not the correct type
        // no resource
        ProxyVerifier.verifyServer("{\"command\": \"REMOVE\"}",
                "{ \"response\" : \"error\", \"errorMessage\" : \"missing resource\" }",
                false);

        // incorrect type
        ProxyVerifier.verifyServer("{\"command\": \"REMOVE\", \"resource\": []}",
                "{ \"response\" : \"error\", \"errorMessage\" : \"missing resource\" }",
                false);
    }

    @Test
    void testInvalidRequestsQuery() throws InterruptedException {
        // resourceTemplate field not given or not the correct type
        // no resourceTemplate
        ProxyVerifier.verifyServer("{\"command\": \"QUERY\"}",
                "{ \"response\" : \"error\", \"errorMessage\" : \"missing resourceTemplate\" }",
                false);

        // incorrect type
        ProxyVerifier.verifyServer("{\"command\": \"QUERY\", \"resourceTemplate\": []}",
                "{ \"response\" : \"error\", \"errorMessage\" : \"missing resourceTemplate\" }",
                false);
    }

    @Test
    void testInvalidRequestsFetch() throws InterruptedException {
        // resourceTemplate field not given or not the correct type
        // no resourceTemplate
        ProxyVerifier.verifyServer("{\"command\": \"FETCH\"}",
                "{ \"response\" : \"error\", \"errorMessage\" : \"missing resourceTemplate\" }",
                false);

        // incorrect type
        ProxyVerifier.verifyServer("{\"command\": \"FETCH\", \"resourceTemplate\": []}",
                "{ \"response\" : \"error\", \"errorMessage\" : \"missing resourceTemplate\" }",
                false);
    }

    @Test
    void testInvalidRequestsExchange() throws InterruptedException {
        // missing serverList
        ProxyVerifier.verifyServer("{\"command\": \"EXCHANGE\"}",
                "{ \"response\" : \"error\", \"errorMessage\" : \"missing or invalid server list\" }",
                false);

        // incorrect type
        ProxyVerifier.verifyServer("{\"command\": \"EXCHANGE\", \"serverList\": {}}",
                "{ \"response\" : \"error\", \"errorMessage\" : \"missing or invalid server list\" }",
                false);
    }

    @Test
    void testSecurityFeature() {

    }

    @Test
    void testSubscribe() {
        
    }

}
