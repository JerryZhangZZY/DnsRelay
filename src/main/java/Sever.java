import myDNS.MyRecord;
import myDNS.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Sever {

    // default settings
    static boolean useCache = true;
    static int cacheLimitInDays = 2;
    static int threadPoolSize = 10;
    static String remoteDnsServer = "114.114.114.114";

    public static void main(String[] args) {
        // start logging
        Log log = new Log();
        log.addLog("starting dns-relay server...");

        // load config
        Properties config = new Properties();
        try {
            FileInputStream in = new FileInputStream("boot.properties");
            config.load(in);
            in.close();
            useCache = Boolean.parseBoolean(config.getProperty("use-cache", "true"));
            cacheLimitInDays = Integer.parseInt(config.getProperty("cache-limit-in-days", "2"));
            threadPoolSize = Integer.parseInt(config.getProperty("thread-pool-size", "10"));
            remoteDnsServer = config.getProperty("remote-dns-server", "114.114.114.114");
            log.addLog("config loaded");
        } catch (IOException ignored) {
            log.addLog("config load failed, use default settings");
        }

        byte[] buf = new byte[1024];
        DatagramPacket request = new DatagramPacket(buf, buf.length);
        log.addLog("\tuse-cache: " + useCache);
        log.addLog("\tcache-limit-in-days: " + cacheLimitInDays);
        log.addLog("\tthread-pool-size: " + threadPoolSize);
        log.addLog("\tremote-dns-server: " + remoteDnsServer);
        log.writeLog();

        Cache cache = new Cache();

        DatagramSocket socket;
        try {
            socket = new DatagramSocket(53);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        log.addLog("socket connected");

        //dns threads
        ExecutorService pool = Executors.newFixedThreadPool(threadPoolSize);
        //flush cache thread
        ScheduledExecutorService executorService =
                Executors.newScheduledThreadPool(1);
        executorService.scheduleAtFixedRate(() -> {
            cache.flushCacheFile(cacheLimitInDays);
            log.addLog("dns cache flushed and loaded");
            log.writeLog();
        }, 0, 1, TimeUnit.DAYS);

        // wait for cache loaded
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log.addLog("server started");
        while (true) {
            try {
                socket.receive(request);
            } catch (IOException e) {
                e.printStackTrace();
            }
            pool.execute(new Service(request, socket, cache, new Log(), remoteDnsServer, useCache));
        }
    }

    public static class Service implements Runnable {
        DatagramPacket request;
        DatagramSocket socket;
        Cache cache;
        Log log;
        String remoteDnsServer;
        boolean useCache;

        public Service(DatagramPacket request, DatagramSocket socket,
                       Cache cache, Log log, String remoteDnsServer, boolean useCache) {
            this.request = request;
            this.socket = socket;
            this.log = log;
            this.remoteDnsServer = remoteDnsServer;
            this.useCache = useCache;
            if (useCache)
                this.cache = cache;
        }

        @Override
        public void run() {
            InetAddress srcIp = request.getAddress();
            int srcPort = request.getPort();
            MyMessage messageIn;
            try {
                messageIn = new MyMessage(request.getData());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            MyRecord question = messageIn.getQuestion();
            String domain = question.getName().toString();
            boolean valid = true, useV6 = false, nop = false;

            int type = question.getType();
            switch (type) {
                // inverse dns
                case 12 -> {
                    valid = false;
                    log.addLog("[" + Thread.currentThread().getName() + "] " + "inverse dns");
                }
                // ipv4 question
                case 1 -> {
                    log.addLog("[" + Thread.currentThread().getName() + "] " + "ipv4 question for domain: " + domain);
                }
                // ipv6 question
                case 28 -> {
                    useV6 = true;
                    log.addLog("[" + Thread.currentThread().getName() + "] " + "ipv6 question for domain: " + domain);
                }
                default -> {
                    nop = true;
                    log.addLog("[" + Thread.currentThread().getName() + "] " + "other type, simple relay");
                }
            }

            InetAddress ansIp = null;
            DatagramPacket response = null;

            if (valid) {
                if (useCache)
                    ansIp = cache.getIpFromCache(domain + (useV6 ? "-v6" : ""));
                if (!nop && (ansIp != null)) {
                    log.addLog("[" + Thread.currentThread().getName() + "] " + "found in cache");
                } else {
                    if (!nop && useCache)
                        log.addLog("[" + Thread.currentThread().getName() + "] " + "not in cache");
                    DatagramSocket relaySocket;
                    try {
                        relaySocket = new DatagramSocket();
                    } catch (SocketException e) {
                        throw new RuntimeException(e);
                    }
                    byte[] relayBuf = messageIn.toWire();
                    InetAddress dnsSeverIp;
                    try {
                        dnsSeverIp = InetAddress.getByName(remoteDnsServer);
                    } catch (UnknownHostException e) {
                        throw new RuntimeException(e);
                    }
                    DatagramPacket relayRequest = new DatagramPacket(relayBuf, relayBuf.length, dnsSeverIp, 53);
                    byte[] buf = new byte[1024];
                    DatagramPacket relayResponse = new DatagramPacket(buf, buf.length);

                    try {
                        relaySocket.send(relayRequest);
                        relaySocket.receive(relayResponse);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    relaySocket.close();

                    if (nop) {
                        response = relayResponse;
                    } else {
                        MyMessage messageResponse;
                        try {
                            messageResponse = new MyMessage(relayResponse.getData());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        List<MyRecord> records = messageResponse.getSection(1);
                        ArrayList<InetAddress> ips = new ArrayList<>();
                        for (MyRecord record : records) {
                            if (!useV6 && record instanceof MyARecord) {
                                // ipv4 records
                                MyARecord aRecord = (MyARecord) record;
                                try {
                                    InetAddress ip = InetAddress.getByAddress(aRecord.getAddress().getAddress());
                                    ips.add(ip);
                                } catch (UnknownHostException e) {
                                    throw new RuntimeException(e);
                                }
                            } else if (useV6 && record instanceof MyAAAARecord) {
                                // ipv6 records
                                MyAAAARecord aaaaRecord = (MyAAAARecord) record;
                                try {
                                    InetAddress ip = InetAddress.getByAddress(aaaaRecord.getAddress().getAddress());
                                    ips.add(ip);
                                } catch (UnknownHostException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                        if (ips.size() == 0) {
                            log.addLog("[" + Thread.currentThread().getName() + "] " + "no ipv" + (useV6 ? 6 : 4) + " result found from remote dns");
                            valid = false;
                        } else {
                            log.addLog("[" + Thread.currentThread().getName() + "] " + "in total " + ips.size() + " result(s)");
                            ansIp = ips.get(new Random().nextInt(ips.size()));
                            if (useCache) {
                                boolean finalUseV6 = useV6;
                                String parentName = Thread.currentThread().getName();
                                Thread update = new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        synchronized (cache.getCacheLock()) {
                                            if (cache.getIpFromCache(domain + (finalUseV6 ? "-v6" : "")) == null) {
                                                cache.addCacheToFile(domain + (finalUseV6 ? "-v6" : ""), ips);
                                                log.addLog("[" + parentName + "-child] " + "added to cache file and reloaded cache");
                                            }
                                        }

                                    }
                                });
                                update.start();
                            }
                        }
                    }
                }
            }
            if (!nop) {
                MyMessage messageOut = messageIn.clone();
                if (!valid || ansIp.toString().substring(1).equals("0.0.0.0")
                        || ansIp.toString().substring(1).equals("::")
                        || ansIp.toString().substring(1).equals("0:0:0:0:0:0:0:0")) {
                    messageOut.getHeader().setRcode(3);
                    log.addLog("[" + Thread.currentThread().getName() + "] " + "answer: non-existent domain");
                } else {
                    MyRecord answer;
                    // ipv4 answer
                    if (!useV6)
                        answer = new MyARecord(question.getName(), question.getDClass(), 64, ansIp);
                        // ipv6 answer
                    else
                        answer = new MyAAAARecord(question.getName(), question.getDClass(), 64, ansIp);
                    messageOut.addRecord(answer, 1);
                    log.addLog("[" + Thread.currentThread().getName() + "] " + "answer ip: " + ansIp.toString().substring(1));
                }
                byte[] buf = messageOut.toWire();
                response = new DatagramPacket(buf, buf.length);
            }
            response.setAddress(srcIp);
            response.setPort(srcPort);
            try {
                socket.send(response);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            log.writeLog();
        }
    }
}
