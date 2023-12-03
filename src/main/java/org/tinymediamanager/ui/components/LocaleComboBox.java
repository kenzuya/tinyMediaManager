package org.tinymediamanager.ui.components;

import java.util.Locale;

public class LocaleComboBox implements Comparable<LocaleComboBox> {
  private final Locale loc;

  public LocaleComboBox(Locale loc) {
    this.loc = loc;
  }

  public Locale getLocale() {
    return loc;
  }

  @Override
  public String toString() {
    return loc.getDisplayName() + " (" + loc.toLanguageTag() + ")";
  }

  @Override
  public int compareTo(LocaleComboBox o) {
    return toString().toLowerCase(Locale.ROOT).compareTo(o.toString().toLowerCase(Locale.ROOT));
  }
}
