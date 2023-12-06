package org.tinymediamanager.ui.movies.filters;

import javax.swing.JLabel;
import javax.swing.SpinnerNumberModel;

import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.ui.components.TmmLabel;

public class MovieThumbSizeFilter extends AbstractNumberMovieFilter {

  public MovieThumbSizeFilter() {
    super();

    // display the size with px at the end
    spinnerLow.setEditor(prepareNumberEditor(spinnerLow, "####0 px"));
    spinnerHigh.setEditor(prepareNumberEditor(spinnerHigh, "####0 px"));

  }

  @Override
  protected JLabel createLabel() {
    return new TmmLabel(TmmResourceBundle.getString("filter.thumb.width"));
  }

  @Override
  public String getId() {
    return "movieThumbSize";
  }

  @Override
  public boolean accept(Movie movie) {
    return matchInt(movie.getArtworkDimension(MediaFileType.THUMB).width);
  }

  @Override
  protected SpinnerNumberModel getNumberModel() {
    return new SpinnerNumberModel(0, 0, 99999, 1);
  }
}
