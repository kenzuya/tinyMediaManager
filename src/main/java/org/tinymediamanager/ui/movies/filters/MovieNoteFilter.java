package org.tinymediamanager.ui.movies.filters;

import java.util.regex.Matcher;

import javax.swing.JLabel;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.scraper.util.StrgUtils;
import org.tinymediamanager.ui.components.TmmLabel;

public class MovieNoteFilter extends AbstractTextMovieUIFilter {
  @Override
  protected JLabel createLabel() {
    return new TmmLabel(TmmResourceBundle.getString("metatag.note"));
  }

  @Override
  public String getId() {
    return "movieNote";
  }

  @Override
  public boolean accept(Movie movie) {

    if (StringUtils.isBlank(normalizedFilterText)) {
      return true;
    }

    try {
      if (StringUtils.isNotEmpty(movie.getNote())) {
        Matcher matcher = filterPattern.matcher(StrgUtils.normalizeString(movie.getNote()));
        return matcher.find();
      }
    }
    catch (Exception e) {
      return true;
    }

    return false;
  }
}
