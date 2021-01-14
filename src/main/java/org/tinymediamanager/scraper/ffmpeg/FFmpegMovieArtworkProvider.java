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
package org.tinymediamanager.scraper.ffmpeg;

import java.util.Collections;
import java.util.List;

import org.tinymediamanager.scraper.ArtworkSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.IMovieArtworkProvider;

/**
 * the class {@link FFmpegMovieArtworkProvider} is used to provide FFmpeg stills as an artwork provider
 * 
 * @author Manuel Laggner
 */
public class FFmpegMovieArtworkProvider extends FFmpegArtworkProvider implements IMovieArtworkProvider {

  @Override
  protected MediaProviderInfo createMediaProviderInfo() {
    MediaProviderInfo providerInfo = super.createMediaProviderInfo();

    providerInfo.getConfig().addInteger("start", "", 5);
    providerInfo.getConfig().addInteger("end", "", 95);
    providerInfo.getConfig().addInteger("count", "", 10);
    providerInfo.getConfig().addLabel("type", "");
    providerInfo.getConfig().addBoolean("thumb", "mediafiletype.thumb", true);
    providerInfo.getConfig().addBoolean("fanart", "mediafiletype.fanart", true);
    providerInfo.getConfig().load();

    return providerInfo;
  }

  @Override
  protected String getSubId() {
    return "movie_artwork";
  }

  @Override
  public List<MediaArtwork> getArtwork(ArtworkSearchAndScrapeOptions options) throws ScrapeException, MissingIdException {
    // only allow ALL, THUMB and BACKGROUD per se
    if (options.getArtworkType() != MediaArtwork.MediaArtworkType.ALL && options.getArtworkType() != MediaArtwork.MediaArtworkType.THUMB
        && options.getArtworkType() != MediaArtwork.MediaArtworkType.BACKGROUND) {
      return Collections.emptyList();
    }

    if ((options.getArtworkType() != MediaArtwork.MediaArtworkType.ALL || options.getArtworkType() != MediaArtwork.MediaArtworkType.THUMB)
        && Boolean.FALSE.equals(getProviderInfo().getConfig().getValueAsBool("thumb"))) {
      return Collections.emptyList();
    }

    if ((options.getArtworkType() != MediaArtwork.MediaArtworkType.ALL || options.getArtworkType() != MediaArtwork.MediaArtworkType.BACKGROUND)
        && Boolean.FALSE.equals(getProviderInfo().getConfig().getValueAsBool("fanart"))) {
      return Collections.emptyList();
    }

    return super.getArtwork(options);
  }
}
