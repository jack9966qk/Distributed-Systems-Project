package com.allstars.project1;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Created by Jack on 24/3/2017.
 */
public class ServiceThread extends Thread {
    Socket clientSocket;

    public ServiceThread(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    public static void publish(Resource r) {

    }

    public static void remove(Resource r) {

    }

    public static void share(Resource r) {

    }

    public static ArrayList<Resource> query(Resource template, boolean relay) {
        return new ArrayList<>();
    }

    public static void fetch(Resource template) {

    }

    @Override
    public void run() {
        try {
            DataInputStream inputStream =
                    new DataInputStream(clientSocket.getInputStream());

            // TODO read json from socket
            String reqJson = inputStream.readUTF();

            // TODO determine command type

            // TODO call methods respectively

            // TODO send response to client


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
