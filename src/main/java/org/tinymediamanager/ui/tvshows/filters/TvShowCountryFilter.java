package org.tinymediamanager.ui.tvshows.filters;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.scraper.util.StrgUtils;
import org.tinymediamanager.ui.components.TmmLabel;

import javax.swing.JLabel;
import java.util.List;
import java.util.regex.Matcher;

public class TvShowCountryFilter extends AbstractTextTvShowUIFilter {
  @Override
  protected JLabel createLabel() {
    return new TmmLabel(TmmResourceBundle.getString("metatag.country"));
  }

  @Override
  public String getId() {
    return "tvShowCountry";
  }

  @Override
  protected boolean accept(TvShow tvShow, List<TvShowEpisode> episodes, boolean invert) {

    if (StringUtils.isBlank(normalizedFilterText)) {
      return true;
    }

    try {
      // country
      if (StringUtils.isNotEmpty(tvShow.getCountry())) {
        Matcher matcher = filterPattern.matcher(StrgUtils.normalizeString(tvShow.getCountry()));
        if (invert) {
          return !matcher.find();
        }
        else {
          return matcher.find();
        }
      }
    }
    catch (Exception e) {
      // if any exceptions are thrown, just return true
      return true;
    }

    return false;
  }
}
