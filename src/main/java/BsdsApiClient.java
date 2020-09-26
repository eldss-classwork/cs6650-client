import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BsdsApiClient {

  private static final Logger logger = LogManager.getLogger(BsdsApiClient.class);

  private static AtomicInteger totalRequests = new AtomicInteger();
  private static AtomicInteger totalBadRequests = new AtomicInteger();

  public static void main(String[] args) throws InterruptedException {
    infoLogAndPrint("Starting client...");

    // Get arguments from properties file
    logger.trace("parsing properties file");
    Arguments propertyArgs = null;
    try {
      propertyArgs = Arguments.fromPropertiesFile("arguments.properties");
    } catch (IOException e) {
      System.out.println("Problem reading properties file, please try again: " + e.getMessage());;
      System.exit(1);
    } catch (IllegalArgumentException e) {
      System.out.println("Invalid property found: " + e.getMessage());
      System.exit(1);
    }
    final Arguments arguments = propertyArgs;

    // Track total execution time
    // Keeping setup of first phase because all others will be included
    long start = System.currentTimeMillis();
    /*
     * =====================================================================
     * Phase one of the client process. Warmup. Phase specifications at
     * https://gortonator.github.io/bsds-6650/assignments-2020/Assignment-1
     * =====================================================================
     */
    logger.trace("phase 1 set-up");

    // Set up trigger for phase two (starts after 10% threads finish)
    int numThreadsP1 = arguments.getMaxThreads() / 4;
    // Next line is confusing, but spec states 10% should be rounded *up*
    int triggerNum = (int) Math.ceil((double) numThreadsP1 / 10);
    CountDownLatch phase2Latch = new CountDownLatch(triggerNum);

    // Create phase 1
    Runnable run1 = () -> {
      int startTime = 1;
      int endTime = 90;
      int numPostRequestsPerThread = 100;
      int numGetRequestsPerThread = 5;
      executePhase(
          arguments,
          numThreadsP1,
          startTime,
          endTime,
          numPostRequestsPerThread,
          numGetRequestsPerThread,
          phase2Latch
      );
    };
    Thread phase1 = new Thread(run1);

    infoLogAndPrint("Starting phase 1...");
    phase1.start();
    phase2Latch.await();

    /*
     * =====================================================================
     * Phase two of the client process. Peak.
     * =====================================================================
     */
    logger.trace("phase 2 set-up");

    // Set up trigger for phase two (starts after 10% threads finish)
    int numThreadsP2 = arguments.getMaxThreads();
    triggerNum = (int) Math.ceil((double) numThreadsP2 / 10);
    CountDownLatch phase3Latch = new CountDownLatch(triggerNum);

    // Create phase 2
    Runnable run2 = () -> {
      int startTime = 91;
      int endTime = 360;
      int numPostRequestsPerThread = 100;
      int numGetRequestsPerThread = 5;
      executePhase(
          arguments,
          numThreadsP2,
          startTime,
          endTime,
          numPostRequestsPerThread,
          numGetRequestsPerThread,
          phase3Latch
      );
    };
    Thread phase2 = new Thread(run2);

    infoLogAndPrint("Starting phase 2...");
    phase2.start();
    phase3Latch.await();

    /*
     * =====================================================================
     * Phase three of the client process. Cooldown.
     * =====================================================================
     */
    logger.trace("phase 3 set-up");

    // No trigger needed for phase 3, last phase
    int numThreadsP3 = numThreadsP1;

    // Create phase 2
    Runnable run3 = () -> {
      int startTime = 361;
      int endTime = 420;
      int numPostRequestsPerThread = 100;
      int numGetRequestsPerThread = 10;
      executePhase(
          arguments,
          numThreadsP3,
          startTime,
          endTime,
          numPostRequestsPerThread,
          numGetRequestsPerThread,
          new CountDownLatch(0)
      );
    };
    Thread phase3 = new Thread(run3);

    infoLogAndPrint("Starting phase 3...");
    phase3.start();

    // Ensure all phases complete
    phase1.join();
    phase2.join();
    phase3.join();
    long end = System.currentTimeMillis();

    infoLogAndPrint("All phases complete");
    System.out.println();  // newline for terminal users

    // Final stats
    int millisecsPerSec = 1000;
    double wallTime = (double)(end - start) / millisecsPerSec;
    double throughput = totalRequests.get() / wallTime;
    double goodThroughput = (totalRequests.get() - totalBadRequests.get()) / wallTime;

    String stats = String.format("Execution Statistics:\n"
        + "\tTotal requests: %d\n"
        + "\tBad Requests: %d\n"
        + "\tWall Time: %.2f seconds\n"
        + "\tTotal Throughput: %.2f requests/second\n"
        + "\tSuccess Throughput: %.2f requests/second\n"
        , totalRequests.get(), totalBadRequests.get(), wallTime, throughput, goodThroughput);
    infoLogAndPrint(stats);
  }

  /**
   * Executes one phase of the client process.
   * @param arguments arguments provided to the client
   * @param numThreads number of threads to create
   * @param startTime start of time range for this phase
   * @param endTime end of time range for this phase
   * @param numPostRequestsPerThread number of POST requests to make per thread
   * @param numGetRequestsPerThread number of GET requests to make per thread
   * @param nextPhaseLatch a CountDownLatch to determine when the next phase can start
   *                       (null or set to 0 if there is no next phase).
   */
  private static void executePhase(
      Arguments arguments,
      int numThreads,
      int startTime,
      int endTime,
      int numPostRequestsPerThread,
      int numGetRequestsPerThread,
      CountDownLatch nextPhaseLatch)
  {
    // Set-up vars given in spec
    int skiersPerThread = arguments.getNumSkiers() / numThreads;

    // Start threads
    CountDownLatch completionLatch = new CountDownLatch(numThreads);
    int skierIdStart = 1;
    int skierIdEnd = skiersPerThread;
    for (int i = 0; i < numThreads; i++) {
      // Ensure last thread does not have too many skiers
      if (i == numThreads - 1) {
        skierIdEnd = arguments.getNumSkiers();
      }

      // Create and start thread
      PhaseRunner runner = new PhaseRunner(
          numPostRequestsPerThread,
          numGetRequestsPerThread,
          arguments,
          completionLatch,
          totalBadRequests,
          nextPhaseLatch
      );
      // Probably a poor design choice here, will fix given the time
      runner.setSkierIdRange(skierIdStart, skierIdEnd);
      runner.setTimeRange(startTime, endTime);
      new Thread(runner).start();

      // Calculate skier range for next thread
      skierIdStart = skierIdEnd + 1;
      skierIdEnd = skierIdEnd + skiersPerThread;
    }

    // Wait for threads to complete
    try {
      completionLatch.await();
    } catch (InterruptedException e) {
      System.err.println("An issue occurred executing threads: " + e.getMessage());
      e.printStackTrace();
    }

    // Calculate total requests completed
    int numPhaseRequests = (numPostRequestsPerThread + numGetRequestsPerThread) * numThreads;
    totalRequests.getAndAdd(numPhaseRequests);
  }

  /**
   * Produces an INFO level log and prints to System.out for user friendly readability
   * @param msg A message to output
   */
  static private void infoLogAndPrint(String msg) {
    logger.info(msg);
    System.out.println(msg);
  }
}
