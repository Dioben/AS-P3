package Server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Main server class for this project
 * Endlessly accepts connection requests, requests ultimately may be rejected based on current workload
 */
public class TServerDispatcher extends Thread implements IRequestCompleted, IRequestParsed, IServerStatusProvider {

    private final static int MAX_COMPLEXITY = 20;
    private final static int MAX_THREADS = 3;
    private final static int MAX_PENDING = 2;

    private ServerSocket serverSocket;
    private final int port;
    private final TWatcherContact watcherContact;

    private final ReentrantLock rl;

    //workload stats
    private int threadsRunning = 0;
    private int complexityLoad = 0;
    private ArrayList<QueuedRequest> waiting = new ArrayList<>();

    //request statistics
    private int returned = 0;
    private int refused = 0;

    private GUI gui;

    public TServerDispatcher(int port, int watcher, GUI gui) {
        this.gui = gui;
        this.port = port;
        rl = new ReentrantLock();
        watcherContact = new TWatcherContact(watcher,port,this, gui);
        watcherContact.start();
    }
    public void run() { // run socket thread creation indefinitely
        try {
            serverSocket = new ServerSocket(port);
            gui.setSelfPortValidity(true);
            while (true) {
                new TCommsAssessor(serverSocket.accept(),this).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
            gui.setSelfPortValidity(false);
            watcherContact.interrupt();
        }
    }

    public void registerNewRequest(QueuedRequest request){
        if (request.getPrecision()>13){
            reportRefusal(request);
            return;
        }
        gui.updateRequest(request.getRequestID(), request.getReturnPort(), request.getPrecision(), request.getDeadline(), "Pending", "");
        rl.lock();

        if (threadsRunning<MAX_THREADS){
            if (complexityLoad+request.getPrecision()<=MAX_COMPLEXITY){
                threadsRunning++;
                complexityLoad+=request.getPrecision();
                rl.unlock();
                new TSolver(request,this,port).start();
            }else{
                refused++;
                rl.unlock();
                reportRefusal(request);
            }


        }else if( waiting.size() <MAX_PENDING){
            if (complexityLoad+request.getPrecision()<=MAX_COMPLEXITY){
                //keep pending sorted
                int size = waiting.size();
                int i = 0;
                for(;i<size;i++){
                    if (waiting.get(i).getDeadline()>request.getDeadline()){
                        waiting.add(i,request);
                        break;
                    }
                }
                if (i==size){
                    waiting.add(request);
                }


                complexityLoad+=request.getPrecision();
                rl.unlock();
            }else{
                refused++;
                rl.unlock();
                reportRefusal(request);
            }
        }
        else{ //one of the pending requests may be swapped out, keep complexity load maximum and scheduling policy in mind
            for(int i = 0;i<MAX_PENDING;i++){
                QueuedRequest queued = waiting.get(i);
                if (request.getDeadline()<queued.getDeadline() &&  (request.getPrecision()+complexityLoad-queued.getPrecision() ) <=MAX_COMPLEXITY ){
                    waiting.set(i, request);
                    refused++;
                    complexityLoad+= request.getPrecision()-queued.getPrecision();
                    rl.unlock();
                    reportRefusal(queued);
                    break;
                }
                if (i==MAX_PENDING-1){
                    refused++;
                    rl.unlock();
                    reportRefusal(request);
                }
            }

        }
    }

    public void onRequestCompletion(QueuedRequest request,String result){

        rl.lock();
        returned++;
        complexityLoad-=request.getPrecision();
        if (waiting.size()>0){
            QueuedRequest next = waiting.remove(0);
            rl.unlock();
            new TSolver(next,this,port).start();
        }else{
         threadsRunning--;
         rl.unlock();
        }
        reportResult(request,result);

    }

    private void reportResult(QueuedRequest request, String result){
        gui.updateRequest(request.getRequestID(), request.getReturnPort(), request.getPrecision(), request.getDeadline(), "Finished", result);
        try {
            Socket res = new Socket("localhost",request.getReturnPort());
            PrintWriter out = new PrintWriter(res.getOutputStream(), true);
            out.println(String.format("%d|%d|%d|02|%d|%s|%d", request.getReturnPort(),
                    request.getRequestID(),
                    this.port,
                    request.getPrecision(),
                    result,
                    request.getDeadline())  );
            out.close();
            res.close();
        } catch (IOException e) {}
        watcherContact.reportSuccessToMonitor(request);
    }

    private void reportRefusal(QueuedRequest request){
        gui.updateRequest(request.getRequestID(), request.getReturnPort(), request.getPrecision(), request.getDeadline(), "Rejected", "");
        try {
            Socket res = new Socket("localhost",request.getReturnPort());
            PrintWriter out = new PrintWriter(res.getOutputStream(), true);
            out.println(String.format("%d|%d|%d|03|%d|00|%d", request.getReturnPort(),
                    request.getRequestID(),
                    this.port,
                    request.getPrecision(),
                    request.getDeadline())  );
            out.close();
            res.close();
        } catch (IOException e) {}
        watcherContact.reportRejectionToMonitor(request);
    }

    @Override
    public int[] getStatus() {
        rl.lock();
        int[] data = new int[]{waiting.size() + threadsRunning, complexityLoad};
        rl.unlock();
        return data;
    }
}

