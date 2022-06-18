package Server;

/**
 * Interface for reporting when a new request has been parsed but not computed
 */
public interface IRequestParsed {
    /**
     * Register a new parsed request
     * @param request Request Data
     */
    void registerNewRequest(QueuedRequest request);
}
