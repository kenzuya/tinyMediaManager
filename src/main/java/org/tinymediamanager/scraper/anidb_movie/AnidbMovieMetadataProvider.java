package org.tinymediamanager.scraper.anidb_movie;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.ParseSettings;
import org.jsoup.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.core.movie.MovieSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.NothingFoundException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.http.OnDiskCachedUrl;
import org.tinymediamanager.scraper.http.Url;
import org.tinymediamanager.scraper.interfaces.IMovieImdbMetadataProvider;
import org.tinymediamanager.scraper.interfaces.IMovieMetadataProvider;
import org.tinymediamanager.scraper.interfaces.IMovieTmdbMetadataProvider;
import org.tinymediamanager.scraper.util.RingBuffer;
import org.tinymediamanager.scraper.util.Similarity;
import org.tinymediamanager.scraper.util.StrgUtils;
import org.tinymediamanager.scraper.util.UrlUtil;

//TODO: Consolidate the common code of two Anidb metadata providers
//TODO: Finish adding Javadoc
//TODO: Add more unit tests?
public class AnidbMovieMetadataProvider implements IMovieMetadataProvider {

    public static final String ID = "anidb";
    private static final Logger LOGGER = LoggerFactory.getLogger(AnidbMovieMetadataProvider.class);
    private static final Map<String, IMovieMetadataProvider> COMPATIBLE_SCRAPERS = new HashMap<>();
    private static final String IMAGE_SERVER = "http://img7.anidb.net/pics/anime/";
    // flood: pager every 2 seconds
    // protection: https://wiki.anidb.net/w/HTTP_API_Definition
    private static final RingBuffer<Long> connectionCounter = new RingBuffer<>(1);

    private final MediaProviderInfo providerInfo;
    private final HashMap<Integer, List<AnidbMovieMetadataProvider.AniDBShow>> moviesForLookup = new HashMap<>();

    public AnidbMovieMetadataProvider() {
        providerInfo = createMediaProviderInfo();
    }

    public static void addProvider(IMovieMetadataProvider provider) {
        // called for each plugin implementing that interface
        if (!provider.getId()
                     .equals(ID) && !COMPATIBLE_SCRAPERS.containsKey(provider.getId()) && (provider instanceof IMovieTmdbMetadataProvider || provider instanceof IMovieImdbMetadataProvider)) {
            COMPATIBLE_SCRAPERS.put(provider.getId(), provider);
        }
    }

    private MediaProviderInfo createMediaProviderInfo() {
        MediaProviderInfo info = new MediaProviderInfo(ID,
                                                       "tvshow",
                                                       "aniDB",
                                                       "<html><h3>aniDB</h3><br />AniDB stands for Anime DataBase. " +
                                                               "AniDB is a non-profit anime database that is open " +
                                                               "freely to the public.</html>",
                                                       AnidbMovieMetadataProvider.class.getResource(
                                                               "/org/tinymediamanager/scraper/anidb_net.png")
        );

        // configure/load settings
        info.getConfig().addInteger("numberOfTags", 20);
        info.getConfig().addInteger("minimumTagsWeight", 200);
        info.getConfig().load();

        return info;
    }

    /**
     * Gets a general information about the metadata provider
     *
     * @return the provider info containing metadata of the provider
     */
    @Override
    public MediaProviderInfo getProviderInfo() {
        return providerInfo;
    }

    /**
     * indicates whether this scraper is active or not (private and valid API key OR public to be active)
     *
     * @return true/false
     */
    @Override
    public boolean isActive() {
        return isFeatureEnabled() && isApiKeyAvailable(null);
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
            if (moviesForLookup.isEmpty()) {
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

        return moviesForLookup.entrySet()
                              .stream()
                              .flatMap(entry -> entry.getValue().stream())
                              .map(movie -> new MediaSearchResult.Builder(MediaType.MOVIE).providerId(providerInfo.getId())
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

        MediaMetadata md = new MediaMetadata(providerInfo.getId());
        String language = options.getLanguage().getLanguage();

        // do we have an id from the options?
        String id = options.getIdAsString(providerInfo.getId());

        if (StringUtils.isEmpty(id)) {
            throw new MissingIdException("anidb");
        }

        // call API
        // http://api.anidb.net:9001/httpapi?request=anime&apikey&aid=4242
        Document doc;

        try {
            trackConnections();
            Url url =
                    new OnDiskCachedUrl("http://api.anidb.net:9001/httpapi?request=anime&" + getApiKey() + "aid=" + id,
                                          1,
                                          TimeUnit.DAYS
            );
            try (InputStream is = url.getInputStream()) {
                doc = Jsoup.parse(is, UrlUtil.UTF_8, "", Parser.xmlParser().settings(ParseSettings.htmlDefault));
            }
        } catch (InterruptedException | InterruptedIOException e) {
            // do not swallow these Exceptions
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            throw new ScrapeException(e);
        }

        if (doc.children().isEmpty()) {
            throw new NothingFoundException();
        }

        md.setId(providerInfo.getId(), id);

        fillAnimeMetadata(md, language, doc.child(0));

        // add static "Anime" genre
        md.addGenre(MediaGenres.ANIME);

        return md;
    }

    /**
     * <pre>{@code
     * <anime id="777" restricted="false">
     *     <episodecount>1</episodecount>
     *     <startdate>...</startdate>
     *     <titles>...</titles>
     *     <creators>...</creators>
     *     <description>...</description>
     *     <ratings>...</ratings>
     *     <picture>...</picture>
     *     <tags>...</tags>
     *     <characters>...</characters>
     *     <episodes>...</episodes>
     * </anime>
     * }</pre>
     *
     * @param md
     * @param language
     *         Language of desired Title and Language to set on Artwork. This must match the {@code xml:lang} attribute
     *         exactly.
     * @param anime
     *         XML Element for {@code <characters></characters>} as shown in example above.
     */
    private void fillAnimeMetadata(MediaMetadata md, String language, Element anime) {
        for (Element e : anime.children()) {
            switch (e.tagName()) {
                case "startdate":
                    fillDateMetadata(md, e);
                    break;
                case "titles":
                    fillTitleMetadata(md, language, e);
                    break;
                case "description":
                    md.setPlot(e.text());
                    break;
                case "ratings":
                    fillRatingsMetadata(md, e);
                    break;
                case "tags":
                    fillTagsMetadata(md, e);
                    break;
                case "picture":
                    fillArtworkMetadata(md, language, e);
                    break;
                case "characters":
                    fillActorsMetadata(md, e);
                    break;
                default:
            }
        }
    }

    /**
     * <pre>{@code <startdate>1989-07-15</startdate>}</pre>
     *
     * @param md
     * @param startDate
     *         XML Element for {@code <startdate></startdate>} as shown in example above.
     */
    private void fillDateMetadata(MediaMetadata md, Element startDate) {
        try {
            Date date = StrgUtils.parseDate(startDate.text());
            md.setReleaseDate(date);

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            md.setYear(calendar.get(Calendar.YEAR));
        } catch (ParseException ex) {
            LOGGER.debug("could not parse date: {}", startDate.text());
        }
    }

    private void fillArtworkMetadata(MediaMetadata md, String language, Element e) {
        // Poster
        MediaArtwork ma = new MediaArtwork(providerInfo.getId(), MediaArtwork.MediaArtworkType.POSTER);
        ma.setPreviewUrl(IMAGE_SERVER + e.text());
        ma.setDefaultUrl(IMAGE_SERVER + e.text());
        ma.setOriginalUrl(IMAGE_SERVER + e.text());
        ma.setLanguage(language);
        md.addMediaArt(ma);
    }

    /**
     * Example of XML returned from Animdb call:
     * <pre>{@code
     *     <characters>
     *         <character id="9011" type="main character in" update="2013-08-10">
     *             <rating votes="114">7.83</rating>
     *             <name>Izumi Noa</name>
     *             <gender>female</gender>
     *             <charactertype id="1">Character</charactertype>
     *             <description>Born in Tomakomai, Hokkaido in 1978, she is the pilot of the first Ingram in
     *             Unit 2...</description>
     *             <picture>141281.jpg</picture>
     *             <seiyuu id="3940" picture="54324.jpg">Tominaga Miina</seiyuu>
     *         </character>
     *         <character id="9016" type="secondary cast in" update="2013-08-13">
     *             <rating votes="203">9.25</rating>
     *             <name>Gotou Kiichi</name>
     *             <gender>male</gender>
     *             <charactertype id="1">Character</charactertype>
     *             <description>Gotou is the Captain of Unit 2 and was born in the Taito Ward in Tokyo.
     *             Although he....</description>
     *             <picture>141450.jpg</picture>
     *             <seiyuu id="3383" picture="24325.jpg">Oobayashi Ryuusuke</seiyuu>
     *         </character>
     *     </characters>
     *   }</pre>
     * <p>
     * NOTE: Animdb's name field maps to TMM's role field while Anidb's seiyuu text maps to TMM's name. This is b/c
     * seiyuu means voice actor in Japanese.
     * </p>
     *
     * @param md
     * @param characters
     *         XML Element for <pre>&lt;characters&gt;&lt;/characters&gt;</pre> as shown in example above.
     */
    private void fillActorsMetadata(MediaMetadata md, Element characters) {
        for (Element character : characters.children()) {
            Person member = new Person(Person.Type.ACTOR);
            for (Element characterInfo : character.children()) {
                // NOTE: Animdb's name field maps to TMM's role field while Anidb's seiyuu text maps to TMM's name
                if ("name".equalsIgnoreCase(characterInfo.tagName())) {
                    member.setRole(characterInfo.text());
                }
                if ("seiyuu".equalsIgnoreCase(characterInfo.tagName())) {
                    member.setName(characterInfo.text());
                    String image = characterInfo.attr("picture");
                    if (StringUtils.isNotBlank(image)) {
                        member.setThumbUrl("http://img7.anidb.net/pics/anime/" + image);
                    }
                }
            }
            md.addCastMember(member);
        }
    }

    /**
     * <pre>{@code
     *     <ratings>
     *         <permanent count="1558">7.40</permanent>
     *         <temporary count="1562">7.53</temporary>
     *         <review count="1">8.00</review>
     *     </ratings>
     * }</pre>
     * <p>
     * Note: Only the `temporary` rating is used.
     *
     * @param md
     * @param ratings
     *         XML Element for {@code <ratings></ratings>} as shown in example above.
     */
    private void fillRatingsMetadata(MediaMetadata md, Element ratings) {
        for (Element rating : ratings.children()) {
            if ("temporary".equalsIgnoreCase(rating.tagName())) {
                try {
                    MediaRating mediaRating = new MediaRating("anidb");
                    mediaRating.setRating(Float.parseFloat(rating.text()));
                    mediaRating.setVotes(Integer.parseInt(rating.attr("count")));
                    mediaRating.setMaxValue(10);
                    md.addRating(mediaRating);
                    break;
                } catch (NumberFormatException ex) {
                    LOGGER.debug("could not rating: {} - {}", rating.text(), rating.attr("count"));
                }
            }
        }
    }

    /**
     * <pre>{@code
     *     <tags>
     *         <tag id="308" parentid="2611" infobox="true" weight="400" localspoiler="false" globalspoiler="false" verified="true" update="2017-12-17">
     *             <name>detective</name>
     *             <description>A detective is an investigator, generally either a member of a law enforcement agency or as an individual working in the capacity of private investigators, tasked with solving crimes and other mysteries, such as disappearances, by examining and evaluating clues and records in order to solve the mystery; for a crime, this would mean uncovering the criminal`s identity and/or whereabouts. In some police systems, a detective position is achieved by passing a written test after a person completes the requirements for being a police officer; in others, detectives are college graduates who join directly from civilian life without first serving as uniformed officers.
     *                 Source: Wikipedia</description>
     *             <picurl>210906.jpg</picurl>
     *         </tag>
     *         <tag id="2274" parentid="2638" weight="0" localspoiler="false" globalspoiler="false" verified="true" update="2018-03-30">
     *             <name>robot</name>
     *             <description>A robot is an automatically guided machine, able to do tasks on its own. Another common characteristic is that by its appearance or movements, a robot often conveys a sense that it has intent or agency of its own.
     *                 Not to be confused with android (human-like robot).</description>
     *             <picurl>36563.jpg</picurl>
     *         </tag>
     *     <tags>
     * }</pre>
     *
     * @param md
     * @param tags
     *         XML Element for {@code <tags></tags>}> as shown in example above.
     */
    private void fillTagsMetadata(MediaMetadata md, Element tags) {
        Integer maxTags = providerInfo.getConfig().getValueAsInteger("numberOfTags");
        Integer minWeight = providerInfo.getConfig().getValueAsInteger("minimumTagsWeight");

        for (Element tag : tags.children()) {
            Element name = tag.getElementsByTag("name").first();
            int weight = 0;
            try {
                weight = Integer.parseInt(tag.attr("weight"));
            } catch (Exception ex) {
                LOGGER.trace("Could not parse tags weight: {}", ex.getMessage());
            }
            if (name != null && weight >= minWeight) {
                md.addTag(name.text());
                if (md.getTags().size() >= maxTags) {
                    break;
                }
            }
        }
    }

    /**
     * <pre>{@code
     *     <titles>
     *         <title xml:lang="x-jat" type="main">Kidou Keisatsu Patlabor</title>
     *         <title xml:lang="ja" type="synonym">機動警察パトレイバー 劇場版</title>
     *         <title xml:lang="en" type="synonym">Patlabor Movie 1</title>
     *         <title xml:lang="en" type="synonym">Mobile Police Patlabor: The Movie</title>
     *         <title xml:lang="x-jat" type="synonym">Kidou Keisatsu Patlabor Gekijouban</title>
     *         <title xml:lang="ja" type="official">機動警察パトレイバー PATLABOR THE MOBILE POLICE</title>
     *         <title xml:lang="en" type="official">Patlabor the Movie</title>
     *         <title xml:lang="de" type="official">Patlabor - The Movie</title>
     *     </titles>
     * }</pre>
     *
     * @param md
     * @param language
     *         The desired language of the Title. This must match the {@code xml:lang} attribute exactly.
     * @param titles
     *         XML Element for {@code <titles></titles>} as shown in example above.
     */
    private void fillTitleMetadata(MediaMetadata md, String language, Element titles) {
        String titleEN = "";
        String titleScraperLangu = "";
        String titleMain = "";

        for (Element title : titles.children()) {
            // store first title if neither the requested one nor the english one available

            // do not work further with short titles/synonyms
            if ("short".equals(title.attr("type")) || "synonym".equals(title.attr("type"))) {
                continue;
            }

            // main title aka original title
            if ("main".equalsIgnoreCase(title.attr("type"))) {
                titleMain = title.text();
            }

            // store the english one for fallback
            if ("en".equalsIgnoreCase(title.attr("xml:lang"))) {
                titleEN = title.text();
            }

            // search for the requested one
            if (language.equalsIgnoreCase(title.attr("xml:lang"))) {
                titleScraperLangu = title.text();
            }
        }

        if (StringUtils.isNotBlank(titleMain)) {
            md.setOriginalTitle(titleMain);
        }

        if (StringUtils.isNotBlank(titleScraperLangu)) {
            md.setTitle(titleScraperLangu);
        } else if (StringUtils.isNotBlank(titleEN)) {
            md.setTitle(titleEN);
        } else { // QUESTION: This gets set even if `titleMain` is blank. Is that ok?
            md.setTitle(titleMain);
        }
    }

    /**
     * build up the hashmap for a fast title search
     */
    private void buildTitleHashMap() throws ScrapeException {
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
            animeList = new OnDiskCachedUrl("http://anidb.net/api/anime-titles.dat.gz",
                                            7,
                                            // TODO: change back to 2 days when done testing.
                                            TimeUnit.DAYS
            ); // use 2 days instead of 1
        } catch (Exception e) {
            LOGGER.error("error getting AniDB index: {}", e.getMessage());
            return;
        }

        try (InputStream is = animeList.getInputStream(); Scanner scanner = new Scanner(new GZIPInputStream(is),
                                                                                        StandardCharsets.UTF_8
        )) {
            while (scanner.hasNextLine()) {
                Matcher matcher = pattern.matcher(scanner.nextLine());

                if (matcher.matches()) {
                    AnidbMovieMetadataProvider.AniDBShow movie = new AnidbMovieMetadataProvider.AniDBShow();
                    movie.aniDbId = Integer.parseInt(matcher.group(1));
                    movie.language = matcher.group(3);
                    movie.title = matcher.group(4);

                    List<AnidbMovieMetadataProvider.AniDBShow> movies = moviesForLookup.computeIfAbsent(movie.aniDbId,
                                                                                                        k -> new ArrayList<>()
                    );
                    movies.add(movie);
                }
            }
        } catch (InterruptedException | InterruptedIOException e) {
            // do not swallow these Exceptions
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            LOGGER.error("error getting AniDB index: {}", e.getMessage());
            throw new ScrapeException(e);
        }
    }

    /**
     * Track connections and throttle if needed.
     */
    private static synchronized void trackConnections() throws InterruptedException {
        long currentTime = System.currentTimeMillis();
        if (connectionCounter.count() == connectionCounter.maxSize()) {
            long oldestConnection = connectionCounter.getTailItem();
            if (oldestConnection > (currentTime - 2000)) {
                LOGGER.debug("connection limit reached, throttling...");
                do {
                    AnidbMovieMetadataProvider.class.wait(2000 - (currentTime - oldestConnection));
                    currentTime = System.currentTimeMillis();
                } while (oldestConnection > (currentTime - 2000));
            }
        }

        currentTime = System.currentTimeMillis();
        connectionCounter.add(currentTime);
    }

    /****************************************************************************
     * helper class to buffer search results from AniDB
     ****************************************************************************/
    private static class AniDBShow {

        int aniDbId;
        String language;
        String title;
    }
}
