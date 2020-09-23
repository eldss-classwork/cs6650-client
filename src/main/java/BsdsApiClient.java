import io.swagger.client.*;
import io.swagger.client.api.SkiersApi;
import io.swagger.client.auth.*;
import io.swagger.client.model.*;
import io.swagger.client.api.ResortsApi;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class BsdsApiClient {

  public static void main(String[] args) {
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

    System.out.println(arguments);
    ResortsApi resortApiInstance = new ResortsApi();
    SkiersApi skiersApiInstance = new SkiersApi();
    resortApiInstance.getApiClient().setBasePath(arguments.getHostAddress());
    skiersApiInstance.getApiClient().setBasePath(arguments.getHostAddress());
    List<String> resort = Arrays.asList("resort_example"); // List<String> | resort to query by
    List<String> dayID = Arrays.asList("dayID_example"); // List<String> | day number in the season
    try {
      TopTen result1 = resortApiInstance.getTopTenVert(resort, dayID);
      SkierVertical result2 = skiersApiInstance.getSkierDayVertical("123", "123", "123");
      SkierVertical result3 = skiersApiInstance.getSkierResortTotals("123", resort);
      System.out.println(result1);
      System.out.println(result2);
      System.out.println(result3);

      skiersApiInstance.writeNewLiftRide(new LiftRide());
      System.out.println("No error, write worked?");
    } catch (ApiException e) {
      System.err.println("Exception when calling ResortsApi#getTopTenVert");
      e.printStackTrace();
    }
  }
}
