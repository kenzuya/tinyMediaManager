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

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.IFileNaming;

/**
 * The Enum MoviePosterNaming.
 * 
 * @author Manuel Laggner
 */
public enum MoviePosterNaming implements IFileNaming {

  /** [filename].* */
  FILENAME {
    @Override
    public String getFilename(String basename, String extension) {
      return StringUtils.isNotBlank(basename) ? basename + "." + extension : "";
    }
  },

  /** [filename]-poster.* */
  FILENAME_POSTER {
    @Override
    public String getFilename(String basename, String extension) {
      return StringUtils.isNotBlank(basename) ? basename + "-poster." + extension : "";
    }
  },

  /** movie.* */
  MOVIE {
    @Override
    public String getFilename(String basename, String extension) {
      return "movie." + extension;
    }
  },

  /** poster.* */
  POSTER {
    @Override
    public String getFilename(String basename, String extension) {
      return "poster." + extension;
    }
  },

  /** folder.* */
  FOLDER {
    @Override
    public String getFilename(String basename, String extension) {
      return "folder." + extension;
    }
  },

  /** cover.* */
  COVER {
    @Override
    public String getFilename(String basename, String extension) {
      return "cover." + extension;
    }
  }
}
