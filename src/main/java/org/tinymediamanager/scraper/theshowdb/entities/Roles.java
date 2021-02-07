package org.tinymediamanager.scraper.theshowdb.entities;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class Roles {

  @SerializedName("roles")
  private List<Role> roles;

  public List<Role> getRoles() {
    return roles;
  }
}
