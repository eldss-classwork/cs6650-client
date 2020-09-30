/**
 * Holds and provides access to statistics from a single request.
 */
public class SingleRequestStatistics {

  private final String requestType;
  private final int startTime;
  private final int latency;
  private final int responseCode;

  /**
   * Constructor for SingleRequestStatistics. Once set, values are final.
   * @param requestType The request type (i.e. "GET" or "POST")
   * @param startTime The unix time at the start of the request
   * @param latency The latency of the request (e.g. the round trip time)
   * @param responseCode The response code returned from the server
   */
  public SingleRequestStatistics(String requestType, int startTime, int latency, int responseCode) {
    this.requestType = requestType;
    this.startTime = startTime;
    this.latency = latency;
    this.responseCode = responseCode;
  }

  public String getRequestType() {
    return requestType;
  }

  public int getStartTime() {
    return startTime;
  }

  public int getLatency() {
    return latency;
  }

  public int getResponseCode() {
    return responseCode;
  }
}
