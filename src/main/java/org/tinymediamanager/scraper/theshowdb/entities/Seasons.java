package org.tinymediamanager.scraper.theshowdb.entities;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Seasons {

    @SerializedName("seasons")
    private List<Season> seasons;

    public List<Season> getSeasons() {
        return seasons;
    }

}
