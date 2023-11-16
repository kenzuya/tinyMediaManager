package org.tinymediamanager.ui.components;

import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.LocaleUtils;

public class LocaleComboBox implements Comparable<LocaleComboBox> {
  private final Locale       loc;
  private final List<Locale> countries;

  public LocaleComboBox(Locale loc) {
    this.loc = loc;
    countries = LocaleUtils.countriesByLanguage(loc.getLanguage().toLowerCase(Locale.ROOT));
  }

  public Locale getLocale() {
    return loc;
  }

  @Override
  public String toString() {
    String code = " (" + loc.getLanguage();
    if (!loc.getCountry().isBlank()) {
      code += "-" + loc.getCountry();
    }
    code += ")";
    return loc.getDisplayLanguage() + code;
  }

  @Override
  public int compareTo(LocaleComboBox o) {
    return toString().toLowerCase(Locale.ROOT).compareTo(o.toString().toLowerCase(Locale.ROOT));
  }
}
