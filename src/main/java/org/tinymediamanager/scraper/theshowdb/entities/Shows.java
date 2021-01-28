package org.tinymediamanager.scraper.theshowdb.entities;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class Shows {

  @SerializedName("shows")
  private List<Show> shows;

  public List<Show> getShows() {
    return shows;
  }

}
