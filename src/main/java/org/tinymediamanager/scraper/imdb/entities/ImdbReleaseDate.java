package org.tinymediamanager.scraper.imdb.entities;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class ImdbReleaseDate {
  public int                  day;
  public int                  month;
  public int                  year;
  @JsonIgnore
  private Map<String, Object> additionalProperties = new HashMap<>();

  @JsonAnySetter
  public void setAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
  }

  /**
   * @return Date or NULL
   */
  public Date toDate() {
    try {
      Date date = new GregorianCalendar(year, month - 1, day).getTime();
      return date;
    }
    catch (Exception e) {
      return null;
    }
  }

}
