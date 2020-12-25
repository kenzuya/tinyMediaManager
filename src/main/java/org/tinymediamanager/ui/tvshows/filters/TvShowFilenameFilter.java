package org.tinymediamanager.ui.tvshows.filters;

import java.util.List;
import java.util.regex.Matcher;

import javax.swing.JLabel;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.scraper.util.StrgUtils;
import org.tinymediamanager.ui.components.TmmLabel;

public class TvShowFilenameFilter extends AbstractTextTvShowUIFilter {
  @Override
  protected boolean accept(TvShow tvShow, List<TvShowEpisode> episodes, boolean invert) {

    if (StringUtils.isBlank(normalizedFilterText)) {
      return true;
    }

    try {
      // first: filter on the production companies of the Tv show
      boolean foundShow = false;
      Matcher matcher = filterPattern.matcher(StrgUtils.normalizeString(tvShow.getOriginalFilename()));
      if (matcher.find()) {
        foundShow = true;
      }

      // if we found anything in the show we can quit here
      if (!invert && foundShow) {
        return true;
      }
      else if (invert && foundShow) {
        return false;
      }

      // second: filter production company from the episodes
      for (TvShowEpisode episode : episodes) {
        boolean foundEpisode = false;

        matcher = filterPattern.matcher(StrgUtils.normalizeString(episode.getOriginalFilename()));
        if (matcher.find()) {
          foundEpisode = true;
        }

        // if there is a match in this episode, we can stop
        if (invert && !foundEpisode) {
          return true;
        }
        else if (!invert && foundEpisode) {
          return true;
        }
      }
    }
    catch (Exception e) {
      // if any exceptions are thrown, just return true
      return true;
    }

    return false;
  }

  @Override
  protected JLabel createLabel() {
    return new TmmLabel(TmmResourceBundle.getString("metatag.filename"));
  }

  @Override
  public String getId() {
    return "TvShowFilename";
  }
}
