# ⚡ FlashKV — High-Throughput In-Memory Key-Value Store

[![Java](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://www.oracle.com/java/)

[![Architecture](https://img.shields.io/badge/Architecture-Non--Blocking%20NIO-blue.svg)]()

[![Persistence](https://img.shields.io/badge/Storage-WAL%20%2B%20fsync-green.svg)]()

[![License](https://img.shields.io/badge/License-MIT-purple.svg)]()

A lightweight, high-performance distributed caching and key-value storage engine engineered from scratch in pure Java. Designed around an asynchronous, non-blocking single-threaded Reactor pattern (via Linux `epoll`), featuring bounded $O(1)$ LRU cache eviction, lazy TTL expiration, and durability via Write-Ahead Logging (WAL).

---

## 🏛 System Architecture

```text
[ Client Applications / CLI (Port 6379) ]
                   │
                   ▼ (Raw TCP Wire Protocol)
┌────────────────────────────────────────────────────────────────────────┐
│                             FLASHKV CORE                               │
│                                                                        │
│  1. Non-Blocking Network Tier (Java NIO)                              │
│     • ServerSocketChannel, Selector multiplexing (Linux epoll)         │
│     • Zero-copy ByteBuffers, non-blocking connection dispatch          │
│                   │                                                    │
│                   ▼                                                    │
│  2. In-Memory Storage Engine                                           │
│     • ConcurrentHashMap for lock-striped O(1) thread-safe reads/writes │
│     • O(1) Doubly-Linked List + Hash Map LRU Eviction Policy           │
│     • Lazy TTL Expiration Manager (Timestamp delta checks)             │
│                   │                                                    │
│                   ▼                                                    │
│  3. Durability & Recovery Tier (Write-Ahead Log)                       │
│     • Append-only sequential disk writes via FileChannel               │
│     • Synchronous OS fsync() flush to physical disk                    │
│     • Automatic state resurrection & log replay on crash reboot        │
└────────────────────────────────────────────────────────────────────────┘

## ✨ Key Features

* **Non-Blocking I/O Multiplexing:** Built on Java NIO `Selector` and `ServerSocketChannel`, eliminating the thread-per-client model to support thousands of concurrent client connections with minimal context-switching overhead.

* **Bounded $O(1)$ LRU Eviction:** Implements a custom Doubly Linked List with dummy sentinel nodes and hash indexing to enforce memory caps, automatically evicting least-recently-used records in constant time when capacity is reached.

* **Time-To-Live (TTL) Caching:** Supports key expiration with passive/lazy deletion on read access, preventing stale key retention without wasting background CPU cycles.

* **Crash Resilience (WAL):** Guarantees zero data loss across power outages and unexpected process terminations through sequential append-only logging with immediate `fsync` disk flushes.

* **Production Wire Compatibility:** Protocol structure adheres to Redis serialization standards over TCP port 6379, enabling interaction via native TCP sockets, `nc`, or standard database CLI clients.

## 📊 Benchmark & Performance Metrics

Benchmarked under high concurrency using a custom multi-threaded stress harness simulating 50 concurrent client threads over localhost TCP:

| Metric | Verified Benchmark Result |
| :--- | :--- |
| **Total Operations** | **10,000 / 10,000 (100% Success Rate)** |
| **Packet Loss / Socket Drops** | **0% (Zero dropped connections)** |
| **Average Request Latency** | **1.267 ms** |
| **Durability Mode** | **Synchronous `fsync` per write** |
| **Throughput** | **~790 ops/sec (Fully Persistent Mode)** |

> *Note: Throughput is bounded by synchronous physical disk flushing (`fsync`) on each mutation to guarantee complete ACID durability across crashes.*

---

## 🚀 Quickstart & Setup

### Prerequisites
* **Java 17+**
* **Maven 3.8+**
* **Linux / WSL**

### 1. Build and Run Server
```bash
# Clone the repository
git clone https://github.com/asishkommireddy-debug/Flash-kv.git
cd Flash-kv

# Compile bytecode
mvn clean compile

# Launch FlashKV Server on Port 6379
mvn exec:java -Dexec.mainClass="com.flashkv.FlashKVServer"

2. Connect via Terminal Client
In a separate terminal, connect using standard Netcat:
code
Bash
nc localhost 6379

3. Supported Commands & Protocol Reference
Command	Syntax	Description	Example Response
PING	PING	Health check heartbeat	+PONG
SET	SET <key> <val>	Store key-value pair in RAM & WAL	+OK
GET	GET <key>	Fetch value (O(1) LRU lookup)	+value or $-1 (Nil)
DEL	DEL <key>	Remove key from memory and log	:1 (success) or :0
EXPIRE	EXPIRE <key> <sec>	Attach TTL expiration timeout	:1 (timer set)
TTL	TTL <key>	Query remaining seconds to live	:45 (sec) or :-1 (infinite)
code
Code
---

