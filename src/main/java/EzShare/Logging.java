package EzShare;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Class for logging messages with "info" and "fine" level
 * Created by Jack on 23/3/2017.
 */
public class Logging {
    private static boolean enablePrint = false;

    public static void setEnablePrint(boolean enablePrint) {
        Logging.enablePrint = enablePrint;
    }

    /**
     * Get prefix of line to be logged
     *
     * @return prefix of line
     */
    private static String getPrefix() {
        SimpleDateFormat dt = new SimpleDateFormat("dd/mm/yyyy hh:mm:ss.SSS");
        String date = dt.format(new Date());

        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        StackTraceElement elem = trace[trace.length - 1];
        String pos = elem.toString()
                .replaceAll("com\\.allstars\\.project1\\.", "")
                .replaceAll("\\(.*\\)", "");

        return date + " - [" + pos + "] ";
    }

    /**
     * Log a message in "info" level
     *
     * @param obj object to be logged
     */
    public static void logInfo(Object obj) {
        System.out.println(getPrefix() + "- [INFO] - " + obj.toString());
    }

    /**
     * Log a message in "fine"(debug) level
     *
     * @param obj object to be logged
     */
    public static void logFine(Object obj) {
        if (enablePrint) {
            System.out.println(getPrefix() + "- [FINE] - " + obj.toString());
        }
    }
}
