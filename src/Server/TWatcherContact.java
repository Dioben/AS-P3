package Server;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Entity responsible for keeping up contact with the watcher thread
 * Sends periodic updates, if this mechanism fails the server is unregistered
 */
public class TWatcherContact extends Thread{
    private final int ID;
    private final int watcherPort;
    private PrintWriter out;
    private final IServerStatusProvider provider;
    private final ReentrantLock rl = new ReentrantLock();
    private final GUI gui;

    public TWatcherContact(int watcher,int ID, IServerStatusProvider provider, GUI gui) {
       watcherPort = watcher;
       this.ID = ID;
       this.provider = provider;
       this.gui = gui;
    }

    /**
     * Connect to monitor and send updates every 5 seconds
     */
    @Override
    public void run() {
        try {
            Socket watcherSocket = new Socket("localhost",watcherPort);
            gui.setMonitorPortValidity(true);
            out = new PrintWriter(watcherSocket.getOutputStream(), true);

            while(! Thread.interrupted()){
                Thread.sleep(5000);
                reportToMonitor();
            }

        } catch (IOException | InterruptedException e) {
            gui.setMonitorPortValidity(false);
            this.interrupt();
        }
    }

    /**
     * Report existence and current load stats to monitor
     */
    private void reportToMonitor() {
        rl.lock();
        int[] info = provider.getStatus();
        String report = String.format("S|%d|%d|%d",ID,info[0],info[1]);
        out.println(report);
        rl.unlock();
    }

    /**
     * Report request success to a monitor including current load status
     * @param request Completed request
     */
    public void reportSuccessToMonitor(QueuedRequest request) {
        rl.lock();
        int[] info = provider.getStatus();
        String report = String.format("SD|%d|%d|%d|%d|%d",ID,info[0],info[1],request.getReturnPort(),request.getRequestID());
        out.println(report);
        rl.unlock();
    }

    /**
     * Report request rejection to monitor
     * @param request Rejected Request
     */
    public void reportRejectionToMonitor(QueuedRequest request) {
        rl.lock();
        int[] info = provider.getStatus();
        String report = String.format("SR|%d|%d|%d|%d|%d",ID,info[0],info[1],request.getReturnPort(),request.getRequestID());
        out.println(report);
        rl.unlock();
    }

}