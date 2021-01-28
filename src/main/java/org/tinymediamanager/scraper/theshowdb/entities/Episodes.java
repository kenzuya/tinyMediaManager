package org.tinymediamanager.scraper.theshowdb.entities;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class Episodes {

  @SerializedName("episodes")
  List<Episode> episodes;

  public List<Episode> getEpisodes() {
    return episodes;
  }
}
