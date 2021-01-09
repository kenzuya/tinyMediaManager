package org.tinymediamanager.scraper.theshowdb.entities;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Episodes {

    @SerializedName("episodes")
    List<Episode> episodes;

    public List<Episode> getEpisodes() {
        return episodes;
    }
}
