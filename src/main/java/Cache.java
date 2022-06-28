import java.io.*;
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
                FileOutputStream fos = new FileOutputStream(cacheFile);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            readCacheFromFile();
        }
    }

    public void readCacheFromFile() {
        synchronized (cacheFile) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(cacheFile));
                String line = null;
                while ((line = br.readLine()) != null) {
                    String[] contents = line.split(" ");
                    String[] ips = Arrays.copyOfRange(contents, 2, contents.length);
                    synchronized (cache) {
                        cache.put(contents[1], ips);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
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
        synchronized (cache) {
            this.cache = cache;
        }
    }

}
