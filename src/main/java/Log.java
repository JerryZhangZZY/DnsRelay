import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Log {
    private final String path = "log.txt";
    private final File log;
    private String buf = "";

    public Log() {
        log = new File(path);
        if (!log.exists()) {
            try {
                log.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void addLog(String logPiece) {
        System.out.println(logPiece);
        buf = buf.concat("\t").concat(logPiece).concat("\n");
    }

    synchronized public void writeLog() {
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yy/MM/dd HH:mm:ss");
        String logItem = "[" + formatter.format(date) + "]\n" + buf + "\n";
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(log, true));
            bw.write(logItem);
            bw.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
