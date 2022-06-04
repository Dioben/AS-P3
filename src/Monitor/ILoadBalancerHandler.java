package Monitor;

public interface ILoadBalancerHandler {
    boolean registerLoadBalancer(int port,TBackendMonitor updater);

    void removeLoadBalancer(int port);

    //TODO: BETTER PARAMS FOR THESE
    String notifyHandling(String request);

    void notifyDispatched(String request);
}
