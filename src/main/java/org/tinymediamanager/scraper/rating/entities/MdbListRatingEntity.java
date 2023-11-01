package org.tinymediamanager.scraper.rating.entities;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class MdbListRatingEntity {
  @SerializedName("ratings")
  public List<MdbListRatings> ratings = null;
}
