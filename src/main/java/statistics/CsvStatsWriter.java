package statistics;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Writes request statistics to a CSV file.
 */
public class CsvStatsWriter {

  private Path filePath;

  public CsvStatsWriter(String csvPathStr) {
    this.filePath = Paths.get(csvPathStr);
  }
}
