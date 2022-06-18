/**
 * Implementation of the load balancer process.
 * Only active after receiving permission from the monitor.
 * Receives requests from a client to distribute to a server.
 * Can reject requests if there are no servers.
 * Gets the state of the servers from the monitor to know where to
 * distribute the requests.
 */
package LB;