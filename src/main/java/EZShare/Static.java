package EZShare;

import com.google.gson.Gson;
import java.io.*;

/**
 * Class for globally available static variables, functions and constants
 * Created by Jack on 25/3/2017.
 */
public class Static {
    public static final Gson GSON = new Gson();
    public static final int DEFAULT_EXCHANGE_INTERVAL = 1000 * 60 * 10;
    public static final int DEFAULT_CONNECTION_INTERVAL = 1000;
    public static final int DEFAULT_TIMEOUT = 1000 * 10;
    public static final int FILE_READ_WRITE_CHUNK_SIZE = 16 * 1024;
    public static final int DEFAULT_SPORT = 3781;
    public static final int DEFAULT_PORT = 3780;

    // extract jks files to be loaded to keyStore/trustStore
    static {
        try {
            setupKeyStoreFile("client.jks");
            setupKeyStoreFile("server.jks");
        } catch (IOException e) {
            System.out.println("Error setting up keystore files, exiting...");
            System.exit(0);
        }
    }

    /**
     * Set security related properties so that SSL/TLS related functions can be used
     */
    public static void configSecurity(String keyStoreFilename) {
        System.setProperty("javax.net.ssl.keyStore", keyStoreFilename);
        System.setProperty("javax.net.ssl.trustStore", keyStoreFilename);
        System.setProperty("javax.net.ssl.keyStorePassword", "distributed");
    }

    /**
     * Set up keyStore files from JAR
     *
     * @param keyStoreFilename keyStore filename inside the JAR
     * @throws IOException Error extracting the JKS file
     */
    public static void setupKeyStoreFile(String keyStoreFilename) throws IOException {
        InputStream input = Static.class.getResourceAsStream(keyStoreFilename);
        File file = new File(keyStoreFilename);
        OutputStream output = new FileOutputStream(file);

        byte[] bytes = new byte[FILE_READ_WRITE_CHUNK_SIZE];
        int read;
        while ((read = input.read(bytes)) != -1) {
            output.write(bytes, 0, read);
        }
    }

    /**
     * Escape '/' characters with '\'
     *
     * @param str string
     * @return string with '/' characters escaped
     */
    private static String addEscChar(String str) {
        return str.replace("/", "\\/");
    }

    /**
     * Remove '\' before '/' characters in string
     *
     * @param str string
     * @return string with '\' characters before '/' characters removed
     */
    private static String removeEscChar(String str) {
        return str.replace("\\/", "/");
    }

    /**
     * Send EzServer JSON message
     *
     * @param out output stream to send to
     * @param str string of JSON message
     * @throws IOException any network error
     */
    public static void sendJsonUTF(DataOutputStream out, String str) throws IOException {
        String escaped = addEscChar(str);
        out.writeUTF(escaped);
        Logging.logFine("SENT: " + escaped);
    }

    /**
     * Remove EzServer JSON message
     *
     * @param in input stream to read from
     * @return string of JSON message
     * @throws IOException any network error
     */
    public static String readJsonUTF(DataInputStream in) throws IOException {
        String str = in.readUTF();
        Logging.logFine("RECEIVED: " + str);
        String removed = removeEscChar(str);
        return removeEscChar(removed);
    }
}
