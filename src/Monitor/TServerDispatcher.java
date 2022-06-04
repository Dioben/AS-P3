package Monitor;


import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
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
    private final ArrayList<LoadBalancerHolder> loadBalancers = new ArrayList<LoadBalancerHolder>();



    //private TGUI gui; TODO: INSERT UI CLASS LATER

    public TServerDispatcher(int port, int loadBalancer) {
        this.port = port;
        rl = new ReentrantLock();
        loadBalancerPrimaryPort = loadBalancer;
    }
    public void run() { // run socket thread creation indefinitely
        try {
            serverSocket = new ServerSocket(port);
            while (true) {
                new TBackendMonitor(serverSocket.accept(),this).start();
            }
        } catch (IOException e) {}
    }

    @Override
    public void registerServer(int port, int complexityLoad){
        rl.lock();
        servers.put(port,complexityLoad);
        rl.unlock();
    }
    @Override
    public void removeServer(int port){
        rl.lock();
        servers.remove(port);
        rl.unlock();
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
        }
        rl.unlock();
        if (primary)
        holder.shiftToPrimaryLoadBalancer("TAKEOVER "+loadBalancerPrimaryPort);
        return allowed;
    }

    @Override
    public void removeLoadBalancer(int port){
        LoadBalancerHolder hld = null;
        rl.lock();
        for (int i = 0;i<loadBalancers.size();i++){
            LoadBalancerHolder lb = loadBalancers.get(i);
            if (lb.getPort()==port){
                loadBalancers.remove(i);
                break;
            }
        }
        if (loadBalancers.size()>0)
            hld = loadBalancers.get(0);
        rl.unlock();
        if (hld!=null)
            hld.getMngThread().shiftToPrimaryLoadBalancer("TAKEOVER "+loadBalancerPrimaryPort); //long operation, avoid doing it in lock
    }

    //TODO: BETTER PARAMS FOR THESE
    @Override
    public String notifyHandling(String request){
        String content = "";
        rl.lock();
        for (int port:servers.keySet())
        {
            content+=port+"|"+servers.get(port)+"|";
        }
        rl.unlock();
        return content.substring(0,content.length()-1); //cut out last |
    }
    //TODO
    @Override
    public void notifyDispatched(String request){

    }
    //TODO: UI STUFF
    @Override
    public void notifyRefused(int request){

    }
    //TODO: UI STUFF
    @Override
    public void notifyDone(int request){

    }

    @Override
    public IServerHandler getServerHandler(){
        return this;
    }
    @Override
    public ILoadBalancerHandler getLoadBalancerHandler(){
        return this;
    }


}

