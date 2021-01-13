package org.tinymediamanager.scraper.theshowdb.entities;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.scraper.util.MetadataUtil;
import org.tinymediamanager.scraper.util.ParserUtils;

import com.google.gson.annotations.SerializedName;

public class Episode {

  @SerializedName("idEpisode")
  private String episodeId;

  @SerializedName("idShow")
  private String showId;

  @SerializedName("idSeason")
  private String seasonId;

  @SerializedName("idEpisodeTVDB")
  private String tvdbEpisodeId;

  @SerializedName("idShowTVDB")
  private String tvdbShowId;

  @SerializedName("idSeasonTVDB")
  private String tvdbSeasonId;

  @SerializedName("idIMDB")
  private String imdbId;

  @SerializedName("strShow")
  private String showTitle;

  @SerializedName("intEpisode")
  private String episodeNumber;

  @SerializedName("strEpisode")
  private String episodeTitle;

  @SerializedName("intSeason")
  private String seasonNumber;

  @SerializedName("strFirstAired")
  private String firstAired;

  @SerializedName("strGuestStars")
  private String guestStars;

  @SerializedName("strDirector")
  private String directorName;

  @SerializedName("strWriter")
  private String writerName;

  @SerializedName("strDescriptionEN")
  private String episodePlot;

  @SerializedName("strProductionCode")
  private String productionCode;

  @SerializedName("strThumb")
  private String thumbUrl;

  @SerializedName("DVD_season")
  private String dvdSeasonNumber;

  @SerializedName("DVD_episodenumber")
  private String dvdEpisodeNumber;

  @SerializedName("absolute_number")
  private String absoluteNumber;

  public int getAbsoluteNumber() {
    if (absoluteNumber != null) {
      return MetadataUtil.parseInt(absoluteNumber);
    }
    else {
      return -1;
    }
  }

  public int getDvdSeasonNumber() {
    if (dvdSeasonNumber != null) {
      return MetadataUtil.parseInt(dvdSeasonNumber);
    }
    else {
      return -1;
    }
  }

  public int getDvdEpisodeNumber() {
    if (dvdEpisodeNumber != null) {
      return MetadataUtil.parseInt(dvdEpisodeNumber);
    }
    else {
      return -1;
    }
  }

  public String getEpisodeId() {
    return episodeId;
  }

  public String getShowId() {
    return showId;
  }

  public String getSeasonId() {
    return seasonId;
  }

  public String getTvdbEpisodeId() {
    return tvdbEpisodeId;
  }

  public String getTvdbShowId() {
    return tvdbShowId;
  }

  public String getTvdbSeasonId() {
    return tvdbSeasonId;
  }

  public String getImdbId() {
    return imdbId;
  }

  public String getShowTitle() {
    return showTitle;
  }

  public int getSeasonNumber() {
    return MetadataUtil.parseInt(seasonNumber);
  }

  public int getEpisodeNumber() {
    return MetadataUtil.parseInt(episodeNumber);
  }

  public String getEpisodeTitle() {
    return episodeTitle;
  }

  public Date getFirstAired() {
    if (StringUtils.isBlank(firstAired)) {
      return null;
    }
    try {
      return new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(firstAired);
    }
    catch (ParseException e) {
      return null;
    }
  }

  public boolean isAiredFilled() {
    return firstAired != null;
  }

  public List<String> getGuestStars() {
    if (guestStars != null) {
      return ParserUtils.split(guestStars);
    }
    else {
      return new ArrayList<>();
    }
  }

  public List<String> getDirectorName() {
    if (directorName != null) {
      return ParserUtils.split(directorName);
    }
    else {
      return new ArrayList<>();
    }
  }

  public List<String> getWriterName() {
    if (writerName != null) {
      return ParserUtils.split(writerName);
    }
    else {
      return new ArrayList<>();
    }
  }

  public String getEpisodePlot() {
    return episodePlot;
  }

  public String getProductionCode() {
    return productionCode;
  }

  public String getThumbUrl() {
    return thumbUrl;
  }
}
