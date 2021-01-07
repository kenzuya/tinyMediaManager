package org.tinymediamanager.scraper.tvmaze;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;

import org.h2.util.StringUtils;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.core.tvshow.TvShowEpisodeSearchAndScrapeOptions;
import org.tinymediamanager.core.tvshow.TvShowSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.NothingFoundException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.ITvShowMetadataProvider;
import org.tinymediamanager.scraper.tvmaze.entities.Cast;
import org.tinymediamanager.scraper.tvmaze.entities.Episode;
import org.tinymediamanager.scraper.tvmaze.entities.Image;
import org.tinymediamanager.scraper.tvmaze.entities.Show;
import org.tinymediamanager.scraper.tvmaze.entities.Shows;

public class TvMazeTvShowMetadataProvider extends TvMazeMetadataProvider implements ITvShowMetadataProvider {

  private static final Logger LOGGER          = LoggerFactory.getLogger(TvMazeTvShowMetadataProvider.class);
  DateFormat                  premieredFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
  List<Episode>               episodeList;
  List<Image>                 imageList;
  int                         tvMazeId;

  @Override
  public MediaProviderInfo getProviderInfo() {
    return super.providerInfo;
  }

  @Override
  public boolean isActive() {
    return false;
  }

  @Override
  public MediaMetadata getMetadata(TvShowSearchAndScrapeOptions options) throws ScrapeException, MissingIdException, NothingFoundException {
    LOGGER.debug("getMetadata() TvShow: {}", options);
    String mazeId = (String) options.getIds().get("tvmaze");
    tvMazeId = Integer.parseInt(mazeId);
    MediaMetadata md = new MediaMetadata(getId());
    Show show = null;
    List<Cast> castList;

    // We have to search with the internal tvmaze id here to get
    // all the information :)

    // get show information
    LOGGER.info("========= BEGIN TVMAZE Scraping");
    try {
      show = controller.getMainInformation(tvMazeId);
    }
    catch (IOException e) {
      LOGGER.trace("could not get Main TvShow information: {}", e.getMessage());
    }

    if (show != null) {

      md.setId(MediaMetadata.IMDB, show.tvShowIds.imdb);
      md.setId(MediaMetadata.TVDB, show.tvShowIds.thetvdb);
      md.setId("tvrage", show.tvShowIds.tvrage);

      md.setTitle(show.title);

      try {
        md.setYear(parseYear(show.premiered));
      }
      catch (ParseException e) {
        LOGGER.trace("could not parse year: {}", e.getMessage());
      }

      try {
        md.setReleaseDate(premieredFormat.parse(show.premiered));
      }
      catch (ParseException e) {
        LOGGER.trace("could not parse releasedate: {}", e.getMessage());
      }

      md.setRuntime(show.runtime);

      for (String gen : show.genres) {
        MediaGenres genre = MediaGenres.getGenre(gen);
        md.addGenre(genre);
      }

      md.setPlot(Jsoup.parse(show.summary).text());
      md.setOriginalLanguage(show.language);

      MediaRating rating = new MediaRating("tvmaze");
      rating.setRating(show.rating.average);
      rating.setMaxValue(10);
      md.addRating(rating);

      // Get Cast
      try {
        castList = controller.getCast(tvMazeId);

        if (!castList.isEmpty()) {

          for (Cast cast : castList) {
            Person person = new Person(Person.Type.ACTOR);
            person.setName(cast.person.name);
            person.setProfileUrl(cast.person.image.original);
            person.setThumbUrl(cast.person.image.medium);
            md.addCastMember(person);
          }

        }

      }
      catch (IOException e) {
        LOGGER.trace("could not get cast information: {}", e.getMessage());
      }
    }
    return md;
  }

  @Override
  public MediaMetadata getMetadata(TvShowEpisodeSearchAndScrapeOptions options) throws ScrapeException, MissingIdException, NothingFoundException {
    LOGGER.debug("getMetadata() TvShowEpisode: {}", options);

    MediaMetadata md = new MediaMetadata(getId());
    MediaArtwork ma;

    // get episode number and season number
    int seasonNr = options.getIdAsIntOrDefault(MediaMetadata.SEASON_NR, -1);
    int episodeNr = options.getIdAsIntOrDefault(MediaMetadata.EPISODE_NR, -1);

    // Get all Episode and Season Information for the given TvShow
    try {
      episodeList = controller.getEpisodes(tvMazeId);
    }
    catch (IOException e) {
      LOGGER.trace("could not get Episode information: {}", e.getMessage());

    }

    // Get Image Information for the given TV Show
    try {
      imageList = controller.getImages(tvMazeId);
    }
    catch (IOException e) {
      LOGGER.trace("could not get Image information: {}", e.getMessage());
    }

    for (Image image : imageList) {
      switch (image.type) {
        case "poster":
          ma = new MediaArtwork(getId(), MediaArtwork.MediaArtworkType.POSTER);
          ma.setDefaultUrl(image.resolutions.original.url);
          md.addMediaArt(ma);
          break;

        case "background":
          ma = new MediaArtwork(getId(), MediaArtwork.MediaArtworkType.BACKGROUND);
          ma.setDefaultUrl(image.resolutions.original.url);
          md.addMediaArt(ma);
          break;

        case "banner":
          ma = new MediaArtwork(getId(), MediaArtwork.MediaArtworkType.BANNER);
          ma.setDefaultUrl(image.resolutions.original.url);
          md.addMediaArt(ma);
          break;
      }
    }

    // get the correct information
    for (Episode episode : episodeList) {

      // found the correct episode
      if (seasonNr == episode.season && episodeNr == episode.episode) {

        md.setTitle(episode.name);
        md.setPlot(Jsoup.parse(episode.summary).text());
        md.setEpisodeNumber(episode.episode);
        md.setSeasonNumber(episode.season);
        md.setRuntime(episode.runtime);
        try {
          md.setReleaseDate(premieredFormat.parse(episode.airdate));
          md.setYear(parseYear(episode.airdate));
        }
        catch (ParseException ignored) {
        }

        break;
      }
    }
    return md;
  }

  @Override
  public SortedSet<MediaSearchResult> search(TvShowSearchAndScrapeOptions options) throws ScrapeException {
    LOGGER.debug("search(): {}", options);
    SortedSet<MediaSearchResult> searchResults = new TreeSet<>();

    List<Shows> searchResult;

    try {
      searchResult = controller.getTvShowSearchResults(options.getSearchQuery());
    }
    catch (IOException e) {
      LOGGER.error("error searching: {}", e.getMessage());
      throw new ScrapeException(e);
    }

    if (searchResult == null) {
      LOGGER.warn("no result from tvmaze.com");
      return searchResults;
    }

    for (Shows shows : searchResult) {
      MediaSearchResult result = new MediaSearchResult(getId(), MediaType.TV_SHOW);
      result.setProviderId(getId());
      result.setTitle(shows.show.title);
      result.setScore((float) shows.show.rating.average);
      result.setUrl(shows.show.url);
      if (shows.show.image != null) {
        result.setPosterUrl(shows.show.image.original);
      }
      result.setOverview(Jsoup.parse(shows.show.summary).text());
      result.setIMDBId(shows.show.tvShowIds.imdb);
      result.setId("tvrage", String.valueOf(shows.show.tvShowIds.tvrage));
      result.setId(MediaMetadata.TVDB, String.valueOf(shows.show.tvShowIds.thetvdb));
      result.setId("tvmaze", String.valueOf(shows.show.id));
      result.setOriginalLanguage(shows.show.language);
      if (!StringUtils.isNullOrEmpty(shows.show.premiered)) {
        try {
          result.setYear(parseYear(shows.show.premiered));
        }
        catch (ParseException ignored) {
        }
      }

      // calculate score
      if ((org.apache.commons.lang3.StringUtils.isNotBlank(options.getImdbId()) && options.getImdbId().equals(result.getIMDBId()))
          || String.valueOf(options.getTmdbId()).equals(result.getId())) {
        LOGGER.debug("perfect match by ID - set score to 1");
        result.setScore(1);
      }
      else {
        // calculate the score by comparing the search result with the search options
        result.calculateScore(options);
      }

      searchResults.add(result);

    }

    return searchResults;
  }

  @Override
  public List<MediaMetadata> getEpisodeList(TvShowSearchAndScrapeOptions options) throws ScrapeException, MissingIdException {
    List<MediaMetadata> list = new ArrayList<>();

    // get the correct information
    for (Episode episode : episodeList) {
      MediaMetadata md = new MediaMetadata(getId());
      // found the correct episode
      md.setTitle(episode.name);
      md.setPlot(Jsoup.parse(episode.summary).text());
      md.setEpisodeNumber(episode.episode);
      md.setSeasonNumber(episode.season);
      md.setRuntime(episode.runtime);
      try {
        md.setReleaseDate(premieredFormat.parse(episode.airdate));
        md.setYear(parseYear(episode.airdate));
      }
      catch (ParseException ignored) {
      }
      list.add(md);
    }

    return list;
  }

  @Override
  protected String getSubId() {
    return "tvshow";
  }
}
