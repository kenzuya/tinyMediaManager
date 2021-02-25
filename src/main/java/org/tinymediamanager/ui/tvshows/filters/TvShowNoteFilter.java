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

public class TvShowNoteFilter extends AbstractTextTvShowUIFilter {

  @Override
  protected JLabel createLabel() {
    return new TmmLabel(TmmResourceBundle.getString("metatag.note"));
  }

  @Override
  public String getId() {
    return "TvShowNote";
  }

  @Override
  protected boolean accept(TvShow tvShow, List<TvShowEpisode> episodes, boolean invert) {

    if (StringUtils.isBlank(normalizedFilterText)) {
      return true;
    }

    try {
      boolean foundShow = false;
      if (StringUtils.isNotBlank(tvShow.getNote())) {
        Matcher matcher = filterPattern.matcher(StrgUtils.normalizeString(tvShow.getNote()));
        if (matcher.find()) {
          foundShow = true;
        }
        if (!invert && foundShow) {
          return true;
        }
        else if (invert && foundShow) {
          return false;
        }
      }

      for (TvShowEpisode episode : episodes) {
        boolean foundEpisode = false;
        if (StringUtils.isNotBlank(episode.getNote())) {
          Matcher matcher = filterPattern.matcher(StrgUtils.normalizeString(episode.getNote()));
          if (matcher.find()) {
            foundEpisode = true;
          }

          if (invert && !foundEpisode) {
            return true;
          }
          else if (!invert && foundEpisode) {
            return true;
          }
        }
      }
    }
    catch (Exception e) {
      return true;
    }

    return false;
  }
}
