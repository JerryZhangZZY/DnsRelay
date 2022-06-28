import java.io.*;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.*;

public class Cache {
    private final String path = "cache.txt";
    private File cacheFile;
    public final Object cacheFileLock = new Object();

    private Map<String, String[]> cache;
    public final Object cacheLock = new Object();

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
        else {
            readCacheFromFile();
        }
    }

    public void readCacheFromFile() {
        synchronized (cacheFileLock) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(cacheFile));
                String line = null;
                while ((line = br.readLine()) != null) {
                    String[] contents = line.split(" ");
                    String[] ips = Arrays.copyOfRange(contents, 2, contents.length);
                    synchronized (cacheLock) {
                        cache.put(contents[1], ips);
                    }
                }
                br.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void addCacheToFile(String domain, ArrayList<InetAddress> ips) {
        synchronized (cacheFileLock) {
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

    // TODO @zaitian
    public void deleteCacheFromFile(String timeStamp) {

        // remember to use locks rigorously
        // ...

        // update cache finally
        readCacheFromFile();
    }

    public Map<String, String[]> getCache() {
        synchronized (cache) { return cache; }
    }

    public void setCache(Map<String, String[]> cache) {
        synchronized (cacheLock) {
            this.cache = cache;
        }
    }

}
