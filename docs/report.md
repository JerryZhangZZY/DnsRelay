# REPORT OF DNS RELAY SERVER

**Course Title:** Design and implementation of a DNS Relay

**Group Member:**

- Wang Zaitian (2019213481)
- Zhang Zeyu (2019213482)

**Date:** 2022/7/2

[TOC]

## 0. Overview

The aim of this project is to design and implement a DNS relay that connects local DNS resolver with remote DNS servers. Typically, when a user program is trying to interact with the domain name space, it send queries to the resolver, and the resolver sends back the response directly if found in its cache, otherwise the resolver queries remote DNS servers and forwards the response back to user the program. In this design, the DNS relay works as a local DNS server whom the resolver resorts to, and takes the role of connecting to remote DNS server. The new domain system configuration has two benefits. First, it can ease the burden or remote DNS servers by replying to resolver with its own DNS cache. Second, it increase Internet connection speed by using local DNS cache, which is faster than remote DNS server. Third, it can block some undesired connection to the Internet by adding certain domain names to blacklist. 

## 1. Requirement Analysis

### 1.1. Functional Requirements

The DNS relay should be able to:

1. Listen to port 53 for DNS queries
2. Receive and resolve DNS query messages
3. Retrieve IPs for the domain name queried in local cache
4. Query the remote DNS server if IP not found in cache
5. Receive remote DNS server reply and fetch the returned IPs
6. Pack the IP from cache or remote DNS server in a reply message
7. Respond the resolver with this message containing the IP address
8. Block queries for domains that are recorded in blacklist
9. Support multi-threading to given best performance
10. Log connection and  operation information.

### 1.2. Non-functional Requirements

1. The software works on **Windows** or Linux
2. The program is written is C, C++, or **Java**

## 2. Overall Design

### 2.1. 

```mermaid
flowchart TB
A[Client]
B[Resolver]
C[(Cache)]
D[Remote DNS Server]

A --> |request| B
B --> |response| A

subgraph DNS Relay
B --> |save| C
C --> |read| B
end

B --> |request| D
D --> |response| B
```

<center><b><font size ='2'>Figure 1. Functional modules</font></b></center></font>

```mermaid
flowchart TB

st([Start])
1[/Read config/]
2[Block at receive]
3[Request received]
4[Pick a new thread from thread pool]
5[Resolve request]
6{Domain in cache?}
7[Get ip from cache]
8[Relay request to remote DNS server]
9[Get ip from remote]
10[Add to cache]
11{Valid ip?}
12[Encapsulate ip into datagram]
13[Set RCODE]
14[Send response back to client]
nd([End of child thread])

st-->1-->2-->3-->4-->2
4-->5-->6
6-->|true|7-->11
6-->|false|8-->9-->10-->11
11-->|true|12-->14
11-->|false|13-->14
14-->nd
```

<center><b><font size ='2'>Figure 2. Overall flowchart</font></b></center></font>

## 3. Detail Design

## 4. Testing & Results

## 5. Summary & Future Improvement

### 5.1. Summary

The DNS relay program is capable of handling various DNS query requests. On entering `nslookup example.com localhost` in `cmd`, the relay program receives the question from resolver and return with a answer from its local cache or remote DNS server. Apart from IPv4, the program has support for **IPv6** DNS query by returning type AAAA record, and it can also deal with records of other types including **NS, CNAME, and MX**. By using **a thread pool**, the server is able to handle multiple request at the same time. This **concurrency** configuration greatly helps improve the performance of the program. Mutex Lock is used to ensure **thread-safe** and avoid race condition.  In order to easy to burden or remote DNS server and speed up DNS look-up, the program uses a **cache** to store DNS query result for a preconfigured period of time and expired DNS cache is **automatically flushed**. The cache is supported by a **hash map** in the memory, which means any query in the cache can be done with time complexity of $O(1)$, and thus other threads is very unlikely to be blocked when they access the cache. This program also enables **flow control**. If multiple IP addresses are found in cache or received from remote server (which means the Internet service has many servers for load balancing), it will randomly select one to send back to resolver. In this way, user programs access the service via different IP addresses and servers fairly. **Log** functions is used to record the connection and operations of the relay program. Users can track all query and response records in the log file.

### 5.2. Room for Improvements

- Blacklist can be separate from cache so that it can be maintained more conveniently
- Log can be clearable in case the file is very large
- Cache size limit can be used to restrain memory usage
