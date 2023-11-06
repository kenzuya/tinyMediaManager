package org.tinymediamanager.ui.movies.filters;

import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.ui.components.TmmLabel;

import javax.swing.*;

public class MovieBannerSizeFilter extends AbstractNumberMovieFilter {

  public MovieBannerSizeFilter() {
    super();

    // display the size with px at the end
    spinnerLow.setEditor(prepareNumberEditor(spinnerLow, "####0 px"));
    spinnerHigh.setEditor(prepareNumberEditor(spinnerHigh, "####0 px"));

  }

  @Override
  protected JLabel createLabel() {
    return new TmmLabel(TmmResourceBundle.getString("filter.banner.width"));
  }

  @Override
  public String getId() {
    return "movieBannerSize";
  }

  @Override
  public boolean accept(Movie movie) {
    return matchInt(movie.getArtworkDimension(MediaFileType.BANNER).width);
  }

  @Override
  protected SpinnerNumberModel getNumberModel() {
    return new SpinnerNumberModel(0, 0, 99999, 1);
  }
}
