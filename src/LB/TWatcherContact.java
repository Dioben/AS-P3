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
    private final GUI gui;
    private boolean isPrimary= false;

    public TWatcherContact(int watcher, int ID, GUI gui) {
       watcherPort = watcher;
       this.ID = ID;
       this.gui = gui;
    }

    @Override
    public void run() {
        try {
            Socket watcherSocket = new Socket("localhost",watcherPort); //TODO: MAYBE GENERIC THIS TO NOT ONLY LH
            gui.setMonitorPortValidity(true);
            out = new PrintWriter(watcherSocket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(watcherSocket.getInputStream()));
            new TWaitTakeover(reader,this).start();
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
        String report = String.format("LB|%s",ID);
        out.println(report);
        rl.unlock();
    }

    public String requestStatusFromMonitor(String request) {
        if (!isPrimary){
            throw new RuntimeException("Should not be fetching info while not primary");
        }

        String[] requestSplit = request.split("\\|");
        int clientId = Integer.parseInt(requestSplit[0]);
        int requestId = Integer.parseInt(requestSplit[1]);
        int iterations = Integer.parseInt(requestSplit[4]);
        int deadline= Integer.parseInt(requestSplit[6]);
        gui.addRequest(requestId, clientId, iterations, deadline);

        rl.lock();
        String report = String.format("LBIR|%s|%s",ID,request);
        out.println(report);

        String content = null;
        try {
            content = reader.readLine();
        } catch (IOException e) {}
        rl.unlock();

        String[] contentSplit = content.split("\\|");
        int available = 0;
        int full = 0;
        for (int i = 1; i < contentSplit.length; i+=2) {
            if (Integer.parseInt(contentSplit[i]) < 20)
                available++;
            else
                full++;
        }
        gui.setServerCounts(available, full);

        return content;

    }

    public void reportDispatchToMonitor(String reqID, int serverID) {
        gui.removeRequest(Integer.parseInt(reqID));
        rl.lock();
        String report = String.format("LBD|%d|%s|%d",ID,reqID,serverID);
        out.println(report);
        rl.unlock();
    }

    @Override
    public void reportCancelToMonitor(String reqID) {
    reportDispatchToMonitor(reqID,-1);
    }

    @Override
    public void reportReady() {
        rl.lock();
        String report = String.format("LBR");
        out.println(report);
        rl.unlock();
    }


    private void setPrimary(int port) {
        if (isPrimary)
            throw new RuntimeException("Assigned Primary to same entity twice");
        new TServerDispatcher(port,this).start();
        this.isPrimary = true;
        gui.setSelfMain();
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
                if (content == null)
                    System.exit(1);
                if (content.startsWith("TAKEOVER")){
                    parent.setPrimary(Integer.parseInt(content.split(" ")[1]) );
                    break;
                }

            }
        }
    }
}