package Monitor;

/**
 * Interface for load balancer threads to report events
 */
public interface ILoadBalancerHandler {
    /**
     * Register LB existence
     * @param ID Load balancer ID
     * @param updater Thread that load balancer can be reached with
     * @return whether load balancer is accepted
     */
    boolean registerLoadBalancer(int ID,TBackendMonitor updater);

    /**
     * Remove load balancer
     * @param ID Load Balancer ID
     */
    void removeLoadBalancer(int ID);

    /**
     * Notify that LB is handling a request
     * @param loadBalancerId Self ID
     * @param client Client ID
     * @param request Request ID
     * @param iter Complexity
     * @param deadline Deadline
     * @return list of known servers
     */
    String notifyHandling(int loadBalancerId, int client, int request, int iter, int deadline);

    /**
     * Notify that a request has been dispatched to a server
     * @param loadBalancerId Self ID
     * @param server Server ID
     * @param request Request ID
     */
    void notifyDispatched(int loadBalancerId, int server ,int request);
}
