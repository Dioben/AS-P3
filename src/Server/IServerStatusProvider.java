package Server;

/**
 * Interface for obtaining the current server's status
 */
public interface IServerStatusProvider {
    /**
     * Returns server status in  format int[2]
     * @return { #threads + #waiting, complexity load}
     */
    public int[] getStatus();
}
