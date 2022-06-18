/**
 * Implementation of the load balancer process<br>
 * Only active after receiving permission from the monitor<br>
 * Receives requests from a client to distribute to a server<br>
 * Can reject requests if there are no servers<br>
 * Gets the state of the servers from the monitor to know where to
 * distribute the requests
 */
package LB;