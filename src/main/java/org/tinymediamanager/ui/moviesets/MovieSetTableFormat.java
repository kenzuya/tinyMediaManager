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
package org.tinymediamanager.ui.moviesets;

import java.awt.FontMetrics;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.swing.ImageIcon;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.ScraperMetadataConfig;
import org.tinymediamanager.core.TmmDateFormat;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaEntity;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.movie.MovieList;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieScraperMetadataConfig;
import org.tinymediamanager.core.movie.MovieSetScraperMetadataConfig;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.entities.MovieSet;
import org.tinymediamanager.scraper.util.StrgUtils;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.components.tree.TmmTreeNode;
import org.tinymediamanager.ui.components.treetable.TmmTreeTableFormat;
import org.tinymediamanager.ui.renderer.DateTableCellRenderer;
import org.tinymediamanager.ui.renderer.RightAlignTableCellRenderer;
import org.tinymediamanager.ui.renderer.RuntimeTableCellRenderer;

/**
 * The class MovieSetTableFormat is used to define the columns for the movie set tree table
 *
 * @author Manuel Laggner
 */
public class MovieSetTableFormat extends TmmTreeTableFormat<TmmTreeNode> {

  private final MovieList movieList;

  public MovieSetTableFormat() {

    movieList = MovieModuleManager.getInstance().getMovieList();

    FontMetrics fontMetrics = getFontMetrics();

    Comparator<String> integerComparator = (o1, o2) -> {
      int value1 = 0;
      int value2 = 0;

      try {
        value1 = Integer.parseInt(o1);
      }
      catch (Exception ignored) {
        // do nothing
      }
      try {
        value2 = Integer.parseInt(o2);
      }
      catch (Exception ignored) {
        // do nothing
      }

      return Integer.compare(value1, value2);
    };
    Comparator<String> floatComparator = (o1, o2) -> {
      float value1 = 0;
      float value2 = 0;

      try {
        value1 = Float.parseFloat(o1);
      }
      catch (Exception ignored) {
        // do nothing
      }
      try {
        value2 = Float.parseFloat(o2);
      }
      catch (Exception ignored) {
        // do nothing
      }

      return Float.compare(value1, value2);
    };
    Comparator<String> videoFormatComparator = new VideoFormatComparator();
    Comparator<String> fileSizeComparator = new FileSizeComparator();
    Comparator<ImageIcon> imageComparator = new ImageComparator();
    Comparator<String> stringComparator = new StringComparator();
    Comparator<Date> dateTimeComparator = new DateTimeComparator();

    /*
     * movie count
     */
    Column col = new Column(TmmResourceBundle.getString("movieset.moviecount"), "seasons", this::getMovieCount, String.class);
    col.setHeaderIcon(IconManager.COUNT);
    col.setColumnResizeable(false);
    col.setMinWidth((int) (fontMetrics.stringWidth("99") * 1.2f));
    col.setColumnComparator(integerComparator);
    addColumn(col);

    /*
     * year
     */
    col = new Column(TmmResourceBundle.getString("metatag.year"), "year", this::getYear, String.class);
    col.setColumnComparator(integerComparator);
    col.setColumnResizeable(false);
    // col.setMinWidth((int) (fontMetrics.stringWidth("2000") * 1.3f + 10));
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * file name (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.filename"), "filename", this::getVideoFilename, String.class);
    col.setColumnComparator(stringComparator);
    col.setColumnResizeable(true);
    col.setColumnTooltip(this::getVideoFilename);
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * folder name (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.path"), "path", this::getMoviePath, String.class);
    col.setColumnComparator(stringComparator);
    col.setColumnResizeable(true);
    col.setColumnTooltip(this::getMoviePath);
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * rating
     */
    col = new Column(TmmResourceBundle.getString("metatag.rating"), "rating", this::getRating, String.class);
    col.setHeaderIcon(IconManager.RATING);
    col.setCellRenderer(new RightAlignTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth((int) (fontMetrics.stringWidth("99.9") * 1.2f));
    col.setDefaultHidden(true);
    col.setColumnComparator(floatComparator);
    addColumn(col);

    /*
     * user rating
     */
    col = new Column(TmmResourceBundle.getString("metatag.userrating"), "userrating", this::getUserRating, String.class);
    col.setColumnComparator(floatComparator);
    col.setHeaderIcon(IconManager.RATING);
    col.setCellRenderer(new RightAlignTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth((int) (fontMetrics.stringWidth("99.9") * 1.2f + 10));
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * votes (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.votes"), "votes", this::getVotes, String.class);
    col.setColumnComparator(integerComparator);
    col.setHeaderIcon(IconManager.VOTES);
    col.setCellRenderer(new RightAlignTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth((int) (fontMetrics.stringWidth("1000000") * 1.2f + 10));
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * date added (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.dateadded"), "dateAdded", this::getDateAdded, Date.class);
    col.setColumnComparator(dateTimeComparator);
    col.setHeaderIcon(IconManager.DATE_ADDED);
    col.setCellRenderer(new DateTableCellRenderer());
    col.setColumnResizeable(false);
    col.setDefaultHidden(true);
    try {
      Date date = StrgUtils.parseDate("2012-12-12");
      col.setMinWidth((int) (fontMetrics.stringWidth(TmmDateFormat.MEDIUM_DATE_FORMAT.format(date)) * 1.2f + 10));
    }
    catch (Exception ignored) {
      // ignore
    }
    addColumn(col);

    /*
     * runtime (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.runtime") + " [min]", "runtime", this::getRuntime, Integer.class);
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
    col = new Column(TmmResourceBundle.getString("metatag.runtime") + " [hh:mm]", "runtime2", this::getRuntime, Integer.class);
    col.setColumnComparator(integerComparator);
    col.setHeaderIcon(IconManager.RUNTIME);
    col.setCellRenderer(new RuntimeTableCellRenderer(RuntimeTableCellRenderer.FORMAT.HOURS_MINUTES));
    col.setColumnResizeable(false);
    col.setMinWidth((int) (fontMetrics.stringWidth("4:00") * 1.2f + 10));
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * video format
     */
    col = new Column(TmmResourceBundle.getString("metatag.format"), "format", this::getMediaInfoVideoFormat, String.class);
    col.setHeaderIcon(IconManager.VIDEO_FORMAT);
    col.setColumnResizeable(false);
    col.setMinWidth((int) (fontMetrics.stringWidth("1080p") * 1.2f));
    col.setColumnComparator(videoFormatComparator);
    col.setCellRenderer(new RightAlignTableCellRenderer());
    addColumn(col);

    /*
     * video codec (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.videocodec"), "videoCodec", this::getMediaInfoVideoCodec, String.class);
    col.setColumnComparator(stringComparator);
    col.setHeaderIcon(IconManager.VIDEO_CODEC);
    col.setMinWidth((int) (fontMetrics.stringWidth("MPEG-2") * 1.2f + 10));
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * video bitrate (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.videobitrate"), "videoBitrate", this::getMediaInfoVideoBitrate, String.class);
    col.setColumnComparator(integerComparator);
    col.setHeaderIcon(IconManager.VIDEO_BITRATE);
    col.setMinWidth((int) (fontMetrics.stringWidth("20000") * 1.2f + 10));
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * audio codec and channels(hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.audio"), "audio", this::getAudioInformation, String.class);
    col.setColumnComparator(stringComparator);
    col.setHeaderIcon(IconManager.AUDIO);
    col.setMinWidth((int) (fontMetrics.stringWidth("DTS 7ch") * 1.2f + 10));
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * main video file size (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.videofilesize"), "fileSize", this::getFileSize, String.class);
    col.setHeaderIcon(IconManager.FILE_SIZE);
    col.setCellRenderer(new RightAlignTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth((int) (fontMetrics.stringWidth("50000M") * 1.2f));
    col.setColumnComparator(fileSizeComparator);
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * total file size (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.totalfilesize"), "totalFileSize", this::getTotalFileSize, String.class);
    col.setHeaderIcon(IconManager.FILE_SIZE);
    col.setCellRenderer(new RightAlignTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth((int) (fontMetrics.stringWidth("50000M") * 1.2f));
    col.setDefaultHidden(true);
    col.setColumnComparator(fileSizeComparator);
    addColumn(col);

    /*
     * metadata
     */
    col = new Column(TmmResourceBundle.getString("tmm.metadata"), "metadata", this::hasMetadata, ImageIcon.class);
    col.setHeaderIcon(IconManager.NFO);
    col.setColumnResizeable(false);
    col.setColumnComparator(imageComparator);
    col.setColumnTooltip(this::hasMetadataTooltip);
    addColumn(col);

    /*
     * images
     */
    col = new Column(TmmResourceBundle.getString("tmm.images"), "images", this::hasImages, ImageIcon.class);
    col.setHeaderIcon(IconManager.IMAGES);
    col.setColumnResizeable(false);
    col.setColumnComparator(imageComparator);
    col.setColumnTooltip(this::hasImagesTooltip);
    addColumn(col);

    /*
     * watched
     */
    col = new Column(TmmResourceBundle.getString("metatag.watched"), "watched", this::isWatched, ImageIcon.class);
    col.setHeaderIcon(IconManager.WATCHED);
    col.setColumnResizeable(false);
    col.setColumnComparator(imageComparator);
    addColumn(col);
  }

  @Override
  public String getColumnName(int i) {
    if (i == NODE_COLUMN) {
      return TmmResourceBundle.getString("metatag.title");
    }
    else {
      return super.getColumnName(i);
    }
  }

  private String getMovieCount(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof MovieSet) {
      int size = ((MovieSet) userObject).getMovies().size();
      if (size > 0) {
        return String.valueOf(size);
      }
    }
    return null;
  }

  private String getRating(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof Movie) {
      MediaRating mediaRating = ((MediaEntity) userObject).getRating();
      if (mediaRating != null && mediaRating.getRating() > 0) {
        return String.valueOf(mediaRating.getRating());
      }
    }
    return null;
  }

  private String getFileSize(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof MovieSet.MovieSetMovie) {
      return null;
    }
    else if (userObject instanceof Movie) {
      long size = ((Movie) userObject).getVideoFilesize();
      return (int) (size / (1000.0 * 1000.0)) + " M";
    }
    return null;
  }

  private String getTotalFileSize(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof MovieSet.MovieSetMovie) {
      return null;
    }
    else if (userObject instanceof Movie) {
      long size = ((Movie) userObject).getTotalFilesize();
      return (int) (size / (1000.0 * 1000.0)) + " M";
    }
    return null;
  }

  private ImageIcon hasMetadata(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof MovieSet) {
      return getCheckIcon(movieList.detectMissingMetadata((MovieSet) userObject).isEmpty());
    }
    else if (userObject instanceof MovieSet.MovieSetMovie) {
      return null;
    }
    else if (userObject instanceof Movie) {
      return getCheckIcon(movieList.detectMissingMetadata((Movie) userObject).isEmpty());
    }
    return null;
  }

  private String hasMetadataTooltip(TmmTreeNode node) {
    if (!MovieModuleManager.getInstance().getSettings().isShowMovieSetTableTooltips()) {
      return null;
    }

    Object userObject = node.getUserObject();

    if (userObject instanceof MovieSet) {
      List<MovieSetScraperMetadataConfig> values = new ArrayList<>();
      if (MovieModuleManager.getInstance().getSettings().isMovieSetDisplayAllMissingMetadata()) {
        for (MovieSetScraperMetadataConfig config : MovieSetScraperMetadataConfig.values()) {
          if (config.isMetaData() || config.isCast()) {
            values.add(config);
          }
        }
      }
      else {
        values.addAll(MovieModuleManager.getInstance().getSettings().getMovieSetCheckMetadata());
      }
      return getMissingToolTip(movieList.detectMissingFields((MovieSet) userObject, values));
    }
    else if (userObject instanceof MovieSet.MovieSetMovie) {
      return null;
    }
    else if (userObject instanceof Movie) {
      List<MovieScraperMetadataConfig> values = new ArrayList<>();
      if (MovieModuleManager.getInstance().getSettings().isMovieDisplayAllMissingMetadata()) {
        for (MovieScraperMetadataConfig config : MovieScraperMetadataConfig.values()) {
          if (config.isMetaData() || config.isCast()) {
            values.add(config);
          }
        }
      }
      else {
        values.addAll(MovieModuleManager.getInstance().getSettings().getMovieCheckMetadata());
      }
      return getMissingToolTip(movieList.detectMissingFields((Movie) userObject, values));
    }
    return null;
  }

  private ImageIcon hasImages(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof MovieSet) {
      return getCheckIcon(movieList.detectMissingArtwork((MovieSet) userObject).isEmpty());
    }
    else if (userObject instanceof MovieSet.MovieSetMovie) {
      return null;
    }
    else if (userObject instanceof Movie) {
      return getCheckIcon(movieList.detectMissingArtwork((Movie) userObject).isEmpty());
    }
    return null;
  }

  private String hasImagesTooltip(TmmTreeNode node) {
    if (!MovieModuleManager.getInstance().getSettings().isShowMovieSetTableTooltips()) {
      return null;
    }

    Object userObject = node.getUserObject();

    if (userObject instanceof MovieSet) {
      List<MovieSetScraperMetadataConfig> values = new ArrayList<>();
      if (MovieModuleManager.getInstance().getSettings().isMovieSetDisplayAllMissingArtwork()) {
        for (MovieSetScraperMetadataConfig config : MovieSetScraperMetadataConfig.values()) {
          if (config.isArtwork()) {
            values.add(config);
          }
        }
      }
      else {
        values.addAll(MovieModuleManager.getInstance().getSettings().getMovieSetCheckArtwork());
      }
      return getMissingToolTip(movieList.detectMissingFields((MovieSet) userObject, values));
    }
    else if (userObject instanceof MovieSet.MovieSetMovie) {
      return null;
    }
    else if (userObject instanceof Movie) {
      List<MovieScraperMetadataConfig> values = new ArrayList<>();
      if (MovieModuleManager.getInstance().getSettings().isMovieDisplayAllMissingArtwork()) {
        for (MovieScraperMetadataConfig config : MovieScraperMetadataConfig.values()) {
          if (config.isArtwork()) {
            values.add(config);
          }
        }
      }
      else {
        values.addAll(MovieModuleManager.getInstance().getSettings().getMovieCheckArtwork());
      }
      return getMissingToolTip(movieList.detectMissingFields((Movie) userObject, values));
    }
    return null;
  }

  private ImageIcon isWatched(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof MovieSet) {
      return getCheckIcon(((MovieSet) userObject).isWatched());
    }
    else if (userObject instanceof MovieSet.MovieSetMovie) {
      return null;
    }
    else if (userObject instanceof Movie) {
      return getCheckIcon(((Movie) userObject).isWatched());
    }
    return null;
  }

  private Integer getRuntime(TmmTreeNode node) {
    Object userObject = node.getUserObject();

    if (userObject instanceof Movie) {
      int runtime = ((Movie) userObject).getRuntime();

      if (runtime > 0) {
        return runtime;
      }
    }

    return null;
  }

  private String getYear(TmmTreeNode node) {
    Object userobject = node.getUserObject();

    if (userobject instanceof MovieSet) {
      String years = ((MovieSet) userobject).getYears();
      if (StringUtils.isNotBlank(years)) {
        return years;
      }
    }

    if (userobject instanceof Movie) {
      int year = ((Movie) userobject).getYear();

      if (year > 0) {
        return String.valueOf(year);
      }
    }

    return null;
  }

  private String getMediaInfoVideoFormat(TmmTreeNode node) {
    Object userObject = node.getUserObject();

    if (userObject instanceof MovieSet || userObject instanceof MovieSet.MovieSetMovie) {
      return null;
    }

    if (userObject instanceof Movie) {
      return ((Movie) userObject).getMediaInfoVideoFormat();
    }

    return null;
  }

  private String getMediaInfoVideoCodec(TmmTreeNode node) {
    Object userObject = node.getUserObject();

    if (userObject instanceof MovieSet || userObject instanceof MovieSet.MovieSetMovie) {
      return null;
    }

    if (userObject instanceof Movie) {
      return ((Movie) userObject).getMediaInfoVideoCodec();
    }

    return null;
  }

  private String getMediaInfoVideoBitrate(TmmTreeNode node) {
    Object userObject = node.getUserObject();

    if (userObject instanceof MovieSet || userObject instanceof MovieSet.MovieSetMovie) {
      return null;
    }

    if (userObject instanceof Movie) {
      return String.valueOf(((Movie) userObject).getMediaInfoVideoBitrate());
    }

    return null;
  }

  private String getUserRating(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof MovieSet || userObject instanceof MovieSet.MovieSetMovie) {
      return null;
    }

    if (userObject instanceof Movie) {
      MediaRating mediaRating = ((MediaEntity) userObject).getUserRating();
      if (mediaRating != null && mediaRating.getRating() > 0) {
        return String.valueOf(mediaRating.getRating());
      }
    }

    return null;
  }

  private String getVotes(TmmTreeNode node) {
    Object userObject = node.getUserObject();

    if (userObject instanceof MovieSet || userObject instanceof MovieSet.MovieSetMovie) {
      return null;
    }

    if (userObject instanceof Movie) {
      return String.valueOf(((MediaEntity) userObject).getRating().getVotes());
    }

    return null;
  }

  private Date getDateAdded(TmmTreeNode node) {
    Object userObject = node.getUserObject();

    if (userObject instanceof MovieSet || userObject instanceof MovieSet.MovieSetMovie) {
      return null;
    }

    if (userObject instanceof Movie) {
      return ((MediaEntity) userObject).getDateAddedForUi();
    }

    return null;
  }

  private String getAudioInformation(TmmTreeNode node) {
    Object userObject = node.getUserObject();

    if (userObject instanceof MovieSet || userObject instanceof MovieSet.MovieSetMovie) {
      return null;
    }

    if (userObject instanceof Movie) {
      List<MediaFile> videos = ((MediaEntity) userObject).getMediaFiles(MediaFileType.VIDEO);
      if (!videos.isEmpty()) {
        MediaFile mediaFile = videos.get(0);
        if (StringUtils.isNotBlank(mediaFile.getAudioCodec())) {
          return mediaFile.getAudioCodec() + " " + mediaFile.getAudioChannels();
        }
      }
    }

    return null;
  }

  private String getMoviePath(TmmTreeNode node) {
    Object userObject = node.getUserObject();

    if (userObject instanceof MovieSet || userObject instanceof MovieSet.MovieSetMovie) {
      return null;
    }

    if (userObject instanceof Movie) {
      return ((MediaEntity) userObject).getPathNIO().toString();
    }

    return null;
  }

  private String getVideoFilename(TmmTreeNode node) {
    Object userObject = node.getUserObject();

    if (userObject instanceof MovieSet || userObject instanceof MovieSet.MovieSetMovie) {
      return null;
    }

    if (userObject instanceof Movie) {
      return ((Movie) userObject).getMainVideoFile().getFilename();
    }

    return null;
  }

  private String getMissingToolTip(List<? extends ScraperMetadataConfig> missingValues) {
    if (!missingValues.isEmpty()) {
      StringBuilder missing = new StringBuilder(TmmResourceBundle.getString("tmm.missing") + ":");

      for (ScraperMetadataConfig metadataConfig : missingValues) {
        missing.append("\n").append(metadataConfig.getDescription());
      }

      return missing.toString();
    }

    return null;
  }
}
