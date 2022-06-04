package Monitor;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TBackendMonitor extends  Thread{
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
            comms.setSoTimeout(11000); //set this socket to time out if nothing is said in 11 second window
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

    //TODO
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
    //TODO
    private void handleLoadBalancer(ILoadBalancerHandler handler, String request){
        isLoadBalancer = true;
        //handler.registerLoadBalancer();
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
    //TODO
    private void parseLoadBalancerInput(String line){}

    public void shiftToPrimaryLoadBalancer(String catchUp){
        if (!isLoadBalancer)
            throw new RuntimeException("Takeover called for something that is not a load balancer");
            out.println(catchUp);
    }
}
