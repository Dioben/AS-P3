package Server;

/**
 * Interface for reporting when a request is fully completed
 */
public interface IRequestCompleted {
    /**
     * Report request completion
     * @param request Request Object that has been parsed
     * @param result Parsing result
     */
    void onRequestCompletion(QueuedRequest request,String result);
}
