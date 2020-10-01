/**
 * Holds and provides access to statistics from a single request.
 */
public class SingleRequestStatistics {

  private String requestType;
  private long startTime;
  private long latency;
  private int responseCode;

  /**
   * Constructor for SingleRequestStatistics.
   * @param requestType The request type (i.e. "GET" or "POST")
   * @param startTime The unix time at the start of the request
   * @param latency The latency of the request (e.g. the round trip time)
   * @param responseCode The response code returned from the server
   */
  public SingleRequestStatistics(String requestType, long startTime, long latency, int responseCode) {
    this.requestType = requestType;
    this.startTime = startTime;
    this.latency = latency;
    this.responseCode = responseCode;
  }

  public String getRequestType() {
    return requestType;
  }

  public long getStartTime() {
    return startTime;
  }

  public long getLatency() {
    return latency;
  }

  public int getResponseCode() {
    return responseCode;
  }


}
