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

    //private TGUI gui; TODO: INSERT UI CLASS LATER

    public TServerDispatcher(int port, int watcher) {
        this.port = port;
        rl = new ReentrantLock();
        watcherContact = new TWatcherContact(watcher,port,this);
    }
    public void run() { // run socket thread creation indefinitely
        watcherContact.run();
        try {
            serverSocket = new ServerSocket(port);
            while (true) {
                new TCommsAssessor(serverSocket.accept(),this).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
            watcherContact.interrupt();
        }
    }

    public void registerNewRequest(QueuedRequest request){
        if (request.getPrecision()>13){
            reportRefusal(request);
            return;
        }
        rl.lock();

        if (threadsRunning<MAX_THREADS){
            if (complexityLoad+request.getPrecision()<=MAX_COMPLEXITY){
                threadsRunning++;
                complexityLoad+=request.getPrecision();
                new TSolver(request,this,port).start();
            }else{
                refused++;
                reportRefusal(request);
            }


    }else if( waiting.size() <MAX_PENDING){
            if (complexityLoad+request.getPrecision()<=MAX_COMPLEXITY){
                //keep pending sorted
                if (waiting.size()!=0 && waiting.get(0).getDeadline()>request.getDeadline()){
                    waiting.add(0,request);
                }
                else{
                    waiting.add(request);
                }
                complexityLoad+=request.getPrecision();
            }else{
                refused++;
                reportRefusal(request);
            }
        }
        else{ //one of the pending requests may be swapped out, keep complexity load maximum and scheduling policy in mind
            for(int i = 0;i<MAX_PENDING;i++){
                QueuedRequest queued = waiting.get(i);
                if (request.getDeadline()>queued.getDeadline() &&  (request.getPrecision()+complexityLoad-queued.getPrecision() ) <=MAX_COMPLEXITY ){
                    waiting.set(i, request);
                    reportRefusal(queued);
                    break;
                }
            }

        }
        rl.unlock();
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
        try {
            Socket res = new Socket("localhost",request.getReturnPort());
            PrintWriter out = new PrintWriter(res.getOutputStream());
            out.println(String.format("%d|%d|%d|02|%d|%d|%d", request.getReturnPort(),
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
        try {
            Socket res = new Socket("localhost",request.getReturnPort());
            PrintWriter out = new PrintWriter(res.getOutputStream());
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

