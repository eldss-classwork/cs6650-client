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
 * Stores all statistics gathered in the client and provides methods to
 * calculate further information from the raw stats.
 */
public class BulkRequestStatistics {

  public static final int MILLISECS_PER_SEC = 1000;

  private static final Logger logger = LogManager.getLogger(BulkRequestStatistics.class);

  private final String POST = "POST";
  private final String GET = "GET";

  private AtomicInteger totalRequests = new AtomicInteger();
  private AtomicInteger totalBadRequests = new AtomicInteger();
  private Map<String, Double> avgLatencyByPath;
  private Map<String, Integer> maxLatencyByPath;
  private long medianPostTime;
  private long medianGetTime;
  private long maxPostTime = -1;  // Initialized for assertion in median calculation
  private long maxGetTime = -1;
  private long p99PostTime;
  private long p99GetTime;
  private long wallStart;
  private long wallStop;

  private CsvStatsReader reader;

  public BulkRequestStatistics(String filePathStr) {
    this.reader = new CsvStatsReader(filePathStr);
  }

  // List of references to arrays that contain raw stats from the requests of individual
  // threads of PhaseRunners
  private List<SingleRequestStatistics[]> singleStatsArrays = new LinkedList<>();

  /**
   * Adds the given array reference to a linked list synchronously.
   * @param array an array of stats
   */
  public synchronized void addToStatArray(SingleRequestStatistics[] array) {
    singleStatsArrays.add(array);
  }

  /**
   * Creates a CSV file with statistics from every request in a list of statistics.SingleRequestStatistics
   * arrays. If writing fails, prints a message to stderr and logs it.
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
   * Calculates final statistics for the client run. This will only work after all
   * phases of main are complete.
   * @throws InterruptedException if threads are interrupted
   */
  public void performFinalCalcs() throws InterruptedException {
    Runnable maxAndP99R = () -> {
      // Max has to come before
      caclulateMaxLatencies();
      calculateMedianAndP99Latencies();
    };

    // Create and run threads
    Thread maxAndP99T = new Thread(maxAndP99R);
    maxAndP99T.start();

    Thread mean = launchMeanCalculation();
    Thread max = launchMaxCalculation();

    // Let work finish
    maxAndP99T.join();
    mean.join();
    max.join();
  }

  /**
   * Launches a mean latency calculation in a new thread.
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
   * Launches a max latency calculation in a new thread.
   * @return the thread handle
   */
  private Thread launchMaxCalculation() {
    // Set up work to be done
    Runnable work = () -> {
      try {
        this.maxLatencyByPath = reader.calculateMaxLatencies();
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
   * @param e the exception
   */
  private void handleError(Exception e) {
    String msg = "problem during execution: " + e.getMessage();
    logger.error(msg);
    System.err.println(msg);
  }

  /**
   * Calculates the maximum latency for each request type.
   */
  private void caclulateMaxLatencies() {
    long maxPost = 0;
    long maxGet = 0;

    // Find max values
    for (SingleRequestStatistics[] arr : singleStatsArrays) {
      for (SingleRequestStatistics statistics : arr) {
        long latency = statistics.getLatency();
        if (statistics.getRequestType().equals(POST)
            && latency > maxPost) {
          maxPost = latency;
        } else if (statistics.getRequestType().equals(GET)
            && latency > maxGet) {
          maxGet = latency;
        }
      }
    }

    this.maxPostTime = maxPost;
    this.maxGetTime = maxGet;
  }

  /**
   * Calculates the median and 99th percentile latency for each request type.
   * These are calculated together due to implementation details. For the same reason,
   * max latencies must me calculated before calling this method.
   */
  private void calculateMedianAndP99Latencies() {
    // Ensure max latencies exist
    assert maxPostTime != -1;
    assert maxGetTime != -1;

    long[] postLatencyMap = new long[(int) this.maxPostTime + 1];
    long[] getLatencyMap = new long[(int) this.maxGetTime + 1];
    long totalPosts = 0;
    long totalGets = 0;

    // Fill maps with count of each latency
    for (SingleRequestStatistics[] arr : singleStatsArrays) {
      for (SingleRequestStatistics statistics : arr) {
        int index = (int) statistics.getLatency();
        if (statistics.getRequestType().equals(POST)) {
          postLatencyMap[index]++;
          totalPosts++;
        } else if (statistics.getRequestType().equals(GET)) {
          getLatencyMap[index]++;
          totalGets++;
        }
      }
    }

    this.medianPostTime = getMedianFromCountingArray(postLatencyMap, totalPosts);
    this.p99PostTime = getP99FromCountingArray(postLatencyMap, totalPosts);
    this.medianGetTime = getMedianFromCountingArray(getLatencyMap, totalGets);
    this.p99GetTime = getP99FromCountingArray(getLatencyMap, totalGets);
  }

  /**
   * Helper for finding the median of an array efficiently.
   * @param arr an array in which the index is the latency and the values are the counts
   *            of requests that had that latency
   * @return the median latency
   */
  private long getMedianFromCountingArray(long[] arr, long numRequests) {
    long middleRequest = Math.round(numRequests / 2.0);  // approximate in some cases
    long currTotal = 0;
    for (int i = 0; i < arr.length; i++) {
      // Find the middle request bucket
      // its index is the median
      currTotal += arr[i];
      if (currTotal >= middleRequest) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Helper for finding the 99th percentile of an array efficiently.
   * @param arr an array in which the index is the latency and the values are the counts
   *            of requests that had that latency
   * @return the median latency
   */
  private long getP99FromCountingArray(long[] arr, long numRequests) {
    long p99Request = Math.round(numRequests * 0.99);  // approximate for decimal values
    long currTotal = numRequests;
    for (int i = arr.length-1; i >= 0; i--) {
      // Find the bucket of the p99 request
      // its index is value we want
      currTotal -= arr[i];
      if (currTotal <= p99Request) {
        return i;
      }
    }
    return -1;
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
    return String.format("Execution Statistics:\n"
            + "Total Requests: %d\n"
            + "Bad Requests: %d\n"
            + "Wall Time: %.2f seconds\n"
            + "Total Throughput: %.2f requests/second\n"
            + "Success Throughput: %.2f requests/second\n"
            + "POST Latencies (milliseconds):\n"
            + "\tMedian: %d\n"
            + "\t99th Percentile: %d\n"
            + "\tMax: %d\n"
            + "GET Latencies (millisconds):\n"
            + "\tMedian: %d\n"
            + "\t99th Percentile: %d\n"
            + "\tMax: %d\n"
            , totalRequests.get()
            , totalBadRequests.get()
            , getWallTimeSecs()
            , getThroughputPerSec()
            , getGoodThroughputPerSec()
            , medianPostTime
            , p99PostTime
            , maxPostTime
            , medianGetTime
            , p99GetTime
            , maxGetTime
    ) + "\n" + statsPerPathToString();
  }

  /**
   * Provides statistics for each path as a string.
   * @return a string of statistics for each path
   */
  private String statsPerPathToString() {
    // Same keys for every map
    Set<String> keys = this.avgLatencyByPath.keySet();
    StringBuilder builder = new StringBuilder();
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

      // P99

      // Max
      builder.append("\tMax: ");
      int max = this.maxLatencyByPath.get(key);
      builder.append(max);
      builder.append("\n");
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
