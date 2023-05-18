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
 * The Enum MovieDiscNaming.
 * 
 * @author Manuel Laggner
 */
public enum MovieSetDiscartNaming implements IMovieSetFileNaming {

  /** movieset-disc.* */
  MOVIE_DISC {
    @Override
    public String getFilename(String basename, String extension) {
      return "movieset-disc." + extension;
    }

    @Override
    public Location getFolderLocation() {
      return Location.MOVIE_FOLDER;
    }
  },

  /** movieset-discart.* */
  MOVIE_DISCART {
    @Override
    public String getFilename(String basename, String extension) {
      return "movieset-discart." + extension;
    }

    @Override
    public Location getFolderLocation() {
      return Location.MOVIE_FOLDER;
    }
  },

  /** disc.* */
  KODI_DISC {
    @Override
    public String getFilename(String basename, String extension) {
      return "disc." + extension;
    }

    @Override
    public Location getFolderLocation() {
      return Location.KODI_STYLE_FOLDER;
    }
  },

  /** discart.* */
  KODI_DISCART {
    @Override
    public String getFilename(String basename, String extension) {
      return "discart." + extension;
    }

    @Override
    public Location getFolderLocation() {
      return Location.KODI_STYLE_FOLDER;
    }
  },

  /** [movie set name]-disc.* */
  AUTOMATOR_DISC {
    @Override
    public String getFilename(String basename, String extension) {
      return StringUtils.isNotBlank(basename) ? basename + "-disc." + extension : "";
    }

    @Override
    public Location getFolderLocation() {
      return Location.AUTOMATOR_STYLE_FOLDER;
    }
  },

  /** [movie set name]-discart.* */
  AUTOMATOR_DISCART {
    @Override
    public String getFilename(String basename, String extension) {
      return StringUtils.isNotBlank(basename) ? basename + "-discart." + extension : "";
    }

    @Override
    public Location getFolderLocation() {
      return Location.AUTOMATOR_STYLE_FOLDER;
    }
  }
}
