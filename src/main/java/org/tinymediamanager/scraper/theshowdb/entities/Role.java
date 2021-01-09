package org.tinymediamanager.scraper.theshowdb.entities;

import com.google.gson.annotations.SerializedName;

public class Role {

    @SerializedName("idRole")
    private String roleId;

    @SerializedName("idActor")
    private String actorId;

    @SerializedName("idRoleTVDB")
    private String tvdbRoleId;

    @SerializedName("idIMDB")
    private String imdbId;

    @SerializedName("idShow")
    private String showId;

    @SerializedName("idShowTVDB")
    private String tvdbShowId;

    @SerializedName("strActor")
    private String actorName;

    @SerializedName("strRole")
    private String roleName;

    @SerializedName("strDescriptionEN")
    private String actorPlot;

    @SerializedName("strCutout")
    private String CutoutUrl;

    @SerializedName("strPoster")
    private String posterUrl;

    public String getRoleId() {
        return roleId;
    }

    public String getActorId() {
        return actorId;
    }

    public String getTvdbRoleId() {
        return tvdbRoleId;
    }

    public String getImdbId() {
        return imdbId;
    }

    public String getShowId() {
        return showId;
    }

    public String getTvdbShowId() {
        return tvdbShowId;
    }

    public String getActorName() {
        return actorName;
    }

    public String getRoleName() {
        return roleName;
    }

    public String getActorPlot() {
        return actorPlot;
    }

    public String getCutoutUrl() {
        return CutoutUrl;
    }

    public String getPosterUrl() {
        return posterUrl;
    }
}
