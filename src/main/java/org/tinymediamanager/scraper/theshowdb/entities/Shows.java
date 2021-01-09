package org.tinymediamanager.scraper.theshowdb.entities;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Shows {

    @SerializedName("shows")
    private List<Show> shows;

    public List<Show> getShows() {
        return shows;
    }

}
