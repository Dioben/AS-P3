package Server;

import java.io.IOException;
import java.net.ServerSocket;
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
        rl.lock();

        if (threadsRunning<MAX_THREADS){
            if (complexityLoad+request.getPrecision()>MAX_COMPLEXITY){
                threadsRunning++;
                complexityLoad+=request.getPrecision();
                new TSolver(request,this,port).start();
            }else{
                refused++;
                new TRejector(request,port).start();
            }


    }else if( waiting.size() <MAX_PENDING){
            if (complexityLoad+request.getPrecision()>MAX_COMPLEXITY){
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
                new TRejector(request,port).start();
            }
        }
        else{ //one of the pending requests may be swapped out, keep complexity load maximum and scheduling policy in mind
            for(int i = 0;i<2;i++){
                QueuedRequest queued = waiting.get(i);
                if (request.getDeadline()>queued.getDeadline() &&  (request.getPrecision()+complexityLoad-queued.getPrecision() ) <=MAX_COMPLEXITY ){
                    new TRejector(queued,port).start();
                    waiting.set(i, request);
                    break;
                }
            }

        }
        rl.unlock();
    }

    public void onRequestCompletion(QueuedRequest request){
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
        watcherContact.reportToMonitor();
    }

    @Override
    public int[] getStatus() {
        rl.lock();
        int[] data = new int[]{waiting.size() + threadsRunning, complexityLoad};
        rl.unlock();
        return data;
    }
}

