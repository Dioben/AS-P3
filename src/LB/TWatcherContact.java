package LB;


import java.io.*;
import java.net.Socket;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Keeps up regular contact with monitor
 * If this fails to regularly contact Monitor load balancer is unregistered
 */
public class TWatcherContact extends Thread implements IMonitorInfoContact {
    private final int ID;
    private final int watcherPort;
    private PrintWriter out;
    private BufferedReader reader;

    private final ReentrantLock rl = new ReentrantLock();
    private final GUI gui;
    private boolean isPrimary= false;

    /**
     *
     * @param watcher Monitor port
     * @param ID This entity's self assigned ID #TODO: SUSSY
     * @param gui UI entity
     */
    public TWatcherContact(int watcher, int ID, GUI gui) {
       watcherPort = watcher;
       this.ID = ID;
       this.gui = gui;
    }

    /**
     * Report to monitor every 5 seconds
     */
    @Override
    public void run() {
        try {
            Socket watcherSocket = new Socket("localhost",watcherPort);
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

    /**
     * Send plain heartbeat
     */
    private void reportToMonitor() {
        rl.lock();
        String report = String.format("LB|%s",ID);
        out.println(report);
        rl.unlock();
    }

    /**
     * Request information about servers
     * @param request Request we are registering with monitor
     * @return Server Statuses
     */
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

    /**
     * Report a dispatch to monitor
     * @param reqID Request ID
     * @param serverID Server ID
     */
    public void reportDispatchToMonitor(String reqID, int serverID) {
        gui.removeRequest(Integer.parseInt(reqID));
        rl.lock();
        String report = String.format("LBD|%d|%s|%d",ID,reqID,serverID);
        out.println(report);
        rl.unlock();
    }

    /**
     * Report a cancelled request to monitor
     * @param reqID Request ID
     */
    @Override
    public void reportCancelToMonitor(String reqID) {
    reportDispatchToMonitor(reqID,-1);
    }

    /**
     * Report that process is ready to take over as primary
     */
    @Override
    public void reportReady() {
        rl.lock();
        String report = String.format("LBR");
        out.println(report);
        rl.unlock();
    }

    /**
     * Set self as a primary load balancer
     * @param port
     */
    private void setPrimary(int port) {
        if (isPrimary)
            throw new RuntimeException("Assigned Primary to same entity twice");
        new TServerDispatcher(port,this, gui).start();
        this.isPrimary = true;
        gui.setSelfMain();
    }

    /**
     * Inner thread class that waits for a takeover event
     */
    private class TWaitTakeover extends Thread{
        private final BufferedReader reader;
        private final TWatcherContact parent;

        /**
         *
         * @param reader Data input
         * @param parent Entity to declare as primary
         */
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