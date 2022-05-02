package org.tinymediamanager.scraper.anidb_movie;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.movie.MovieSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.anidb.AniDbMetadataParser;
import org.tinymediamanager.scraper.anidb.AniDbMetadataProvider;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.NothingFoundException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.IMovieImdbMetadataProvider;
import org.tinymediamanager.scraper.interfaces.IMovieMetadataProvider;
import org.tinymediamanager.scraper.interfaces.IMovieTmdbMetadataProvider;
import org.tinymediamanager.scraper.util.Similarity;

//TODO: Consolidate the common code of two Anidb metadata providers
//TODO: Add more unit tests?
public class AnidbMovieMetadataProvider extends AniDbMetadataProvider implements IMovieMetadataProvider {

    public static final String ID = "anidb";
    private static final Logger LOGGER = LoggerFactory.getLogger(AnidbMovieMetadataProvider.class);
    private static final Map<String, IMovieMetadataProvider> COMPATIBLE_SCRAPERS = new HashMap<>();

    public static void addProvider(IMovieMetadataProvider provider) {
        // called for each plugin implementing that interface
        if (!provider.getId()
                     .equals(ID) && !COMPATIBLE_SCRAPERS.containsKey(provider.getId()) && (provider instanceof IMovieTmdbMetadataProvider || provider instanceof IMovieImdbMetadataProvider)) {
            COMPATIBLE_SCRAPERS.put(provider.getId(), provider);
        }
    }

    @Override
    protected MediaProviderInfo createMediaProviderInfo() {
        MediaProviderInfo info = new MediaProviderInfo(
                ID,
                "movie",
                "aniDB",
                "<html><h3>aniDB</h3><br />AniDB stands for Anime DataBase. " + "AniDB is a non-profit anime database" +
                        " that is open " + "freely to the public.</html>",
                AnidbMovieMetadataProvider.class.getResource("/org/tinymediamanager/scraper/anidb_net.png")
        );

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
     *         the options
     *
     * @return a {@link SortedSet} of all search result (ordered descending)
     *
     * @throws ScrapeException
     *         any exception which can be thrown while scraping
     */
    @Override
    public SortedSet<MediaSearchResult> search(MovieSearchAndScrapeOptions options) throws ScrapeException {
        LOGGER.debug("search(): {}", options);
        // CURRENT: FIXME:
        //        if (!isActive()) {
        //            throw new ScrapeException(new FeatureNotEnabledException(this));
        //        }

        synchronized (AnidbMovieMetadataProvider.class) {
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
                             .map(movie -> new MediaSearchResult.Builder(MediaType.MOVIE)
                                                .providerId(providerInfo.getId())
                                                 .id(String.valueOf(movie.aniDbId))
                                                 .title(movie.title)
                                                 .score(Similarity.compareStrings(
                                                         movie.title,
                                                         finalSearchString
                                                 ))
                                                 .build()
                             )
                             .collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public String getApiKey() {
        // CURRENT: FIXME:
        return "client=tinymediamanager&clientver=2&protover=1&";
    }

    /**
     * Gets the meta data.
     *
     * @param options
     *         the options
     *
     * @return the meta data
     *
     * @throws ScrapeException
     *         any exception which can be thrown while scraping
     * @throws MissingIdException
     *         indicates that there was no usable id to scrape
     * @throws NothingFoundException
     *         indicated that nothing has been found
     */
    @Override
    public MediaMetadata getMetadata(MovieSearchAndScrapeOptions options) throws ScrapeException {
        LOGGER.debug("getMetadata(): {}", options);
        // CURRENT: FIXME:
        //        if (!isActive()) {
        //            throw new ScrapeException(new FeatureNotEnabledException(this));
        //        }

        Document doc = requestAnimeDocument(options);
        if (doc == null || doc.children().isEmpty())
            return null;

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
