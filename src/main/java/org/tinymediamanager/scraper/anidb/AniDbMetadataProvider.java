package org.tinymediamanager.scraper.anidb;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.ParseSettings;
import org.jsoup.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.license.TmmFeature;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.MediaSearchAndScrapeOptions;
import org.tinymediamanager.scraper.exceptions.HttpException;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.http.OnDiskCachedUrl;
import org.tinymediamanager.scraper.http.Url;
import org.tinymediamanager.scraper.util.MetadataUtil;
import org.tinymediamanager.scraper.util.RingBuffer;
import org.tinymediamanager.scraper.util.UrlUtil;

/**
 * The common elements for AniDB's Movie and Show Metadata Provider.
 *
 * @see <a href="https://anidb.net/">https://anidb.net/</a>
 * @see <a href="https://wiki.anidb.net/API">https://wiki.anidb.net/API</a>
 * @see AniDbTvShowMetadataProvider
 * @see AniDbMovieMetadataProvider
 */
public abstract class AniDbMetadataProvider implements TmmFeature {

  public static final String                        ID                = "anidb";
  private static final Logger                       LOGGER            = LoggerFactory.getLogger(AniDbMetadataProvider.class);

  // flood: pager every 2 seconds
  // protection: https://wiki.anidb.net/w/HTTP_API_Definition
  private static final RingBuffer<Long>             connectionCounter = new RingBuffer<>(1);
  protected final HashMap<Integer, List<AniDBShow>> showsForLookup    = new HashMap<>();
  protected final MediaProviderInfo                 providerInfo;

  public AniDbMetadataProvider() {
    providerInfo = createMediaProviderInfo();
  }

  protected abstract MediaProviderInfo createMediaProviderInfo();

  /**
   * build up the hashmap for a fast title search First 3 lines are comments
   *
   * <pre>
   * {@code
   *      # created: Tue May  3 03:00:02 2022
   *      # <aid>|<type>|<language>|<title>
   *      # type: 1=primary title (one per anime), 2=synonyms (multiple per anime), 3=shorttitles (multiple per anime), 4=official title (one per language)
   *      4598|2|x-jat|_summer
   *      4598|2|ru|_Лето
   *      10004|2|en|-Dark night at NTR Village-, Kagachi-sama, please honor me with your presence
   *      7307|2|he|!!קיי-און
   *      ...
   * }
   * </pre>
   * <p>
   *
   * @see <a href="https://wiki.anidb.net/w/API#Anime_Titles">wiki.anidb.net/w/API#Anime_Titles</a>
   *
   * @throws ScrapeException
   *           error getting AniDB index
   */
  protected void buildTitleHashMap() throws ScrapeException {
    // <aid>|<type>|<language>|<title>
    // type:
    // 1=primary title (one per anime),
    // 2=synonyms (multiple per anime),
    // 3=shorttitles (multiple per anime),
    // 4=official title (one per language)
    Pattern pattern = Pattern.compile("^(?!#)(\\d+)[|](\\d)[|]([\\w-]+)[|](.+)$");

    // we are only allowed to fetch this file once per 24 hrs
    // see https://wiki.anidb.net/w/API#Anime_Titles
    Url animeList;

    try {
      animeList = new OnDiskCachedUrl("http://anidb.net/api/anime-titles.dat.gz", 2, TimeUnit.DAYS); // use 2 days instead of 1
    }
    catch (Exception e) {
      LOGGER.error("error getting AniDB index: {}", e.getMessage());
      return;
    }

    try (Scanner scanner = new Scanner(new GZIPInputStream(animeList.getInputStream()), StandardCharsets.UTF_8)) {
      while (scanner.hasNextLine()) {
        Matcher matcher = pattern.matcher(scanner.nextLine());

        if (matcher.matches()) {
          try {
            AniDBShow show = new AniDBShow();
            show.aniDbId = Integer.parseInt(matcher.group(1));
            show.language = matcher.group(3);
            show.title = matcher.group(4);

            List<AniDBShow> shows = showsForLookup.computeIfAbsent(show.aniDbId, k -> new ArrayList<>());
            shows.add(show);
          }
          catch (NumberFormatException e) {
            LOGGER.debug("could not parse anidb id - '{}'", e.getMessage());
          }
        }
      }
    }
    catch (InterruptedException | InterruptedIOException e) {
      // do not swallow these Exceptions
      Thread.currentThread().interrupt();
    }
    catch (IOException e) {
      LOGGER.error("error getting AniDB index: {}", e.getMessage());
      throw new ScrapeException(e);
    }
  }

  @Nullable
  protected Document requestAnimeDocument(MediaSearchAndScrapeOptions options) throws ScrapeException {
    // do we have an id from the options?
    String id = options.getIdAsString(providerInfo.getId());

    if (StringUtils.isEmpty(id)) {
      throw new MissingIdException(MediaMetadata.ANIDB);
    }

    // call API
    // http://api.anidb.net:9001/httpapi?request=anime&apikey&aid=4242
    Document doc;

    try {
      trackConnections();
      Url url = new OnDiskCachedUrl("http://api.anidb.net:9001/httpapi?request=anime&" + getApiKey() + "aid=" + id, 1, TimeUnit.DAYS);
      try (InputStream is = url.getInputStream()) {
        doc = Jsoup.parse(is, UrlUtil.UTF_8, "", Parser.xmlParser().settings(ParseSettings.htmlDefault));
      }

      // check if there is an error response
      Element error = doc.getElementsByTag("error").first();
      if (error != null) {
        // we got an error
        int httpCode = MetadataUtil.parseInt(error.attr("code"), 500);
        String message = error.ownText();
        throw new HttpException(httpCode, message);
      }
    }
    catch (InterruptedException | InterruptedIOException e) {
      // do not swallow these Exceptions
      Thread.currentThread().interrupt();
      return null;
    }
    catch (Exception e) {
      throw new ScrapeException(e);
    }

    return doc;
  }

  /**
   * Track connections and throttle if needed.
   */
  protected static synchronized void trackConnections() throws InterruptedException {
    long currentTime = System.currentTimeMillis();
    if (connectionCounter.count() == connectionCounter.maxSize()) {
      long oldestConnection = connectionCounter.getTailItem();
      if (oldestConnection > (currentTime - 2000)) {
        LOGGER.debug("connection limit reached, throttling...");
        do {
          AniDbMetadataProvider.class.wait(2000 - (currentTime - oldestConnection));
          currentTime = System.currentTimeMillis();
        } while (oldestConnection > (currentTime - 2000));
      }
    }

    currentTime = System.currentTimeMillis();
    connectionCounter.add(currentTime);
  }

  /**
   * Gets a general information about the metadata provider
   *
   * @return the provider info containing metadata of the provider
   */
  public MediaProviderInfo getProviderInfo() {
    return providerInfo;
  }

  /**
   * indicates whether this scraper is active or not (private and valid API key OR public to be active)
   *
   * @return true/false
   */
  public boolean isActive() {
    return isFeatureEnabled() && isApiKeyAvailable(null);
  }

  /****************************************************************************
   * helper class to buffer search results from AniDB
   ****************************************************************************/
  static class AniDBShow {
    public int    aniDbId;
    public String language;
    public String title;
  }
}
