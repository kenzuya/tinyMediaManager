package org.tinymediamanager.scraper.imdb.entities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class ImdbVideo {
  public String                     id                   = "";
  public boolean                    isMature             = false;
  public String                     creeatedDate         = "";
  public ImdbImage                  thumbnail            = null;
  public ImdbLocalizedString        description          = null;
  public ImdbLocalizedString        name                 = null;
  public ArrayList<ImdbPlaybackUrl> playbackURLs         = new ArrayList<ImdbPlaybackUrl>();

  @JsonIgnore
  private Map<String, Object>       additionalProperties = new HashMap<String, Object>();

  @JsonAnySetter
  public void setAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
  }

  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }
}
