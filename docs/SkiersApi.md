# SkiersApi

All URIs are relative to */*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getSkierDayVertical**](SkiersApi.md#getSkierDayVertical) | **GET** /skiers/{resortID}/days/{dayID}/skiers/{skierID} | 
[**getSkierResortTotals**](SkiersApi.md#getSkierResortTotals) | **GET** /skiers/{skierID}/vertical | get the total vertical for the skier for the specified resort
[**writeNewLiftRide**](SkiersApi.md#writeNewLiftRide) | **POST** /skiers/liftrides | write a new lift ride for the skier

<a name="getSkierDayVertical"></a>
# **getSkierDayVertical**
> SkierVertical getSkierDayVertical(resortID, dayID, skierID)



get the total vertical for the skier for the specified ski day

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.SkiersApi;


SkiersApi apiInstance = new SkiersApi();
String resortID = "resortID_example"; // String | ID of the resort the skier is at
String dayID = "dayID_example"; // String | ID number of ski day in the ski season
String skierID = "skierID_example"; // String | ID of the skier riding the lift
try {
    SkierVertical result = apiInstance.getSkierDayVertical(resortID, dayID, skierID);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling SkiersApi#getSkierDayVertical");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **resortID** | **String**| ID of the resort the skier is at |
 **dayID** | **String**| ID number of ski day in the ski season |
 **skierID** | **String**| ID of the skier riding the lift |

### Return type

[**SkierVertical**](SkierVertical.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a name="getSkierResortTotals"></a>
# **getSkierResortTotals**
> SkierVertical getSkierResortTotals(skierID, resort)

get the total vertical for the skier for the specified resort

get the total vertical for the skier the specified resort.

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.SkiersApi;


SkiersApi apiInstance = new SkiersApi();
String skierID = "skierID_example"; // String | ID the skier to retrieve data for
List<String> resort = Arrays.asList("resort_example"); // List<String> | resort to filter by
try {
    SkierVertical result = apiInstance.getSkierResortTotals(skierID, resort);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling SkiersApi#getSkierResortTotals");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **skierID** | **String**| ID the skier to retrieve data for |
 **resort** | [**List&lt;String&gt;**](String.md)| resort to filter by |

### Return type

[**SkierVertical**](SkierVertical.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a name="writeNewLiftRide"></a>
# **writeNewLiftRide**
> writeNewLiftRide(body)

write a new lift ride for the skier

Stores new lift ride details in the data store

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.SkiersApi;


SkiersApi apiInstance = new SkiersApi();
LiftRide body = new LiftRide(); // LiftRide | information for new lift ride event
try {
    apiInstance.writeNewLiftRide(body);
} catch (ApiException e) {
    System.err.println("Exception when calling SkiersApi#writeNewLiftRide");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**LiftRide**](LiftRide.md)| information for new lift ride event |

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

