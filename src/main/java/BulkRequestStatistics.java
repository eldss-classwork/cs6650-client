import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BulkRequestStatistics {
  // TODO: add a logData method to write to csv
  // TODO: add private calculateStats to calculate stats

  public static final int MILLISECS_PER_SEC = 1000;
  private static final Logger logger = LogManager.getLogger(BulkRequestStatistics.class);

  private AtomicInteger totalRequests = new AtomicInteger();
  private AtomicInteger totalBadRequests = new AtomicInteger();
  private long wallStart;
  private long wallStop;

  // List of references to arrays that contain raw stats from the requests of individual
  // threads of PhaseRunners
  private List<SingleRequestStatistics[]> singleStatsArrays = new LinkedList<>();

  /**
   * Adds the given array reference to a linked list synchronously.
   * @param array an array of stats
   */
  public synchronized void addStatArray(SingleRequestStatistics[] array) {
    singleStatsArrays.add(array);
  }

  /**
   * Creates a CSV file with statistics from every request in a list of SingleRequestStatistics
   * arrays. If writing fails, prints a message to stderr and logs it.
   * @param filename the desired filename
   */
  public void requestStatsToCsv(String filename) throws IOException {
    final File csvOutputFile = new File(filename);

    // Ensure any old file is overwritten
    if (csvOutputFile.exists()) {
      // First try deleting the old file, then try creating a new one
      boolean deleted = csvOutputFile.delete();
      boolean created = csvOutputFile.createNewFile();
      if (!(deleted && created)) {
        String msg = "Problem overwriting existing CSV file";
        logger.error(msg);
        System.err.println(msg);
        return;
      }
    }

    final String headers = "RequestType,StartTimestamp(ms),Latency(ms),ResponseCode";
    try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
      // Add the headers
      pw.println(headers);

      // Add the data
      for (SingleRequestStatistics[] arr : singleStatsArrays) {
        for (SingleRequestStatistics statistics : arr) {
          pw.println(buildCsvLine(statistics));
        }
      }
    } catch (IOException e) {
      String msg = "Problem writing csv file: ";
      logger.error(msg + e.getMessage()
          + "\n" + Arrays.toString(e.getStackTrace()));
      System.err.println(msg + "See log output for details");
    }
  }

  /**
   * Creates a one line string in CSV format from a SingleRequestStatistics object.
   * @param singleStats the stats to make a string
   * @return A string with the stats in CSV format
   */
  private String buildCsvLine(SingleRequestStatistics singleStats) {
    String type = singleStats.getRequestType();
    String start = String.valueOf(singleStats.getStartTime());
    String latency = String.valueOf(singleStats.getLatency());
    String code = String.valueOf(singleStats.getResponseCode());

    String[] data = new String[]{type, start, latency, code};
    return String.join(",", data);
  }

  public void startWallTimer() {
    this.wallStart = System.currentTimeMillis();
  }

  public void stopWallTimer() {
    this.wallStop = System.currentTimeMillis();
  }

  public double getWallTimeSecs() {
    return (double) (wallStop - wallStart) / MILLISECS_PER_SEC;
  }

  public double getThroughputPerSec() {
    return totalRequests.get() / getWallTimeSecs();
  }

  public double getGoodThroughputPerSec() {
    return (totalRequests.get() - totalBadRequests.get()) / getWallTimeSecs();
  }

  @Override
  public String toString() {
    return String.format("Execution Statistics:\n"
            + "\tTotal requests: %d\n"
            + "\tBad Requests: %d\n"
            + "\tWall Time: %.2f seconds\n"
            + "\tTotal Throughput: %.2f requests/second\n"
            + "\tSuccess Throughput: %.2f requests/second\n"
        , totalRequests.get(), totalBadRequests.get(), getWallTimeSecs(), getThroughputPerSec(), getGoodThroughputPerSec());
  }

  public AtomicInteger getTotalRequests() {
    return totalRequests;
  }

  public AtomicInteger getTotalBadRequests() {
    return totalBadRequests;
  }

  public long getWallStart() {
    return wallStart;
  }

  public long getWallStop() {
    return wallStop;
  }
}
