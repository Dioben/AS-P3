package Server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

public class TCommsAssessor extends Thread{
    private final Socket comms;
    private final IRequestParsed parent;

    public TCommsAssessor(Socket accept, IRequestParsed parent) {
        comms = accept;
        this.parent = parent;
    }

    @Override
    public void run() {
        QueuedRequest request = null;
        try{
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(comms.getInputStream()));
            String inputLine = in.readLine();
            String[] content = inputLine.split("|");
            int clientID = Integer.parseInt(content[0]);
            int requestID = Integer.parseInt(content[1]);
            int precision = Integer.parseInt(content[4]);
            int deadline = Integer.parseInt(content[6]);
            request = new QueuedRequest(deadline,clientID,precision,requestID);
            in.close();
            comms.close();
        }catch (Exception e){}

        parent.registerNewRequest(request);
    }

}
