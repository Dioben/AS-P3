package Server;

public class QueuedRequest {
    private final int deadline;
    private final int returnPort;
    private final int precision;
    private final int requestID;

    QueuedRequest(int timeLimit, int origin, int precision, int ID){
        deadline = timeLimit;
        returnPort = origin;
        this.precision = precision;
        requestID = ID;
    }
    public int getDeadline() {
        return deadline;
    }

    public int getReturnPort() {
        return returnPort;
    }

    public int getPrecision() {
        return precision;
    }

    public int getRequestID() {
        return requestID;
    }

}
