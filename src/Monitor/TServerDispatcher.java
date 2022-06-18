package Monitor;


import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Main server class for this project
 * Endlessly accepts connection requests, requests ultimately may be rejected based on current workload
 */
public class TServerDispatcher extends Thread implements ILoadBalancerHandler, IServerHandler, IHandlerSource {

    private ServerSocket serverSocket;
    private final int port;
    private final int loadBalancerPrimaryPort;
    private final ReentrantLock rl;
    private final Map<Integer,Integer> servers = new HashMap<>();
    private final List<LoadBalancerHolder> loadBalancers = new ArrayList<LoadBalancerHolder>();
    private List<Request> awaitingDispatch = new ArrayList<>();
    private final Map<Integer,List<Request>> awaitingResolution = new HashMap<>();
    private InitLB lbInitThread;


    private GUI gui;

    public TServerDispatcher(int port, int loadBalancer, GUI gui) {
        this.port = port;
        this.gui = gui;
        rl = new ReentrantLock();
        loadBalancerPrimaryPort = loadBalancer;
    }
    public void run() { // run socket thread creation indefinitely
        try {
            serverSocket = new ServerSocket(port);
            gui.setSelfPortValidity(true);
            while (true) {
                new TBackendMonitor(serverSocket.accept(),this).start();
            }
        } catch (IOException e) {
            gui.setSelfPortValidity(false);
        }
    }

    @Override
    public void registerServer(int port, int complexityLoad){
        rl.lock();
        servers.put(port,complexityLoad);
        rl.unlock();
        gui.addServer(port);
        gui.changeStatus(port, complexityLoad < 20 ? "Available" : "Full");
    }
    @Override
    public void removeServer(int port){
        rl.lock();
        servers.remove(port);
        List<Request> lost = awaitingResolution.remove(port);

        rl.unlock();

        if (lost!=null && lost.size()>0){
            sendRequestsToPrimaryLB(lost);
        }
        gui.changeStatus(port, "Stopped");
    }


    @Override
    public boolean registerLoadBalancer(int port,TBackendMonitor holder){
        boolean allowed = false;
        boolean primary = false;
        rl.lock();
        if (loadBalancers.size()<2){
            loadBalancers.add(new LoadBalancerHolder(port,holder));
            allowed = true;
            if (loadBalancers.size()==1){
                primary=true;
            }
            gui.addLoadBalancer(port, primary);
        }
        rl.unlock();
        if (primary){
            lbInitThread = new InitLB(holder);
            lbInitThread.start();
        }
        return allowed;
    }

    @Override
    public void removeLoadBalancer(int port){
        LoadBalancerHolder hld = null;
        rl.lock();
        int currentSize = loadBalancers.size();
        for (int i = 0;i<currentSize;i++){
            LoadBalancerHolder lb = loadBalancers.get(i);
            if (lb.getPort()==port){
                loadBalancers.remove(i);
                break;
            }
        }
        if (loadBalancers.size()>0)
            hld = loadBalancers.get(0);
        rl.unlock();
        if (hld!=null){
            lbInitThread = new InitLB(hld.getMngThread());
            lbInitThread.start();
            gui.addLoadBalancer(hld.getPort(), true);
        }
        gui.changeStatus(port, "Stopped");
    }

    @Override
    public String notifyHandling(int loadBalancerId, int client, int reqID, int iter, int deadline){
        String content = "";
        rl.lock();
        for (int port:servers.keySet())
        {
            content+=port+"|"+servers.get(port)+"|";
        }
        awaitingDispatch.add(new Request(client,reqID,iter,deadline));
        rl.unlock();
        gui.addLoadBalancerRequest(loadBalancerId, reqID, client, iter, deadline);
        if (content.isBlank())
            return "";
        return content.substring(0,content.length()-1); //cut out last |
    }

    @Override
    public void notifyDispatched(int loadBalancerId, int port, int request){
        gui.removeLoadBalancerRequest(loadBalancerId, request, port);
        if (port==-1){
            notifyRefusedByLB(request);
            return;
        }
        rl.lock();
        for (int i =0;i<awaitingDispatch.size();i++)
            if (awaitingDispatch.get(i).getReqID()==request){

                if (!awaitingResolution.containsKey(port)){ //entry may have to be initiated
                    awaitingResolution.put(port,new ArrayList<>());
                }

                awaitingResolution.get(port).add(awaitingDispatch.remove(i)); //move this from await dispatch to await resolution
                break;
            }
        rl.unlock();
    }

    private void notifyRefusedByLB(int request) {
        rl.lock();
        for (int i =0;i<awaitingDispatch.size();i++)
            if (awaitingDispatch.get(i).getReqID()==request){
                awaitingDispatch.remove(i); //move this from await dispatch to await resolution
                break;
            }
        rl.unlock();
    }

    @Override
    public void notifyRefused(int port, int request){
        removeRequestFromServerPending(port, request);
        gui.updateServerRequest(port, request, null, null, null, "Rejected");
    }

    @Override
    public void notifyDone(int port, int request){
        removeRequestFromServerPending(port, request);
        gui.updateServerRequest(port, request, null, null, null, "Finished");
    }
    private void removeRequestFromServerPending(int port, int request) {
        rl.lock();
        List<Request> requests = awaitingResolution.get(port);
        for (int i =0;i<requests.size();i++)
            if (requests.get(i).getReqID()==request){
                requests.remove(i);
                break;
            }
        rl.unlock();
    }

    private void sendRequestsToPrimaryLB(List<Request> requests){
        rl.lock();
        if (loadBalancers.size()<1 && requests!=awaitingDispatch) //direct pointer comparison
             {
            awaitingDispatch.addAll(requests); //this makes it so that they're sent when a new load balancer registers
                 rl.unlock();
                 return;
        }
        if (requests==awaitingDispatch)
            awaitingDispatch = new ArrayList<>(); //change pointer so we can do this outside of a lock
        rl.unlock();
        while (requests.size()>0){
            Request request = requests.remove(0);
            try {
                Socket ext = new Socket("localhost",loadBalancerPrimaryPort);
                PrintWriter out = new PrintWriter(ext.getOutputStream(), true);
                String msg = String.format("%d|%d|00|01|%d|00|%d", request.getClient(),
                        request.getReqID(),
                        request.getIter(),
                        request.getDeadline());
                out.println(msg);
                out.close();
                ext.close();
            } catch (IOException e) {
                rl.lock();
                if (loadBalancers.size()>0){
                    rl.unlock();
                    requests.add(request); //try again on fail
                }
                else{
                    awaitingDispatch.addAll(requests); // we'll get 'em next time
                    rl.unlock();
                    return;
                }
            }
        }

    }

    @Override
    public IServerHandler getServerHandler(){
        return this;
    }
    @Override
    public ILoadBalancerHandler getLoadBalancerHandler(){
        return this;
    }

    private class InitLB extends Thread{
        private final TBackendMonitor lbContact;
        InitLB(TBackendMonitor holder){
            lbContact = holder;
        }

        @Override
        public void run() {
            lbContact.shiftToPrimaryLoadBalancer(loadBalancerPrimaryPort);
            sendRequestsToPrimaryLB(awaitingDispatch);
        }
    }

}

