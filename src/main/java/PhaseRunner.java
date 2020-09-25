import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.SkiersApi;
import io.swagger.client.model.LiftRide;
import io.swagger.client.model.SkierVertical;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PhaseRunner uses the client SDK to call the server API in an automated way.
 *
 * Source of truth for how phases are run is found here:
 * https://gortonator.github.io/bsds-6650/assignments-2020/Assignment-1
 */
public class PhaseRunner implements Runnable {

  private SkiersApi skiersApiInstance;
  private Arguments args;
  private CountDownLatch completionLatch;
  private CountDownLatch nextPhaseLatch;
  private AtomicInteger badRequestCount;
  private ThreadLocalRandom rand;
  private int numPosts;
  private int numGets;
  private int skierIdLow;
  private int skierIdHigh;
  private int timeLow;
  private int timeHigh;

  /**
   * Basic constructor for a PhaseRunner.
   *
   * To aid in readability, set skier and time ranges in helper methods,
   * setSkierIdRange and setTimeRange. These fields will be null if not set.
   * @throws IllegalArgumentException if args is null or either numPosts or numGets is negative
   */
  public PhaseRunner(
      int numPosts,
      int numGets,
      Arguments args,
      CountDownLatch completionLatch,
      AtomicInteger badRequestCount,
      CountDownLatch nextPhaseLatch)
      throws IllegalArgumentException
  {
    if (args == null || completionLatch == null || numPosts < 0 || numGets < 0) {
      throw new IllegalArgumentException(
          "invalid arguments - args cannot be null, posts and gets cannot be negative");
    }
    this.numPosts = numPosts;
    this.numGets = numGets;
    this.args = args;
    this.completionLatch = completionLatch;
    this.nextPhaseLatch = nextPhaseLatch;
    this.badRequestCount = badRequestCount;

    // Prevent null pointer errors if no next phase is given
    if (nextPhaseLatch == null) {
      this.nextPhaseLatch = new CountDownLatch(0);
    }

    // Set up api caller instance
    this.skiersApiInstance = new SkiersApi();
    this.skiersApiInstance.getApiClient().setBasePath(this.args.getHostAddress());

    // Thread-safe random number generator for generating API calls
    this.rand = ThreadLocalRandom.current();
  }

  /**
   * Sets the skier ID range (inclusive) for this runner.
   * @param low low bound
   * @param high high bound
   * @throws IllegalArgumentException if invalid bounds are given
   */
  public void setSkierIdRange(int low, int high) throws IllegalArgumentException {
    // Basic validation (consider a separate class for arg validation)
    if (low < 0 || high < 0) {
      throw new IllegalArgumentException("bounds cannot be negative");
    }
    if (low > high) {
      throw new IllegalArgumentException("low bound cannot be greater than high bound");
    }
    this.skierIdLow = low;
    this.skierIdHigh = high;
  }

  /**
   * Sets the time range (inclusive) for this runner.
   * @param low low bound
   * @param high high bound
   * @throws IllegalArgumentException if invalid bounds are given
   */
  public void setTimeRange(int low, int high) {
    // Basic validation (consider a separate class for arg validation)
    if (low < 0 || high < 0) {
      throw new IllegalArgumentException("bounds cannot be negative");
    }
    if (low > high) {
      throw new IllegalArgumentException("low bound cannot be greater than high bound");
    }
    this.timeLow = low;
    this.timeHigh = high;
  }

  @Override
  public void run() {
    performPosts();
    performGets();
    nextPhaseLatch.countDown();
    completionLatch.countDown();
  }

  private void performPosts() {
    // Set up reusable parts of a lift ride
    LiftRide liftRide = new LiftRide();
    liftRide.setResortID(args.getResort());
    liftRide.setDayID(
        String.valueOf(args.getSkiDay())
    );

    for (int i = 0; i < numPosts; i++) {
      // Set up random variables for skier, lift and time
      liftRide.setSkierID(nextSkierId());
      liftRide.setTime(nextTime());
      liftRide.setLiftID(nextLift());

      // Attempt request
      try {
        skiersApiInstance.writeNewLiftRide(liftRide);
      } catch (ApiException e) {
        // TODO: log all failed requests with log4j
        badRequestCount.getAndIncrement();
        System.err.println("API error: " + e.getMessage());
      }
    }
  }

  private void performGets() {
    for (int i = 0; i < numGets; i++) {
      try {
        // Don't need results at this time
        skiersApiInstance.getSkierDayVertical(
            args.getResort(),
            String.valueOf(args.getSkiDay()),
            nextSkierId()
        );
      } catch (ApiException e) {
        // TODO: log all failed requests with log4j
        badRequestCount.getAndIncrement();
        System.err.println("API error: " + e.getMessage());
      }
    }
  }

  private String nextSkierId() {
    return String.valueOf(rand.nextInt(skierIdLow, skierIdHigh + 1));
  }

  private String nextTime() {
    return String.valueOf(rand.nextInt(timeLow, timeHigh + 1));
  }

  private String nextLift() {
    return String.valueOf(rand.nextInt(args.getNumSkiLifts()));
  }
}
