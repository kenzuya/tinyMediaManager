package org.tinymediamanager.ui.movies.filters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.tinymediamanager.core.Constants;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.movie.MovieList;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.ui.components.TmmLabel;

public class MovieCountAudioStreamFilter extends AbstractCheckComboBoxMovieUIFilter<Integer> {

  private MovieList movieList = MovieList.getInstance();

  public MovieCountAudioStreamFilter() {
    super();
    checkComboBox.enableFilter((s, s2) -> String.valueOf(s).startsWith(s2.toLowerCase(Locale.ROOT)));
    buildCountAudioStreamArray();
    movieList.addPropertyChangeListener(Constants.AUDIOSTREAMS_COUNT, evt -> SwingUtilities.invokeLater(this::buildCountAudioStreamArray));
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
    return new TmmLabel(TmmResourceBundle.getString("metatag.countAudioStreams"));
  }

  @Override
  public String getId() {
    return "movieCountAudioStream";
  }

  @Override
  public boolean accept(Movie movie) {

    List<Integer> selectedItems = checkComboBox.getSelectedItems();
    List<MediaFile> mediaFileList = movie.getMediaFiles(MediaFileType.VIDEO);

    for (MediaFile mf : mediaFileList) {
      // check for explicit empty search
      if (selectedItems.isEmpty() && mf.getAudioStreams().isEmpty()) {
        return true;
      }
      if (selectedItems.contains(mf.getAudioStreams().size())) {
        return true;
      }
    }

    return false;
  }

  private void buildCountAudioStreamArray() {
    List<Integer> audiostreams = new ArrayList<>(movieList.getAudioStreamsInMovies());
    Collections.sort(audiostreams);
    setValues(audiostreams);
  }

}
