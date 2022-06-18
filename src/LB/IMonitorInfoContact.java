package LB;

/**
 * Interface for contacting the monitor
 */
public interface IMonitorInfoContact {
    /**
     * Request information about servers from monitor
     * Simultaneously registers a request with monitor
     * @param request Request we are registering with monitor
     * @return list of servers and complexity loads
     */
    String requestStatusFromMonitor(String request);

    /**
     * Report a dispatch to monitor
     * @param reqID Request ID
     * @param serverID Server ID
     */
    void reportDispatchToMonitor(String reqID, int serverID);

    /**
     * Report request cancel to monitor
     * Generally caused by no available servers
     * @param reqID Request ID
     */
    void reportCancelToMonitor(String reqID);

    /**
     * Report readiness to act as primary load balancer
     */
    void reportReady();
}
