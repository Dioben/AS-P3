package Monitor;

public interface IHandlerSource {
    IServerHandler getServerHandler();

    ILoadBalancerHandler getLoadBalancerHandler();
}
