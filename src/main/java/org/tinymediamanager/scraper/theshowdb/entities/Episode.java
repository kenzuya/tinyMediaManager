package org.tinymediamanager.scraper.theshowdb.entities;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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

  public String getAbsoluteNumber() {
    return absoluteNumber;
  }

  public String getDvdSeasonNumber() {
    return dvdSeasonNumber;
  }

  public String getDvdEpisodeNumber() {
    return dvdEpisodeNumber;
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
    try {
      return Integer.parseInt(seasonNumber);
    }
    catch (NumberFormatException ignored) {
      return -1;
    }
  }

  public int getEpisodeNumber() {
    try {
      return Integer.parseInt(episodeNumber);
    }
    catch (NumberFormatException ignored) {
      return -1;
    }
  }

  public String getEpisodeTitle() {
    return episodeTitle;
  }

  public Date getFirstAired() throws ParseException {
    return new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(firstAired);
  }

  public boolean isAiredFilled() {
    return firstAired != null;
  }

  public String getGuestStars() {
    return guestStars;
  }

  public String getDirectorName() {
    return directorName;
  }

  public String getWriterName() {
    return writerName;
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