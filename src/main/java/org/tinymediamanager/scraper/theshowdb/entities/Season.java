package org.tinymediamanager.scraper.theshowdb.entities;

import com.google.gson.annotations.SerializedName;

public class Season {

    @SerializedName("idSeason")
    private String seasonId;

    @SerializedName("idShow")
    private String showId;

    @SerializedName("strShow")
    private String showTitle;

    @SerializedName("idSeasonTVDB")
    private String tvdbSeasonId;

    @SerializedName("idShowTVDB")
    private String tvdbShowId;

    @SerializedName("intSeason")
    private int seasonNumber;

    @SerializedName("strDescriptionEN")
    private String seasonPlot;

    @SerializedName("strPoster")
    private String seasonPosterUrl;

    @SerializedName("strWideThumb")
    private String seasonThumbUrl;

    @SerializedName("strBanner")
    private String seasonBannerUrl;

    public String getSeasonId() {
        return seasonId;
    }

    public String getShowId() {
        return showId;
    }

    public String getShowTitle() {
        return showTitle;
    }

    public String getTvdbSeasonId() {
        return tvdbSeasonId;
    }

    public String getTvdbShowId() {
        return tvdbShowId;
    }

    public int getSeasonNumber() {
        return seasonNumber;
    }

    public String getSeasonPlot() {
        return seasonPlot;
    }

    public String getSeasonPosterUrl() {
        return seasonPosterUrl;
    }

    public String getSeasonThumbUrl() {
        return seasonThumbUrl;
    }

    public String getSeasonBannerUrl() {
        return seasonBannerUrl;
    }
}
