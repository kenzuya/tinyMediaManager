package org.tinymediamanager.ui.movies.filters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.tinymediamanager.core.Constants;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.movie.MovieList;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.ui.components.TmmLabel;

public class MovieCountSubtitleFilter extends AbstractCheckComboBoxMovieUIFilter<Integer> {

  private MovieList movieList = MovieList.getInstance();

  public MovieCountSubtitleFilter() {
    super();
    checkComboBox.enableFilter((s, s2) -> String.valueOf(s).startsWith(s2.toLowerCase(Locale.ROOT)));
    buildCountSubtitleArray();
    movieList.addPropertyChangeListener(Constants.SUBTITLES_COUNT, evt -> SwingUtilities.invokeLater(this::buildCountSubtitleArray));
  }

  @Override
  protected String parseTypeToString(Integer type) throws Exception {
    return type.toString();
  }

  @Override
  protected Integer parseStringToType(String string) throws Exception {
    return Integer.parseInt(string);
  }

  @Override
  protected JLabel createLabel() {
    return new TmmLabel(BUNDLE.getString("metatag.subtitles"));
  }

  @Override
  public String getId() {
    return "movieCountSubtitle";
  }

  @Override
  public boolean accept(Movie movie) {

    List<Integer> selectedItems = checkComboBox.getSelectedItems();
    List<MediaFile> mediaFileList = movie.getMediaFiles(MediaFileType.VIDEO);

    for (MediaFile mf : mediaFileList) {
      // check for explicit empty search
      if (selectedItems.isEmpty() && mf.getSubtitles().isEmpty()) {
        return true;
      }
      if (selectedItems.contains(mf.getSubtitles().size())) {
        return true;
      }
    }

    return false;
  }

  private void buildCountSubtitleArray() {
    List<Integer> subtitles = new ArrayList<>(movieList.getSubtitlesInMovies());
    Collections.sort(subtitles);
    setValues(subtitles);
  }
}
