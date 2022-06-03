package Client;

public interface IRegisterMessage {
    void registerDecline(int ID, int server);

    void registerResponse(int ID, int server, String response);
}
