import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class BsdsApiClient {

  private static AtomicInteger totalRequests = new AtomicInteger();
  private static AtomicInteger totalBadRequests = new AtomicInteger();

  public static void main(String[] args) throws InterruptedException {
    // Get arguments from properties file
    Arguments arguments = null;
    try {
      arguments = Arguments.fromPropertiesFile("arguments.properties");
    } catch (IOException e) {
      System.out.println("Problem reading properties file, please try again: " + e.getMessage());;
      System.exit(1);
    } catch (IllegalArgumentException e) {
      System.out.println("Invalid property found: " + e.getMessage());
      System.exit(1);
    }

    /*
     * =====================================================================
     * Phase one of the client process. Phase specifications at
     * https://gortonator.github.io/bsds-6650/assignments-2020/Assignment-1
     * =====================================================================
     */
    int numThreads = arguments.getMaxThreads() / 4;
    int startTime = 1;
    int endTime = 90;
    int numPostRequestsPerThread = 100;
    int numGetRequestsPerThread = 5;

    // Set up trigger for phase two (starts after 10% threads finish)
    // Next line is confusing, but spec states 10% should be rounded *up*
    int triggerNum = (int) Math.ceil((double) numThreads / 10);
    CountDownLatch phase2Latch = new CountDownLatch(triggerNum);

    // Start phase
    Arguments finalArguments = arguments;  // don't know why needed, but IntelliJ complained
    Runnable run = () -> {
      executePhase(
          finalArguments,
          numThreads,
          startTime,
          endTime,
          numPostRequestsPerThread,
          numGetRequestsPerThread,
          phase2Latch
      );
    };
    Thread phase1 = new Thread(run);
    System.out.println("Phase 1 Start");
    phase1.start();
    phase2Latch.await();
    System.out.println("Phase 2 Start");


    /*
     * =====================================================================
     * Phase two of the client process.
     * =====================================================================
     */

    // Ensure all phases complete
    phase1.join();

    // Final stats
    System.out.println("Total requests: " + totalRequests
        + ", Bad Requests: " + totalBadRequests);
  }

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

      // Start thread
      PhaseRunner runner = new PhaseRunner(
          numPostRequestsPerThread,
          numGetRequestsPerThread,
          arguments,
          completionLatch,
          totalBadRequests,
          nextPhaseLatch
      );
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
