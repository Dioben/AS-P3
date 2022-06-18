package Monitor;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Class that reads new connections and handles servers and load balancers<br>
 * Can time out when information does not come in for too long
 */
public class TBackendMonitor extends  Thread{
    private static final int SOCKET_TIMEOUT = 11000;
    private final Socket comms;
    private BufferedReader in;
    private PrintWriter out;
    private final ReentrantLock rl = new ReentrantLock();
    private final Condition primaryActivated = rl.newCondition();
    private final IHandlerSource parent;
    private boolean isLoadBalancer = false;
    private boolean activatedPrimary = false;

    /**
     *
     * @param accept connection data is received on
     * @param tServerDispatcher Origin of server or load balancer handler
     */
    public TBackendMonitor(Socket accept, IHandlerSource tServerDispatcher) {
        comms = accept;
        parent = tServerDispatcher;
    }

    @Override
    public void run() {
        try {
            comms.setSoTimeout(SOCKET_TIMEOUT); //set this socket to time out if nothing is said in 11 second window
            in = new BufferedReader(new InputStreamReader(comms.getInputStream()));
            out = new PrintWriter(comms.getOutputStream(), true);
            String inputLine = in.readLine();
            if (inputLine.startsWith("S")){
                handleServer(parent.getServerHandler(),inputLine);
            }
            else if (inputLine.startsWith("LB")){
                handleLoadBalancer(parent.getLoadBalancerHandler(),inputLine);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Parse server info requests on a loop<br>
     * Can time out
     * @param handler Server method container
     * @param request First received request
     */
    private void handleServer(IServerHandler handler, String request){
        int port = Integer.parseInt(request.split("\\|")[1]);
        parseServerInput(handler,request);
        while (true){
            try {
                parseServerInput(handler,in.readLine());
            } catch (IOException | NullPointerException e) {
                handler.removeServer(port);
                try{
                    in.close();
                    out.close();
                    comms.close();

                } catch (IOException ioException) {}
                return;

            }
        }
    }

    /**
     * Parse LB info requests on a loop<br>
     * Can time out
     * Can be rejected if overly full
     * @param handler LB method container
     * @param request First received request
     */
    private void handleLoadBalancer(ILoadBalancerHandler handler, String request) {
        isLoadBalancer = true;
        if (!request.startsWith("LB|")) //basic keepalive rather than advanced message
            throw new RuntimeException("Load Balancer is asking for information before primary");
        int port = Integer.parseInt(request.split("\\|")[1]);
        if (!handler.registerLoadBalancer(port,this)){
            try {
                in.close();
                out.close();
                comms.close();
            } catch (IOException e) {}
            return;
        }
        while (true){
            try {
                parseLoadBalancerInput(handler,in.readLine());
            } catch (IOException | NullPointerException e) {
                handler.removeLoadBalancer(port);
                try {
                    in.close();
                    out.close();
                    comms.close();
                } catch (IOException ex) {}
                return;
            }

        }
    }

    private void parseServerInput(IServerHandler handler,String line){
        String[] request = line.split("\\|");
        int port = Integer.parseInt(request[1]);
        int complexity = Integer.parseInt(request[3]);
        handler.registerServer(port,complexity);

        if (request[0].equals("SR")){//refusal
            handler.notifyRefused(Integer.parseInt(request[1]),Integer.parseInt(request[request.length-1]) );
        }
        else if (request[0].equals("SD")){ //done
            handler.notifyDone(Integer.parseInt(request[1]),Integer.parseInt(request[request.length-1]) );
        }

    }

    private void parseLoadBalancerInput(ILoadBalancerHandler handler,String line){
        String[] request = line.split("\\|");
        if (request[0].equals("LBIR")){
            int id = Integer.parseInt(request[1]);
            int client = Integer.parseInt(request[2]);
            int reqID = Integer.parseInt(request[3]);
            int iter = Integer.parseInt(request[6]);
            int deadline= Integer.parseInt(request[8]);
            String status = handler.notifyHandling(id,client,reqID,iter,deadline);
            rl.lock();
            out.println(status);
            rl.unlock();
        }
        else if (request[0].equals("LBD")){
            int id = Integer.parseInt(request[1]);
            int reqID = Integer.parseInt(request[2]);
            int serverID = Integer.parseInt(request[3]);
            handler.notifyDispatched(id,serverID,reqID);
        }
        else if (request[0].equals("LBR")){
            rl.lock();
            activatedPrimary = true;
            primaryActivated.signalAll();
            rl.unlock();
        }
    }

    /**
     * Shift load balancer into primary mode<br>
     * This operation is blocking so that no data is sent to primary before it is ready
     * @param port Port that LB takes over on
     */
    public void shiftToPrimaryLoadBalancer(int port){
        if (!isLoadBalancer)
            throw new RuntimeException("Takeover called for something that is not a load balancer");
            rl.lock();
            out.println("TAKEOVER "+port);
            while(!activatedPrimary) {
                try {
                    primaryActivated.await();
                } catch (InterruptedException e) {}
            }
            rl.unlock();
    }
}
