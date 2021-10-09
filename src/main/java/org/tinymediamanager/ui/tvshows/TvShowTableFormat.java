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
package org.tinymediamanager.ui.tvshows;

import java.awt.FontMetrics;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.swing.ImageIcon;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.TmmDateFormat;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaEntity;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.tvshow.TvShowEpisodeScraperMetadataConfig;
import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.TvShowScraperMetadataConfig;
import org.tinymediamanager.core.tvshow.TvShowSettings;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.util.StrgUtils;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.components.tree.TmmTreeNode;
import org.tinymediamanager.ui.components.treetable.TmmTreeTableFormat;
import org.tinymediamanager.ui.renderer.DateTableCellRenderer;
import org.tinymediamanager.ui.renderer.RightAlignTableCellRenderer;

/**
 * The class TvShowTableFormat is used to define the columns for the TV show tree table
 *
 * @author Manuel Laggner
 */
public class TvShowTableFormat extends TmmTreeTableFormat<TmmTreeNode> {

  private final TvShowList     tvShowList;
  private final TvShowSettings settings;
  private final Calendar       calendar = Calendar.getInstance();

  public TvShowTableFormat() {
    tvShowList = TvShowModuleManager.getInstance().getTvShowList();
    settings = TvShowModuleManager.getInstance().getSettings();
    FontMetrics fontMetrics = getFontMetrics();

    Comparator<String> stringComparator = new StringComparator();
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
    Comparator<Date> dateComparator = new DateComparator();
    Comparator<ImageIcon> imageComparator = new ImageComparator();
    Comparator<String> videoFormatComparator = new VideoFormatComparator();
    Comparator<String> fileSizeComparator = new FileSizeComparator();

    /*
     * Original Title
     */
    Column col = new Column(TmmResourceBundle.getString("metatag.originaltitle"), "originalTitle", this::getOriginialTitle, String.class);
    col.setDefaultHidden(true);
    col.setColumnComparator(stringComparator);
    addColumn(col);

    /*
     * year
     */
    col = new Column(TmmResourceBundle.getString("metatag.year"), "year", this::getYear, String.class);
    col.setColumnResizeable(false);
    col.setMinWidth((int) (fontMetrics.stringWidth("2000") * 1.3f + 10));
    col.setColumnComparator(integerComparator);
    addColumn(col);

    /*
     * season count
     */
    col = new Column(TmmResourceBundle.getString("metatag.season.count"), "seasons", this::getSeasons, String.class);
    col.setHeaderIcon(IconManager.SEASONS);
    col.setCellRenderer(new RightAlignTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth((int) (fontMetrics.stringWidth("99") * 1.2f));
    col.setColumnComparator(integerComparator);
    addColumn(col);

    /*
     * episode count
     */
    col = new Column(TmmResourceBundle.getString("metatag.episode.count"), "episodes", this::getEpisodes, String.class);
    col.setHeaderIcon(IconManager.EPISODES);
    col.setCellRenderer(new RightAlignTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth((int) (fontMetrics.stringWidth("999") * 1.2f + 10));
    col.setColumnComparator(integerComparator);
    addColumn(col);

    /*
     * rating
     */
    col = new Column(TmmResourceBundle.getString("metatag.rating"), "rating", this::getRating, String.class);
    col.setHeaderIcon(IconManager.RATING);
    col.setCellRenderer(new RightAlignTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth((int) (fontMetrics.stringWidth("99.9") * 1.2f));
    col.setColumnComparator(floatComparator);
    addColumn(col);

    /*
     * votes (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.votes"), "votes", this::getVotes, String.class);
    col.setHeaderIcon(IconManager.VOTES);
    col.setCellRenderer(new RightAlignTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth((int) (fontMetrics.stringWidth("1000000") * 1.2f + 10));
    col.setDefaultHidden(true);
    col.setColumnComparator(integerComparator);
    addColumn(col);

    /*
     * user rating
     */
    col = new Column(TmmResourceBundle.getString("metatag.userrating"), "userrating", this::getUserRating, String.class);
    col.setHeaderIcon(IconManager.USER_RATING);
    col.setCellRenderer(new RightAlignTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth((int) (fontMetrics.stringWidth("99.9") * 1.2f));
    col.setColumnComparator(floatComparator);
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * aired
     */
    col = new Column(TmmResourceBundle.getString("metatag.aired"), "aired", this::getAiredDate, Date.class);
    col.setHeaderIcon(IconManager.DATE_AIRED);
    col.setCellRenderer(new DateTableCellRenderer());
    col.setColumnResizeable(false);
    try {
      Date date = StrgUtils.parseDate("2012-12-12");
      col.setMinWidth((int) (fontMetrics.stringWidth(TmmDateFormat.MEDIUM_DATE_FORMAT.format(date)) * 1.2f));
    }
    catch (Exception ignored) {
    }
    col.setColumnComparator(dateComparator);
    addColumn(col);

    /*
     * date added (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.dateadded"), "dateAdded", this::getDateAdded, Date.class);
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
    col.setColumnComparator(dateComparator);
    addColumn(col);

    /*
     * file creation date (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.filecreationdate"), "fileCreationDate", this::getFileCreationDate, Date.class);
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
    col.setColumnComparator(dateComparator);
    addColumn(col);

    /*
     * video format (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.format"), "format", this::getFormat, String.class);
    col.setHeaderIcon(IconManager.VIDEO_FORMAT);
    col.setColumnResizeable(false);
    col.setMinWidth((int) (fontMetrics.stringWidth("1080p") * 1.2f));
    col.setDefaultHidden(true);
    col.setColumnComparator(videoFormatComparator);
    addColumn(col);

    /*
     * video codec (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.videocodec"), "videoCodec", this::getVideoCodec, String.class);
    col.setHeaderIcon(IconManager.VIDEO_CODEC);
    col.setMinWidth((int) (fontMetrics.stringWidth("MPEG-2") * 1.2f + 10));
    col.setDefaultHidden(true);
    col.setColumnComparator(stringComparator);
    addColumn(col);

    /*
     * Video Bitrate
     */
    col = new Column(TmmResourceBundle.getString("metatag.videobitrate"), "videoBitrate", this::getVideoBitrate, Integer.class);
    col.setHeaderIcon(IconManager.VIDEO_BITRATE);
    col.setMinWidth((int) (fontMetrics.stringWidth("20000") * 1.2f + 10));
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * main video file size (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.videofilesize"), "fileSize", this::getVideoFileSize, String.class);
    col.setHeaderIcon(IconManager.FILE_SIZE);
    col.setCellRenderer(new RightAlignTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth((int) (fontMetrics.stringWidth("50000M") * 1.2f));
    col.setDefaultHidden(true);
    col.setColumnComparator(fileSizeComparator);
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
     * aspect ratio (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.aspectratio"), "aspectratio", this::getAspectRatio, Float.class);
    col.setHeaderIcon(IconManager.ASPECT_RATIO);
    col.setCellRenderer(new RightAlignTableCellRenderer());
    col.setColumnResizeable(false);
    col.setMinWidth((int) (fontMetrics.stringWidth("1.78") * 1.2f + 10));
    col.setDefaultHidden(true);
    col.setColumnComparator(floatComparator);
    addColumn(col);

    /*
     * HDR (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.hdr"), "hdr", this::isHDR, ImageIcon.class);
    col.setHeaderIcon(IconManager.HDR);
    col.setColumnResizeable(false);
    col.setDefaultHidden(true);
    col.setColumnComparator(imageComparator);
    addColumn(col);

    /*
     * new indicator
     */
    col = new Column(TmmResourceBundle.getString("movieextendedsearch.newepisodes"), "new", this::getNewIcon, ImageIcon.class);
    col.setHeaderIcon(IconManager.NEW);
    col.setColumnResizeable(false);
    col.setColumnComparator(imageComparator);
    addColumn(col);

    /*
     * metadata
     */
    col = new Column(TmmResourceBundle.getString("tmm.metadata"), "metadata", this::hasMetadata, ImageIcon.class);
    col.setHeaderIcon(IconManager.NFO);
    col.setColumnResizeable(false);
    col.setColumnTooltip(this::hasMetadataTooltip);
    col.setColumnComparator(imageComparator);
    addColumn(col);

    /*
     * images
     */
    col = new Column(TmmResourceBundle.getString("tmm.images"), "images", this::hasArtwork, ImageIcon.class);
    col.setHeaderIcon(IconManager.IMAGES);
    col.setColumnResizeable(false);
    col.setColumnTooltip(this::hasImageTooltip);
    col.setColumnComparator(imageComparator);
    addColumn(col);

    /*
     * subtitles
     */
    col = new Column(TmmResourceBundle.getString("tmm.subtitles"), "subtitles", this::hasSubtitles, ImageIcon.class);
    col.setHeaderIcon(IconManager.SUBTITLES);
    col.setColumnResizeable(false);
    col.setColumnComparator(imageComparator);
    addColumn(col);

    /*
     * watched
     */
    col = new Column(TmmResourceBundle.getString("metatag.watched"), "watched", this::isWatched, ImageIcon.class);
    col.setHeaderIcon(IconManager.WATCHED);
    col.setColumnResizeable(false);
    col.setColumnComparator(imageComparator);
    addColumn(col);

    /*
     * has downloaded theme
     */
    col = new Column(TmmResourceBundle.getString("metatag.musictheme"), "theme", this::hasDownloadedMusicTheme, ImageIcon.class);
    col.setHeaderIcon(IconManager.MUSIC_HEADER);
    col.setColumnResizeable(false);
    col.setDefaultHidden(true);
    addColumn(col);

    /*
     * has Note (hidden per default)
     */
    col = new Column(TmmResourceBundle.getString("metatag.note"), "note", this::hasNote, ImageIcon.class);
    col.setHeaderIcon(IconManager.INFO);
    col.setColumnResizeable(false);
    col.setDefaultHidden(true);
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

  private String getYear(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShow) {
      int year = ((TvShow) userObject).getYear();
      if (year > 0) {
        return String.valueOf(year);
      }
      return null;
    }
    else if (userObject instanceof TvShowSeason) {
      Date firstAired = ((TvShowSeason) userObject).getFirstAired();
      if (firstAired != null) {
        calendar.setTime(firstAired);
        return String.valueOf(calendar.get(Calendar.YEAR));
      }
      return null;
    }
    return null;
  }

  private String getSeasons(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShow) {
      return String.valueOf(((TvShow) userObject).getSeasonCount());
    }
    return null;
  }

  private String getEpisodes(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShow) {
      return String.valueOf(((TvShow) userObject).getEpisodeCount());
    }
    if (userObject instanceof TvShowSeason) {
      if (!((TvShowSeason) userObject).getEpisodes().isEmpty()) {
        return String.valueOf(((TvShowSeason) userObject).getEpisodes().size());
      }
    }
    return null;
  }

  private ImageIcon getNewIcon(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShow) {
      TvShow tvShow = (TvShow) userObject;
      return getNewIcon(tvShow.isNewlyAdded() || tvShow.hasNewlyAddedEpisodes());
    }
    else if (userObject instanceof TvShowSeason) {
      TvShowSeason season = (TvShowSeason) userObject;
      return getNewIcon(season.isNewlyAdded());
    }
    else if (userObject instanceof TvShowEpisode) {
      TvShowEpisode episode = (TvShowEpisode) userObject;
      return getNewIcon(episode.isNewlyAdded());
    }
    return null;
  }

  private String getRating(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShow || userObject instanceof TvShowEpisode) {
      MediaRating mediaRating = ((MediaEntity) userObject).getRating();
      if (mediaRating != null && mediaRating != MediaMetadata.EMPTY_RATING && mediaRating.getRating() > 0) {
        return String.valueOf(mediaRating.getRating());
      }
    }
    return null;
  }

  private String getVotes(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShow || userObject instanceof TvShowEpisode) {
      MediaRating mediaRating = ((MediaEntity) userObject).getRating();
      if (mediaRating != null && mediaRating != MediaMetadata.EMPTY_RATING && mediaRating.getRating() > 0) {
        return String.valueOf(mediaRating.getVotes());
      }
    }
    return null;
  }

  private String getUserRating(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShow || userObject instanceof TvShowEpisode) {
      MediaRating mediaRating = ((MediaEntity) userObject).getUserRating();
      if (mediaRating != null && mediaRating.getRating() > 0) {
        return String.valueOf(mediaRating.getRating());
      }
    }
    return null;
  }

  private Date getAiredDate(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShow) {
      return ((TvShow) userObject).getFirstAired();
    }
    if (userObject instanceof TvShowSeason) {
      return ((TvShowSeason) userObject).getFirstAired();
    }
    if (userObject instanceof TvShowEpisode) {
      return ((TvShowEpisode) userObject).getFirstAired();
    }
    return null;
  }

  private Date getDateAdded(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShow) {
      return ((TvShow) userObject).getDateAddedForUi();
    }
    if (userObject instanceof TvShowEpisode) {
      TvShowEpisode episode = (TvShowEpisode) userObject;
      if (!episode.isDummy()) {
        return episode.getDateAddedForUi();
      }
    }
    return null;
  }

  private Date getFileCreationDate(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShowEpisode) {
      TvShowEpisode episode = (TvShowEpisode) userObject;
      if (!episode.isDummy()) {
        return episode.getMainVideoFile().getDateCreated();
      }
    }
    return null;
  }

  private String getFormat(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShowEpisode) {
      return ((TvShowEpisode) userObject).getMediaInfoVideoFormat();
    }
    return "";
  }

  private String getVideoCodec(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShowEpisode) {
      return ((TvShowEpisode) userObject).getMediaInfoVideoCodec();
    }
    return "";
  }

  private Integer getVideoBitrate(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShowEpisode) {
      return ((TvShowEpisode) userObject).getMediaInfoVideoBitrate();
    }
    return null;
  }

  private String getVideoFileSize(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShow) {
      long size = ((TvShow) userObject).getVideoFilesize();
      return (int) (size / (1000.0 * 1000.0)) + " M";
    }
    if (userObject instanceof TvShowSeason) {
      long size = ((TvShowSeason) userObject).getVideoFilesize();
      return (int) (size / (1000.0 * 1000.0)) + " M";
    }
    if (userObject instanceof TvShowEpisode) {
      long size = ((TvShowEpisode) userObject).getVideoFilesize();
      return (int) (size / (1000.0 * 1000.0)) + " M";
    }
    return "";
  }

  private String getTotalFileSize(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShow) {
      long size = ((TvShow) userObject).getTotalFilesize();
      return (int) (size / (1000.0 * 1000.0)) + " M";
    }
    if (userObject instanceof TvShowSeason) {
      long size = ((TvShowSeason) userObject).getTotalFilesize();
      return (int) (size / (1000.0 * 1000.0)) + " M";
    }
    if (userObject instanceof TvShowEpisode) {
      long size = ((TvShowEpisode) userObject).getTotalFilesize();
      return (int) (size / (1000.0 * 1000.0)) + " M";
    }
    return "";
  }

  private Float getAspectRatio(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShowEpisode) {
      return ((TvShowEpisode) userObject).getMediaInfoAspectRatio();
    }
    return null;
  }

  private ImageIcon isHDR(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShowEpisode) {
      TvShowEpisode episode = ((TvShowEpisode) userObject);
      return getCheckIcon(StringUtils.isNotEmpty(episode.getVideoHDRFormat()));
    }
    return null;
  }

  private ImageIcon hasMetadata(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShow) {
      TvShow tvShow = (TvShow) userObject;
      return getTriStateIcon(TRI_STATE.getState(tvShowList.detectMissingMetadata(tvShow).isEmpty(), tvShow.getHasEpisodeMetadata()));
    }
    else if (userObject instanceof TvShowSeason) {
      TvShowSeason season = ((TvShowSeason) userObject);
      return getCheckIcon(season.getHasEpisodeMetadata());
    }
    else if (userObject instanceof TvShowEpisode) {
      TvShowEpisode episode = ((TvShowEpisode) userObject);
      return getCheckIcon(tvShowList.detectMissingMetadata(episode).isEmpty());
    }
    return null;
  }

  private ImageIcon hasArtwork(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShow) {
      TvShow tvShow = (TvShow) userObject;
      return getTriStateIcon(TRI_STATE.getState(tvShowList.detectMissingArtwork(tvShow).isEmpty(), tvShow.getHasSeasonAndEpisodeImages()));
    }
    else if (userObject instanceof TvShowSeason) {
      TvShowSeason season = ((TvShowSeason) userObject);
      return getTriStateIcon(TRI_STATE.getState(tvShowList.detectMissingArtwork(season).isEmpty(), season.getHasEpisodeImages()));
    }
    else if (userObject instanceof TvShowEpisode) {
      TvShowEpisode episode = ((TvShowEpisode) userObject);
      return getCheckIcon(tvShowList.detectMissingArtwork(episode).isEmpty());
    }
    return null;
  }

  private ImageIcon hasSubtitles(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShow) {
      TvShow tvShow = (TvShow) userObject;
      return getCheckIcon(tvShow.hasEpisodeSubtitles());
    }
    else if (userObject instanceof TvShowSeason) {
      TvShowSeason season = ((TvShowSeason) userObject);
      return getCheckIcon(season.hasEpisodeSubtitles());
    }
    else if (userObject instanceof TvShowEpisode) {
      TvShowEpisode episode = ((TvShowEpisode) userObject);
      return getCheckIcon(episode.getHasSubtitles());
    }
    return null;
  }

  private ImageIcon isWatched(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShow) {
      return getCheckIcon(((TvShow) userObject).isWatched());
    }
    else if (userObject instanceof TvShowSeason) {
      return getCheckIcon(((TvShowSeason) userObject).isWatched());
    }
    else if (userObject instanceof TvShowEpisode) {
      return getCheckIcon(((TvShowEpisode) userObject).isWatched());
    }
    return null;
  }

  private String hasMetadataTooltip(TmmTreeNode node) {
    if (!settings.isShowTvShowTableTooltips()) {
      return null;
    }

    List<TvShowScraperMetadataConfig> tvShowValues = new ArrayList<>();
    if (settings.isTvShowDisplayAllMissingMetadata()) {
      for (TvShowScraperMetadataConfig config : TvShowScraperMetadataConfig.values()) {
        if (config.isMetaData() || config.isCast()) {
          tvShowValues.add(config);
        }
      }
    }
    else {
      tvShowValues.addAll(settings.getTvShowCheckMetadata());
    }

    List<TvShowEpisodeScraperMetadataConfig> episodeValues = new ArrayList<>();
    if (settings.isEpisodeDisplayAllMissingMetadata()) {
      for (TvShowEpisodeScraperMetadataConfig config : TvShowEpisodeScraperMetadataConfig.values()) {
        if (config.isMetaData() || config.isCast()) {
          episodeValues.add(config);
        }
      }
    }
    else {
      episodeValues.addAll(settings.getEpisodeCheckMetadata());
    }

    if (node.getUserObject() instanceof TvShow) {
      TvShow tvShow = (TvShow) node.getUserObject();

      List<TvShowScraperMetadataConfig> missingMetadata = tvShowList.detectMissingFields(tvShow, tvShowValues);
      boolean missingEpisodeData = false;

      for (TvShowEpisode episode : tvShow.getEpisodes()) {
        if (episode.isDummy()
            || (episode.getSeason() == 0 && !TvShowModuleManager.getInstance().getSettings().isEpisodeSpecialsCheckMissingMetadata())) {
          continue;
        }
        if (!tvShowList.detectMissingFields(episode, episodeValues).isEmpty()) {
          missingEpisodeData = true;
          break;
        }
      }

      String text = "";

      if (!missingMetadata.isEmpty()) {
        StringBuilder missing = new StringBuilder(TmmResourceBundle.getString("tmm.missing") + ":");
        for (TvShowScraperMetadataConfig metadataConfig : missingMetadata) {
          missing.append("\n").append(metadataConfig.getDescription());
        }

        text = missing.toString();
      }

      if (missingEpisodeData) {
        if (StringUtils.isNotBlank(text)) {
          text += "\n\n";
        }
        text += TmmResourceBundle.getString("tvshow.tree.episode.metadata.problem");
      }

      if (StringUtils.isNotBlank(text)) {
        return text;
      }
    }
    else if (node.getUserObject() instanceof TvShowSeason) {
      TvShowSeason season = (TvShowSeason) node.getUserObject();

      for (TvShowEpisode episode : season.getEpisodes()) {
        if (!episode.isDummy() && !tvShowList.detectMissingFields(episode, episodeValues).isEmpty()) {
          return TmmResourceBundle.getString("tvshow.tree.episode.metadata.problem");
        }
      }
    }
    else if (node.getUserObject() instanceof TvShowEpisode) {
      TvShowEpisode episode = (TvShowEpisode) node.getUserObject();

      List<TvShowEpisodeScraperMetadataConfig> missingMetadata = tvShowList.detectMissingFields(episode, episodeValues);
      if (!missingMetadata.isEmpty()) {
        StringBuilder missing = new StringBuilder(TmmResourceBundle.getString("tmm.missing") + ":");
        for (TvShowEpisodeScraperMetadataConfig metadataConfig : missingMetadata) {
          missing.append("\n").append(metadataConfig.getDescription());
        }

        return missing.toString();
      }
    }
    return null;
  }

  private String hasImageTooltip(TmmTreeNode node) {
    if (!settings.isShowTvShowTableTooltips()) {
      return null;
    }

    List<TvShowScraperMetadataConfig> tvShowValues = new ArrayList<>();
    if (settings.isTvShowDisplayAllMissingArtwork()) {
      for (TvShowScraperMetadataConfig config : TvShowScraperMetadataConfig.values()) {
        if (config.isArtwork()) {
          tvShowValues.add(config);
        }
      }
    }
    else {
      tvShowValues.addAll(settings.getTvShowCheckArtwork());
    }

    List<TvShowScraperMetadataConfig> seasonValues = new ArrayList<>();
    if (settings.isSeasonDisplayAllMissingArtwork()) {
      for (TvShowScraperMetadataConfig config : TvShowScraperMetadataConfig.values()) {
        if (config.isArtwork() && config.name().startsWith("SEASON")) {
          seasonValues.add(config);
        }
      }
    }
    else {
      seasonValues.addAll(settings.getSeasonCheckArtwork());
    }

    List<TvShowEpisodeScraperMetadataConfig> episodeValues = new ArrayList<>();
    if (settings.isEpisodeDisplayAllMissingArtwork()) {
      for (TvShowEpisodeScraperMetadataConfig config : TvShowEpisodeScraperMetadataConfig.values()) {
        if (config.isArtwork()) {
          episodeValues.add(config);
        }
      }
    }
    else {
      episodeValues.addAll(settings.getEpisodeCheckArtwork());
    }

    if (node.getUserObject() instanceof TvShow) {
      TvShow tvShow = (TvShow) node.getUserObject();

      List<TvShowScraperMetadataConfig> missingMetadata = tvShowList.detectMissingFields(tvShow, tvShowValues);
      boolean missingSeasonEpisodeData = false;

      for (TvShowSeason season : tvShow.getSeasons()) {
        if (!tvShowList.detectMissingFields(season, seasonValues).isEmpty()) {
          missingSeasonEpisodeData = true;
          break;
        }
        for (TvShowEpisode episode : season.getEpisodes()) {
          if (episode.isDummy()
              || (episode.getSeason() == 0 && !TvShowModuleManager.getInstance().getSettings().isEpisodeSpecialsCheckMissingMetadata())) {
            continue;
          }
          if (!tvShowList.detectMissingFields(episode, episodeValues).isEmpty()) {
            missingSeasonEpisodeData = true;
            break;
          }
        }

        if (missingSeasonEpisodeData) {
          break;
        }
      }

      String text = "";

      if (!missingMetadata.isEmpty()) {
        StringBuilder missing = new StringBuilder(TmmResourceBundle.getString("tmm.missing") + ":");
        for (TvShowScraperMetadataConfig metadataConfig : missingMetadata) {
          missing.append("\n").append(metadataConfig.getDescription());
        }

        text = missing.toString();
      }

      if (missingSeasonEpisodeData) {
        if (StringUtils.isNotBlank(text)) {
          text += "\n\n";
        }
        text += TmmResourceBundle.getString("tvshow.tree.season.artwork.problem");
      }

      if (StringUtils.isNotBlank(text)) {
        return text;
      }
    }
    else if (node.getUserObject() instanceof TvShowSeason) {
      TvShowSeason season = (TvShowSeason) node.getUserObject();

      List<TvShowScraperMetadataConfig> missingMetadata = tvShowList.detectMissingFields(season, seasonValues);
      boolean missingEpisodeData = false;

      for (TvShowEpisode episode : season.getEpisodes()) {
        if (!episode.isDummy() && !tvShowList.detectMissingFields(episode, episodeValues).isEmpty()) {
          missingEpisodeData = true;
          break;
        }
      }

      String text = "";

      if (!missingMetadata.isEmpty()) {
        StringBuilder missing = new StringBuilder(TmmResourceBundle.getString("tmm.missing") + ":");
        for (TvShowScraperMetadataConfig metadataConfig : missingMetadata) {
          missing.append("\n").append(metadataConfig.getDescription());
        }

        text = missing.toString();
      }

      if (missingEpisodeData) {
        if (StringUtils.isNotBlank(text)) {
          text += "\n\n";
        }
        text += TmmResourceBundle.getString("tvshow.tree.episode.artwork.problem");
      }

      if (StringUtils.isNotBlank(text)) {
        return text;
      }
    }
    else if (node.getUserObject() instanceof TvShowEpisode) {
      TvShowEpisode episode = (TvShowEpisode) node.getUserObject();

      List<TvShowEpisodeScraperMetadataConfig> missingMetadata = tvShowList.detectMissingFields(episode, episodeValues);
      if (!missingMetadata.isEmpty()) {
        StringBuilder missing = new StringBuilder(TmmResourceBundle.getString("tmm.missing") + ":");
        for (TvShowEpisodeScraperMetadataConfig metadataConfig : missingMetadata) {
          missing.append("\n").append(metadataConfig.getDescription());
        }

        return missing.toString();
      }
    }
    return null;
  }

  private String getOriginialTitle(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShow) {
      return ((TvShow) userObject).getOriginalTitle();
    }
    else if (userObject instanceof TvShowEpisode) {
      return ((TvShowEpisode) userObject).getOriginalTitle();
    }
    return null;
  }

  private ImageIcon hasDownloadedMusicTheme(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShow) {
      return getCheckIcon(((TvShow) userObject).getHasMusicTheme());
    }
    return null;
  }

  private ImageIcon hasNote(TmmTreeNode node) {
    Object userObject = node.getUserObject();
    if (userObject instanceof TvShow) {
      return getCheckIcon(((TvShow) userObject).getHasNote());
    }
    else if (userObject instanceof TvShowEpisode) {
      return getCheckIcon(((TvShowEpisode) userObject).getHasNote());
    }
    return null;
  }

}
