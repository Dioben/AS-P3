package LB;


import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Endlessly accepts connections and dispatches comm directors
 */
public class TServerDispatcher extends Thread {

    private final TWatcherContact watcherContact;
    private ServerSocket serverSocket;
    private final int port;
    private final ReentrantLock rl;
    private final GUI gui;

    /**
     *
     * @param port Server port
     * @param watcherContact Entity that communicates with monitor
     * @param gui UI entity
     */
    public TServerDispatcher(int port, TWatcherContact watcherContact, GUI gui) {
        this.port = port;
        rl = new ReentrantLock();
        this.watcherContact = watcherContact;
        this.gui = gui;
    }

    /**
     *  Endlessly accepts connections and dispatches comm directors
     */
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

