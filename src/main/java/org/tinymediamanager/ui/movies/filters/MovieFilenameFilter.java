package org.tinymediamanager.ui.movies.filters;

import java.util.regex.Matcher;

import javax.swing.JLabel;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.entities.MediaFile;
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
      for (MediaFile mediaFile : movie.getMediaFiles(MediaFileType.VIDEO)) {
        Matcher matcher = filterPattern.matcher(StrgUtils.normalizeString(mediaFile.getFilename()));
        if (matcher.find()) {
          return true;
        }
      }

      // also have a look at the original filename
      if (StringUtils.isNotEmpty(movie.getOriginalFilename())) {
        Matcher matcher = filterPattern.matcher(StrgUtils.normalizeString(movie.getOriginalFilename()));
        if (matcher.find()) {
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
}
