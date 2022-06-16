package LB;


import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.locks.ReentrantLock;


public class TServerDispatcher extends Thread {

    private final TWatcherContact watcherContact;
    private ServerSocket serverSocket;
    private final int port;
    private final ReentrantLock rl;
    //private TGUI gui; TODO: INSERT UI CLASS LATER

    public TServerDispatcher(int port, TWatcherContact watcherContact) {
        this.port = port;
        rl = new ReentrantLock();
        this.watcherContact = watcherContact;

    }
    public void run() { // run socket thread creation indefinitely
        try {
            serverSocket = new ServerSocket(port);
            watcherContact.reportReady();
            while (true) {
                new TCommsDirector(serverSocket.accept(),watcherContact).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

