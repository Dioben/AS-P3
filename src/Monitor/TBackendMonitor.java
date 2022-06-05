package Monitor;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TBackendMonitor extends  Thread{
    private static final int SOCKET_TIMEOUT = 11000;
    private final Socket comms;
    private BufferedReader in;
    private PrintWriter out;
    private final IHandlerSource parent;
    private boolean isLoadBalancer = false;

    public TBackendMonitor(Socket accept, IHandlerSource tServerDispatcher) {
        comms = accept;
        parent = tServerDispatcher;
    }

    @Override
    public void run() {
        try {
            comms.setSoTimeout(SOCKET_TIMEOUT); //set this socket to time out if nothing is said in 11 second window
            in = new BufferedReader(new InputStreamReader(comms.getInputStream()));
            out = new PrintWriter(comms.getOutputStream());
            String inputLine = in.readLine();
            if (inputLine.startsWith("S")){
                handleServer(parent.getServerHandler(),inputLine);
            }
            else if (inputLine.startsWith("LB")){
                handleLoadBalancer(parent.getLoadBalancerHandler(),inputLine);
            }
        } catch (IOException e) {}
    }
    //TODO: MAYBE MOVE THESE HANDLE/PARSE INTO CUSTOM OBJECTS
    private void handleServer(IServerHandler handler, String request){
        int port = Integer.parseInt(request.split("\\|")[1]);
        parseServerInput(handler,request);
        while (true){
            try {
                parseServerInput(handler,in.readLine());
            } catch (IOException e) {
                handler.removeServer(port);
            }
        }
    }

    private void handleLoadBalancer(ILoadBalancerHandler handler, String request){
        isLoadBalancer = true;
        if (!request.startsWith("LB|")) //basic keepalive rather than advanced message
            throw new RuntimeException("Load Balancer is asking for information before primary");
        int port = Integer.parseInt(request.split("|")[1]);
        handler.registerLoadBalancer(port,this);
        while (true){
            try {
                parseLoadBalancerInput(handler,in.readLine());
            } catch (IOException e) {
                handler.removeLoadBalancer(port);
            }
        }
    }

    //TODO
    private void parseServerInput(IServerHandler handler,String line){
        String[] request = line.split("\\|");
        int port = Integer.parseInt(request[1]);
        int complexity = Integer.parseInt(request[3]);
        handler.registerServer(port,complexity);

        if (request[0].equals("SR")){//refusal
            handler.notifyRefused(Integer.parseInt(request[request.length-1]) );
        }
        else if (request[0].equals("SD")){ //done
            handler.notifyDone(Integer.parseInt(request[request.length-1]) );
        }

    }

    private void parseLoadBalancerInput(ILoadBalancerHandler handler,String line){
        String[] request = line.split("\\|");
        if (request[0].equals("LBIR")){
            int client = Integer.parseInt(request[2]);
            int reqID = Integer.parseInt(request[3]);
            int iter = Integer.parseInt(request[6]);
            int deadline= Integer.parseInt(request[8]);
            String status = handler.notifyHandling(client,reqID,iter,deadline);
            out.println(status);
        }
        else if (request[0].equals("LBD")){
            int reqID = Integer.parseInt(request[2]);
            int serverID = Integer.parseInt(request[3]);
            handler.notifyDispatched(reqID,serverID);
        }
    }

    public void shiftToPrimaryLoadBalancer(String catchUp){
        if (!isLoadBalancer)
            throw new RuntimeException("Takeover called for something that is not a load balancer");
            out.println(catchUp);
    }
}
