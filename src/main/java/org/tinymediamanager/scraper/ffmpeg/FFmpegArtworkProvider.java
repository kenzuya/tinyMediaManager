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
package org.tinymediamanager.scraper.ffmpeg;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.addon.FFmpegAddon;
import org.tinymediamanager.core.MediaFileHelper;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.mediainfo.MediaInfoFile;
import org.tinymediamanager.scraper.ArtworkSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.IMediaProvider;
import org.tinymediamanager.thirdparty.FFmpeg;

/**
 * the class {@link FFmpegArtworkProvider} is used to provide FFmpeg stills as an artwork provider
 *
 * @author Manuel Laggner
 */
abstract class FFmpegArtworkProvider implements IMediaProvider {
  static final String             ID = "ffmpeg";

  private final MediaProviderInfo providerInfo;

  FFmpegArtworkProvider() {
    providerInfo = createMediaProviderInfo();
  }

  /**
   * get the sub id of this scraper (for dedicated storage)
   *
   * @return the sub id
   */
  protected abstract String getSubId();

  protected MediaProviderInfo createMediaProviderInfo() {
    return new MediaProviderInfo(ID, getSubId(), "ffmpeg",
        "<html><h3>FFmpeg</h3><br />The FFmpeg artwork provider is a meta provider which uses the local FFmpeg installation to extract several stills from your video files</html>",
        FFmpegArtworkProvider.class.getResource("/org/tinymediamanager/scraper/ffmpeg.svg"));
  }

  @Override
  public String getId() {
    return providerInfo.getId();
  }

  public MediaProviderInfo getProviderInfo() {
    return providerInfo;
  }

  public boolean isActive() {
    return isFeatureEnabled() && (StringUtils.isNotBlank(Settings.getInstance().getMediaFramework()) || new FFmpegAddon().isAvailable());
  }

  public List<MediaArtwork> getArtwork(ArtworkSearchAndScrapeOptions options) throws ScrapeException {

    // FFmpeg must be specified in the settings
    if (!FFmpeg.isAvailable()) {
      throw new MissingIdException("FFmpeg");
    }

    // the file must be available
    Object mf = options.getIds().get("mediaFile");
    if (!(mf instanceof MediaFile)) {
      throw new ScrapeException(new FileNotFoundException());
    }

    MediaFile mediaFile = (MediaFile) mf;
    if (mediaFile.isISO() || mediaFile.getDuration() == 0) {
      return Collections.emptyList();
    }
    else if (mediaFile.isDiscFile()) {
      // need to get the thumbs from the various disc files
      return createStillsFromDiscFiles(mediaFile, options);
    }
    else {
      // plain file - just need to get the stills from there
      return createStillsFromPlainFile(mediaFile, options);
    }
  }

  private List<MediaArtwork> createStillsFromPlainFile(MediaFile mediaFile, ArtworkSearchAndScrapeOptions options) {
    // take the runtime
    int duration = mediaFile.getDuration();

    // create up to {count} stills between {start}% and {end}% of the runtime
    int count = providerInfo.getConfig().getValueAsInteger("count");
    int start = providerInfo.getConfig().getValueAsInteger("start");
    int end = providerInfo.getConfig().getValueAsInteger("end");

    // add some mitigations for wrong values (since the UI cannot handle this better)
    if (start <= 0) {
      start = 1;
    }
    else if (start >= 100) {
      start = 99;
    }

    if (end <= 0) {
      end = 1;
    }
    else if (end >= 100) {
      end = 99;
    }

    if (start > end) {
      int temp = start;
      start = end;
      end = temp;
    }

    if (count <= 0) {
      count = 1;
    }

    float increment = (end - start) / (100f * count);

    List<MediaArtwork> artworks = new ArrayList<>();

    Random random = new Random(System.nanoTime());

    for (int i = 0; i < count; i++) {
      int second = (int) (duration * (start / 100f + i * increment));

      // add some variance to produce different stills every time (-0.5 * increment ... +0.5 * increment)
      int variance = (int) (duration * increment * (-0.5 + random.nextDouble())); // NOSONAR
      if (second + variance <= 0 || second + variance > duration) {
        variance = 0;
      }

      try {
        Path tempFile = Paths.get(Utils.getTempFolder(), "ffmpeg-still." + System.currentTimeMillis() + ".jpg");
        FFmpeg.createStill(mediaFile.getFile(), tempFile, second + variance);

        // set the artwork type depending on the configured type
        int width = mediaFile.getVideoWidth();
        int height = mediaFile.getVideoHeight();
        int artworkSizeOrder = detectArtworkSizeOrder(width);

        if (isFanartEnabled() && (options.getArtworkType() == MediaArtwork.MediaArtworkType.ALL
                || options.getArtworkType() == MediaArtwork.MediaArtworkType.BACKGROUND)) {
          MediaArtwork still = new MediaArtwork(getId(), MediaArtwork.MediaArtworkType.BACKGROUND);
          still.addImageSize(width, height, "file:/" + tempFile.toAbsolutePath());
          still.setDefaultUrl("file:/" + tempFile.toAbsolutePath());
          still.setOriginalUrl("file:/" + tempFile.toAbsolutePath());
          still.setSizeOrder(artworkSizeOrder);
          still.setLanguage("-");
          artworks.add(still);
        }
        if (isThumbEnabled()
                && (options.getArtworkType() == MediaArtwork.MediaArtworkType.ALL || options.getArtworkType() == MediaArtwork.MediaArtworkType.THUMB)) {
          MediaArtwork still = new MediaArtwork(getId(), MediaArtwork.MediaArtworkType.THUMB);
          still.addImageSize(width, height, "file:/" + tempFile.toAbsolutePath());
          still.setDefaultUrl("file:/" + tempFile.toAbsolutePath());
          still.setOriginalUrl("file:/" + tempFile.toAbsolutePath());
          still.setSizeOrder(artworkSizeOrder);
          still.setLanguage("-");
          artworks.add(still);
        }

      }
      catch (Exception e) {
        // has already been logged in FFmpeg
      }
    }

    return artworks;
  }

  private List<MediaArtwork> createStillsFromDiscFiles(MediaFile mediaFile, ArtworkSearchAndScrapeOptions options) throws ScrapeException {
    // take the runtime
    int duration = mediaFile.getDuration();

    // create up to {count} stills between {start}% and {end}% of the runtime
    int count = providerInfo.getConfig().getValueAsInteger("count");
    int start = providerInfo.getConfig().getValueAsInteger("start");
    int end = providerInfo.getConfig().getValueAsInteger("end");

    if (count <= 0 || start <= 0 || end >= 100 || start > end) {
      throw new ScrapeException(new IllegalArgumentException());
    }

    float increment = (end - start) / (100f * count);

    // get the amount of disc files and split the amount of stills over every disc file
    List<MediaInfoFile> files = MediaFileHelper.detectRelevantFiles(mediaFile);
    for (int i = files.size() - 1; i >= 0; i--) {
      String ext = files.get(i).getFileExtension();
      // rule out non disc video files
      if (!ext.equalsIgnoreCase("vob") && !ext.equalsIgnoreCase("m2ts") && !ext.equalsIgnoreCase("evo")) {
        files.remove(i);
      }
    }

    if (files.isEmpty()) {
      return Collections.emptyList();
    }

    int countPerFile = (int) Math.ceil(count / (double) files.size());
    int fileDuration = duration / files.size();
    List<MediaArtwork> artworks = new ArrayList<>();

    for (int fileIndex = 0; fileIndex < files.size(); fileIndex++) {
      Path path = Paths.get(files.get(fileIndex).getPath(), files.get(fileIndex).getFilename());

      for (int i = 0; i < countPerFile; i++) {
        // the second needs to be split across _all_ files
        int second = (int) (fileDuration * (start / 100f + i * increment));

        try {
          Path tempFile = Paths.get(Utils.getTempFolder(), "ffmpeg-still." + System.currentTimeMillis() + ".jpg");
          FFmpeg.createStill(path, tempFile, second);

          // set the artwork type depending on the configured type
          int width = mediaFile.getVideoWidth();
          int height = mediaFile.getVideoHeight();
          int artworkSizeOrder = detectArtworkSizeOrder(width);

          if (isFanartEnabled() && (options.getArtworkType() == MediaArtwork.MediaArtworkType.ALL
                  || options.getArtworkType() == MediaArtwork.MediaArtworkType.BACKGROUND)) {
            MediaArtwork still = new MediaArtwork(getId(), MediaArtwork.MediaArtworkType.BACKGROUND);
            still.addImageSize(width, height, "file:/" + tempFile.toAbsolutePath());
            still.setDefaultUrl("file:/" + tempFile.toAbsolutePath());
            still.setOriginalUrl("file:/" + tempFile.toAbsolutePath());
            still.setSizeOrder(artworkSizeOrder);
            still.setLanguage("-");
            artworks.add(still);
          }
          if (isThumbEnabled()
                  && (options.getArtworkType() == MediaArtwork.MediaArtworkType.ALL || options.getArtworkType() == MediaArtwork.MediaArtworkType.THUMB)) {
            MediaArtwork still = new MediaArtwork(getId(), MediaArtwork.MediaArtworkType.THUMB);
            still.addImageSize(width, height, "file:/" + tempFile.toAbsolutePath());
            still.setDefaultUrl("file:/" + tempFile.toAbsolutePath());
            still.setOriginalUrl("file:/" + tempFile.toAbsolutePath());
            still.setSizeOrder(artworkSizeOrder);
            still.setLanguage("-");
            artworks.add(still);
          }
        }
        catch (Exception e) {
          // has already been logged in FFmpeg
        }
      }
    }
    return artworks;
  }

  private int detectArtworkSizeOrder(int width) {
    if (width >= 3840) {
      return MediaArtwork.FanartSizes.XLARGE.getOrder();
    }
    if (width >= 1920) {
      return MediaArtwork.FanartSizes.LARGE.getOrder();
    }
    else if (width >= 1280) {
      return MediaArtwork.FanartSizes.MEDIUM.getOrder();
    }
    else {
      return MediaArtwork.FanartSizes.SMALL.getOrder();
    }
  }

  /**
   * checks if fanart is activated in the scraper settings
   * 
   * @return true/false
   */
  protected abstract boolean isFanartEnabled();

  /**
   * checks if thumb is activated in the scraper settings
   * 
   * @return true/false
   */
  protected abstract boolean isThumbEnabled();
}
