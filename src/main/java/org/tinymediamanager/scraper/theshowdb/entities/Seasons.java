package org.tinymediamanager.scraper.theshowdb.entities;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class Seasons {

  @SerializedName("seasons")
  private List<Season> seasons;

  public List<Season> getSeasons() {
    return seasons;
  }

}
