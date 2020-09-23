# ResortsApi

All URIs are relative to */*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getTopTenVert**](ResortsApi.md#getTopTenVert) | **GET** /resort/day/top10vert | get the top 10 skier vertical totals for this day

<a name="getTopTenVert"></a>
# **getTopTenVert**
> TopTen getTopTenVert(resort, dayID)

get the top 10 skier vertical totals for this day

get the top 10 skier vertical totals for this day

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.ResortsApi;


ResortsApi apiInstance = new ResortsApi();
List<String> resort = Arrays.asList("resort_example"); // List<String> | resort to query by
List<String> dayID = Arrays.asList("dayID_example"); // List<String> | day number in the season
try {
    TopTen result = apiInstance.getTopTenVert(resort, dayID);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ResortsApi#getTopTenVert");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **resort** | [**List&lt;String&gt;**](String.md)| resort to query by |
 **dayID** | [**List&lt;String&gt;**](String.md)| day number in the season |

### Return type

[**TopTen**](TopTen.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

