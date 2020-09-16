package org.tinymediamanager.scraper.util.youtube.model.playlist;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.JsonNode;

public class PlaylistVideoDetails {

  public String        videoId;
  public int           lengthSeconds;
  private List<String> thumbnails = new ArrayList<>();
  protected boolean    isLive;

  public PlaylistVideoDetails() {
  }

  public PlaylistVideoDetails(JsonNode json) {
    if (json.has("shortBylineText")) {
      String author = json.get("shortBylineText").get("runs").get(0).get("text").asText();
    }
    JsonNode jsonTitle = json.get("title");
    String title;
    if (jsonTitle.has("simpleText")) {
      title = jsonTitle.get("simpleText").asText();
    }
    else {
      title = jsonTitle.get("runs").get(0).get("text").asText();
    }
    if (!thumbnails.isEmpty()) {
      // Otherwise, contains "/hqdefault.jpg?"
      isLive = thumbnails.get(0).contains("/hqdefault_live.jpg?");
    }

    if (json.has("index")) {
      int index = json.get("index").get("simpleText").asInt();
    }
    boolean isPlayable = json.get("isPlayable").asBoolean();
  }

}
