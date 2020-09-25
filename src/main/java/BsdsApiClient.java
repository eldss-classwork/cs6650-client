import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class BsdsApiClient {

  private static AtomicInteger totalRequests = new AtomicInteger();
  private static AtomicInteger totalBadRequests = new AtomicInteger();

  public static void main(String[] args) throws InterruptedException {
    // Get arguments from properties file
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

    /*
     * =====================================================================
     * Phase one of the client process. Warmup. Phase specifications at
     * https://gortonator.github.io/bsds-6650/assignments-2020/Assignment-1
     * =====================================================================
     */

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

    System.out.println("Phase 1 Start");
    phase1.start();
    phase2Latch.await();

    /*
     * =====================================================================
     * Phase two of the client process. Peak.
     * =====================================================================
     */

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

    System.out.println("Phase 2 Start");
    phase2.start();
    phase3Latch.await();

    /*
     * =====================================================================
     * Phase three of the client process. Cooldown.
     * =====================================================================
     */

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

    System.out.println("Phase 3 Start");
    phase3.start();

    // Ensure all phases complete
    phase1.join();
    phase2.join();
    phase3.join();
    System.out.println("All phases complete. Calculating statistics.");

    // Final stats
    System.out.println("Statistics:");
    System.out.println("\tTotal requests: " + totalRequests);
    System.out.println("\tBad Requests: " + totalBadRequests);
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
}
