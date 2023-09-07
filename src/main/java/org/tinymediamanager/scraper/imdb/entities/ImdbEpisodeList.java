package org.tinymediamanager.scraper.imdb.entities;

import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.THUMB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.Constants;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.scraper.util.MetadataUtil;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class ImdbEpisodeList {
  private static final Logger    LOGGER               = LoggerFactory.getLogger(ImdbEpisodeList.class);

  public String                  currentSeason        = "";
  public String                  currentYear          = "";
  public ImdbEpisodeListEpisodes episodes             = null;
  public List<ImdbIdValueType>   seasons              = null;
  public List<ImdbIdValueType>   years                = null;

  @JsonIgnore
  private Map<String, Object>    additionalProperties = new HashMap<>();

  @JsonAnySetter
  public void setAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
  }

  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

  public List<MediaMetadata> getEpisodes() {
    List<MediaMetadata> eps = new ArrayList<>();

    if (episodes != null) {
      for (ImdbEpisodeListEpisode ep : ListUtils.nullSafe(episodes.items)) {
        MediaMetadata md = new MediaMetadata(Constants.IMDB);
        md.setTitle(ep.titleText);
        md.setPlot(ep.plot);
        md.setId(Constants.IMDB, ep.id);

        int s = MetadataUtil.parseInt(ep.season, -1);
        int e = MetadataUtil.parseInt(ep.episode, -1);
        if (e < 0 || s < 0) {
          continue;
        }
        md.setEpisodeNumber(e);
        md.setSeasonNumber(s);

        if (ep.image != null) {
          MediaArtwork img = new MediaArtwork(Constants.IMDB, THUMB);
          img.setOriginalUrl(ep.image.url);
          img.setSeason(s);
          img.addImageSize(ep.image.maxWidth, ep.image.maxHeight, ep.image.url);
          md.addMediaArt(img);
        }

        if (ep.aggregateRating > 0.0) {
          MediaRating rat = new MediaRating(Constants.IMDB);
          rat.setMaxValue(10);
          rat.setRating(ep.aggregateRating);
          rat.setVotes(ep.voteCount);
          md.addRating(rat);
        }

        md.setYear(ep.releaseYear);
        if (ep.releaseDate != null) {
          md.setReleaseDate(ep.releaseDate.toDate());
        }

        eps.add(md);
      }
    }
    return eps;
  }

  public class ImdbEpisodeListImage {

    public String               caption              = "";
    public String               url                  = "";
    public Integer              maxHeight            = 0;
    public Integer              maxWidth             = 0;

    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<>();

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
      this.additionalProperties.put(name, value);
    }

    public String toString() {
      return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
  }
}
