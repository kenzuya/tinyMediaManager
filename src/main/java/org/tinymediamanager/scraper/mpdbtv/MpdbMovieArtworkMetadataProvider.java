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
package org.tinymediamanager.scraper.mpdbtv;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.scraper.ArtworkSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.IMovieArtworkProvider;
import org.tinymediamanager.scraper.mpdbtv.entities.DiscArt;
import org.tinymediamanager.scraper.mpdbtv.entities.Fanart;
import org.tinymediamanager.scraper.mpdbtv.entities.HDClearArt;
import org.tinymediamanager.scraper.mpdbtv.entities.HDLogo;
import org.tinymediamanager.scraper.mpdbtv.entities.MovieEntity;
import org.tinymediamanager.scraper.mpdbtv.entities.Poster;
import org.tinymediamanager.scraper.mpdbtv.services.Controller;

/**
 * The Class {@link MpdbMovieArtworkMetadataProvider}. Movie metdata provider for the site MPDB.tv
 *
 * @author Wolfgang Janes
 */
public class MpdbMovieArtworkMetadataProvider extends MpdbMetadataProvider implements IMovieArtworkProvider {
  private static final Logger     LOGGER = LoggerFactory.getLogger(MpdbMovieArtworkMetadataProvider.class);

  private final MediaProviderInfo providerInfo;
  private final Controller        controller;

  public MpdbMovieArtworkMetadataProvider() {
    this.providerInfo = createMediaProviderInfo();
    controller = new Controller(false);
  }

  @Override
  protected String getSubId() {
    return "movie_artwork";
  }

  @Override
  public List<MediaArtwork> getArtwork(ArtworkSearchAndScrapeOptions options) throws ScrapeException {

    initAPI();

    MovieEntity scrapeResult;
    List<MediaArtwork> ma = new ArrayList<>();
    String id;

    // search with mpdbtv id, tmdb id and imdb id
    id = options.getIdAsString("mpdbtv");
    if (id == null || id.equals("0")) {
      id = Integer.toString(options.getTmdbId());
      if (id == null || id.equals("0")) {
        id = options.getImdbId();
      }
    }
    if (id.equals("0")) {
      LOGGER.warn("Cannot get artwork - neither imdb/tmdb set");
      throw new MissingIdException(MediaMetadata.TMDB, MediaMetadata.IMDB);
    }

    LOGGER.info("========= BEGIN MPDB.tv artwork scraping");
    try {
      scrapeResult = controller.getScrapeInformation(getEncodedUserName(), getSubscriptionKey(), id,
          options.getLanguage().toLocale(), null, FORMAT);
    }
    catch (Exception e) {
      LOGGER.error("error searching: {} ", e.getMessage());
      throw new ScrapeException(e);
    }

    // Poster
    for (Poster poster : scrapeResult.posters) {
      MediaArtwork mediaArtwork = new MediaArtwork(providerInfo.getId(), MediaArtwork.MediaArtworkType.POSTER);
      mediaArtwork.setPreviewUrl(poster.preview);
      mediaArtwork.setDefaultUrl(poster.original);
      mediaArtwork.setOriginalUrl(poster.original);
      mediaArtwork.setLikes(poster.votes);

      ma.add(mediaArtwork);
    }

    // Fanarts
    for (Fanart fanart : scrapeResult.fanarts) {
      MediaArtwork mediaArtwork = new MediaArtwork(providerInfo.getId(), MediaArtwork.MediaArtworkType.BACKGROUND);
      mediaArtwork.setPreviewUrl(fanart.preview);
      mediaArtwork.setDefaultUrl(fanart.original);
      mediaArtwork.setOriginalUrl(fanart.original);
      mediaArtwork.setLikes(fanart.votes);

      ma.add(mediaArtwork);
    }

    // DiscArt
    for (DiscArt discArt : scrapeResult.discarts) {
      MediaArtwork mediaArtwork = new MediaArtwork(providerInfo.getId(), MediaArtwork.MediaArtworkType.DISC);
      mediaArtwork.setPreviewUrl(discArt.preview);
      mediaArtwork.setDefaultUrl(discArt.original);
      mediaArtwork.setOriginalUrl(discArt.original);
      mediaArtwork.setLikes(discArt.votes);

      ma.add(mediaArtwork);
    }

    // HDClearArt
    for (HDClearArt hdClearArt : scrapeResult.hdcleararts) {
      MediaArtwork mediaArtwork = new MediaArtwork(providerInfo.getId(), MediaArtwork.MediaArtworkType.CLEARART);
      mediaArtwork.setPreviewUrl(hdClearArt.preview);
      mediaArtwork.setDefaultUrl(hdClearArt.original);
      mediaArtwork.setOriginalUrl(hdClearArt.original);
      mediaArtwork.setLikes(hdClearArt.votes);

      ma.add(mediaArtwork);
    }

    // HDLogo
    for (HDLogo hdLogo : scrapeResult.hdlogos) {
      MediaArtwork mediaArtwork = new MediaArtwork(providerInfo.getId(), MediaArtwork.MediaArtworkType.CLEARLOGO);
      mediaArtwork.setPreviewUrl(hdLogo.preview);
      mediaArtwork.setDefaultUrl(hdLogo.original);
      mediaArtwork.setOriginalUrl(hdLogo.original);
      mediaArtwork.setLikes(hdLogo.votes);

      ma.add(mediaArtwork);
    }
    return ma;
  }
}
