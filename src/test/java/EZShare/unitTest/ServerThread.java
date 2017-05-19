package EZShare.unitTest;

import EZShare.Server;

public class ServerThread extends Thread {
    String[] args;

    public ServerThread(String[] args) {
        this.args = args;
    }

    public void run() {
        Server.main(args);
    }
}
