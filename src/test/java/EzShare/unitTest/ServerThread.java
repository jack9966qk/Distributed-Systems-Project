package EzShare.unitTest;

import EzShare.Server;

public class ServerThread extends Thread {
    String[] args;

    public ServerThread(String[] args) {
        this.args = args;
    }

    public void run() {
        Server.main(args);
    }
}
