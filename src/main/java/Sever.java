import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Date;
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
            System.out.println("DNS cache flushed");
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

            String domain = messageIn.getQuestion().getName().toString();
            if (domain.contains("in-addr"))
                return;

            System.out.println("\ndomain: " + domain);

            InetAddress ansIp;

            boolean contains;
            synchronized (cache.cacheLock) {
                contains = cache.getCache().containsKey(domain);
            }
            if (contains) {
                System.out.println("found in cache");
                String[] ips;
                synchronized (cache.cacheLock) {
                    ips = cache.getCache().get(domain);
                }
                try {
                    ansIp = InetAddress.getByName(ips[new Random().nextInt(ips.length)]);
                } catch (UnknownHostException e) {
                    throw new RuntimeException(e);
                }
            }
            else {
                System.out.println("not in cache");
                DatagramSocket relaySocket;
                try {
                    relaySocket = new DatagramSocket();
                } catch (SocketException e) {
                    throw new RuntimeException(e);
                }
                byte[] relayBuf = messageIn.toWire();
                InetAddress dnsSeverIp;
                try {
                    dnsSeverIp = InetAddress.getByName("8.8.8.8");
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

                //records contain both ARecord(ipv4) and AAAARecord(ipv6)
                List<Record> records = messageResponse.getSection(Section.ANSWER);
                ArrayList<InetAddress> ips = new ArrayList<>();
                int[] ipCount = {0, 0};
                for (Record record : records) {
                    if (record instanceof ARecord) {
                        //ipv4
                        ARecord aRecord = (ARecord)record;
                        try {
                            InetAddress ip = InetAddress.getByAddress(aRecord.getAddress().getAddress());
                            ips.add(ip);
                            ipCount[0]++;
                        } catch (UnknownHostException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    else if (record instanceof AAAARecord) {
                        //ipv6
                        AAAARecord aaaaRecord = (AAAARecord)record;
                        try {
                            InetAddress ip = InetAddress.getByAddress(aaaaRecord.getAddress().getAddress());
                            ips.add(ip);
                            ipCount[1]++;
                        } catch (UnknownHostException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                if (ips.size() == 0)
                    return;
                System.out.println("in total " + ips.size() + " result(s)");
                System.out.println("ipv4: " + ipCount[0] + " result(s)");
                System.out.println("ipv6: " + ipCount[1] + " result(s)");
                ansIp = ips.get(new Random().nextInt(ips.size()));
                cache.addCacheToFile(domain, ips);
            }

            System.out.println("answer ip: " + ansIp.toString().substring(1));

            Message messageOut = messageIn.clone();

            if (ansIp.toString().substring(1).equals("0.0.0.0") || ansIp.toString().substring(1).equals("::")) {
                messageOut.getHeader().setRcode(3);
            }
            else {
                Record answer = null;
                if (ansIp instanceof Inet4Address) {            //ipv4
                    answer = new ARecord(question.getName(), question.getDClass(), 64, ansIp);
                }
                else if (ansIp instanceof Inet6Address){        //ipv6
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
