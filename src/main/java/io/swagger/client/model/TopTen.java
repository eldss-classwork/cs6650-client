/*
 * Ski Data API for NEU Seattle distributed systems course
 * An API for an emulation of skier managment system for RFID tagged lift tickets. Basis for CS6650 Assignments for 2019
 *
 * OpenAPI spec version: 1.13
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package io.swagger.client.model;

import java.util.Objects;
import java.util.Arrays;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.swagger.client.model.TopTenTopTenSkiers;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
/**
 * TopTen
 */

@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.JavaClientCodegen", date = "2020-09-22T20:44:21.827Z[GMT]")
public class TopTen {
  @SerializedName("topTenSkiers")
  private List<TopTenTopTenSkiers> topTenSkiers = null;

  public TopTen topTenSkiers(List<TopTenTopTenSkiers> topTenSkiers) {
    this.topTenSkiers = topTenSkiers;
    return this;
  }

  public TopTen addTopTenSkiersItem(TopTenTopTenSkiers topTenSkiersItem) {
    if (this.topTenSkiers == null) {
      this.topTenSkiers = new ArrayList<TopTenTopTenSkiers>();
    }
    this.topTenSkiers.add(topTenSkiersItem);
    return this;
  }

   /**
   * Get topTenSkiers
   * @return topTenSkiers
  **/
  @Schema(description = "")
  public List<TopTenTopTenSkiers> getTopTenSkiers() {
    return topTenSkiers;
  }

  public void setTopTenSkiers(List<TopTenTopTenSkiers> topTenSkiers) {
    this.topTenSkiers = topTenSkiers;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TopTen topTen = (TopTen) o;
    return Objects.equals(this.topTenSkiers, topTen.topTenSkiers);
  }

  @Override
  public int hashCode() {
    return Objects.hash(topTenSkiers);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TopTen {\n");
    
    sb.append("    topTenSkiers: ").append(toIndentedString(topTenSkiers)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}
