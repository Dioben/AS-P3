package Monitor;

public class LoadBalancerHolder {
    private final int port;
    private final TBackendMonitor mngThread;
    public LoadBalancerHolder(int port, TBackendMonitor holder) {
    this.mngThread = holder;
    this.port = port;
    }

    public int getPort() {
        return port;
    }

    public TBackendMonitor getMngThread() {
        return mngThread;
    }
}
