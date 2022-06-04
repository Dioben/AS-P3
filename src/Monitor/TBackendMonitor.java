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
    private void handleServer(IServerHandler handler, String initialRequest){
        //handler.registerServer();
    }
    //TODO
    private void handleLoadBalancer(ILoadBalancerHandler handler, String initialRequest){
        isLoadBalancer = true;
        //handler.registerLoadBalancer();
    }

    //TODO
    private void parseServerInput(String line){}
    //TODO
    private void parseLoadBalancerInput(String line){}

    public void shiftToPrimaryLoadBalancer(String catchUp){
        if (!isLoadBalancer)
            throw new RuntimeException("Takeover called for something that is not a load balancer");
            out.println(catchUp);
    }
}
