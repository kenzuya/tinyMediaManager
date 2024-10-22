/*
 * Copyright 2012 - 2024 Manuel Laggner
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

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.IFileNaming;

/**
 * The Enum {@link MovieTrailerNaming} is used to provide movie related trailer filenames
 *
 * @author Manuel Laggner
 */
public enum MovieTrailerNaming implements IFileNaming {
  /**
   * filename-trailer.*
   */
  FILENAME_TRAILER {
    @Override
    public String getFilename(String basename, String extension) {
      return StringUtils.isNotBlank(basename) ? basename + "-trailer." + extension : "";
    }
  },

  /**
   * movie-trailer.*
   */
  MOVIE_TRAILER {
    @Override
    public String getFilename(String basename, String extension) {
      return "movie-trailer." + extension;
    }
  },

  /**
   * trailers/filename-trailer.*
   */
  TRAILERS_FILENAME_TRAILER {
    @Override
    public String getFilename(String basename, String extension) {
      return StringUtils.isNotBlank(basename) ? "trailers" + File.separator + basename + "-trailer." + extension : "";
    }
  },

  /**
   * trailer.*
   */
  TRAILER {
    @Override
    public String getFilename(String basename, String extension) {
      return "trailer." + extension;
    }
  }

}
