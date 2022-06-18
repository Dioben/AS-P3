package Server;

/**
 * Entity responsible for calculating PI up to x decimal places
 */
public class TSolver extends Thread{
    private final static String PI = "3.1415926589793";
    private final QueuedRequest request;
    private final IRequestCompleted parent;


    /**
     *
     * @param request Request to handle
     * @param reportTo Entity to report result to
     */
    public TSolver(QueuedRequest request, IRequestCompleted reportTo){
        this.request = request;
        parent = reportTo;
    }

    /**
     * Compute result, actually a big wait in a trench coat
     */
    @Override
    public void run() {

        try {
            Thread.sleep(5000* request.getPrecision());
        } catch (InterruptedException e) {}
        parent.onRequestCompletion(request,PI.substring(0,2+ request.getPrecision()));

    }
}
