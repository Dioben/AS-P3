package Server;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Main server class for this project
 * Endlessly accepts connection requests, requests ultimately may be rejected based on current workload
 */
public class TServerDispatcher extends Thread{
    private ServerSocket serverSocket;
    private final int port;
    private final int watcherPort;


    //workload stats
    private int threadsRunning = 0;
    private int queuedRequests = 0;
    private int complexityLoad = 0;
    private  QueuedRequest[] waiting = new QueuedRequest[2];

    //request statistics
    private int returned = 0;
    private int refused = 0;

    //private TGUI gui; TODO: INSERT UI CLASS LATER

    public TServerDispatcher(int port, int watcher) {
        this.port = port;
        this.watcherPort = watcher;
    }
    //TODO: HANDLE REQUEST STORAGE LOGIC, DISPATCHING, MAKE THE HEARTBEAT THINGY
    public void run() { // run socket thread creation indefinitely
        try {
            serverSocket = new ServerSocket(port);
            while (true) {
                new TCommsHandler(serverSocket.accept(), gui).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

