package org.tinymediamanager.scraper.tpdb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.MediaCertification;
import org.tinymediamanager.core.entities.MediaTrailer;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.core.movie.MovieSearchAndScrapeOptions;
import org.tinymediamanager.scraper.ArtworkSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.TrailerSearchAndScrapeOptions;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.NothingFoundException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.IMovieArtworkProvider;
import org.tinymediamanager.scraper.interfaces.IMovieMetadataProvider;
import org.tinymediamanager.scraper.interfaces.IMovieSetArtworkProvider;
import org.tinymediamanager.scraper.interfaces.IMovieTrailerProvider;
import org.tinymediamanager.scraper.tpdb.entities.*;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class TpdbMovieMetadataProvider extends TpdbMetadataProvider implements IMovieMetadataProvider, IMovieArtworkProvider, IMovieSetArtworkProvider, IMovieTrailerProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(TpdbMovieMetadataProvider.class);

    @Override
    protected String getSubId() {
        return "movie";
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    public SortedSet<MediaSearchResult> search(MovieSearchAndScrapeOptions options) throws ScrapeException {
        LOGGER.debug("search(): {}", options);

        initAPI();

        SortedSet<MediaSearchResult> results = new TreeSet<>();

        SceneSearch search;
        try {
            search = controller.getScenesFromTitle(options.getSearchQuery());
        } catch (Exception e) {
            LOGGER.error("error searching: {}", e.getMessage());
            throw new ScrapeException(e);
        }

        if (search == null) {
            LOGGER.warn("no result found");
            throw new NothingFoundException();
        }

        float score = 100.0F;
        for (SceneEntity scene : search.data) {
            MediaSearchResult data = new MediaSearchResult(getId(), MediaType.MOVIE);

            data.setId(getId(), scene.id);
            data.setTitle(scene.title);

            Date date = getDate(scene.date);
            if (date != null) {
                int year = getYear(date);
                if (year > 0) {
                    data.setYear(year);
                }
            }

            data.setPosterUrl(scene.posters.small);

            data.setScore(score--);
            results.add(data);
        }

        return results;
    }

    @Override
    public MediaMetadata getMetadata(MovieSearchAndScrapeOptions options) throws ScrapeException {
        LOGGER.debug("getMetadata(): {}", options);

        SceneEntity scene;
        try {
            scene = getScene(options.getIdAsString(getId()));
        } catch (Exception e) {
            throw new ScrapeException(e);
        }

        return setMediaMetadata(scene);
    }

    @Override
    public List<MediaArtwork> getArtwork(ArtworkSearchAndScrapeOptions options) throws ScrapeException {
        LOGGER.debug("getArtwork(): {}", options);

        List<MediaArtwork> artworks = new ArrayList<>();

        SceneEntity scene;
        try {
            scene = getScene(options.getIdAsString(getId()));
        } catch (Exception e) {
            throw new ScrapeException(e);
        }

        MediaArtwork artwork = new MediaArtwork(getId(), MediaArtwork.MediaArtworkType.POSTER);
        artwork.setDefaultUrl(scene.posters.large);
        artwork.setPreviewUrl(scene.posters.small);
        artworks.add(artwork);

        artwork = new MediaArtwork(getId(), MediaArtwork.MediaArtworkType.BACKGROUND);
        artwork.setDefaultUrl(scene.background.large);
        artwork.setPreviewUrl(scene.posters.small);
        artworks.add(artwork);

        return artworks;
    }

    @Override
    public List<MediaTrailer> getTrailers(TrailerSearchAndScrapeOptions options) throws ScrapeException {
        LOGGER.debug("getTrailers(): {}", options);

        SceneEntity scene;
        try {
            scene = getScene(options.getIdAsString(getId()));
        } catch (Exception e) {
            throw new ScrapeException(e);
        }

        List<MediaTrailer> trailers = new ArrayList<>();

        MediaTrailer trailer = new MediaTrailer();
        trailer.setName(scene.title);
        trailer.setProvider(scene.site.name);
        trailer.setUrl(scene.trailer);
        trailers.add(trailer);

        return trailers;
    }

    private SceneEntity getScene(String id) throws ScrapeException {
        initAPI();

        SceneGet search;
        try {
            search = controller.getSceneFromId(id);
        } catch (Exception e) {
            LOGGER.error("error scene: {}", e.getMessage());
            throw new ScrapeException(e);
        }

        if (search == null) {
            LOGGER.warn("no result found");
            throw new NothingFoundException();
        }

        return search.data;
    }

    private MediaMetadata setMediaMetadata(SceneEntity scene) {
        MediaMetadata result = new MediaMetadata(getId());

        result.setId(getId(), scene.id);
        result.setCertifications(getCertifications());
        result.setTitle(scene.title);
        result.setPlot(scene.description);

        try {
            result.setProductionCompanies(getProductionCompanies(scene.site));
        } catch (Exception e) {
            LOGGER.error("error site: {}", e.getMessage());
        }

        Date date = getDate(scene.date);
        if (date != null) {
            result.setReleaseDate(date);
            int year = getYear(date);
            if (year > 0) {
                result.setYear(year);
            }
        }

        result.setCastMembers(getCastMembers(scene.performers));
        result.setTags(scene.tags.stream().map(o -> o.name).sorted().collect(Collectors.toList()));

        return result;
    }

    private Date getDate(String date) {
        Date date_obj = null;
        try {
            date_obj = new SimpleDateFormat("yyyy-MM-dd").parse(date);
        } catch (Exception e) {
            LOGGER.error("error date: {}", e.getMessage());
        }

        return date_obj;
    }

    private int getYear(Date date) {
        int year = 0;
        try {
            Calendar calendar = new GregorianCalendar();
            calendar.setTime(date);
            year = calendar.get(Calendar.YEAR);
        } catch (Exception e) {
            LOGGER.error("error year: {}", e.getMessage());
        }

        return year;
    }

    private List<MediaCertification> getCertifications() {
        List<MediaCertification> certifications = new ArrayList<>();

        certifications.add(MediaCertification.US_R);

        return certifications;
    }

    private List<Person> getCastMembers(List<PerformerEntity> performers) {
        List<Person> castMembers = new ArrayList<>();

        for (PerformerEntity performer : performers) {
            Person person = new Person();
            person.setType(Person.Type.ACTOR);
            person.setName(performer.name);
            person.setThumbUrl(performer.image);
            person.setProfileUrl("https://metadataapi.net/performers/" + performer.slug);

            castMembers.add(person);
        }

        return castMembers;
    }

    private List<String> getProductionCompanies(SiteEntity site) throws ScrapeException {
        List<String> productionCompanies = new ArrayList<>();

        productionCompanies.add(site.name);

        if (site.parent_id != null && !site.id.equals(site.parent_id)) {
            SiteGet parent_site;
            try {
                parent_site = controller.getSiteFromId(Integer.toString(site.parent_id));
            } catch (Exception e) {
                LOGGER.error("error scene: {}", e.getMessage());
                throw new ScrapeException(e);
            }

            productionCompanies.add(parent_site.data.name);
        }

        if (site.network_id != null && !site.id.equals(site.network_id)) {
            SiteGet network_site;
            try {
                network_site = controller.getSiteFromId(Integer.toString(site.network_id));
            } catch (Exception e) {
                LOGGER.error("error scene: {}", e.getMessage());
                throw new ScrapeException(e);
            }

            productionCompanies.add(network_site.data.name);
        }

        return productionCompanies;
    }
}
