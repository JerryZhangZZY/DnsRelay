# DNS Relay Server

> Project of BUPT IoT 2022 junior summer school.

## Features

### Basic Functions

- Supports all DNS request types
- Caching mechanism
- Multi-threaded concurrent handling of requests
- Customizable blacklist

### Advanced Features

- Support IPv6 address
- DNS load balancing
- Auto cache cleaning
- Blacklist expiry time
- Server logs
- User configuration
  - Customizable remote DNS server address
  - Enable/disable caching mechanism
  - Customizable thread pool size
  - Customizable cache validity period

## Environment requirements

Java 15 or higher

## How To Use

### Boot settings

Customize your settings in [**boot.properties**](boot.properties).

### Add blacklist

Add websites to [**cache.txt**](cache.txt) as follows:

```text
blacklist [website you want to block]. 0.0.0.0
blacklist [website you want to block].-v6 ::
```

If you want to set expiry time, here's an example:

```text
2022/07/02 www.taobao.com. 0.0.0.0
2022/07/02 www.taobao.com.-v6 ::
```

Assume the cache limit is set to 2 days. Then the server will block [www.taobao.com](www.taobao.com) until 2022/07/04.

### Start server and test

Run the application and check if the server started successfully on the terminal.

Then create a new terminal window and execute:

```shell
nslookup www.bupt.edu.cn 127.0.0.1
```

The server should return both IPv4 and IPv6 addresses(ipv6 protocol enabled).

### Set as default

Now you can set your local DNS server as 127.0.0.1(ipv4) and ::1(ipv6).

### Log

If something wrong happens, you can check it in **_log.txt_**.

## Developer Team

- Wang Zaitian [@ZaitianWang](https://github.com/ZaitianWang)
- Zhang Zeyu [@JerryZhangZZY](https://github.com/JerryZhangZZY)
