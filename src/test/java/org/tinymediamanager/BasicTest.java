package org.tinymediamanager;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Random;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.MediaCertification;
import org.tinymediamanager.core.MediaFileHelper;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.MediaSource;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.entities.MediaFileAudioStream;
import org.tinymediamanager.core.entities.MediaFileSubtitle;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.entities.MediaTrailer;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.core.movie.MovieEdition;
import org.tinymediamanager.core.movie.MovieList;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.entities.MovieSet;
import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.license.License;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

public class BasicTest {

  private static final String LOREM = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Praesent vel lacus libero. Ut vel lacus erat. Maecenas maximus vestibulum ante at efficitur. Sed id ex eget purus commodo feugiat. Suspendisse ultricies felis sed interdum luctus. Class aptent taciti sociosqu ad litora torquent per conubia nostra, per inceptos himenaeos. Nunc et scelerisque nibh. Donec maximus nunc nunc, non commodo nulla rhoncus id. Curabitur pharetra maximus tellus non porta. Ut vehicula elit nec ante elementum, ut semper ligula consectetur.";

  protected static void setLicenseKey() throws Exception {
    String key = "";
    // take the env variable for the license key (offered in CI)
    if (StringUtils.isNotBlank(System.getenv("TMM_BUILD_LICENSE"))) {
      key = System.getenv("TMM_BUILD_LICENSE");
    }
    else {
      // try to read it from a local file
      try {
        key = Utils.readFileToString(Paths.get("target/test-classes/tmm_build_license"));
      }
      catch (Exception e) {
        // just not crashing
      }
    }

    if (StringUtils.isNotBlank(key)) {
      License.getInstance().setLicenseCode(key);
    }
  }

  // own method to get some logging ;)
  public static void assertEqual(Object expected, Object actual) {
    try {
      Assert.assertEquals(expected, actual);
      // System.out.println(actual + " - passed");
    }
    catch (AssertionError e) {
      System.err.println(actual + " - FAILED: " + e.getMessage());
      throw e;
    }
  }

  public static void assertEqual(String message, Object expected, Object actual) {
    try {
      Assert.assertEquals(message, expected, actual);
      // System.out.println(expected + " - passed");
    }
    catch (AssertionError e) {
      System.err.println(expected + " - FAILED: " + message + "(" + e.getMessage() + ")");
      throw e;
    }
  }

  public static void setTraceLogging() {
    LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
    lc.getLogger("org.tinymediamanager").setLevel(Level.TRACE);
  }

  public static String getSettingsFolder() {
    StackTraceElement ste = Thread.currentThread().getStackTrace()[2];
    return "target/testdata/" + ste.getClassName();
  }

  public static void deleteSettingsFolder() {
    StackTraceElement ste = Thread.currentThread().getStackTrace()[2];
    try {
      Utils.deleteDirectoryRecursive(Paths.get("target", "testdata", ste.getClassName()));
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static Date getRandomDate() {
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    cal.set(1950, 1, 1, 0, 0, 0);
    Date startInclusive = cal.getTime();
    Date endExclusive = Calendar.getInstance().getTime();
    long startMillis = startInclusive.getTime();
    long endMillis = endExclusive.getTime();
    long randomMillisSinceEpoch = ThreadLocalRandom.current().nextLong(startMillis, endMillis);
    return new Date(randomMillisSinceEpoch);
  }

  public static void createFakeMovie(String title) {
    Movie movie = new Movie();
    movie.setDbId(getUUID(title)); // fixate

    movie.setTitle(title);
    movie.setPath("/media/movies/" + title);
    movie.setOriginalTitle("Original " + title);
    movie.setSortTitle(title);
    movie.setRating(new MediaRating(MediaRating.NFO, (float) ThreadLocalRandom.current().nextDouble(1.0d, 9.0d), 5987, 10));
    movie.setYear(1950 + ThreadLocalRandom.current().nextInt(20, 60));
    movie.setTop250(ThreadLocalRandom.current().nextInt(250));
    movie.setPlot(LOREM);
    movie.setTagline("Wish granted");
    movie.setRuntime(70 + ThreadLocalRandom.current().nextInt(20, 50));
    movie.setImdbId("tt0103639");
    movie.setTmdbId(ThreadLocalRandom.current().nextInt(1000, 5000));
    movie.setId("trakt", ThreadLocalRandom.current().nextInt(1000, 5000));
    movie.setProductionCompany("Walt Disney");
    movie.setCountry("US");
    movie.setCertification(MediaCertification.US_G);
    movie.setReleaseDate(getRandomDate());

    MediaTrailer trailer = new MediaTrailer();
    trailer.setUrl("https://trailer");
    trailer.setInNfo(true);
    movie.addToTrailer(Collections.singletonList(trailer));

    MovieSet movieSet = new MovieSet();
    movieSet.setTitle(title + " Collection");
    movieSet.setPlot(title + " plot");
    movie.setMovieSet(movieSet);

    // MF video
    MediaFile mf = new MediaFile();
    mf.setType(MediaFileType.VIDEO);
    mf.setFilename(title + ".mkv");
    mf.setVideoCodec("h264");
    mf.setVideoHeight(720);
    mf.setVideoWidth(1280);
    mf.setDuration(3600);
    mf.setOverallBitRate(3500);
    mf.setVideo3DFormat(MediaFileHelper.VIDEO_3D_SBS);

    MediaFileAudioStream audio = new MediaFileAudioStream();
    audio.setCodec("AC3");
    audio.setLanguage("en");
    audio.setAudioChannels(6);
    mf.setAudioStreams(Collections.singletonList(audio));

    MediaFileSubtitle sub = new MediaFileSubtitle();
    sub.setLanguage("de");
    mf.addSubtitle(sub);
    movie.addToMediaFiles(mf);

    // MF poster
    mf = new MediaFile(Paths.get("target/test-classes/dummy-poster.jpg"));
    movie.addToMediaFiles(mf);
    // MF fanart
    mf = new MediaFile(Paths.get("target/test-classes/dummy-fanart.jpg"));
    movie.addToMediaFiles(mf);

    movie.setWatched(true);
    movie.addToGenres(Arrays.asList(MediaGenres.values()[new Random().nextInt(MediaGenres.values().length)],
        MediaGenres.values()[new Random().nextInt(MediaGenres.values().length)],
        MediaGenres.values()[new Random().nextInt(MediaGenres.values().length)],
        MediaGenres.values()[new Random().nextInt(MediaGenres.values().length)]));
    movie
        .addToWriters(Arrays.asList(new Person(Person.Type.WRITER, "Ted Elliott", "Writer"), new Person(Person.Type.WRITER, "Terry Rossio", "Writer"),
            new Person(Person.Type.WRITER, "Ron Clements", "Writer"), new Person(Person.Type.WRITER, "John Jusker", "Writer"),
            new Person(Person.Type.DIRECTOR, "Ron Clements", "Director"), new Person(Person.Type.DIRECTOR, "John Jusker", "Director")));
    movie.addToTags(Arrays.asList("Disney", "Oriental"));

    movie.addToActors(Arrays.asList(new Person(Person.Type.ACTOR, "Scott Weinger", "Aladdin 'Al' (voice)"),
        new Person(Person.Type.ACTOR, "Robin Williams", "Genie (voice)")));

    movie.addToProducers(
        Arrays.asList(new Person(Person.Type.PRODUCER, "Ron Clements", "Producer"), new Person(Person.Type.PRODUCER, "Donald W. Ernst", "Producer")));

    movie.setSpokenLanguages("en");
    movie.setMediaSource(MediaSource.values()[new Random().nextInt(MediaSource.values().length)]);
    movie.setEdition(MovieEdition.values()[new Random().nextInt(MovieEdition.values().length)]);

    MovieList.getInstance().addMovie(movie);
    movie.saveToDb();
    System.out.println("Created movie " + movie.getDbId());
  }

  public static void createFakeShow(String title) {
    TvShow tvShow = new TvShow();
    tvShow.setDbId(getUUID(title)); // fixate

    tvShow.setTitle(title);
    tvShow.setPath("/media/tvshows/" + title);
    tvShow.setYear(1950 + ThreadLocalRandom.current().nextInt(20, 60));
    tvShow.setRating(new MediaRating(MediaRating.NFO, (float) ThreadLocalRandom.current().nextDouble(1.0d, 9.0d), 5987, 10));
    tvShow.setCertification(MediaCertification.US_TVPG);
    tvShow.addToGenres(Arrays.asList(MediaGenres.values()[new Random().nextInt(MediaGenres.values().length)],
        MediaGenres.values()[new Random().nextInt(MediaGenres.values().length)],
        MediaGenres.values()[new Random().nextInt(MediaGenres.values().length)],
        MediaGenres.values()[new Random().nextInt(MediaGenres.values().length)]));
    tvShow.setTvdbId(String.valueOf(ThreadLocalRandom.current().nextInt(1000, 5000)));
    tvShow.setFirstAired(getRandomDate());
    tvShow.setProductionCompany("FOX (US)");
    tvShow.setPlot(LOREM);

    MediaFile mf = new MediaFile();
    // show MF poster
    mf = new MediaFile(Paths.get("target/test-classes/dummy-poster.jpg"));
    tvShow.addToMediaFiles(mf);
    // show MF fanart
    mf = new MediaFile(Paths.get("target/test-classes/dummy-fanart.jpg"));
    tvShow.addToMediaFiles(mf);

    // ========= EPISODE start =========
    TvShowEpisode episode = new TvShowEpisode();
    episode.setTvShow(tvShow);
    episode.setTitle(title + "-EP");
    episode.setSeason(1);
    episode.setEpisode(2);
    episode.setDvdSeason(3);
    episode.setDvdEpisode(4);
    episode.setTitle("Don't Pet the Teacher");
    episode.setYear(1950 + ThreadLocalRandom.current().nextInt(20, 60));
    episode.setFirstAired(getRandomDate());
    episode.setMediaSource(MediaSource.BLURAY);
    episode.setPlot(LOREM);

    mf = new MediaFile();
    mf.setType(MediaFileType.VIDEO);
    mf.setFilename(title + ".mkv");
    mf.setVideoCodec("h264");
    mf.setVideoHeight(720);
    mf.setVideoWidth(1280);
    mf.setDuration(70 + ThreadLocalRandom.current().nextInt(20, 60));
    mf.setOverallBitRate(3500);
    mf.setVideo3DFormat(MediaFileHelper.VIDEO_3D_SBS);

    MediaFileAudioStream audio = new MediaFileAudioStream();
    audio.setCodec("AC3");
    audio.setLanguage("en");
    audio.setAudioChannels(6);
    mf.setAudioStreams(Collections.singletonList(audio));

    MediaFileSubtitle sub = new MediaFileSubtitle();
    sub.setLanguage("de");
    mf.addSubtitle(sub);

    episode.addToMediaFiles(mf);

    // EP MF poster
    mf = new MediaFile(Paths.get("target/test-classes/dummy-poster.jpg"), MediaFileType.THUMB);
    episode.addToMediaFiles(mf);
    episode.saveToDb();

    tvShow.addEpisode(episode);
    // ========= EPISODE end =========

    TvShowList.getInstance().addTvShow(tvShow);
    tvShow.saveToDb();
    System.out.println("Created show " + tvShow.getDbId());
  }

  public static UUID getUUID(String uuid) {
    return UUID.nameUUIDFromBytes(uuid.getBytes());
  }
}
