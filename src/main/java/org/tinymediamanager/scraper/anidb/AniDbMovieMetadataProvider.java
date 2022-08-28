package org.tinymediamanager.scraper.anidb;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.FeatureNotEnabledException;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.movie.MovieSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.NothingFoundException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.IMovieMetadataProvider;
import org.tinymediamanager.scraper.util.Similarity;

/**
 * The elements for AniDB's Movie Metadata Provider. The majority of the work is done in {@link AniDbMetadataParser}
 *
 * @see <a href="https://anidb.net/">https://anidb.net/</a>
 * @see AniDbMetadataParser
 * @see AniDbMetadataProvider
 */
public class AniDbMovieMetadataProvider extends AniDbMetadataProvider implements IMovieMetadataProvider {

  public static final String  ID     = "anidb";
  private static final Logger LOGGER = LoggerFactory.getLogger(AniDbMovieMetadataProvider.class);

  @Override
  protected MediaProviderInfo createMediaProviderInfo() {
    MediaProviderInfo info = new MediaProviderInfo(
        ID, "movie", "aniDB", "<html><h3>aniDB</h3><br />AniDB stands for Anime DataBase. " + "AniDB is a non-profit anime database"
            + " that is open " + "freely to the public.</html>",
        AniDbMovieMetadataProvider.class.getResource("/org/tinymediamanager/scraper/anidb_net.png"), -10);

    // configure/load settings
    info.getConfig().addInteger("numberOfTags", 20);
    info.getConfig().addInteger("minimumTagsWeight", 200);
    info.getConfig().load();

    return info;
  }

  /**
   * Search for media.
   *
   * @param options
   *          the options
   *
   * @return a {@link SortedSet} of all search result (ordered descending)
   *
   * @throws ScrapeException
   *           any exception which can be thrown while scraping
   */
  @Override
  public SortedSet<MediaSearchResult> search(MovieSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("search(): {}", options);
    if (!isActive()) {
      throw new ScrapeException(new FeatureNotEnabledException(this));
    }

    synchronized (AniDbMovieMetadataProvider.class) {
      // first run: build up the anime name list
      if (showsForLookup.isEmpty()) {
        buildTitleHashMap();
      }
    }

    // detect the string to search
    String searchString = "";
    if (StringUtils.isNotEmpty(options.getSearchQuery())) {
      searchString = options.getSearchQuery();
    }

    // return an empty search result if no query provided
    if (StringUtils.isEmpty(searchString)) {
      return new TreeSet<>();
    }
    String finalSearchString = searchString;

    return showsForLookup.entrySet()
        .stream()
        .flatMap(entry -> entry.getValue().stream())
        .map(movie -> new MediaSearchResult.Builder(MediaType.MOVIE).providerId(providerInfo.getId())
            .id(String.valueOf(movie.aniDbId))
            .title(movie.title)
            .score(Similarity.compareStrings(movie.title, finalSearchString))
            .build())
        .collect(Collectors.toCollection(TreeSet::new));
  }

  /**
   * Gets the metadata.
   *
   * @param options
   *          the options
   *
   * @return the meta data
   *
   * @throws ScrapeException
   *           any exception which can be thrown while scraping
   * @throws MissingIdException
   *           indicates that there was no usable id to scrape
   * @throws NothingFoundException
   *           indicated that nothing has been found
   */
  @Override
  public MediaMetadata getMetadata(MovieSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("getMetadata(): {}", options);

    if (!isActive()) {
      throw new ScrapeException(new FeatureNotEnabledException(this));
    }

    Document doc = requestAnimeDocument(options);
    if (doc == null || doc.children().isEmpty()) {
      return null;
    }

    // do we have an id from the options?
    MediaMetadata md = new MediaMetadata(providerInfo.getId());
    String language = options.getLanguage().getLanguage();
    String id = options.getIdAsString(providerInfo.getId());

    md.setId(providerInfo.getId(), id);
    AniDbMetadataParser.fillAnimeMetadata(md, language, doc.child(0), providerInfo);

    // add static "Anime" genre
    md.addGenre(MediaGenres.ANIME);

    return md;
  }
}
