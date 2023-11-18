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

    Path oldShowFolder = tvShow.getPathNIO();
    if (!oldShowFolder.equals(container.newPath)) {
      container.needsRename = true;
      // update already the "old" files with new path, so we can simply do a contains check ;)
      for (MediaFile omf : oldFiles.values()) {
        omf.replacePathForRenamedFolder(oldShowFolder, container.newPath);
      }

      // do the same for (some) new files too, since EXTRAS in dedicated folder can not determine their new folder
      newFiles.forEach(mf -> mf.replacePathForRenamedFolder(oldShowFolder, container.newPath));
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
      oldFiles.put(oldMf.getFileAsPath().toString(), oldMf);

      TvShow clone = new TvShow();
      clone.merge(tvShow);
      clone.setDataSource(tvShow.getDataSource());
      clone.setPath(container.newPath.toString());
      newFiles.addAll(TvShowRenamer.generateFilename(clone, new MediaFile(mf)));
    }
  }

  private void processEpisodes() {
    List<TvShowEpisode> episodes = new ArrayList<>(tvShow.getEpisodes());
    Collections.sort(episodes);

    for (TvShowEpisode episode : episodes) {
      MediaFile mainVideoFile = episode.getMainVideoFile();

      // BASENAME
      String oldVideoBasename = episode.getVideoBasenameWithoutStacking();

      // test for valid season/episode number
      if (episode.getSeason() < 0 || episode.getEpisode() < 0) {
        // nothing to rename if S/E < 0
        for (MediaFile mf : episode.getMediaFiles()) {
          MediaFile oldMf = new MediaFile(mf);
          oldFiles.put(oldMf.getFileAsPath().toString(), oldMf);

          MediaFile newMf = new MediaFile(mf);
          newFiles.add(newMf);
        }

      }
      else if (episode.isDisc()) {
        String seasonFoldername = getSeasonFoldername(episode.getTvShow(), episode);
        Path seasonFolder = container.newPath;
        if (StringUtils.isNotBlank(seasonFoldername)) {
          seasonFolder = container.newPath.resolve(seasonFoldername);
        }

        String newFoldername = FilenameUtils.getBaseName(generateFoldername(episode.getTvShow(), mainVideoFile)); // w/o extension
        Path newEpFolder = seasonFolder.resolve(newFoldername);

        for (MediaFile mf : episode.getMediaFiles()) {
          MediaFile oldMf = new MediaFile(mf);
          oldFiles.put(oldMf.getFileAsPath().toString(), oldMf);

          MediaFile newMf = new MediaFile(mf);
          String newMfFolder = newMf.getPath().replace(mainVideoFile.getPath(), newEpFolder.toString());
          newMf.replacePathForRenamedFolder(mf.getFileAsPath().getParent(), Paths.get(newMfFolder));
          newFiles.add(newMf);
        }
      }
      else {
        for (MediaFile mf : episode.getMediaFiles()) {
          MediaFile oldMf = new MediaFile(mf);
          oldFiles.put(oldMf.getFileAsPath().toString(), oldMf);

          TvShow clone = new TvShow();
          clone.merge(tvShow);
          clone.setDataSource(tvShow.getDataSource());
          clone.setPath(container.newPath.toString());
          newFiles.addAll(TvShowRenamer.generateEpisodeFilenames(clone, new MediaFile(mf), oldVideoBasename));
        }
      }
    }
  }
}
