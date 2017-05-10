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
        ServerVerifier.verifyServer("{\n" +
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
                "{ \"response\" : \"success\" }",
                new HashSet<String>(),
                false).test();
    }

    @Test
    void testInvalidRequestsShare() throws InterruptedException {
        // cannot share resource
        // uri not present
        ServerVerifier.verifyServer("{\n" +
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
                false).test();

        // uri not file scheme
        ServerVerifier.verifyServer("{\n" +
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
                false).test();

        // file does not exist
        ServerVerifier.verifyServer("{\n" +
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
                false).test();

        // resource contained incorrect information that could not be recovered from
        // name field type
        ServerVerifier.verifyServer("{\n" +
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
                false).test();

        // incorrect secret
        ServerVerifier.verifyServer("{\n" +
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
                false).test();

        // resource field not given or not the correct type
        // no resource
        ServerVerifier.verifyServer("{\"command\": \"REMOVE\"}",
                "{ \"response\" : \"error\", \"errorMessage\" : \"missing resource\" }",
                false).test();

        // incorrect type
        ServerVerifier.verifyServer("{\"command\": \"REMOVE\", \"resource\": []}",
                "{ \"response\" : \"error\", \"errorMessage\" : \"missing resource\" }",
                false).test();
    }

    @Test
    void testInvalidRequestsRemove() throws InterruptedException {
        // resource did not exist: tested in testPublishRemove()

        // resource contained incorrect information that could not be recovered from
        // TODO

        // resource field not given or not the correct type
        // no resource
        ServerVerifier.verifyServer("{\"command\": \"REMOVE\"}",
                "{ \"response\" : \"error\", \"errorMessage\" : \"missing resource\" }",
                false).test();

        // incorrect type
        ServerVerifier.verifyServer("{\"command\": \"REMOVE\", \"resource\": []}",
                "{ \"response\" : \"error\", \"errorMessage\" : \"missing resource\" }",
                false).test();
    }

    @Test
    void testInvalidRequestsQuery() throws InterruptedException {
        // resourceTemplate field not given or not the correct type
        // no resourceTemplate
        ServerVerifier.verifyServer("{\"command\": \"QUERY\"}",
                "{ \"response\" : \"error\", \"errorMessage\" : \"missing resourceTemplate\" }",
                false).test();

        // incorrect type
        ServerVerifier.verifyServer("{\"command\": \"QUERY\", \"resourceTemplate\": []}",
                "{ \"response\" : \"error\", \"errorMessage\" : \"missing resourceTemplate\" }",
                false).test();
    }

    @Test
    void testInvalidRequestsFetch() throws InterruptedException {
        // resourceTemplate field not given or not the correct type
        // no resourceTemplate
        ServerVerifier.verifyServer("{\"command\": \"FETCH\"}",
                "{ \"response\" : \"error\", \"errorMessage\" : \"missing resourceTemplate\" }",
                false).test();

        // incorrect type
        ServerVerifier.verifyServer("{\"command\": \"FETCH\", \"resourceTemplate\": []}",
                "{ \"response\" : \"error\", \"errorMessage\" : \"missing resourceTemplate\" }",
                false).test();
    }

    @Test
    void testInvalidRequestsExchange() throws InterruptedException {
        // missing serverList
        ServerVerifier.verifyServer("{\"command\": \"EXCHANGE\"}",
                "{ \"response\" : \"error\", \"errorMessage\" : \"missing or invalid server list\" }",
                false).test();

        // incorrect type
        ServerVerifier.verifyServer("{\"command\": \"EXCHANGE\", \"serverList\": {}}",
                "{ \"response\" : \"error\", \"errorMessage\" : \"missing or invalid server list\" }",
                false).test();
    }

    @Test
    void testSecurityFeature() {

    }

    @Test
    void testSubscribe() {
        
    }

}
