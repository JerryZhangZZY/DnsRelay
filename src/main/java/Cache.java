import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Cache {
    private final String path = "cache.txt";
    private final File cacheFile;
    private Map<String, String[]> cache;
    private final Object cacheLock = new Object();

    public Cache() {
        cacheFile = new File(path);
        cache = new HashMap<>();
        if (!cacheFile.exists()) {
            try {
                new FileOutputStream(cacheFile);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public InetAddress getIpFromCache(String domain){
        synchronized (cacheLock) {
            if (cache.containsKey(domain)) {
                try {
                    return InetAddress.getByName(cache.get(domain)[new Random().nextInt(cache.get(domain).length)]);
                } catch (UnknownHostException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return null;
    }

    public void readCacheFromFile() {
        synchronized (cacheLock) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(cacheFile));
                String line;
                while ((line = br.readLine()) != null) {
                    String[] contents = line.split(" ");
                    String[] ips = Arrays.copyOfRange(contents, 2, contents.length);
                    cache.put(contents[1], ips);
                }
                br.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void addCacheToFile(String domain, ArrayList<InetAddress> ips) {
        synchronized (cacheLock) {
            BufferedWriter bw = null;
            try {
                bw = new BufferedWriter(new FileWriter(cacheFile, true));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Date date = new Date();
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
            String line = formatter.format(date) + " " + domain;
            for (InetAddress ip : ips) {
                line += (" " + ip.toString().substring(1));
            }
            try {
                bw.write(line);
                bw.newLine();
                bw.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        readCacheFromFile();
    }

    public void flushCacheFile(int limit) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, -limit * 24);
        Date expireDate = calendar.getTime();
        String newCache = "";
        synchronized (cacheLock) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(cacheFile));
                String line;
                while ((line = br.readLine()) != null) {
                    String[] contents = line.split(" ");
                    if (contents[0].equals("blacklist")) {
                        newCache = newCache + line + "\n";
                    } else {
                        Date cacheDate = dateFormat.parse(contents[0]);
                        if (cacheDate.after(expireDate)) {
                            newCache = newCache + line + "\n";
                        }
                    }
                }
                br.close();
            } catch (IOException | ParseException e) {
                throw new RuntimeException(e);
            }
            try {
                BufferedWriter bw = new BufferedWriter(new FileWriter(cacheFile, false));
                bw.write(newCache);
                bw.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        readCacheFromFile();
    }

    public Object getCacheLock() {
        return cacheLock;
    }
}
