package statistics;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides methods to calculate statistics from the CSV request file created
 * during client execution.
 */
public class CsvStatsReader {

  // For CSV splitting in calculation methods
  private final int csvColIndexMethod = 0;
  private final int csvColIndexPath = 1;
  private final int csvColIndexTimestamp = 2;
  private final int csvColIndexLatency = 3;
  private final int csvColIndexCode = 4;

  private Path filePath;

  public CsvStatsReader(String csvPathStr) {
    this.filePath = Paths.get(csvPathStr);
  }

  /**
   * Calculates the mean latency for each request type from a CSV.
   */
  public Map<String, Double> calculateMeanLatencies() throws IOException, NumberFormatException {
    // Stores "method path" -> [sum, count]
    Map<String, Long[]> tempCounter = new HashMap<>();
    Map<String, Double> avgByPath = new HashMap<>();
    String sep = ",";

    BufferedReader reader = Files.newBufferedReader(this.filePath);
    String line = reader.readLine(); // Ignore column headers
    line = reader.readLine();
    while (line != null) {
      // Get vals from line
      String[] cols = line.split(sep);
      String key = cols[csvColIndexMethod] + " " + cols[csvColIndexPath];
      long nextLatency = Long.parseLong(cols[csvColIndexLatency]);

      // Update map
      Long[] currNums = tempCounter.getOrDefault(key, new Long[]{(long) 0, (long) 0});
      currNums[0] += nextLatency;
      currNums[1] += (long) 1;
      tempCounter.put(key, currNums);

      // Get next line
      line = reader.readLine();
    }

    // Calculate the avg for each path
    for (String key : tempCounter.keySet()) {
      Long[] nums = tempCounter.get(key);

      // Ignore anything that got in with a count of zero
      // and avoid divide by zero
      if (nums[1].equals((long) 0)) {
        continue;
      }

      // Store the value
      Double avg = (double) nums[0] / nums[1];
      avgByPath.put(key, avg);
    }

    return avgByPath;
  }
}
