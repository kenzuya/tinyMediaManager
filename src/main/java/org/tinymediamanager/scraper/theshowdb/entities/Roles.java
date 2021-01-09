package org.tinymediamanager.scraper.theshowdb.entities;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Roles {

    @SerializedName("roles")
    private List<Role> roles;

    public List<Role> getRoles() {
        return roles;
    }
}
