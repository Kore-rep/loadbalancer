# Load Balancer
Very simple, single line message exchange load balancer experiment.
Uses a simplistic Round-Robin scheduling algorithm.


## Executing
Spin up any number of servers using `java Server <port>` eg `java Server 5555`.

Then spin up the Load Balancer using `java LoadBalancer <ports>` eg `java LoadBalancer 5555 5556 5557`

Excute the client class and type a message to send to a server and watch the LoadBalancer distribute the messages among the servers.