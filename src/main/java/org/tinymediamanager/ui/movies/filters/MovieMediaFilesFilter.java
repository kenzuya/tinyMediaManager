package org.tinymediamanager.ui.movies.filters;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.JLabel;

import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.ui.components.TmmLabel;

public class MovieMediaFilesFilter extends AbstractCheckComboBoxMovieUIFilter<MovieMediaFilesFilter.MediaFileTypeContainer> {

  public MovieMediaFilesFilter() {
    super();

    List<MediaFileTypeContainer> mediaFileTypeList = new ArrayList<>();

    for (MediaFileType type : MediaFileType.values()) {
      switch (type) {
        // explicit exclude
        case VIDEO_EXTRA:
        case GRAPHIC:
        case DOUBLE_EXT:
          break;

        default:
          mediaFileTypeList.add(new MediaFileTypeContainer(type));
          break;

      }
    }
    setValues(mediaFileTypeList);
  }

  @Override
  protected String parseTypeToString(MediaFileTypeContainer container) throws Exception {
    return container.type.name();
  }

  @Override
  protected MediaFileTypeContainer parseStringToType(String string) throws Exception {
    return new MediaFileTypeContainer(MediaFileType.valueOf(string));
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

    List<MediaFileType> selectedItems = new ArrayList<>();
    for (MediaFileTypeContainer container : checkComboBox.getSelectedItems()) {
      selectedItems.add(container.type);
    }

    for (MediaFile mf : movie.getMediaFiles()) {
      if (selectedItems.contains(mf.getType())) {
        isValid = true;
        break;
      }
    }

    return isValid;
  }

  public static class MediaFileTypeContainer {
    private final MediaFileType type;

    public MediaFileTypeContainer(MediaFileType type) {
      this.type = type;
    }

    @Override
    public String toString() {
      try {
        return BUNDLE.getString("mediafiletype." + type.name().toLowerCase(Locale.ROOT));
      }
      catch (Exception e) {
        return type.toString();
      }
    }
  }
}
