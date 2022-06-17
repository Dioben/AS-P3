package Client;


import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Class responsible for communicating with the load balancer and servers
 */
public class TComms extends Thread implements IRegisterMessage, ISender {

    private final int selfPort;
    private final int outPort;
    private int sent = 0;
    private final ReentrantLock sendlock = new ReentrantLock();
    private GUI gui;

    /**
     * Constructor for comms class
     * @param selfPort Port we listen on
     * @param outPort Port we send data to
     * @param gui Class that receives our UI updates
     */
    TComms(int selfPort, int outPort, GUI gui) {
        this.selfPort = selfPort;
        this.outPort = outPort;
        this.gui = gui;
    }

    /**
     * Launches on selected port<br>
     * If port is valid class becomes available for request sending
     */
    @Override
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(selfPort);
            gui.setSelfPortValidity(true);
            while (true) {
                new TCommsReader(serverSocket.accept(),selfPort,this).start();
            }
        } catch (IOException e) {
            gui.setSelfPortValidity(false);
            e.printStackTrace();
        }
    }

    /**
     * Send a request to processing server
     * @param precision number of decimal places
     * @param deadline processing deadline
     */
    @Override
    public void sendRequest(int precision, int deadline){
        try {
            Socket ext = new Socket("localhost",outPort);
            PrintWriter out = new PrintWriter(ext.getOutputStream(), true);

            sendlock.lock();
            gui.updateRequest(1000*selfPort+sent, null, precision, deadline, "Pending", null);
            String msg = String.format("%d|%d|00|01|%d|00|%d", selfPort,
                    1000*selfPort+sent,
                    precision,
                    deadline);
            sent++;
            sendlock.unlock();

            out.println(msg);
            out.close();
            ext.close();
        } catch (IOException e) {
            gui.showErrorMessage("Could not connect to load balancer.");
        }
    }

    /**
     * Register a declined message on the UI
     * @param ID Request ID
     * @param server Server ID
     */
    @Override
    public void registerDecline(int ID, int server){
        gui.updateRequest(ID, server, null, null, "Rejected", null);
    }

    /**
     * Register a completed request on the UI
     * @param ID Request ID
     * @param server Server ID
     * @param response Response Data
     */
    @Override
    public void registerResponse(int ID,int server, String response){
        gui.updateRequest(ID, server, null, null, "Finished", response);
    }
}
