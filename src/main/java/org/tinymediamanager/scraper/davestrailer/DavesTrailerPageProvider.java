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
package org.tinymediamanager.scraper.davestrailer;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.FeatureNotEnabledException;
import org.tinymediamanager.core.entities.MediaTrailer;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.TrailerSearchAndScrapeOptions;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.http.OnDiskCachedUrl;
import org.tinymediamanager.scraper.interfaces.IMovieTrailerProvider;

/**
 * The Class DavesTrailerPage. A trailer provider for the site davestrailerpage.co.uk
 *
 * @author Wolfgang Janes
 */
public class DavesTrailerPageProvider implements IMovieTrailerProvider {

  private static final String     ID     = "davesTrailer";
  private static final Logger     LOGGER = LoggerFactory.getLogger(DavesTrailerPageProvider.class);
  private final MediaProviderInfo providerInfo;

  public DavesTrailerPageProvider() {
    providerInfo = createMediaProviderInfo();
  }

  private MediaProviderInfo createMediaProviderInfo() {
    return new MediaProviderInfo(ID, "movie_trailer", "davestrailerpage.co.uk",
        "<html><h3>Dave's Trailer Page</h3>Scraper for Dave's Trailer Page</html>",
        DavesTrailerPageProvider.class.getResource("/org/tinymediamanager/scraper/daves_trailer_page_logo.jpg"));
  }

  @Override
  public MediaProviderInfo getProviderInfo() {
    return providerInfo;
  }

  @Override
  public boolean isActive() {
    return isFeatureEnabled() && isApiKeyAvailable(null);
  }

  @Override
  public List<MediaTrailer> getTrailers(TrailerSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("getTrailers() - {}", options);

    if (!isActive()) {
      throw new ScrapeException(new FeatureNotEnabledException(this));
    }

    List<MediaTrailer> trailers = new ArrayList<>();
    MediaMetadata md = options.getMetadata();

    if (md == null) {
      return Collections.emptyList();
    }

    // 1. search with title
    String title = md.getOriginalTitle();
    if (title.isEmpty()) {
      title = md.getTitle();
    }
    String firstChar = title.substring(0, 1).toLowerCase(Locale.ROOT);
    String url;

    if (firstChar.matches("[xyz]")) {
      url = getApiKey() + "trailers_xyz.html";
    }
    else if (firstChar.matches("[0-9]")) {
      url = getApiKey() + "trailers_0to9.html";
    }
    else {
      url = getApiKey() + "trailers_" + firstChar + ".html";
    }

    try {
      OnDiskCachedUrl disk = new OnDiskCachedUrl(url, 7, TimeUnit.DAYS);
      Document doc = Jsoup.parse(disk.getInputStream(), StandardCharsets.UTF_8.toString(), "");
      Element table = doc.select("table").last();
      Elements rows = table.select("tr");

      for (Element row : rows) {

        // loop 1: check the imdb id in the imdb anchor
        for (Element anchor : row.getElementsByTag("a")) {
          if (anchor.attr("href").contains(options.getImdbId())) {
            Element li = anchor.parent();
            if (li != null) {
              // as long as there are new rows with trailer information :)
              while (li.nextElementSibling() != null) {
                Element nextLi = li.nextElementSibling();
                String trailerName = nextLi.select("b").first().text();
                // loop 2: get all available trailer anchors and the
                // corresponding information
                for (Element trailerAnchor : nextLi.getElementsByTag("a")) {
                  MediaTrailer trailer = new MediaTrailer();
                  trailer.setName(trailerName);
                  trailer.setUrl(trailerAnchor.attr("href"));
                  trailer.setQuality(trailerAnchor.childNode(0).toString());
                  trailer.setProvider(getProviderFromUrl(trailerAnchor.attr("href")));
                  trailer.setScrapedBy(providerInfo.getId());
                  trailers.add(trailer);
                }
                li = nextLi;
              }
            }
            break;
          }
        }
      }
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    catch (Exception e) {
      LOGGER.debug("cannot parse Dave's Trailer Page movie: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    return trailers;
  }

  /**
   * Returns the "Source" for this trailer by parsing the URL.
   *
   * @param url
   *          the url
   * @return the provider from url
   */
  private static String getProviderFromUrl(String url) {
    url = url.toLowerCase(Locale.ROOT);
    String source = "unknown";
    if (url.contains("youtube.com")) {
      source = "youtube";
    }
    else if (url.contains("apple.com")) {
      source = "apple";
    }
    return source;
  }
}
