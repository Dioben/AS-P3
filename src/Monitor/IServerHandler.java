package Monitor;

public interface IServerHandler {
    void registerServer(int port, int complexityLoad);

    void removeServer(int port);

    void notifyRefused(int port,int request);

    void notifyDone(int port, int request);
}
