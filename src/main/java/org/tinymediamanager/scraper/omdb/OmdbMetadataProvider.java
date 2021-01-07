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

package org.tinymediamanager.scraper.omdb;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.license.License;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.omdb.service.Controller;

/**
 * Central metadata provider class
 *
 * @author Wolfgang Janes
 */
abstract class OmdbMetadataProvider {
  private static final String     ID = "omdbapi";

  private final MediaProviderInfo providerInfo;

  protected Controller            controller;

  OmdbMetadataProvider() {
    providerInfo = createMediaProviderInfo();
    controller = new Controller(false);
  }

  /**
   * get the sub id of this scraper (for dedicated storage)
   * 
   * @return the sub id
   */
  protected abstract String getSubId();

  protected MediaProviderInfo createMediaProviderInfo() {
    MediaProviderInfo info = new MediaProviderInfo(ID, getSubId(), "omdbapi.com",
        "<html><h3>Omdbapi.com</h3><br />The OMDb API is a RESTful web service to obtain movie information. All content and images on the site are contributed and maintained by our users. <br /><br />TinyMediaManager offers a limited access to OMDb (10 calls per 15 seconds). If you want to use OMDb with more than this restricted access, you should become a patron of OMDb (https://www.patreon.com/join/omdb)<br /><br />Available languages: EN</html>",
        OmdbMetadataProvider.class.getResource("/org/tinymediamanager/scraper/omdbapi.svg"));

    info.getConfig().addText("apiKey", "", true);
    info.getConfig().load();

    return info;
  }

  public MediaProviderInfo getProviderInfo() {
    return providerInfo;
  }

  protected String getApiKey() {
    String apiKey = providerInfo.getConfig().getValue("apiKey");
    if (StringUtils.isNotBlank(apiKey)) {
      return apiKey;
    }

    try {
      return License.getInstance().getApiKey(ID);
    }
    catch (Exception e) {
      return "";
    }
  }

  // @Override
  // public List<MediaEpisode> getEpisodeList(MediaScrapeOptions query) throws
  // Exception {
  //
  // LOGGER.debug("scrape() Episodes " + query.toString());
  // List<MediaEpisode> mediaEpisode = new ArrayList<>();
  // SeasonSearch season;
  // season = null;
  // MovieEntity result = null;
  // MovieEntity episodes;
  //
  // // First scrape the id to get the total number of Seasons
  // try {
  // if(API_KEY.equals(apiKey)){
  // OmdbConnectionCounter.trackConnections();
  // }
  // LOGGER.debug("Getting TotalSeasons From Scraping");
  // result = controller.getScrapeDataById(apiKey,
  // query.getId(OmdbMetadataProvider.providerinfo.getId()), "series", true);
  // }
  // catch (Exception e) {
  // LOGGER.error("error scraping: " + e.getMessage());
  // }
  //
  // for (int i = 1; i <= Integer.parseInt(result.totalSeasons); i++) {
  // LOGGER.debug("Scrape Season " + i);
  // season = controller.getSeasonsById(apiKey,
  // query.getId(OmdbMetadataProvider.providerinfo.getId()), "series", i);
  //
  // for (SeasonEntity episodeResult : ListUtils.nullSafe(season.episodes)) {
  // MediaEpisode mediaResult = new
  // MediaEpisode(OmdbMetadataProvider.providerinfo.getId());
  // episodes = controller.getEpisodesBySeasons(apiKey,
  // query.getId(OmdbMetadataProvider.providerinfo.getId()), "series", i,
  // Integer.parseInt(episodeResult.episode));
  //
  // mediaResult.season = i;
  // mediaResult.plot = episodes.plot;
  // try {
  // mediaResult.episode = Integer.parseInt(episodeResult.episode);
  // mediaResult.rating = Integer.parseInt(episodes.imdbVotes);
  // }
  // catch (NumberFormatException ignored) {
  //
  // }
  // mediaResult.title = episodes.title;
  //
  // mediaEpisode.add(mediaResult);
  //
  // }
  //
  // }
  //
  // return mediaEpisode;
  //
  // }

  /**
   * return a list of results that were separated by a delimiter
   *
   * @param input
   *          result from API
   * @param delimiter
   *          used delimiter
   * @return List of results
   */
  protected List<String> getResult(String input, String delimiter) {
    String[] result = input.split(delimiter);
    List<String> output = new ArrayList<>();

    for (String r : result) {
      output.add(r.trim());
    }

    return output;
  }

  /**
   * set the Debugmode for JUnit Testing
   *
   * @param verbose
   *          Boolean for debug mode
   */
  void setVerbose(boolean verbose) {
    controller = new Controller(verbose);
  }
}
