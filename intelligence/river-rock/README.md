#River Rock Proxy

This is a proxy which smooths over stones. 

  1. Given a request, it uses [bernardo](../bernardo) to
     assign the request to a cluster. 

  2. It then checks a resource map for a mapping of resource => references in that cluster
  3. It then intelligently selects a reference and reverse proxy's it back to the client.

The "intelligently" part refers to the multi armed bandit algorithm and potentially others
in the future.


| Directory                              | Description                                                                                                  |
|:---------------------------------------|:-------------------------------------------------------------------------------------------------------------|
| [proxy](proxy)                         | Main source for the proxy which implements the HTTP interface|
| [clustering](clustering)               | Code to talk to bernardo and assign a request a cluster id|
| [selection](selection)                 | Implementation of the resource map and resource selection algorithm |
