package org.tinymediamanager.scraper.theshowdb.entities;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.annotations.SerializedName;

public class Show {

  @SerializedName("idShow")
  private String id;

  @SerializedName("idShowTVDB")
  private String tvdbId;

  @SerializedName("strShow")
  private String title;

  @SerializedName("idShowTVcom")
  private String tvComId;

  @SerializedName("strStatus")
  private String showStatus;

  @SerializedName("strFirstAired")
  private String aired;

  @SerializedName("strNetwork")
  private String tvNetwork;

  @SerializedName("idNetwork")
  private String networkId;

  @SerializedName("strRuntime")
  private String runtime;

  @SerializedName("strGenre")
  private String genre;

  @SerializedName("strDescriptionEN")
  private String plot;

  @SerializedName("strPoster")
  private String posterUrl;

  @SerializedName("strBanner")
  private String bannerUrl;

  @SerializedName("strWideThumb")
  private String thumbUrl;

  @SerializedName("strLogo")
  private String logoUrl;

  @SerializedName("strClearart")
  private String clearartUrl;

  @SerializedName("strFanart1")
  private String fanartUrl1;

  @SerializedName("strFanart2")
  private String fanartUrl2;

  @SerializedName("strFanart3")
  private String fanartUrl3;

  @SerializedName("strFanart4")
  private String fanartUrl4;

  @SerializedName("strCharacter1")
  private String characterartUrl1;

  @SerializedName("strCharacter2")
  private String characterartUrl2;

  @SerializedName("strCharacter3")
  private String characterartUrl3;

  @SerializedName("strCharacter4")
  private String characterartUrl4;

  @SerializedName("strCharacter5")
  private String characterartUrl5;

  @SerializedName("Rating")
  private String certification;

  @SerializedName("idShowIMDB")
  private String imdbId;

  @SerializedName("idShowTMDB")
  private String tmdbId;

  @SerializedName("idShowTrakt")
  private String traktId;

  @SerializedName("idZap2it")
  private String zap2itId;

  public String getId() {
    return id;
  }

  public String getTvdbId() {
    return tvdbId;
  }

  public String getTitle() {
    return title;
  }

  public String getTvComId() {
    return tvComId;
  }

  public String getShowStatus() {
    return showStatus;
  }

  public Date getAired() {
    if (StringUtils.isBlank(aired)) {
      return null;
    }
    try {
      return new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(aired);
    }
    catch (ParseException e) {
      return null;
    }
  }

  public int getAiredYear() {
    Date airedDate = getAired();
    if (airedDate != null) {
      Calendar calendar = new GregorianCalendar();
      calendar.setTime(airedDate);
      return calendar.get(Calendar.YEAR);
    }
    return 0;
  }

  public String getTvNetwork() {
    return tvNetwork;
  }

  public String getNetworkId() {
    return networkId;
  }

  public int getRuntime() {
    try {
      return Integer.parseInt(runtime);
    }
    catch (NumberFormatException ignored) {
      return 0;
    }
  }

  public String getGenre() {
    return genre;
  }

  public String getPlot() {
    return plot;
  }

  public String getPosterUrl() {
    return posterUrl;
  }

  public String getBannerUrl() {
    return bannerUrl;
  }

  public String getThumbUrl() {
    return thumbUrl;
  }

  public String getLogoUrl() {
    return logoUrl;
  }

  public String getClearartUrl() {
    return clearartUrl;
  }

  public String getFanartUrl1() {
    return fanartUrl1;
  }

  public String getFanartUrl2() {
    return fanartUrl2;
  }

  public String getFanartUrl3() {
    return fanartUrl3;
  }

  public String getFanartUrl4() {
    return fanartUrl4;
  }

  public String getCharacterartUrl1() {
    return characterartUrl1;
  }

  public String getCharacterartUrl2() {
    return characterartUrl2;
  }

  public String getCharacterartUrl3() {
    return characterartUrl3;
  }

  public String getCharacterartUrl4() {
    return characterartUrl4;
  }

  public String getCharacterartUrl5() {
    return characterartUrl5;
  }

  public String getCertification() {
    return certification;
  }

  public String getImdbId() {
    return imdbId;
  }

  public String getTmdbId() {
    return tmdbId;
  }

  public String getTraktId() {
    return traktId;
  }

  public String getZap2itId() {
    return zap2itId;
  }
}
