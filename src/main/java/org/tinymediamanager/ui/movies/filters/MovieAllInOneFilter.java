package org.tinymediamanager.ui.movies.filters;

import javax.swing.JLabel;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.ui.components.TmmLabel;

public class MovieAllInOneFilter extends AbstractTextMovieUIFilter {

  @Override
  protected JLabel createLabel() {
    return new TmmLabel(TmmResourceBundle.getString("filter.universal"));
  }

  @Override
  public String getId() {
    return "movieAllInOne";
  }

  @Override
  public boolean accept(Movie movie) {

    // MovieCastFilter
    MovieCastFilter movieCastFilter = new MovieCastFilter();
    setFields(movieCastFilter);

    if (movieCastFilter.accept(movie)) {
      return true;
    }

    // MovieCountryFilter
    MovieCountryFilter movieCountryFilter = new MovieCountryFilter();
    setFields(movieCountryFilter);
    if (movieCountryFilter.accept(movie)) {
      return true;
    }

    // MovieFileNameFilter
    MovieFilenameFilter movieFilenameFilter = new MovieFilenameFilter();
    setFields(movieFilenameFilter);
    if (movieFilenameFilter.accept(movie)) {
      return true;
    }

    // MovieLanguageFilter
    MovieLanguageFilter movieLanguageFilter = new MovieLanguageFilter();
    setFields(movieLanguageFilter);
    if (movieLanguageFilter.accept(movie)) {
      return true;
    }

    // MovieNoteFilter
    MovieNoteFilter movieNoteFilter = new MovieNoteFilter();
    setFields(movieNoteFilter);
    if (movieNoteFilter.accept(movie)) {
      return true;
    }

    // MovieProductionCompanyFilter
    MovieProductionCompanyFilter movieProductionCompanyFilter = new MovieProductionCompanyFilter();
    setFields(movieProductionCompanyFilter);
    if (movieProductionCompanyFilter.accept(movie)) {
      return true;
    }

    return false;
  }

  private void setFields(AbstractTextMovieUIFilter filter) {
    filter.textField = this.textField;
    filter.filterPattern = this.filterPattern;
    filter.normalizedFilterText = this.normalizedFilterText;
  }
}
