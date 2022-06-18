package Monitor;

/**
 * Interface that provides server and load balancer handlers
 */
public interface IHandlerSource {
    /**
     * Get a server handler
     * @return Server handler
     */
    IServerHandler getServerHandler();

    /**
     * Get a Load balancer handler
     * @return Load balancer handler
     */
    ILoadBalancerHandler getLoadBalancerHandler();
}
