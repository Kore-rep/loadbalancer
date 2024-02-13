# Load Balancer
Very simple, single line message exchange load balancer experiment.
Uses a simplistic Round-Robin scheduling algorithm.

## Features
 - Round Robin Load Balancing
 - Health Checks
 - Unhealthy servers being returned to the pool once healthy


## Limitations
Some interleaving could occur with the health checks, if threads are dispatched just before a server is removed from the healthy pool.
Additional interleaving could occur with health checks and client calls.

### Major issue
If a server goes offline, and a client call is directed there before the health check removes it from the pool, the client will get stuck.

## Executing
After compiling all 3 files (`javac Server.java`);

Spin up any number of servers using `java Server <port>` eg `java Server 5555`.

Then spin up the Load Balancer using `java LoadBalancer <ports>` eg `java LoadBalancer 5555 5556 5557`

Excute the client class and type a message to send to a server and watch the LoadBalancer distribute the messages among the servers.


