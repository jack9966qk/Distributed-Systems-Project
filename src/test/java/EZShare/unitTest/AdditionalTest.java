package EZShare.unitTest;

import EZShare.Client;
import EZShare.Server;
import org.junit.jupiter.api.*;
import java.util.*;

/**
 * Created by Jack on 25/4/2017.
 */
public class AdditionalTest {
    @Test
    void testFetch() throws InterruptedException {
        // fetch something that does not exist, expect success with resultSize 0
        ProxyVerifier.verifyServerBothSecureAndInsecure("{\n" +
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
                new HashSet<String>());
    }

    @Test
    void testInvalidRequestsShare() throws InterruptedException {
        // cannot share resource
        // uri not present
        ProxyVerifier.verifyServerBothSecureAndInsecure("{\n" +
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
                "{ \"response\" : \"error\", \"errorMessage\" : \"invalid resource\" }");

        // uri not file scheme
        ProxyVerifier.verifyServerBothSecureAndInsecure("{\n" +
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
                "{ \"response\" : \"error\", \"errorMessage\" : \"invalid resource\" }");

        // file does not exist
        ProxyVerifier.verifyServerBothSecureAndInsecure("{\n" +
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
                "{ \"response\" : \"error\", \"errorMessage\" : \"cannot share resource\" }");

        // resource contained incorrect information that could not be recovered from
        // name field type
        ProxyVerifier.verifyServerBothSecureAndInsecure("{\n" +
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
                "{ \"response\" : \"error\", \"errorMessage\" : \"invalid resource\" }");

        // incorrect secret
        ProxyVerifier.verifyServerBothSecureAndInsecure("{\n" +
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
                "{ \"response\" : \"error\", \"errorMessage\" : \"incorrect secret\" }");

        // resource field not given or not the correct type
        // no resource
        ProxyVerifier.verifyServerBothSecureAndInsecure("{\"command\": \"REMOVE\"}",
                "{ \"response\" : \"error\", \"errorMessage\" : \"missing resource\" }");

        // incorrect type
        ProxyVerifier.verifyServerBothSecureAndInsecure("{\"command\": \"REMOVE\", \"resource\": []}",
                "{ \"response\" : \"error\", \"errorMessage\" : \"missing resource\" }");
    }

    @Test
    void testInvalidRequestsRemove() throws InterruptedException {
        // resource did not exist: tested in testPublishRemove()

        // resource contained incorrect information that could not be recovered from
        // TODO

        // resource field not given or not the correct type
        // no resource
        ProxyVerifier.verifyServerBothSecureAndInsecure("{\"command\": \"REMOVE\"}",
                "{ \"response\" : \"error\", \"errorMessage\" : \"missing resource\" }");

        // incorrect type
        ProxyVerifier.verifyServerBothSecureAndInsecure("{\"command\": \"REMOVE\", \"resource\": []}",
                "{ \"response\" : \"error\", \"errorMessage\" : \"missing resource\" }");
    }

    @Test
    void testInvalidRequestsQuery() throws InterruptedException {
        // resourceTemplate field not given or not the correct type
        // no resourceTemplate
        ProxyVerifier.verifyServerBothSecureAndInsecure("{\"command\": \"QUERY\"}",
                "{ \"response\" : \"error\", \"errorMessage\" : \"missing resourceTemplate\" }");

        // incorrect type
        ProxyVerifier.verifyServerBothSecureAndInsecure("{\"command\": \"QUERY\", \"resourceTemplate\": []}",
                "{ \"response\" : \"error\", \"errorMessage\" : \"missing resourceTemplate\" }");
    }

    @Test
    void testInvalidRequestsFetch() throws InterruptedException {
        // resourceTemplate field not given or not the correct type
        // no resourceTemplate
        ProxyVerifier.verifyServerBothSecureAndInsecure("{\"command\": \"FETCH\"}",
                "{ \"response\" : \"error\", \"errorMessage\" : \"missing resourceTemplate\" }");

        // incorrect type
        ProxyVerifier.verifyServerBothSecureAndInsecure("{\"command\": \"FETCH\", \"resourceTemplate\": []}",
                "{ \"response\" : \"error\", \"errorMessage\" : \"missing resourceTemplate\" }");
    }

    @Test
    void testInvalidRequestsExchange() throws InterruptedException {
        // missing serverList
        ProxyVerifier.verifyServerBothSecureAndInsecure("{\"command\": \"EXCHANGE\"}",
                "{ \"response\" : \"error\", \"errorMessage\" : \"missing or invalid server list\" }");

        // incorrect type
        ProxyVerifier.verifyServerBothSecureAndInsecure("{\"command\": \"EXCHANGE\", \"serverList\": {}}",
                "{ \"response\" : \"error\", \"errorMessage\" : \"missing or invalid server list\" }");
    }

}
