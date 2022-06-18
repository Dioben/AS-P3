package Monitor;

/**
 * Interface for server threads to report events
 */
public interface IServerHandler {
    /**
     * Register server existance
     * @param port Server Port
     * @param complexityLoad Current workload
     */
    void registerServer(int port, int complexityLoad);

    /**
     * Remove server
     * @param port Server Port
     */
    void removeServer(int port);

    /**
     * Notify of a refused request
     * @param port Self ID
     * @param request Request ID
     */
    void notifyRefused(int port,int request);

    /**
     * Notify of a completed request
     * @param port Self ID
     * @param request Request ID
     */
    void notifyDone(int port, int request);
}
