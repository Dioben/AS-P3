package Client;

/**
 * Interface allowing for registering of messages received on a handler socket
 */
public interface IRegisterMessage {
    /**
     * Register a declined request
     * @param ID request ID
     * @param server server ID
     */
    void registerDecline(int ID, int server);

    /**
     * Register a request response
     * @param ID request ID
     * @param server server ID
     * @param response response value
     */
    void registerResponse(int ID, int server, String response);
}
