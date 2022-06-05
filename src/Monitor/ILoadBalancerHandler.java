package Monitor;

public interface ILoadBalancerHandler {
    boolean registerLoadBalancer(int port,TBackendMonitor updater);

    void removeLoadBalancer(int port);

    //TODO: BETTER PARAMS FOR THESE
    String notifyHandling(int client, int request,int iter, int deadline);

    void notifyDispatched(int request ,int server);
}