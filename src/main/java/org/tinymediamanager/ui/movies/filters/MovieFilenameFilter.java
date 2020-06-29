package org.tinymediamanager.ui.movies.filters;

import java.util.regex.Matcher;

import javax.swing.JLabel;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.scraper.util.StrgUtils;
import org.tinymediamanager.ui.components.TmmLabel;

public class MovieFilenameFilter extends AbstractTextMovieUIFilter {
  @Override
  protected JLabel createLabel() {
    return new TmmLabel(BUNDLE.getString("metatag.filename"));
  }

  @Override
  public String getId() {
    return "movieFilename";
  }

  @Override
  public boolean accept(Movie movie) {
    if (StringUtils.isBlank(normalizedFilterText)) {
      return true;
    }

    try {
      // Filename
      if (StringUtils.isNotEmpty(movie.getOriginalFilename())) {
        Matcher matcher = filterPattern.matcher(StrgUtils.normalizeString(movie.getOriginalFilename()));
        return matcher.find();
      }
    }
    catch (Exception e) {
      // if any exceptions are thrown, just return true
      return true;
    }

    return false;
  }
}
