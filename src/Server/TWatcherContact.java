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
    public TWatcherContact(int watcher,int ID, IServerStatusProvider provider) {
       watcherPort = watcher;
       this.ID = ID;
       this.provider = provider;
    }

    @Override
    public void run() {
        try {
            Socket watcherSocket = new Socket("localhost",watcherPort); //TODO: MAYBE GENERIC THIS TO NOT ONLY LH
            out = new PrintWriter(watcherSocket.getOutputStream());

            while(! Thread.interrupted()){
                Thread.sleep(5000);
                reportToMonitor();
            }

        } catch (IOException | InterruptedException e) {
            this.interrupt();
        }
    }

    private void reportToMonitor() {
        rl.lock();
        int[] info = provider.getStatus();
        String report = String.format("S|%d|%d|%d",ID,info[0],info[1]);
        out.println(report);
        rl.unlock();
    }

    public void reportSuccessToMonitor(QueuedRequest request) {
        rl.lock();
        int[] info = provider.getStatus();
        String report = String.format("SD|%d|%d|%d|%d|%d",ID,info[0],info[1],request.getReturnPort(),request.getRequestID());
        out.println(report);
        rl.unlock();
    }

    public void reportRejectionToMonitor(QueuedRequest request) {
        rl.lock();
        int[] info = provider.getStatus();
        String report = String.format("SR|%d|%d|%d|%d|%d",ID,info[0],info[1],request.getReturnPort(),request.getRequestID());
        out.println(report);
        rl.unlock();
    }

}