package Server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class TSolver extends Thread{
    private final static String PI = "3.1415926589793";
    private final QueuedRequest request;
    private final IRequestCompleted parent;
    private final int ID;

    public TSolver(QueuedRequest request, IRequestCompleted reportTo, int id){
        this.request = request;
        parent = reportTo;
        ID = id;
    }
    @Override
    public void run() {

        try {
            Thread.sleep(5000* request.getPrecision());
        } catch (InterruptedException e) {}
        parent.onRequestCompletion(request,PI.substring(0,2+ request.getPrecision()));

    }
}
