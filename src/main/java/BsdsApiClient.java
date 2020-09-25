import io.swagger.client.*;
import io.swagger.client.api.SkiersApi;
import io.swagger.client.auth.*;
import io.swagger.client.model.*;
import io.swagger.client.api.ResortsApi;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class BsdsApiClient {

  public static void main(String[] args) {
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

    // Start threads
    CountDownLatch latch = new CountDownLatch(3);
    for (int i = 0; i < 3; i++) {
      PhaseRunner runner = new PhaseRunner(100, 0, arguments, latch);
      runner.setSkierIdRange(1, 100);
      runner.setTimeRange(1, 420);
      new Thread(runner).start();
    }
    try {
      latch.await();
    } catch (InterruptedException e) {
      System.err.println("An issue occurred executing threads: " + e.getMessage());
      e.printStackTrace();
    }
  }
}
