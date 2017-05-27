package EZShare;

import com.google.gson.Gson;

import javax.net.ssl.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyStore;

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

    static SSLContext clientSSLContext;

    public static SSLContext getClientSSLContext() {
        return clientSSLContext;
    }

    static SSLContext serverSSLContext;

    public static SSLContext getServerSSLContext() {
        return serverSSLContext;
    }

    /**
     * Set security related properties so that SSL/TLS related functions can be used
     */
    public static void configSecurity(String keyStoreFilename) {
        System.setProperty("javax.net.ssl.keyStore", keyStoreFilename);
        System.setProperty("javax.net.ssl.trustStore", keyStoreFilename);
        System.setProperty("javax.net.ssl.keyStorePassword", "distributed");
    }

    public static void setClientSslContext() throws Exception {
        clientSSLContext = makeSslContext("client.jks");
    }

    public static void setServerSslContext() throws Exception {
        serverSSLContext = makeSslContext("server.jks");
    }

//    public static Socket makeSecureClientSocket(String host, int port) {
//        return clientSSLContext.getSocketFactory().
//    }


    public static SSLContext makeSslContext(String keyStoreFilename) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        char[] password = "distributed".toCharArray();
        keyStore.load(Static.class.getResourceAsStream(keyStoreFilename), password);

        // Set up keyStore
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, password);
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(Static.class.getResourceAsStream(keyStoreFilename), password);

        // Set up trustStore
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);

        // Set up SSLContext
        SSLContext context;
        context = SSLContext.getInstance("SSL");
        context.init(keyManagerFactory.getKeyManagers(),
                trustManagerFactory.getTrustManagers(),
                null);
        return context;
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
