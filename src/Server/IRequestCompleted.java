package Server;

public interface IRequestCompleted {
    void onRequestCompletion(QueuedRequest request,String result);
}
