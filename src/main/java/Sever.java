import org.xbill.DNS.*;
import org.xbill.DNS.Record;
import org.xbill.DNS.dnssec.R;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Sever {

    public static void main(String[] args) {
        Cache cache = new Cache();
        DatagramSocket socket;
        try {
            socket = new DatagramSocket(53);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        byte[] buf = new byte[1024];
        DatagramPacket request = new DatagramPacket(buf, buf.length);
        //dns threads
        ExecutorService pool = Executors.newFixedThreadPool(20);
        //cache thread
        ScheduledExecutorService executorService =
                Executors.newScheduledThreadPool(1);
        executorService.scheduleAtFixedRate(() -> {
            cache.flushCacheFile();
            System.out.println("[" + Thread.currentThread().getName() + "] " + "DNS cache flushed");
        }, 0, 2, TimeUnit.DAYS);

        while (true) {
            try {
                socket.receive(request);
            } catch (IOException e) {
                e.printStackTrace();
            }
            pool.execute(new Service(request, socket, cache));
        }
    }

    public static class Service implements Runnable {
        DatagramPacket request;
        DatagramSocket socket;
        Cache cache;

        public Service(DatagramPacket request, DatagramSocket socket, Cache cache) {
            this.request = request;
            this.socket = socket;
            this.cache = cache;
        }

        @Override
        public void run() {
            InetAddress srcIp = request.getAddress();
            int sourcePort = request.getPort();
            Message messageIn;
            try {
                messageIn = new Message(request.getData());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Record question = messageIn.getQuestion();

            String domain = question.getName().toString();

            boolean valid = true, useV6 = false;

            int type = question.getType();
            switch (type) {
                // inverse dns
                case 12 -> {
                    valid = false;
                    System.out.println("[" + Thread.currentThread().getName() + "] " + "inverse dns");
                }
                // ipv4 question
                case 1 -> {
                    System.out.println("[" + Thread.currentThread().getName() + "] " + "ipv4 question for domain: " + domain);
                }
                // ipv6 question
                case 28 -> {
                    useV6 = true;
                    System.out.println("[" + Thread.currentThread().getName() + "] " + "ipv6 question for domain: " + domain);
                }
            }

            InetAddress ansIp = null;

            if (valid) {
                ansIp = cache.getIpFromCache(domain + (useV6 ? "-v6" : ""));
                if (ansIp != null) {
                    System.out.println("[" + Thread.currentThread().getName() + "] " + "found in cache");
                }
                else {
                    System.out.println("[" + Thread.currentThread().getName() + "] " + "not in cache");
                    DatagramSocket relaySocket;
                    try {
                        relaySocket = new DatagramSocket();
                    } catch (SocketException e) {
                        throw new RuntimeException(e);
                    }
                    byte[] relayBuf = messageIn.toWire();
                    InetAddress dnsSeverIp;
                    try {
                        dnsSeverIp = InetAddress.getByName("10.0.0.1");
                    } catch (UnknownHostException e) {
                        throw new RuntimeException(e);
                    }
                    DatagramPacket relayRequest = new DatagramPacket(relayBuf, relayBuf.length, dnsSeverIp, 53);
                    try {
                        relaySocket.send(relayRequest);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    byte[] buf = new byte[1024];
                    DatagramPacket relayResponse = new DatagramPacket(buf, buf.length);
                    try {
                        relaySocket.receive(relayResponse);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    relaySocket.close();
                    Message messageResponse;
                    try {
                        messageResponse = new Message(relayResponse.getData());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    List<Record> records = messageResponse.getSection(Section.ANSWER);
                    ArrayList<InetAddress> ips = new ArrayList<>();
                    int ipCount = 0;
                    for (Record record : records) {
                        if (!useV6 && record instanceof ARecord) {
                            // ipv4 records
                            ARecord aRecord = (ARecord)record;
                            try {
                                InetAddress ip = InetAddress.getByAddress(aRecord.getAddress().getAddress());
                                ips.add(ip);
                                ipCount++;
                            } catch (UnknownHostException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        else if (useV6 && record instanceof AAAARecord) {
                            // ipv6 records
                            AAAARecord aaaaRecord = (AAAARecord)record;
                            try {
                                InetAddress ip = InetAddress.getByAddress(aaaaRecord.getAddress().getAddress());
                                ips.add(ip);
                                ipCount++;
                            } catch (UnknownHostException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                    if (ips.size() == 0) {
                        System.out.println("[" + Thread.currentThread().getName() + "] " + "no ipv" + (useV6 ? 6 : 4) + " result found from remote dns");
                        valid = false;
                    }
                    else {
                        System.out.println("[" + Thread.currentThread().getName() + "] " + "in total " + ips.size() + " result(s)");
                        ansIp = ips.get(new Random().nextInt(ips.size()));

                        // TODO conf
                        if (cache.getIpFromCache(domain + (useV6 ? "-v6" : "")) == null)
                            cache.addCacheToFile(domain + (useV6 ? "-v6" : ""), ips);
                    }
                }
            }

            Message messageOut = messageIn.clone();
            if (!valid || ansIp.toString().substring(1).equals("0.0.0.0") || ansIp.toString().substring(1).equals("::")) {
                messageOut.getHeader().setRcode(3);
            }
            else {
                System.out.println("[" + Thread.currentThread().getName() + "] " + "answer ip: " + ansIp.toString().substring(1));
                Record answer;
                // ipv4 answer
                if (!useV6) {
                    answer = new ARecord(question.getName(), question.getDClass(), 64, ansIp);
                }
                // ipv6 answer
                else {
                    answer = new AAAARecord(question.getName(), question.getDClass(), 64, ansIp);
                }
                messageOut.addRecord(answer, Section.ANSWER);
            }
            byte[] buf = messageOut.toWire();
            DatagramPacket response = new DatagramPacket(buf, buf.length, srcIp, sourcePort);
            try {
                socket.send(response);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
