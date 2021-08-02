/*
 * Copyright 2012 - 2021 Manuel Laggner
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
package org.tinymediamanager.core.tvshow;

import static org.tinymediamanager.core.tvshow.TvShowRenamer.generateFoldername;
import static org.tinymediamanager.core.tvshow.TvShowRenamer.getSeasonFoldername;
import static org.tinymediamanager.core.tvshow.TvShowRenamer.getTvShowFoldername;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;

/**
 * the class {@link TvShowRenamerPreview} is used to create a renamer preview for TV shows
 * 
 * @author Manuel Laggner
 */
public class TvShowRenamerPreview {

  private final TvShow                        tvShow;
  private final TvShowRenamerPreviewContainer container;
  private final Map<String, MediaFile>        oldFiles;
  private final Set<MediaFile>                newFiles;

  public TvShowRenamerPreview(TvShow tvShow) {
    this.tvShow = tvShow;
    this.container = new TvShowRenamerPreviewContainer(tvShow);
    this.oldFiles = new LinkedHashMap<>();
    this.newFiles = new LinkedHashSet<>();
  }

  public TvShowRenamerPreviewContainer generatePreview() {
    // generate the new path
    container.newPath = Paths.get(getTvShowFoldername(TvShowModuleManager.getInstance().getSettings().getRenamerTvShowFoldername(), tvShow));

    // process TV show media files
    processTvShow();

    // generate all episode filenames
    processEpisodes();

    if (!container.newPath.equals(container.oldPath)) {
      container.needsRename = true;
      // update all files with new path, so we can simply do a contains check ;)
      for (MediaFile omf : newFiles) {
        omf.replacePathForRenamedFolder(container.oldPath, container.newPath);
      }
    }

    // change status of MFs, if they have been added or not
    for (MediaFile mf : newFiles) {
      if (!oldFiles.containsKey(mf.getFileAsPath().toString())) {
        container.needsRename = true;
        break;
      }
    }

    for (MediaFile mf : oldFiles.values()) {
      if (!newFiles.contains(mf)) {
        container.needsRename = true;
        break;
      }
    }

    container.oldMediaFiles.addAll(oldFiles.values());
    container.newMediaFiles.addAll(newFiles);

    return container;
  }

  private void processTvShow() {
    for (MediaFile mf : tvShow.getMediaFiles()) {
      MediaFile oldMf = new MediaFile(mf);
      oldMf.replacePathForRenamedFolder(container.oldPath, container.newPath); // already replace the path for an easy .contains() check
      oldFiles.put(oldMf.getFileAsPath().toString(), oldMf);
      newFiles.addAll(TvShowRenamer.generateFilename(tvShow, mf));
    }
  }

  private void processEpisodes() {
    List<TvShowEpisode> episodes = new ArrayList<>(tvShow.getEpisodes());
    Collections.sort(episodes);

    for (TvShowEpisode episode : episodes) {
      MediaFile mainVideoFile = episode.getMainVideoFile();

      if (episode.isDisc()) {
        String seasonFoldername = getSeasonFoldername(episode.getTvShow(), episode);
        Path seasonFolder = episode.getTvShow().getPathNIO();
        if (StringUtils.isNotBlank(seasonFoldername)) {
          seasonFolder = episode.getTvShow().getPathNIO().resolve(seasonFoldername);
        }

        String newFoldername = FilenameUtils.getBaseName(generateFoldername(episode.getTvShow(), mainVideoFile)); // w/o extension
        Path newEpFolder = seasonFolder.resolve(newFoldername);

        for (MediaFile mf : episode.getMediaFiles()) {
          MediaFile oldMf = new MediaFile(mf);
          oldMf.replacePathForRenamedFolder(container.oldPath, container.newPath); // already replace the path for an easy .contains() check
          oldFiles.put(oldMf.getFileAsPath().toString(), oldMf);

          MediaFile newMf = new MediaFile(mf);
          String newMfFolder = newMf.getPath().replace(mainVideoFile.getPath(), newEpFolder.toString());
          newMf.replacePathForRenamedFolder(mf.getFileAsPath().getParent(), Paths.get(newMfFolder));
          newFiles.add(newMf);
        }
      }
      else {
        // video (is the new base)
        MediaFile newVideoFile = TvShowRenamer.generateEpisodeFilenames(tvShow, mainVideoFile, mainVideoFile).get(0);

        for (MediaFile mf : episode.getMediaFiles()) {
          MediaFile oldMf = new MediaFile(mf);
          oldMf.replacePathForRenamedFolder(container.oldPath, container.newPath); // already replace the path for an easy .contains() check
          oldFiles.put(oldMf.getFileAsPath().toString(), oldMf);

          newFiles.addAll(TvShowRenamer.generateEpisodeFilenames(tvShow, mf, newVideoFile));
        }
      }
    }
  }
}
