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
package org.tinymediamanager.ui.movies;

import java.awt.FontMetrics;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import javax.swing.ImageIcon;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.MediaCertification;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.TmmDateFormat;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaEntity;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.movie.MovieComparator;
import org.tinymediamanager.core.movie.MovieEdition;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.util.StrgUtils;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.components.table.TmmTableFormat;
import org.tinymediamanager.ui.renderer.DateTableCellRenderer;
import org.tinymediamanager.ui.renderer.RightAlignTableCellRenderer;
import org.tinymediamanager.ui.renderer.RuntimeTableCellRenderer;

/**
 * The MovieTableFormat. Used as definition for the movie table in the movie module
 *
 * @author Manuel Laggner
 */
public class MovieTableFormat extends TmmTableFormat<Movie> {

  public MovieTableFormat() {

    Comparator<Movie> movieComparator = new MovieComparator();
    Comparator<Movie> originalTitleComparator = new MovieComparator() {
      @Override
      public int compare(Movie movie1, Movie movie2) {
        if (stringCollator != null) {
          String titleMovie1 = StrgUtils.normalizeString(movie1.getOriginalTitleSortable().toLowerCase(Locale.ROOT));
          String titleMovie2 = StrgUtils.normalizeString(movie2.getOriginalTitleSortable().toLowerCase(Locale.ROOT));
          return stringCollator.compare(titleMovie1, titleMovie2);
        }
        return movie1.getOriginalTitleSortable().toLowerCase(Locale.ROOT).compareTo(movie2.getOriginalTitleSortable().toLowerCase(Locale.ROOT));
      }
    };
    Comparator<String> stringComparator = new StringComparator();
    Comparator<Float> floatComparator = new FloatComparator();
    Comparator<ImageIcon> imageComparator = new ImageComparator();
    Comparator<Date> dateComparator = new DateComparator();
    Comparator<String> videoFormatComparator = new VideoFormatComparator();
    Comparator<String> fileSizeComparator = new FileSizeComparator();
    Comparator<Integer> integerComparator = new IntegerComparator();
    Comparator<MediaCertification> certificationComparator = new CertificationComparator();

    FontMetrics fontMetrics = getFontMetrics();

    /*
     * title
     */
    Column col = new Column(TmmResourceBundle.getString("metatag.title"), "title", movie -> movie, Movie.class);
    col.setColumnComparator(movieComparator);
    col.setCellRenderer(new MovieBorderTableCellRenderer());
    col.setColumnTooltip(showTooltip(Movie::getTitleSortable));
    addColumn(col);

    /*
     * original title (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.originaltitle"), "originalTitle", movie -> movie, Movie.class);
    col.setColumnComparator(originalTitleComparator);
    col.setCellRenderer(new MovieBorderTableCellRenderer());
    col.setColumnTooltip(showTooltip(Movie::getOriginalTitleSortable));
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * sorttitle (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.sorttitle"), "sortTitle", Movie::getSortTitle, String.class);
    col.setColumnComparator(stringComparator);
    col.setColumnResizeable(true);
    col.setColumnTooltip(showTooltip(Movie::getSortTitle));
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * year
     */
    col = new Column(TmmResourceBundle.getString("metatag.year"), "year", MediaEntity::getYear, Integer.class);
    col.setColumnComparator(integerComparator);
    col.setColumnResizeable(false);
    col.setMinWidth((int) (fontMetrics.stringWidth("2000") * 1.3f + 10));
    addColumn(col);

    /*
     * date added (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.releasedate"), "releaseDate", Movie::getReleaseDate, Date.class);
    col.setColumnComparator(dateComparator);
    col.setHeaderIcon(IconManager.DATE_AIRED);
    col.setCellRenderer(new DateTableCellRenderer());
    col.setColumnResizeable(false);
    col.setDefaultHidden(true);
    try {
      Date date = StrgUtils.parseDate("2012-12-12");
      col.setMinWidth((int) (fontMetrics.stringWidth(TmmDateFormat.MEDIUM_DATE_FORMAT.format(date)) * 1.2f + 10));
    }
    catch (Exception ignored) {
    }
    addColumn(col);

    /*
     * file name (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.filename"), "filename", movie -> movie.getMainVideoFile().getFilename(), String.class);
    col.setColumnComparator(stringComparator);
    col.setColumnResizeable(true);
    col.setColumnTooltip(showTooltip(movie -> movie.getMainVideoFile().getFilename()));
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * folder name (hidden per default)
     */
    Function<Movie, String> pathFunction = movie -> movie.getPathNIO().toString();
    col = new Column(TmmResourceBundle.getString("metatag.path"), "path", pathFunction, String.class);
    col.setColumnComparator(stringComparator);
    col.setColumnResizeable(true);
    col.setColumnTooltip(showTooltip(pathFunction));
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * movie set (hidden per default)
     */
    Function<Movie, String> movieSetFunction = movie -> movie.getMovieSet() == null ? null : movie.getMovieSet().getTitleSortable();
    col = new Column(TmmResourceBundle.getString("metatag.movieset"), "movieset", movieSetFunction, String.class);
    col.setColumnComparator(stringComparator);
    col.setColumnResizeable(true);
    col.setColumnTooltip(showTooltip(movieSetFunction));
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * rating
     */
    col = new Column(TmmResourceBundle.getString("metatag.rating"), "rating", movie -> getRating(movie.getRating()), Float.class);
    col.setColumnComparator(floatComparator);
    col.setHeaderIcon(IconManager.RATING);
    col.setCellRenderer(new RightAlignTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth((int) (fontMetrics.stringWidth("99.9") * 1.2f + 10));
    col.setColumnTooltip(showTooltip(
        movie -> movie.getRating().getRating() + " (" + movie.getRating().getVotes() + " " + TmmResourceBundle.getString("metatag.votes") + ")"));
    addColumn(col);

    /*
     * votes (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.votes"), "votes",
        movie -> movie.getRating() == MediaMetadata.EMPTY_RATING ? null : movie.getRating().getVotes(), Integer.class);
    col.setColumnComparator(integerComparator);
    col.setHeaderIcon(IconManager.VOTES);
    col.setCellRenderer(new RightAlignTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth((int) (fontMetrics.stringWidth("1000000") * 1.2f + 10));
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * user rating
     */
    col = new Column(TmmResourceBundle.getString("metatag.userrating"), "userrating", movie -> getRating(movie.getUserRating()), Float.class);
    col.setColumnComparator(floatComparator);
    col.setHeaderIcon(IconManager.USER_RATING);
    col.setCellRenderer(new RightAlignTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth((int) (fontMetrics.stringWidth("10.0") * 1.2f + 10));
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * imdb rating
     */
    col = new Column(TmmResourceBundle.getString("metatag.rating") + " - IMDb", "imdb", movie -> getRating(movie.getRating(MediaMetadata.IMDB)),
        Float.class);
    col.setColumnComparator(floatComparator);
    col.setHeaderIcon(IconManager.IMDB);
    col.setCellRenderer(new RightAlignTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth((int) (fontMetrics.stringWidth("9.9") * 1.2f + 10));
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * rotten tomatoes rating
     */
    col = new Column(TmmResourceBundle.getString("metatag.rating") + " - Rotten Tomatoes", "rottenTomatoes",
        movie -> getRatingInteger(movie.getRating("tomatometerallcritics")), Integer.class);
    col.setColumnComparator(integerComparator);
    col.setHeaderIcon(IconManager.ROTTEN_TOMATOES);
    col.setCellRenderer(new RightAlignTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth((int) (fontMetrics.stringWidth("100") * 1.2f + 10));
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * metascore rating
     */
    col = new Column(TmmResourceBundle.getString("metatag.rating") + " - Metascore", "metacritic",
        movie -> getRatingInteger(movie.getRating("metacritic")), Integer.class);
    col.setColumnComparator(integerComparator);
    col.setHeaderIcon(IconManager.METASCORE);
    col.setCellRenderer(new RightAlignTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth((int) (fontMetrics.stringWidth("100") * 1.2f + 10));
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * tmdb rating
     */
    col = new Column(TmmResourceBundle.getString("metatag.rating") + " - TMDB", "tmdb", movie -> getRating(movie.getRating(MediaMetadata.TMDB)),
        Float.class);
    col.setColumnComparator(floatComparator);
    col.setHeaderIcon(IconManager.TMDB);
    col.setCellRenderer(new RightAlignTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth((int) (fontMetrics.stringWidth("9.9") * 1.2f + 10));
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * certification (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.certification"), "certification", Movie::getCertification, MediaCertification.class);
    col.setColumnComparator(certificationComparator);
    col.setHeaderIcon(IconManager.CERTIFICATION);
    col.setColumnResizeable(true);
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * date added (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.dateadded"), "dateAdded", MediaEntity::getDateAddedForUi, Date.class);
    col.setColumnComparator(dateComparator);
    col.setHeaderIcon(IconManager.DATE_ADDED);
    col.setCellRenderer(new DateTableCellRenderer());
    col.setColumnResizeable(false);
    col.setDefaultHidden(true);
    try {
      Date date = StrgUtils.parseDate("2012-12-12");
      col.setMinWidth((int) (fontMetrics.stringWidth(TmmDateFormat.MEDIUM_DATE_FORMAT.format(date)) * 1.2f + 10));
    }
    catch (Exception ignored) {
    }
    addColumn(col);

    /*
     * file creation date (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.filecreationdate"), "fileCreationDate", movie -> movie.getMainVideoFile().getDateCreated(),
        Date.class);
    col.setColumnComparator(dateComparator);
    col.setHeaderIcon(IconManager.DATE_CREATED);
    col.setCellRenderer(new DateTableCellRenderer());
    col.setColumnResizeable(false);
    col.setDefaultHidden(true);
    try {
      Date date = StrgUtils.parseDate("2012-12-12");
      col.setMinWidth((int) (fontMetrics.stringWidth(TmmDateFormat.MEDIUM_DATE_FORMAT.format(date)) * 1.2f + 10));
    }
    catch (Exception ignored) {
    }
    addColumn(col);

    /*
     * runtime (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.runtime") + " [min]", "runtime", Movie::getRuntime, Integer.class);
    col.setColumnComparator(integerComparator);
    col.setHeaderIcon(IconManager.RUNTIME);
    col.setCellRenderer(new RuntimeTableCellRenderer(RuntimeTableCellRenderer.FORMAT.MINUTES));
    col.setColumnResizeable(false);
    col.setMinWidth((int) (fontMetrics.stringWidth("200") * 1.2f + 10));
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * runtime HH:MM (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.runtime") + " [hh:mm]", "runtime2", Movie::getRuntime, Integer.class);
    col.setColumnComparator(integerComparator);
    col.setHeaderIcon(IconManager.RUNTIME);
    col.setCellRenderer(new RuntimeTableCellRenderer(RuntimeTableCellRenderer.FORMAT.HOURS_MINUTES));
    col.setColumnResizeable(false);
    col.setMinWidth((int) (fontMetrics.stringWidth("4:00") * 1.2f + 10));
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * video format (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.format"), "videoFormat", Movie::getMediaInfoVideoFormat, String.class);
    col.setColumnComparator(videoFormatComparator);
    col.setHeaderIcon(IconManager.VIDEO_FORMAT);
    col.setColumnResizeable(false);
    col.setMinWidth((int) (fontMetrics.stringWidth("1080p") * 1.2f + 10));
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * aspect ratio (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.aspectratio"), "aspectratio", Movie::getMediaInfoAspectRatio, Float.class);
    col.setColumnComparator(floatComparator);
    col.setHeaderIcon(IconManager.ASPECT_RATIO);
    col.setCellRenderer(new RightAlignTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth((int) (fontMetrics.stringWidth("1.78") * 1.2f + 10));
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * HDR (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.hdr"), "hdr", movie -> getCheckIcon(movie.isVideoInHDR()), ImageIcon.class);
    col.setColumnComparator(imageComparator);
    col.setHeaderIcon(IconManager.HDR);
    col.setColumnResizeable(false);
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * video codec (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.videocodec"), "videoCodec", Movie::getMediaInfoVideoCodec, String.class);
    col.setColumnComparator(stringComparator);
    col.setHeaderIcon(IconManager.VIDEO_CODEC);
    col.setMinWidth((int) (fontMetrics.stringWidth("MPEG-2") * 1.2f + 10));
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * video bitrate (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.videobitrate"), "videoBitrate", Movie::getMediaInfoVideoBitrate, Integer.class);
    col.setColumnComparator(integerComparator);
    col.setHeaderIcon(IconManager.VIDEO_BITRATE);
    col.setMinWidth((int) (fontMetrics.stringWidth("20000") * 1.2f + 10));
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * audio codec and channels(hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.audio"), "audio", movie -> {
      List<MediaFile> videos = movie.getMediaFiles(MediaFileType.VIDEO);
      if (!videos.isEmpty()) {
        MediaFile mediaFile = videos.get(0);
        if (StringUtils.isNotBlank(mediaFile.getAudioCodec())) {
          return mediaFile.getAudioCodec() + " " + mediaFile.getAudioChannels();
        }
      }
      return "";
    }, String.class);
    col.setColumnComparator(stringComparator);
    col.setHeaderIcon(IconManager.AUDIO);
    col.setMinWidth((int) (fontMetrics.stringWidth("DTS 7ch") * 1.2f + 10));
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * main video file size (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.size"), "fileSize", movie -> {
      long size = 0;
      for (MediaFile mf : movie.getMediaFiles(MediaFileType.VIDEO)) {
        size += mf.getFilesize();
      }
      return (int) (size / (1000.0 * 1000.0)) + " M";
    }, String.class);
    col.setColumnComparator(fileSizeComparator);
    col.setHeaderIcon(IconManager.FILE_SIZE);
    col.setCellRenderer(new RightAlignTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth((int) (fontMetrics.stringWidth("50000M") * 1.2f + 10));
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * Edition (hidden per default)
     */
    Function<Movie, String> movieEditionFunction = movie -> movie.getEdition() == null || movie.getEdition() == MovieEdition.NONE ? null
        : movie.getEdition().toString();
    col = new Column(TmmResourceBundle.getString("metatag.edition"), "edition", movieEditionFunction, String.class);
    col.setColumnComparator(stringComparator);
    col.setHeaderIcon(IconManager.EDITION);
    col.setColumnTooltip(showTooltip(movieEditionFunction));
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * Source (hidden per default)
     */
    Function<Movie, String> mediaSourceFunction = movie -> movie.getMediaSource() == null ? null : movie.getMediaSource().toString();
    col = new Column(TmmResourceBundle.getString("metatag.source"), "mediaSource", mediaSourceFunction, String.class);
    col.setColumnComparator(stringComparator);
    col.setHeaderIcon(IconManager.SOURCE);
    col.setColumnTooltip(showTooltip(mediaSourceFunction));
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * new indicator
     */
    col = new Column(TmmResourceBundle.getString("movieextendedsearch.newmovies"), "new", movie -> getNewIcon(movie.isNewlyAdded()), ImageIcon.class);
    col.setColumnComparator(imageComparator);
    col.setHeaderIcon(IconManager.NEW);
    col.setColumnResizeable(false);
    addColumn(col);

    /*
     * 3D (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.3d"), "video3d", movie -> getCheckIcon(movie.isVideoIn3D()), ImageIcon.class);
    col.setColumnComparator(imageComparator);
    col.setHeaderIcon(IconManager.VIDEO_3D);
    col.setColumnResizeable(false);
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * NFO
     */
    col = new Column(TmmResourceBundle.getString("tmm.nfo"), "nfo", movie -> getCheckIcon(movie.getHasNfoFile()), ImageIcon.class);
    col.setColumnComparator(imageComparator);
    col.setHeaderIcon(IconManager.NFO);
    col.setColumnResizeable(false);
    addColumn(col);

    /*
     * images
     */
    col = new Column(TmmResourceBundle.getString("tmm.images"), "images", movie -> getCheckIcon(movie.getHasImages()), ImageIcon.class);
    col.setColumnComparator(imageComparator);
    col.setHeaderIcon(IconManager.IMAGES);
    col.setColumnResizeable(false);
    addColumn(col);

    /*
     * trailer
     */
    col = new Column(TmmResourceBundle.getString("tmm.trailer"), "trailer", movie -> getCheckIcon(movie.getHasTrailer()), ImageIcon.class);
    col.setColumnComparator(imageComparator);
    col.setHeaderIcon(IconManager.TRAILER);
    col.setColumnResizeable(false);
    addColumn(col);

    /*
     * subtitles
     */
    col = new Column(TmmResourceBundle.getString("tmm.subtitles"), "subtitles", movie -> getCheckIcon(movie.getHasSubtitles()), ImageIcon.class);
    col.setColumnComparator(imageComparator);
    col.setHeaderIcon(IconManager.SUBTITLES);
    col.setColumnResizeable(false);
    addColumn(col);

    /*
     * watched
     */
    col = new Column(TmmResourceBundle.getString("metatag.watched"), "watched", movie -> getCheckIcon(movie.isWatched()), ImageIcon.class);
    col.setColumnComparator(imageComparator);
    col.setHeaderIcon(IconManager.WATCHED);
    col.setColumnResizeable(false);
    addColumn(col);
  }

  private Float getRating(MediaRating rating) {
    if (rating == MediaMetadata.EMPTY_RATING) {
      return null;
    }
    return rating.getRating();
  }

  private Integer getRatingInteger(MediaRating rating) {
    if (rating == MediaMetadata.EMPTY_RATING) {
      return null;
    }
    return Math.round(rating.getRating());
  }

  private <E> Function<E, String> showTooltip(Function<E, String> tooltipFunction) {
    return movie -> {
      if (MovieModuleManager.getInstance().getSettings().isShowMovieTableTooltips()) {
        return tooltipFunction.apply(movie);
      }
      else {
        return null;
      }
    };
  }
}
