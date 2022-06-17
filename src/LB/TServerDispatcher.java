package LB;


import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.locks.ReentrantLock;


public class TServerDispatcher extends Thread {

    private final TWatcherContact watcherContact;
    private ServerSocket serverSocket;
    private final int port;
    private final ReentrantLock rl;
    private final GUI gui;

    public TServerDispatcher(int port, TWatcherContact watcherContact, GUI gui) {
        this.port = port;
        rl = new ReentrantLock();
        this.watcherContact = watcherContact;
        this.gui = gui;
    }
    public void run() { // run socket thread creation indefinitely
        try {
            serverSocket = new ServerSocket(port);
            watcherContact.reportReady();
            while (true) {
                new TCommsDirector(serverSocket.accept(),watcherContact).start();
            }
        } catch (IOException e) {
            gui.showErrorMessage("Invalid main load balancer port.");
            e.printStackTrace();
        }
    }

}

