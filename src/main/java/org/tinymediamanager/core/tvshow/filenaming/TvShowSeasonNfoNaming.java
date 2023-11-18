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

package org.tinymediamanager.core.tvshow.filenaming;

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.tvshow.ITvShowSeasonFileNaming;
import org.tinymediamanager.core.tvshow.TvShowHelpers;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;

/**
 * The enum {@link TvShowSeasonNfoNaming} is used to indicate possible season NFO file namings.
 * 
 * @author Manuel Laggner
 */
public enum TvShowSeasonNfoNaming implements ITvShowSeasonFileNaming {
  /** seasonXX.nfo */
  SEASON {
    @Override
    public String getFilename(TvShowSeason tvShowSeason, String extension) {
      String filename;
      if (tvShowSeason.getSeason() == -1) {
        filename = "";
      }
      else if (tvShowSeason.getSeason() == 0 && TvShowModuleManager.getInstance().getSettings().isSpecialSeason()) {
        filename = "season-specials." + extension;
      }
      else if (tvShowSeason.getSeason() > -1) {
        filename = String.format("season%02d.%s", tvShowSeason.getSeason(), extension);
      }
      else {
        filename = "";
      }

      return filename;
    }
  },

  /** season_folder/season.nfo */
  SEASON_FOLDER {
    @Override
    public String getFilename(TvShowSeason tvShowSeason, String extension) {
      TvShow tvShow = tvShowSeason.getTvShow();
      if (tvShow == null) {
        return "";
      }

      String seasonFoldername = TvShowHelpers.detectSeasonFolder(tvShow, tvShowSeason.getSeason());

      // check whether the season folder name exists or not; do not create it just for the artwork!
      if (StringUtils.isBlank(seasonFoldername)) {
        // no season folder name in the templates found - fall back to the show base filename style
        return SEASON.getFilename(tvShowSeason, extension);
      }

      return seasonFoldername + File.separator + "season." + extension;
    }
  }
}
