package EZShare.unitTest;


/* from www.javaworld.com */

import EZShare.Static;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

public class EchoServer {
    public static void main(String[] arstring) {
        try {

            Static.configSecurity("keystore/server.jks");

            SSLServerSocketFactory sslserversocketfactory =
                    (SSLServerSocketFactory)
                            SSLServerSocketFactory.getDefault();
            SSLServerSocket sslserversocket =
                    (SSLServerSocket)
                            sslserversocketfactory.createServerSocket(9999);
            SSLSocket sslsocket = (SSLSocket) sslserversocket.accept();
            InputStream inputstream = sslsocket.getInputStream();
            InputStreamReader inputstreamreader =
                    new InputStreamReader(inputstream);
            BufferedReader bufferedreader =
                    new BufferedReader(inputstreamreader);
            String string = null;
            while ((string = bufferedreader.readLine()) != null) {
                System.out.println(string);
                System.out.flush();
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}
