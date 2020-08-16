package org.tinymediamanager.ui.movies.filters;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JLabel;

import org.tinymediamanager.core.Constants;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.movie.MovieList;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.ui.components.TmmLabel;

public class MovieMediaFilesFilter extends AbstractCheckComboBoxMovieUIFilter<MediaFileType> {

  private MovieList movieList = MovieList.getInstance();

  public MovieMediaFilesFilter() {
    super();
    buildAndInstallCertificationArray();
    PropertyChangeListener propertyChangeListener = evt -> buildAndInstallCertificationArray();
    movieList.addPropertyChangeListener(Constants.MEDIA_FILES, propertyChangeListener);
  }

  @Override
  protected String parseTypeToString(MediaFileType type) throws Exception {
    return type.name();
  }

  @Override
  protected MediaFileType parseStringToType(String string) throws Exception {
    return MediaFileType.valueOf(string);
  }

  @Override
  protected JLabel createLabel() {
    return new TmmLabel(BUNDLE.getString("metatag.mediatype"));
  }

  @Override
  public String getId() {
    return "movieMediaFiles";
  }

  @Override
  public boolean accept(Movie movie) {

    boolean isValid = false;

    List<MediaFileType> selectedItems = checkComboBox.getSelectedItems();
    for (MediaFile mf : movie.getMediaFiles()) {
      if (selectedItems.contains(mf.getType())) {
        isValid = true;
        break;
      }
    }

    return isValid;
  }

  private void buildAndInstallCertificationArray() {
    List<MediaFileType> mediaFileTypeList = new ArrayList<>();
    mediaFileTypeList.addAll(Arrays.asList(MediaFileType.values()));
    setValues(mediaFileTypeList);
  }

}
