package org.tinymediamanager.scraper.rating.entities;

import com.google.gson.annotations.SerializedName;

public class MdbListRatings {

  @SerializedName("source")
  public String source;

  @SerializedName("value")
  public Float value;

  @SerializedName("score")
  public int score;

  @SerializedName("votes")
  public int votes;

  @SerializedName("popular")
  public int popular;

  @SerializedName("url")
  public String url;
}
