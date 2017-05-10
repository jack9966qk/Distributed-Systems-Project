package EzShare.unitTest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.DataInputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Jack on 10/5/2017.
 */
public class Verify {
    private static void removeAllNullFields(JsonObject jsonObject) {
        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            String key = entry.getKey();
            if (jsonObject.get(key).isJsonNull()) {
                jsonObject.remove(key);
            } else if (jsonObject.get(key).isJsonObject()) {
                removeAllNullFields(jsonObject.getAsJsonObject(key));
            }
        }
    }

    static boolean areEquivalentJson(String json1, String json2) {
        JsonParser parser = new JsonParser();
        JsonObject obj1 = parser.parse(json1).getAsJsonObject();
        JsonObject obj2 = parser.parse(json2).getAsJsonObject();
        removeAllNullFields(obj1);
        removeAllNullFields(obj2);
        return obj1.equals(obj2);
    }

    static void assertJsonEquivalent(String actual, String expected) throws Exception {
        if (!areEquivalentJson(actual, expected)) {
            String str = "";
            str += "ACTUAL>>>>>>>>>>>\n";
            str += actual + "\n";
            str += "=================\n";
            str += expected + "\n";
            str += "EXPECTED<<<<<<<<<\n";
            System.err.println(str);
            throw new Exception("assert failed");
        }
    }

    static List<String> checkResources(Socket socket, Set<String> expectedResourcesJson) throws Exception {
        List<String> receivedStr = new ArrayList<>();
        List<String> receivedResources = new ArrayList<>();
        String receivedResultSize = null;
        DataInputStream inStream = new DataInputStream(socket.getInputStream());
        while (true) {
            String str = inStream.readUTF();
            receivedStr.add(str);
            if (str.contains("resultSize")) {
                receivedResultSize = str;
                break;
            } else {
                receivedResources.add(str);
            }
        }

        JsonObject resultSizeObj = new JsonParser().parse(receivedResultSize).getAsJsonObject();

        if (resultSizeObj.get("resultSize").getAsInt() != receivedResources.size()) {
            throw new Exception("size does not match");
        }

        for (String receivedResource : receivedResources) {
            boolean ok = false;
            for (String expectedResource : expectedResourcesJson) {
                if (areEquivalentJson(receivedResource, expectedResource)) {
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                throw new Exception("resources do not match");
            }
        }
        return receivedStr;
    }
}
