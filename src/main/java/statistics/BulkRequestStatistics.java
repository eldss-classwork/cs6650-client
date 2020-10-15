package statistics;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Stores data gathered from the client and calculates descriptive statistics once
 * all request threads have finished.
 */
public class BulkRequestStatistics {

  public static final int MILLISECS_PER_SEC = 1000;

  private static final Logger logger = LogManager.getLogger(BulkRequestStatistics.class);

  private AtomicInteger totalRequests = new AtomicInteger();
  private AtomicInteger totalBadRequests = new AtomicInteger();
  private Map<String, Double> avgLatencyByPath;
  private Map<String, Integer> maxLatencyByPath;
  private Map<String, Integer> medianLatencyByPath;
  private Map<String, Integer> p99LatencyByPath;
  private long wallStart;
  private long wallStop;

  private CsvStatsReader reader;
  private CsvStatsWriter writer;

  public BulkRequestStatistics(String filePathStr) {
    this.reader = new CsvStatsReader(filePathStr);
    this.writer = new CsvStatsWriter(filePathStr);
  }

  // List of references to arrays that contain raw stats from the requests of individual
  // threads of PhaseRunners
  private List<SingleRequestStatistics[]> singleStatsArrays = new LinkedList<>();

  /**
   * Adds the given array reference to a linked list synchronously.
   *
   * @param array an array of stats
   */
  public synchronized void addToStatArray(SingleRequestStatistics[] array) {
    singleStatsArrays.add(array);
  }

  /**
   * Creates a CSV file with statistics from every request in a list of
   * statistics.SingleRequestStatistics arrays. If writing fails, prints a message to stderr and
   * logs it.
   *
   * @param filename the desired filename
   */
  public void buildStatsCsv(String filename) throws IOException {
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

    final String headers = "RequestType,Path,StartTimestamp(ms),Latency(ms),ResponseCode";
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
   * Creates a one line string in CSV format from a statistics.SingleRequestStatistics object.
   *
   * @param singleStats the stats to make a string
   * @return A string with the stats in CSV format
   */
  private String buildCsvLine(SingleRequestStatistics singleStats) {
    String type = singleStats.getRequestType();
    String path = singleStats.getPath();
    String start = String.valueOf(singleStats.getStartTime());
    String latency = String.valueOf(singleStats.getLatency());
    String code = String.valueOf(singleStats.getResponseCode());

    String[] data = new String[]{type, path, start, latency, code};
    return String.join(",", data);
  }

  /**
   * Calculates final statistics for the client run. This will only work after all phases of main
   * are complete.
   *
   * @throws InterruptedException if threads are interrupted
   */
  public void performFinalCalcs() throws InterruptedException {
    // Work that doesn't need a max latency value
    Thread mean = launchMeanCalculation();

    // Max latency calculation
    try {
      this.maxLatencyByPath = reader.calculateMaxLatencies();
    } catch (IOException | NumberFormatException e) {
      handleError(e);
    }

    // Work requiring a max latency (for counting array)
    Thread median = launchMedianCalculation();
    Thread p99 = launchP99Calculation();

    // Let work finish
    mean.join();
    median.join();
    p99.join();
  }

  /**
   * Launches a mean latency calculation in a new thread.
   *
   * @return the thread handle
   */
  private Thread launchMeanCalculation() {
    // Set up work to be done
    Runnable work = () -> {
      try {
        this.avgLatencyByPath = reader.calculateMeanLatencies();
      } catch (IOException | NumberFormatException e) {
        handleError(e);
      }
    };
    Thread thread = new Thread(work);
    thread.start();
    return thread;
  }

  /**
   * Launches a median latency calculation in a new thread.
   *
   * @return the thread handle
   */
  private Thread launchMedianCalculation() {
    // Set up work to be done
    Runnable work = () -> {
      try {
        this.medianLatencyByPath = reader.calculateMedianLatencies(this.maxLatencyByPath);
      } catch (IOException | NumberFormatException e) {
        handleError(e);
      }
    };
    Thread thread = new Thread(work);
    thread.start();
    return thread;
  }

  /**
   * Launches a median latency calculation in a new thread.
   *
   * @return the thread handle
   */
  private Thread launchP99Calculation() {
    // Set up work to be done
    Runnable work = () -> {
      try {
        this.p99LatencyByPath = reader.calculateP99Latencies(this.maxLatencyByPath);
      } catch (IOException | NumberFormatException e) {
        handleError(e);
      }
    };
    Thread thread = new Thread(work);
    thread.start();
    return thread;
  }

  /**
   * Logs and prints an error
   *
   * @param e the exception
   */
  private void handleError(Exception e) {
    String msg = "problem during execution: " + e.getMessage();
    logger.error(msg);
    System.err.println(msg);
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

  public void startWallTimer() {
    this.wallStart = System.currentTimeMillis();
  }

  public void stopWallTimer() {
    this.wallStop = System.currentTimeMillis();
  }

  @Override
  public String toString() {
    return String.format("Execution Statistics\n"
            + "--------------------\n"
            + "Total Requests: %d\n"
            + "Bad Requests: %d\n"
            + "Wall Time: %.2f seconds\n"
            + "Total Throughput: %.2f requests/second\n"
            + "Success Throughput: %.2f requests/second\n"
        , totalRequests.get()
        , totalBadRequests.get()
        , getWallTimeSecs()
        , getThroughputPerSec()
        , getGoodThroughputPerSec()
    )
        + statsPerPathToString();
  }

  /**
   * Provides statistics for each path as a string.
   *
   * @return a string of statistics for each path
   */
  private String statsPerPathToString() {
    // Same keys for every map
    Set<String> keys = this.avgLatencyByPath.keySet();
    StringBuilder builder = new StringBuilder();
    char newline = '\n';
    for (String key : keys) {
      // Section start
      builder.append("Latencies (ms) for ");
      builder.append(key);
      builder.append(":\n");

      // Mean
      builder.append("\tMean: ");
      String mean = String.format("%.2f\n", this.avgLatencyByPath.get(key));
      builder.append(mean);

      // Median
      builder.append("\tMedian: ");
      int median = this.medianLatencyByPath.get(key);
      builder.append(median);
      builder.append(newline);

      // P99
      builder.append("\t99th Percentile: ");
      int p99 = this.p99LatencyByPath.get(key);
      builder.append(p99);
      builder.append(newline);

      // Max
      builder.append("\tMax: ");
      int max = this.maxLatencyByPath.get(key);
      builder.append(max);
      builder.append(newline);
    }

    return builder.toString();
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
