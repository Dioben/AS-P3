package Server;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.locks.ReentrantLock;

public class TWatcherContact extends Thread{
    private final int ID;
    private final int watcherPort;
    private PrintWriter out;
    private final IServerStatusProvider provider;
    private final ReentrantLock rl = new ReentrantLock();
    private GUI gui;

    public TWatcherContact(int watcher,int ID, IServerStatusProvider provider, GUI gui) {
       watcherPort = watcher;
       this.ID = ID;
       this.provider = provider;
       this.gui = gui;
    }

    @Override
    public void run() {
        try {
            Socket watcherSocket = new Socket("localhost",watcherPort); //TODO: MAYBE GENERIC THIS TO NOT ONLY LH
            gui.setMonitorPortValidity(true);
            out = new PrintWriter(watcherSocket.getOutputStream());

            while(! Thread.interrupted()){
                Thread.sleep(5000);
                reportToMonitor();
            }

        } catch (IOException | InterruptedException e) {
            gui.setMonitorPortValidity(false);
            this.interrupt();
        }
    }

    private void reportToMonitor() {
        rl.lock();
        int[] info = provider.getStatus();
        String report = String.format("S|%s|%d|%d",ID,info[0],info[1]);
        out.println(report);
        rl.unlock();
    }

    public void reportSuccessToMonitor(QueuedRequest request) {
        rl.lock();
        int[] info = provider.getStatus();
        String report = String.format("SD|%s|%d|%d|%d|%d",ID,info[0],info[1],request.getReturnPort(),request.getRequestID());
        out.println(report);
        rl.unlock();
    }

    public void reportRejectionToMonitor(QueuedRequest request) {
        rl.lock();
        int[] info = provider.getStatus();
        String report = String.format("SR|%s|%d|%d|%d|%d",ID,info[0],info[1],request.getReturnPort(),request.getRequestID());
        out.println(report);
        rl.unlock();
    }

}