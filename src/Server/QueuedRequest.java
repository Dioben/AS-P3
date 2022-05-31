package Server;

public class QueuedRequest {
    private int deadline;
    private int returnPort;
    private int precision;

    QueuedRequest(int timeLimit, int origin, int precision){
        deadline = timeLimit;
        returnPort = origin;
        this.precision = precision;
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

}
