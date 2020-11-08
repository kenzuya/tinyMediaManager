package org.tinymediamanager.core.movie.filenaming;

import org.tinymediamanager.core.IFileNaming;

/**
 * the interface {@link IMovieSetFileNaming} is used to indicate where this artwork style is located
 *
 * @author Manuel Laggner
 */
public interface IMovieSetFileNaming extends IFileNaming {
  enum Location {
    MOVIE_FOLDER,
    KODI_STYLE_FOLDER,
    AUTOMATOR_STYLE_FOLDER
  }

  /**
   * indicate where this artwork style is located
   * 
   * @return the {@link Location} of the artwork
   */
  Location getFolderLocation();
}
