package LB;


import java.io.*;
import java.net.Socket;
import java.util.concurrent.locks.ReentrantLock;

public class TWatcherContact extends Thread implements IMonitorInfoContact {
    private final int ID;
    private final int watcherPort;
    private PrintWriter out;
    private BufferedReader reader;

    private final ReentrantLock rl = new ReentrantLock();
    private boolean isPrimary= false;

    public TWatcherContact(int watcher, int ID) {
       watcherPort = watcher;
       this.ID = ID;
    }

    @Override
    public void run() {
        try {
            Socket watcherSocket = new Socket("localhost",watcherPort); //TODO: MAYBE GENERIC THIS TO NOT ONLY LH
            out = new PrintWriter(watcherSocket.getOutputStream());
            reader = new BufferedReader(new InputStreamReader(watcherSocket.getInputStream()));
            new TWaitTakeover(reader,this).start();
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
        String report = String.format("LB|%d",ID);
        out.println(report);
        rl.unlock();
    }

    public String requestStatusFromMonitor(String request) {
        if (!isPrimary){
            throw new RuntimeException("Should not be fetching info while not primary");
        }
        rl.lock();
        String report = String.format("LBIR|%d|%s",ID,request);
        out.println(report);

        String content = null;
        try {
            content = reader.readLine();
        } catch (IOException e) {}
        rl.unlock();
        return content;

    }

    public void reportDispatchToMonitor(String reqID, int serverID) {
        rl.lock();
        String report = String.format("LBD|%d|%s|%d",ID,reqID,serverID);
        out.println(report);
        rl.unlock();
    }



    private void setPrimary(int port) {
        if (isPrimary)
            throw new RuntimeException("Assigned Primary to same entity twice");
        new TServerDispatcher(port,this).start();
        this.isPrimary = true;

    }

    private class TWaitTakeover extends Thread{
        private final BufferedReader reader;
        private final TWatcherContact parent;
        public TWaitTakeover(BufferedReader reader,TWatcherContact parent) {
            this.reader = reader;
            this.parent = parent;
        }

        @Override
        public void run() {
            String content="";
            while(true){
                try {
                    content = reader.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (content.startsWith("TAKEOVER")){
                    parent.setPrimary(Integer.parseInt(content.split(" ")[1]) );
                    break;
                }

            }
        }
    }
}