/*
 * Copyright 2012 - 2022 Manuel Laggner
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
package org.tinymediamanager.scraper.imdb;

import static org.tinymediamanager.core.entities.Person.Type.ACTOR;
import static org.tinymediamanager.core.entities.Person.Type.WRITER;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.THUMB;

import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.core.tvshow.TvShowEpisodeSearchAndScrapeOptions;
import org.tinymediamanager.core.tvshow.TvShowSearchAndScrapeOptions;
import org.tinymediamanager.scraper.ArtworkSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaProviders;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.scraper.MediaSearchAndScrapeOptions;
import org.tinymediamanager.scraper.ScraperType;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.NothingFoundException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.http.InMemoryCachedUrl;
import org.tinymediamanager.scraper.http.OnDiskCachedUrl;
import org.tinymediamanager.scraper.http.Url;
import org.tinymediamanager.scraper.interfaces.IMediaProvider;
import org.tinymediamanager.scraper.interfaces.ITvShowMetadataProvider;
import org.tinymediamanager.scraper.util.CacheMap;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.scraper.util.MediaIdUtil;
import org.tinymediamanager.scraper.util.MetadataUtil;

/**
 * The class ImdbTvShowParser is used to parse TV show site of imdb.com
 *
 * @author Manuel Laggner
 */
public class ImdbTvShowParser extends ImdbParser {
  private static final Logger                                LOGGER                 = LoggerFactory.getLogger(ImdbTvShowParser.class);
  private static final CacheMap<String, List<MediaMetadata>> EPISODE_LIST_CACHE_MAP = new CacheMap<>(60, 10);

  ImdbTvShowParser(IMediaProvider metadataProvider, ExecutorService executor) {
    super(metadataProvider, MediaType.TV_SHOW, executor);
  }

  @Override
  protected Logger getLogger() {
    return LOGGER;
  }

  @Override
  protected boolean isIncludeTvSeriesResults() {
    return true;
  }

  @Override
  protected MediaMetadata getMetadata(MediaSearchAndScrapeOptions options) throws ScrapeException {
    switch (options.getMediaType()) {
      case TV_SHOW:
        return getTvShowMetadata((TvShowSearchAndScrapeOptions) options);

      case TV_EPISODE:
        return getEpisodeMetadata((TvShowEpisodeSearchAndScrapeOptions) options);

      default:
        return new MediaMetadata(ImdbMetadataProvider.ID);
    }
  }

  MediaMetadata getTvShowMetadata(TvShowSearchAndScrapeOptions options) throws ScrapeException {
    MediaMetadata md = new MediaMetadata(ImdbMetadataProvider.ID);
    md.setScrapeOptions(options);

    // API key check
    String apiKey;

    try {
      apiKey = metadataProvider.getApiKey();
    }
    catch (Exception e) {
      throw new ScrapeException(e);
    }

    String imdbId = "";

    // imdbId from searchResult
    if (options.getSearchResult() != null) {
      imdbId = options.getSearchResult().getIMDBId();
    }

    // imdbid from scraper option
    if (!MediaIdUtil.isValidImdbId(imdbId)) {
      imdbId = options.getImdbId();
    }

    if (!MediaIdUtil.isValidImdbId(imdbId)) {
      LOGGER.warn("not possible to scrape from IMDB - no imdbId found");
      throw new MissingIdException(MediaMetadata.IMDB);
    }

    LOGGER.debug("IMDB: getMetadata(imdbId): {}", imdbId);

    // worker for tmdb request
    Future<MediaMetadata> futureTmdb = null;
    if (isUseTmdbForTvShows()) {
      Callable<MediaMetadata> worker2 = new TmdbTvShowWorker(options);
      futureTmdb = executor.submit(worker2);
    }

    // get reference data (/reference)
    Callable<Document> worker = new ImdbWorker(constructUrl("title/", imdbId, decode("L3JlZmVyZW5jZQ==")), options.getLanguage().getLanguage(),
        options.getCertificationCountry().getAlpha2());
    Future<Document> futureReference = executor.submit(worker);

    // worker for imdb request (/plotsummary)
    Future<Document> futurePlotsummary;
    worker = new ImdbWorker(constructUrl("title/", imdbId, decode("L3Bsb3RzdW1tYXJ5")), options.getLanguage().getLanguage(),
        options.getCertificationCountry().getAlpha2());
    futurePlotsummary = executor.submit(worker);

    // worker for imdb request (/releaseinfo)
    Future<Document> futureReleaseinfo;
    worker = new ImdbWorker(constructUrl("title/", imdbId, decode("L3JlbGVhc2VpbmZv")), options.getLanguage().getLanguage(),
        options.getCertificationCountry().getAlpha2());
    futureReleaseinfo = executor.submit(worker);

    // worker for imdb keywords (/keywords)
    Future<Document> futureKeywords = null;
    if (isScrapeKeywordsPage()) {
      worker = new ImdbWorker(constructUrl("title/", imdbId, decode("L2tleXdvcmRz")), options.getLanguage().getLanguage(),
          options.getCertificationCountry().getAlpha2());
      futureKeywords = executor.submit(worker);
    }

    Document doc;
    try {
      doc = futureReference.get();
      if (doc != null) {
        parseReferencePage(doc, options, md);
      }

      doc = futurePlotsummary.get();
      if (doc != null) {
        parsePlotsummaryPage(doc, options, md);
      }

      // get the release info page
      Document releaseinfoDoc = futureReleaseinfo.get();
      if (releaseinfoDoc != null) {
        // get the date from the releaseinfo page
        parseReleaseinfoPage(releaseinfoDoc, options, md);
      }

      if (futureKeywords != null) {
        doc = futureKeywords.get();
        if (doc != null) {
          parseKeywordsPage(doc, options, md);
        }
      }

      // if everything worked so far, we can set the given id
      md.setId(ImdbMetadataProvider.ID, imdbId);
    }
    catch (Exception e) {
      LOGGER.error("problem while scraping: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    if (md.getIds().isEmpty()) {
      LOGGER.warn("nothing found");
      throw new NothingFoundException();
    }

    // populate id
    md.setId(ImdbMetadataProvider.ID, imdbId);

    // get data from tmdb?
    if (futureTmdb != null) {
      try {
        MediaMetadata tmdbMd = futureTmdb.get();
        if (tmdbMd != null) {
          // provide all IDs
          for (Map.Entry<String, Object> entry : tmdbMd.getIds().entrySet()) {
            md.setId(entry.getKey(), entry.getValue());
          }
          // title
          if (StringUtils.isNotBlank(tmdbMd.getTitle())) {
            md.setTitle(tmdbMd.getTitle());
          }
          // original title
          if (StringUtils.isNotBlank(tmdbMd.getOriginalTitle())) {
            md.setOriginalTitle(tmdbMd.getOriginalTitle());
          }
          // tagline
          if (StringUtils.isNotBlank(tmdbMd.getTagline())) {
            md.setTagline(tmdbMd.getTagline());
          }
          // plot
          if (StringUtils.isNotBlank(tmdbMd.getPlot())) {
            md.setPlot(tmdbMd.getPlot());
          }
        }
      }
      catch (Exception e) {
        LOGGER.debug("could not fetch data from TMDB: {}", e.getMessage());
      }
    }

    return md;
  }

  MediaMetadata getEpisodeMetadata(TvShowEpisodeSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("getEpisodeMetadata(): {}", options);

    MediaMetadata md = new MediaMetadata(ImdbMetadataProvider.ID);
    md.setScrapeOptions(options);
    String showId = "" + options.getTvShowIds().get(MediaMetadata.IMDB);

    String episodeId = options.getIdAsString(MediaMetadata.IMDB);
    if (!MediaIdUtil.isValidImdbId(episodeId)) {
      episodeId = "";
    }

    // get episode number and season number
    int seasonNr = options.getIdAsIntOrDefault(MediaMetadata.SEASON_NR, -1);
    int episodeNr = options.getIdAsIntOrDefault(MediaMetadata.EPISODE_NR, -1);

    if ((seasonNr == -1 || episodeNr == -1) && StringUtils.isBlank(episodeId)) {
      throw new MissingIdException(MediaMetadata.EPISODE_NR, MediaMetadata.SEASON_NR);
    }

    // first get the base episode metadata which can be gathered via getEpisodeList()
    // only if we get a S/E number
    MediaMetadata wantedEpisode = null;
    if (seasonNr >= 0 && episodeNr > 0) {
      if (!MediaIdUtil.isValidImdbId(showId)) {
        LOGGER.warn("not possible to scrape from IMDB - no imdbId found");
        throw new MissingIdException(MediaMetadata.IMDB);
      }

      List<MediaMetadata> episodes = getEpisodeList(options.createTvShowSearchAndScrapeOptions());

      // search by ID
      if (StringUtils.isNotBlank(episodeId)) {
        for (MediaMetadata episode : episodes) {
          if (episodeId.equals(episode.getId(MediaMetadata.IMDB))) {
            wantedEpisode = episode;
            break;
          }
        }
      }

      // search by S/E
      if (wantedEpisode == null) {
        for (MediaMetadata episode : episodes) {
          if (episode.getSeasonNumber() == seasonNr && episode.getEpisodeNumber() == episodeNr) {
            // search via season/episode number
            wantedEpisode = episode;
            break;
          }
        }
      }
    }

    // we did not find the episode; return
    if (wantedEpisode == null && StringUtils.isBlank(episodeId)) {
      LOGGER.warn("episode not found");
      throw new NothingFoundException();
    }

    // worker for tmdb request
    ExecutorCompletionService<MediaMetadata> compSvcTmdb = new ExecutorCompletionService<>(executor);
    Future<MediaMetadata> futureTmdb = null;

    // match via episodelist found
    if (wantedEpisode != null && wantedEpisode.getId(ImdbMetadataProvider.ID) instanceof String) {
      episodeId = (String) wantedEpisode.getId(ImdbMetadataProvider.ID);
      md.setEpisodeNumber(wantedEpisode.getEpisodeNumber());
      md.setSeasonNumber(wantedEpisode.getSeasonNumber());
      md.setTitle(wantedEpisode.getTitle());
      md.setPlot(wantedEpisode.getPlot());
      md.setRatings(wantedEpisode.getRatings());
      md.setReleaseDate(wantedEpisode.getReleaseDate());

      if (isUseTmdbForTvShows()) {
        Callable<MediaMetadata> worker2 = new TmdbTvShowEpisodeWorker(options);
        futureTmdb = compSvcTmdb.submit(worker2);
      }
    }

    // and finally the cast which needed to be fetched from the reference page
    if (MediaIdUtil.isValidImdbId(episodeId)) {
      md.setId(ImdbMetadataProvider.ID, episodeId);

      if (MediaIdUtil.isValidImdbId(episodeId)) {
        ExecutorCompletionService<Document> compSvcImdb = new ExecutorCompletionService<>(executor);

        Callable<Document> worker = new ImdbWorker(constructUrl("title/", episodeId, "/reference"), options.getLanguage().getLanguage(),
            options.getCertificationCountry().getAlpha2());
        Future<Document> futureReference = compSvcImdb.submit(worker);

        // worker for imdb keywords (/keywords)
        Future<Document> futureKeywords = null;
        if (isScrapeKeywordsPage()) {
          worker = new ImdbWorker(constructUrl("title/", episodeId, "/keywords"), options.getLanguage().getLanguage(),
              options.getCertificationCountry().getAlpha2());
          futureKeywords = compSvcImdb.submit(worker);
        }

        try {
          Document doc = futureReference.get();
          if (doc != null) {
            parseEpisodeReference(doc, md, episodeId);
          }

          if (futureKeywords != null) {
            Document docKeywords = futureKeywords.get();
            if (docKeywords != null) {
              parseKeywordsPage(docKeywords, options, md);
            }
          }

        }
        catch (Exception e) {
          LOGGER.trace("problem parsing: {}", e.getMessage());
        }
      }
    }

    // get data from tmdb?
    if (futureTmdb != null) {
      try {
        MediaMetadata tmdbMd = futureTmdb.get();
        if (tmdbMd != null) {
          // provide all IDs
          for (Map.Entry<String, Object> entry : tmdbMd.getIds().entrySet()) {
            md.setId(entry.getKey(), entry.getValue());
          }
          // title
          if (StringUtils.isNotBlank(tmdbMd.getTitle())) {
            md.setTitle(tmdbMd.getTitle());
          }
          // original title
          if (StringUtils.isNotBlank(tmdbMd.getOriginalTitle())) {
            md.setOriginalTitle(tmdbMd.getOriginalTitle());
          }
          // tagline
          if (StringUtils.isNotBlank(tmdbMd.getTagline())) {
            md.setTagline(tmdbMd.getTagline());
          }
          // plot
          if (StringUtils.isNotBlank(tmdbMd.getPlot())) {
            md.setPlot(tmdbMd.getPlot());
          }
          // thumb (if nothing has been found in imdb)
          if (md.getMediaArt(THUMB).isEmpty() && !tmdbMd.getMediaArt(THUMB).isEmpty()) {
            MediaArtwork thumb = tmdbMd.getMediaArt(THUMB).get(0);
            md.addMediaArt(thumb);
          }
        }
      }
      catch (InterruptedException e) {
        // do not swallow these Exceptions
        Thread.currentThread().interrupt();
      }
      catch (Exception e) {
        LOGGER.warn("could not get cast page: {}", e.getMessage());
      }
    }

    return md;
  }

  List<MediaMetadata> getEpisodeList(TvShowSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("getEpisodeList(): {}", options);

    // parse the episodes from the ratings overview page (e.g.
    // http://www.imdb.com/title/tt0491738/episodes )
    String imdbId = options.getImdbId();
    if (StringUtils.isBlank(imdbId)) {
      throw new MissingIdException(MediaMetadata.IMDB);
    }

    // look in the cache map if there is an entry
    List<MediaMetadata> episodes = EPISODE_LIST_CACHE_MAP.get(imdbId + "_" + options.getLanguage().getLanguage());
    if (ListUtils.isNotEmpty(episodes)) {
      // cache hit!
      return episodes;
    }

    episodes = new ArrayList<>();

    // we need to parse every season for its own _._
    // get the page for the first season (this is available in 99,9% of all cases)

    Document doc;
    Url url;
    try {
      // cache this on disk because that may be called multiple times
      url = new OnDiskCachedUrl(constructUrl("/title/", imdbId, "/episodes?season=1"), 300, TimeUnit.SECONDS);
      url.addHeader("Accept-Language", getAcceptLanguage(options.getLanguage().getLanguage(), options.getCertificationCountry().getAlpha2()));
    }
    catch (Exception e) {
      LOGGER.error("problem scraping: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    List<String> availableSeasons = new ArrayList<>();

    try (InputStream is = url.getInputStream()) {
      doc = Jsoup.parse(is, "UTF-8", "");
      if (doc != null) {
        parseEpisodeList(1, episodes, doc);

        // get the other seasons out of the select option
        Element select = doc.getElementById("bySeason");
        if (select != null) {
          for (Element option : select.getElementsByTag("option")) {
            String value = option.attr("value");
            if (StringUtils.isNotBlank(value) && !"1".equals(value)) {
              availableSeasons.add(value);
            }
          }
        }
      }
    }
    catch (InterruptedException | InterruptedIOException e) {
      // do not swallow these Exceptions
      Thread.currentThread().interrupt();
    }
    catch (Exception e) {
      LOGGER.error("problem scraping: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    // then parse every season
    for (String seasonAsString : availableSeasons) {
      int season;
      try {
        season = Integer.parseInt(seasonAsString);
      }
      catch (Exception e) {
        LOGGER.debug("could not parse season number - {}", e.getMessage());
        continue;
      }

      Url seasonUrl;
      try {
        seasonUrl = new InMemoryCachedUrl(constructUrl("/title/", imdbId, "/epdate?season=" + season));
        seasonUrl.addHeader("Accept-Language", getAcceptLanguage(options.getLanguage().getLanguage(), options.getCertificationCountry().getAlpha2()));
      }
      catch (Exception e) {
        LOGGER.error("problem scraping: {}", e.getMessage());
        throw new ScrapeException(e);
      }

      try (InputStream is = seasonUrl.getInputStream()) {
        doc = Jsoup.parse(is, "UTF-8", "");
        // if the given season number and the parsed one does not match, break here
        if (!parseEpisodeList(season, episodes, doc)) {
          break;
        }
      }
      catch (InterruptedException | InterruptedIOException e) {
        // do not swallow these Exceptions
        Thread.currentThread().interrupt();
      }
      catch (Exception e) {
        LOGGER.warn("problem parsing ep list: {}", e.getMessage());
      }
    }

    // cache for further fast access
    if (!episodes.isEmpty()) {
      EPISODE_LIST_CACHE_MAP.put(imdbId + "_" + options.getLanguage().getLanguage(), episodes);
    }

    return episodes;
  }

  private boolean parseEpisodeList(int season, List<MediaMetadata> episodes, Document doc) {
    Pattern unknownPattern = Pattern.compile("Unknown");
    Pattern seasonEpisodePattern = Pattern.compile("S([0-9]*), Ep([0-9]*)");
    int episodeCounter = 0;

    // parse episodes
    Elements tables = doc.getElementsByClass("eplist");
    if (tables.isEmpty()) {
      // no episodes here? break
      return false;
    }

    for (Element table : tables) {
      Elements rows = table.getElementsByClass("list_item");
      for (Element row : rows) {
        Matcher matcher = season <= 0 ? unknownPattern.matcher(row.text()) : seasonEpisodePattern.matcher(row.text());
        if (matcher.find() && (season <= 0 || matcher.groupCount() >= 2)) {
          try {
            // we found a row containing episode data
            MediaMetadata ep = new MediaMetadata(ImdbMetadataProvider.ID);

            // parse season and ep number
            if (season <= 0) {
              ep.setSeasonNumber(0);
              ep.setEpisodeNumber(++episodeCounter);
            }
            else {
              ep.setSeasonNumber(Integer.parseInt(matcher.group(1)));
              ep.setEpisodeNumber(Integer.parseInt(matcher.group(2)));
            }

            // check if we have still valid data
            if (season > 0 && season != ep.getSeasonNumber()) {
              return false;
            }

            // get ep title and id
            Elements anchors = row.getElementsByAttributeValueStarting("href", "/title/tt");
            for (Element anchor : anchors) {
              if ("name".equals(anchor.attr("itemprop"))) {
                ep.setTitle(anchor.text());
                break;
              }
            }

            String id = "";
            Matcher idMatcher = IMDB_ID_PATTERN.matcher(anchors.get(0).attr("href"));
            while (idMatcher.find()) {
              if (idMatcher.group(1) != null) {
                id = idMatcher.group(1);
              }
            }

            if (StringUtils.isNotBlank(id)) {
              ep.setId(ImdbMetadataProvider.ID, id);
            }

            // plot
            Element plot = row.getElementsByClass("item_description").first();
            if (plot != null) {
              ep.setPlot(plot.ownText());
            }

            // rating and rating count
            Element ratingElement = row.getElementsByClass("ipl-rating-star__rating").first();
            if (ratingElement != null) {
              String ratingAsString = ratingElement.ownText().replace(",", ".");

              Element votesElement = row.getElementsByClass("ipl-rating-star__total-votes").first();
              if (votesElement != null) {
                String countAsString = votesElement.ownText().replaceAll("[.,()]", "").trim();
                try {
                  MediaRating rating = new MediaRating(ImdbMetadataProvider.ID);
                  rating.setRating(Float.parseFloat(ratingAsString));
                  rating.setVotes(MetadataUtil.parseInt(countAsString));
                  ep.addRating(rating);
                }
                catch (Exception e) {
                  LOGGER.trace("could not parse rating/vote count: {}", e.getMessage());
                }
              }
            }

            // release date
            Element releaseDate = row.getElementsByClass("airdate").first();
            if (releaseDate != null) {
              ep.setReleaseDate(parseDate(releaseDate.ownText()));
            }

            // poster
            Element image = row.getElementsByTag("img").first();
            if (image != null) {
              String posterUrl = image.attr("src");
              posterUrl = posterUrl.replaceAll("UX[0-9]{2,4}_", "");
              posterUrl = posterUrl.replaceAll("UY[0-9]{2,4}_", "");
              posterUrl = posterUrl.replaceAll("CR[0-9]{1,3},[0-9]{1,3},[0-9]{1,3},[0-9]{1,3}_", "");

              if (StringUtils.isNotBlank(posterUrl)) {
                MediaArtwork ma = new MediaArtwork(ImdbMetadataProvider.ID, THUMB);
                ma.setPreviewUrl(posterUrl);
                ma.setDefaultUrl(posterUrl);
                ma.setOriginalUrl(posterUrl);
                ep.addMediaArt(ma);
              }
            }

            episodes.add(ep);
          }
          catch (Exception e) {
            LOGGER.warn("failed parsing: {} for ep data - {}", row.text(), e.getMessage());
          }
        }
      }
    }
    return true;
  }

  private void parseEpisodeReference(Document doc, MediaMetadata md, String imdbId) {
    // title (h3 itemprop=name)
    Element header = doc.getElementsByClass("titlereference-header").first();
    if (header != null) {
      Element year = header.getElementsByClass("titlereference-title-year").first();
      if (year != null) {
        String episodeTitle = cleanString(year.parent().ownText());
        if (StringUtils.isNotBlank(episodeTitle)) {
          md.setTitle(episodeTitle);
        }
      }
    }

    // title: fallback to title from meta
    if (StringUtils.isBlank(md.getTitle())) {
      Element title = doc.getElementsByAttributeValue("name", "title").first();
      if (title != null) {
        String episodeTitle = cleanString(title.attr("content"));

        int yearStart = episodeTitle.lastIndexOf('(');
        if (yearStart > 0) {
          episodeTitle = episodeTitle.substring(0, yearStart - 1).trim();
          md.setTitle(episodeTitle);
        }
      }
    }

    // plot
    // class titlereference-section-overview -> first div without class
    Element titlereference = doc.getElementsByClass("titlereference-section-overview").first();
    if (titlereference != null) {
      for (Element child : titlereference.children()) {
        if ("div".equals(child.tagName()) && child.classNames().isEmpty()) {
          String plot = child.text();
          if (StringUtils.isNotBlank(plot)) {
            md.setPlot(plot);
            break;
          }
        }
      }
    }

    // releasedate
    Element releaseDateElement = doc.getElementsByAttributeValue("href", "/title/" + imdbId + "/releaseinfo").first();
    if (releaseDateElement != null) {
      String releaseDateText = releaseDateElement.ownText();
      int startOfCountry = releaseDateText.indexOf('(');
      if (startOfCountry > 0) {
        releaseDateText = releaseDateText.substring(0, startOfCountry - 1).trim();
      }
      md.setReleaseDate(parseDate(releaseDateText));
    }

    // poster
    Element poster = doc.getElementsByAttributeValue("property", "og:image").first();
    if (poster != null) {
      String posterUrl = poster.attr("content");

      int fileStart = posterUrl.lastIndexOf('/');
      if (fileStart > 0) {
        int parameterStart = posterUrl.indexOf('_', fileStart);
        if (parameterStart > 0) {
          int startOfExtension = posterUrl.lastIndexOf('.');
          if (startOfExtension > parameterStart) {
            posterUrl = posterUrl.substring(0, parameterStart) + posterUrl.substring(startOfExtension);
          }
        }
      }
      processMediaArt(md, MediaArtwork.MediaArtworkType.POSTER, posterUrl);
    }

    // rating and rating count
    Element ratingElement = doc.getElementsByClass("ipl-rating-star__rating").first();
    if (ratingElement != null) {
      String ratingAsString = ratingElement.ownText().replace(",", ".");
      Element votesElement = doc.getElementsByClass("ipl-rating-star__total-votes").first();
      if (votesElement != null) {
        String countAsString = votesElement.ownText().replaceAll("[.,()]", "").trim();
        try {
          MediaRating rating = new MediaRating("imdb");
          rating.setRating(Float.parseFloat(ratingAsString));
          rating.setVotes(MetadataUtil.parseInt(countAsString));
          md.addRating(rating);
        }
        catch (Exception e) {
          getLogger().trace("could not parse rating/vote count: {}", e.getMessage());
        }
      }
    }

    // director
    Element directorsElement = doc.getElementById("directors");
    while (directorsElement != null && !"header".equals(directorsElement.tag().getName())) {
      directorsElement = directorsElement.parent();
    }
    if (directorsElement != null) {
      directorsElement = directorsElement.nextElementSibling();
    }
    if (directorsElement != null) {
      for (Element directorElement : directorsElement.getElementsByClass("name")) {
        String director = directorElement.text().trim();

        Person cm = new Person(Person.Type.DIRECTOR, director);
        // profile path
        Element anchor = directorElement.getElementsByAttributeValueStarting("href", "/name/").first();
        if (anchor != null) {
          Matcher matcher = PERSON_ID_PATTERN.matcher(anchor.attr("href"));
          if (matcher.find()) {
            if (matcher.group(0) != null) {
              cm.setProfileUrl("http://www.imdb.com" + matcher.group(0));
            }
            if (matcher.group(1) != null) {
              cm.setId(ImdbMetadataProvider.ID, matcher.group(1));
            }
          }
        }
        md.addCastMember(cm);
      }
    }

    // actors
    Element castTableElement = doc.getElementsByClass("cast_list").first();
    if (castTableElement != null) {
      Elements castListLabel = castTableElement.getElementsByClass("castlist_label");
      Elements tr = castTableElement.getElementsByTag("tr");
      for (Element row : tr) {
        // check if we're at the uncredited cast members
        if (!isScrapeUncreditedActors() && castListLabel.size() > 1 && row.children().contains(castListLabel.get(1))) {
          break;
        }

        Person cm = parseCastMember(row);
        if (cm != null && StringUtils.isNotEmpty(cm.getName())) {
          cm.setType(ACTOR);
          md.addCastMember(cm);
        }
      }
    }

    // writers
    Element writersElement = doc.getElementById("writers");
    while (writersElement != null && !"header".equals(writersElement.tag().getName())) {
      writersElement = writersElement.parent();
    }
    if (writersElement != null) {
      writersElement = writersElement.nextElementSibling();
    }
    if (writersElement != null) {
      Elements writersElements = writersElement.getElementsByAttributeValueStarting("href", "/name/");

      for (Element writerElement : writersElements) {
        String writer = cleanString(writerElement.ownText());
        Person cm = new Person(WRITER, writer);
        // profile path
        Element anchor = writerElement.getElementsByAttributeValueStarting("href", "/name/").first();
        if (anchor != null) {
          Matcher matcher = PERSON_ID_PATTERN.matcher(anchor.attr("href"));
          if (matcher.find()) {
            if (matcher.group(0) != null) {
              cm.setProfileUrl("http://www.imdb.com" + matcher.group(0));
            }
            if (matcher.group(1) != null) {
              cm.setId(ImdbMetadataProvider.ID, matcher.group(1));
            }
          }
        }
        md.addCastMember(cm);
      }
    }
  }

  public List<MediaArtwork> getTvShowArtwork(ArtworkSearchAndScrapeOptions options) throws ScrapeException {
    String imdbId = "";

    // imdbid from scraper option
    if (!MediaIdUtil.isValidImdbId(imdbId)) {
      imdbId = options.getImdbId();
    }

    // imdbid via tmdbid
    if (!MediaIdUtil.isValidImdbId(imdbId) && options.getTmdbId() > 0) {
      imdbId = MediaIdUtil.getTvShowImdbIdViaTmdbId(options.getTmdbId());
    }

    if (!MediaIdUtil.isValidImdbId(imdbId)) {
      LOGGER.warn("not possible to scrape from IMDB - imdbId found");
      throw new MissingIdException(MediaMetadata.IMDB);
    }

    // just get the MediaMetadata via normal scrape and pick the poster from the result
    TvShowSearchAndScrapeOptions tvShowSearchAndScrapeOptions = new TvShowSearchAndScrapeOptions();
    tvShowSearchAndScrapeOptions.setDataFromOtherOptions(options);

    try {
      List<MediaArtwork> artworks = getMetadata(tvShowSearchAndScrapeOptions).getMediaArt(MediaArtwork.MediaArtworkType.POSTER);

      // adopt the url to the wanted size
      for (MediaArtwork artwork : artworks) {
        adoptArtworkToOptions(artwork, options);
      }

      return artworks;
    }
    catch (NothingFoundException e) {
      LOGGER.debug("nothing found");
    }

    return Collections.emptyList();
  }

  private static class TmdbTvShowWorker implements Callable<MediaMetadata> {
    private final TvShowSearchAndScrapeOptions options;

    TmdbTvShowWorker(TvShowSearchAndScrapeOptions options) {
      this.options = options;
    }

    @Override
    public MediaMetadata call() {
      try {
        ITvShowMetadataProvider tmdb = MediaProviders.getProviderById(MediaMetadata.TMDB, ITvShowMetadataProvider.class);
        if (tmdb == null) {
          return null;
        }

        TvShowSearchAndScrapeOptions scrapeOptions = new TvShowSearchAndScrapeOptions(this.options);
        scrapeOptions.setMetadataScraper(new MediaScraper(ScraperType.TV_SHOW, tmdb));
        return tmdb.getMetadata(scrapeOptions);
      }
      catch (Exception e) {
        return null;
      }
    }
  }

  private static class TmdbTvShowEpisodeWorker implements Callable<MediaMetadata> {
    private final TvShowEpisodeSearchAndScrapeOptions options;

    TmdbTvShowEpisodeWorker(TvShowEpisodeSearchAndScrapeOptions options) {
      this.options = options;
    }

    @Override
    public MediaMetadata call() {
      try {
        ITvShowMetadataProvider tmdb = MediaProviders.getProviderById(MediaMetadata.TMDB, ITvShowMetadataProvider.class);
        if (tmdb == null) {
          return null;
        }

        TvShowEpisodeSearchAndScrapeOptions scrapeOptions = new TvShowEpisodeSearchAndScrapeOptions(this.options);
        scrapeOptions.setMetadataScraper(new MediaScraper(ScraperType.TV_SHOW, tmdb));
        return tmdb.getMetadata(scrapeOptions);
      }
      catch (Exception e) {
        return null;
      }
    }
  }
}
