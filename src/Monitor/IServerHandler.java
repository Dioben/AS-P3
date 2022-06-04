package Monitor;

public interface IServerHandler {
    void registerServer(int port, int complexityLoad);

    void removeServer(int port);

    void notifyRefused(int request);

    void notifyDone(int request);
}
