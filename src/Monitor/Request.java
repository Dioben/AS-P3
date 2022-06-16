package Monitor;

public class Request {
    private int client;
    private int reqID;
    private int iter;
    private int deadline;

    public Request(int client, int reqID, int iter, int deadline) {
        this.client = client;
        this.reqID = reqID;
        this.iter = iter;
        this.deadline = deadline;
    }

    public int getClient() {
        return client;
    }

    public int getReqID() {
        return reqID;
    }

    public int getIter() {
        return iter;
    }

    public int getDeadline() {
        return deadline;
    }

}
