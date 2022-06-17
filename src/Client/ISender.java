package Client;

/**
 * Interface allowing for sending of requests to a processing server
 */
public interface ISender {
    /**
     * Send a request to processing server
     * @param precision number of decimal places
     * @param deadline processing deadline
     */
    void sendRequest(int precision, int deadline);
}
