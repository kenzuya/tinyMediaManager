/*
 * Copyright 2012 - 2023 Manuel Laggner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tinymediamanager.core.movie.filenaming;

import org.apache.commons.lang3.StringUtils;

/**
 * The Enum MovieThumbNaming.
 * 
 * @author Manuel Laggner
 */
public enum MovieSetThumbNaming implements IMovieSetFileNaming {

  /** movieset-thumb.* */
  MOVIE_THUMB {
    @Override
    public String getFilename(String basename, String extension) {
      return "movieset-thumb." + extension;
    }

    @Override
    public Location getFolderLocation() {
      return Location.MOVIE_FOLDER;
    }
  },

  /** movieset-landscape.* */
  MOVIE_LANDSCAPE {
    @Override
    public String getFilename(String basename, String extension) {
      return "movieset-landscape." + extension;
    }

    @Override
    public Location getFolderLocation() {
      return Location.MOVIE_FOLDER;
    }
  },

  /** thumb.* */
  KODI_THUMB {
    @Override
    public String getFilename(String basename, String extension) {
      return "thumb." + extension;
    }

    @Override
    public Location getFolderLocation() {
      return Location.KODI_STYLE_FOLDER;
    }
  },

  /** landscape.* */
  KODI_LANDSCAPE {
    @Override
    public String getFilename(String basename, String extension) {
      return "landscape." + extension;
    }

    @Override
    public Location getFolderLocation() {
      return Location.KODI_STYLE_FOLDER;
    }
  },

  /** [movie set name]-thumb.* */
  AUTOMATOR_THUMB {
    @Override
    public String getFilename(String basename, String extension) {
      return StringUtils.isNotBlank(basename) ? basename + "-thumb." + extension : "";
    }

    @Override
    public Location getFolderLocation() {
      return Location.AUTOMATOR_STYLE_FOLDER;
    }
  },

  /** [movie set name]-landscape.* */
  AUTOMATOR_LANDSCAPE {
    @Override
    public String getFilename(String basename, String extension) {
      return StringUtils.isNotBlank(basename) ? basename + "-landscape." + extension : "";
    }

    @Override
    public Location getFolderLocation() {
      return Location.AUTOMATOR_STYLE_FOLDER;
    }
  }
}
