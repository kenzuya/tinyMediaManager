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
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.ITvShowArtworkProvider;

/**
 * the class {@link FFmpegTvShowArtworkProvider} is used to provide FFmpeg stills as an artwork provider
 * 
 * @author Manuel Laggner
 */
public class FFmpegTvShowArtworkProvider extends FFmpegArtworkProvider implements ITvShowArtworkProvider {

  @Override
  protected MediaProviderInfo createMediaProviderInfo() {
    MediaProviderInfo providerInfo = super.createMediaProviderInfo();

    providerInfo.getConfig().addInteger("start", "", 5);
    providerInfo.getConfig().addInteger("end", "", 95);
    providerInfo.getConfig().addInteger("count", "", 10);
    providerInfo.getConfig().addLabel("type", "");
    providerInfo.getConfig().addBoolean("episodeThumb", true);
    providerInfo.getConfig().load();

    return providerInfo;
  }

  @Override
  protected String getSubId() {
    return "tvshow_artwork";
  }

  @Override
  public List<MediaArtwork> getArtwork(ArtworkSearchAndScrapeOptions options) throws ScrapeException, MissingIdException {
    if (options.getMediaType() != MediaType.TV_EPISODE) {
      return Collections.emptyList();
    }

    if (options.getArtworkType() != MediaArtwork.MediaArtworkType.THUMB && options.getArtworkType() != MediaArtwork.MediaArtworkType.ALL) {
      return Collections.emptyList();
    }

    if ((options.getArtworkType() != MediaArtwork.MediaArtworkType.THUMB || options.getArtworkType() != MediaArtwork.MediaArtworkType.ALL)
        && Boolean.FALSE.equals(getProviderInfo().getConfig().getValueAsBool("episodeThumb"))) {
      return Collections.emptyList();
    }

    return super.getArtwork(options);
  }
}
