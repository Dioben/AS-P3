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
            Socket res = new Socket("localhost",request.getReturnPort()); //TODO: MAYBE GENERIC LH
            PrintWriter out = new PrintWriter(res.getOutputStream());
            if (request.getPrecision()>13){
                out.println(String.format("%d|%d|%d| 03 |%d|00|%d", request.getReturnPort(),
                                                                    request.getRequestID(),
                                                                    ID,
                                                                    request.getPrecision(),
                                                                    request.getDeadline())  );
            }
            else{
                Thread.sleep(5000* request.getPrecision());

                out.println(String.format("%d|%d|%d| 03 |%d|%s|%d",
                                                                    request.getReturnPort(),
                                                                    request.getRequestID(),
                                                                    ID,
                                                                    request.getPrecision(),PI.substring(0,2+ request.getPrecision()),
                                                                    request.getDeadline())  );

            }
            parent.onRequestCompletion(request);

        } catch (InterruptedException | IOException e) {

        }
    }
}
