package org.tinymediamanager.ui.movies.filters;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;

import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.ui.components.TmmLabel;

public class MovieDifferentRuntimeFilter extends AbstractMovieUIFilter {

  JSpinner spinner;

  @Override
  protected JLabel createLabel() {
    return new TmmLabel(BUNDLE.getString("metatag.runtimedifference"));
  }

  @Override
  protected JComponent createFilterComponent() {
    SpinnerModel model = new SpinnerNumberModel(1, 1, 100, 1);
    spinner = new JSpinner(model);
    return spinner;
  }

  @Override
  public String getId() {
    return "movieDifferentRuntime";
  }

  @Override
  public String getFilterValueAsString() {
    return spinner.getValue().toString();
  }

  @Override
  public void setFilterValue(Object value) {
  }

  @Override
  public boolean accept(Movie movie) {

    int scrapedRuntimeInMinutes = movie.getRuntime();
    int mediaInfoRuntimeInMinutes = movie.getRuntimeFromMediaFilesInMinutes();

    if ((scrapedRuntimeInMinutes - mediaInfoRuntimeInMinutes >= (int) spinner.getValue())
        || (mediaInfoRuntimeInMinutes - scrapedRuntimeInMinutes >= (int) spinner.getValue())) {
      return true;
    }

    return false;
  }
}
