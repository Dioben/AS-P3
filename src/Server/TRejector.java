package Server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class TRejector extends Thread{
    private final QueuedRequest request;
    private final int ID;

    public TRejector(QueuedRequest request, int id){
        this.request = request;
        ID = id;
    }
    @Override
    public void run() {

        try {
            Socket res = new Socket("localhost",request.getReturnPort()); //TODO: MAYBE GENERIC LH
            PrintWriter out = new PrintWriter(res.getOutputStream());
            out.println(String.format("%d|%d|%d| 03 |%d|00|%d", request.getReturnPort(),
                                                                    request.getRequestID(),
                                                                    ID,
                                                                    request.getPrecision(),
                                                                    request.getDeadline())  );


        } catch (IOException e) {

        }
    }
}
