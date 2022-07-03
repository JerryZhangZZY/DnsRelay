# DNS Relay Server

> Project of BUPT IoT 2022 junior summer school.

## Pro Feature

- IPv6 support
- Flow control
- Configurable remote DNS server
- Configurable cache usage
- Configurable multithreading
- Configurable cache auto-cleaning
- Log file for recording all behaviour in detail

## How To Use

### Boot settings

Customize your settings in **_boot.properties_**.

### Add blacklist

Add websites to **_cache.txt_** as follows:

```text
blacklist [website you want to block]. 0.0.0.0
blacklist [website you want to block].-v6 ::
```

If you want to set expire time, here's an example:

```text
2022/07/02 www.taobao.com. 0.0.0.0
2022/07/02 www.taobao.com.-v6 ::
```

Assume the cache limit is set to 2 days. Then the server will block _www.taobao.com_ until 2022/07/04.

### Start server and test

Run the application and check if the server started successfully on the terminal.

Then create a new terminal window and execute:

```shell
nslookup www.bupt.edu.cn 127.0.0.1
```

The server should return both ipv4 and ipv6 addresses(ipv6 protocol enabled).

### Set as default

Now you can set your local dns server as 127.0.0.1(ipv4) and ::1(ipv6).

### Log

If something wrong happens, you can check it in **_log.txt_**.

## Developer Team

- Wang Zaitian [@ZaitianWang](https://github.com/ZaitianWang)
- Zhang Zeyu [@JerryZhangZZY](https://github.com/JerryZhangZZY)
