package LB;

public interface IMonitorInfoContact {
    String requestStatusFromMonitor(String request);
    void reportDispatchToMonitor(String reqID, int serverID);

    void reportReady();
}
