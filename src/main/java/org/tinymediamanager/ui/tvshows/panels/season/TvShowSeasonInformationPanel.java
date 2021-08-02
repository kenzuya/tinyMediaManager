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
package org.tinymediamanager.ui.tvshows.panels.season;

import static org.tinymediamanager.core.Constants.ADDED_EPISODE;
import static org.tinymediamanager.core.Constants.BANNER;
import static org.tinymediamanager.core.Constants.MEDIA_FILES;
import static org.tinymediamanager.core.Constants.POSTER;
import static org.tinymediamanager.core.Constants.REMOVED_EPISODE;
import static org.tinymediamanager.core.Constants.THUMB;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.beans.PropertyChangeListener;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;

import org.apache.commons.lang3.StringUtils;
import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.ui.ColumnLayout;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.TmmUILayoutStore;
import org.tinymediamanager.ui.components.ImageLabel;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.table.TmmTable;
import org.tinymediamanager.ui.components.table.TmmTableFormat;
import org.tinymediamanager.ui.components.table.TmmTableModel;
import org.tinymediamanager.ui.tvshows.TvShowSeasonSelectionModel;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.swing.GlazedListsSwing;
import net.miginfocom.swing.MigLayout;

/**
 * The Class TvShowInformationPanel.
 * 
 * @author Manuel Laggner
 */
public class TvShowSeasonInformationPanel extends JPanel {
  private static final long                  serialVersionUID = 1911808562993073590L;

  private Color                              defaultColor;
  private Color                              dummyColor;

  private final EventList<TvShowEpisode>     episodeEventList;
  private final TmmTableModel<TvShowEpisode> episodeTableModel;
  private final TvShowSeasonSelectionModel   tvShowSeasonSelectionModel;
  private ImageLabel                         lblTvShowPoster;
  private JLabel                             lblPosterSize;
  private ImageLabel                         lblTvShowBanner;
  private JLabel                             lblBannerSize;
  private ImageLabel                         lblTvShowThumb;
  private JLabel                             lblThumbSize;
  private JLabel                             lblTvshowTitle;
  private JLabel                             lblSeason;
  private TmmTable                           tableEpisodes;

  /**
   * Instantiates a new tv show information panel.
   * 
   * @param tvShowSeasonSelectionModel
   *          the tv show selection model
   */
  public TvShowSeasonInformationPanel(TvShowSeasonSelectionModel tvShowSeasonSelectionModel) {
    this.tvShowSeasonSelectionModel = tvShowSeasonSelectionModel;
    episodeEventList = new ObservableElementList<>(GlazedLists.threadSafeList(new BasicEventList<>()),
        GlazedLists.beanConnector(TvShowEpisode.class));
    episodeTableModel = new TmmTableModel<>(GlazedListsSwing.swingThreadProxyList(episodeEventList), new EpisodeTableFormat());

    initComponents();
    initDataBindings();

    tableEpisodes.setDefaultRenderer(String.class, new EpisodeTableCellRenderer());

    // manual coded binding
    PropertyChangeListener propertyChangeListener = propertyChangeEvent -> {
      String property = propertyChangeEvent.getPropertyName();
      Object source = propertyChangeEvent.getSource();
      // react on selection/change of a seson
      if (source.getClass() != TvShowSeasonSelectionModel.class) {
        return;
      }

      TvShowSeasonSelectionModel model = (TvShowSeasonSelectionModel) source;
      TvShowSeason selectedSeason = model.getSelectedTvShowSeason();

      if ("selectedTvShowSeason".equals(property) || POSTER.equals(property)) {
        setPoster(selectedSeason);
      }

      if ("selectedTvShowSeason".equals(property) || BANNER.equals(property)) {
        setBanner(selectedSeason);
      }

      if ("selectedTvShowSeason".equals(property) || THUMB.equals(property)) {
        setThumb(selectedSeason);
      }

      if ("selectedTvShowSeason".equals(property) || MEDIA_FILES.equals(property) || ADDED_EPISODE.equals(property)
          || REMOVED_EPISODE.equals(property)) {
        try {
          episodeEventList.getReadWriteLock().writeLock().lock();
          episodeEventList.clear();
          episodeEventList.addAll(selectedSeason.getEpisodesForDisplay());
        }
        catch (Exception ignored) {
          // nothing to do here
        }
        finally {
          episodeEventList.getReadWriteLock().writeLock().unlock();
          tableEpisodes.adjustColumnPreferredWidths(6);
        }
      }
    };
    tvShowSeasonSelectionModel.addPropertyChangeListener(propertyChangeListener);
  }

  @Override
  public void updateUI() {
    super.updateUI();

    defaultColor = UIManager.getColor("Table.foreground");
    dummyColor = UIManager.getColor("Component.linkColor");
  }

  private void initComponents() {
    setLayout(new MigLayout("", "[100lp:100lp,grow][300lp:300lp,grow 250]", "[grow]"));

    {
      JPanel panelLeft = new JPanel();
      add(panelLeft, "cell 0 0,grow");
      panelLeft.setLayout(new ColumnLayout());

      lblTvShowPoster = new ImageLabel(false, false, true);
      lblTvShowPoster.setDesiredAspectRatio(2 / 3.0f);
      lblTvShowPoster.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      panelLeft.add(lblTvShowPoster);
      lblTvShowPoster.enableLightbox();
      lblPosterSize = new JLabel(TmmResourceBundle.getString("mediafiletype.poster"));
      panelLeft.add(lblPosterSize);
      panelLeft.add(Box.createVerticalStrut(20));

      lblTvShowThumb = new ImageLabel(false, false, true);
      lblTvShowThumb.setDesiredAspectRatio(16 / 9.0f);
      lblTvShowThumb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      panelLeft.add(lblTvShowThumb);
      lblTvShowThumb.enableLightbox();
      lblThumbSize = new JLabel(TmmResourceBundle.getString("mediafiletype.thumb"));
      panelLeft.add(lblThumbSize);
      panelLeft.add(Box.createVerticalStrut(20));

      lblTvShowBanner = new ImageLabel(false, false, true);
      lblTvShowBanner.setDesiredAspectRatio(25 / 8.0f);
      lblTvShowBanner.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      panelLeft.add(lblTvShowBanner);
      lblTvShowBanner.enableLightbox();
      lblBannerSize = new JLabel(TmmResourceBundle.getString("mediafiletype.banner"));
      panelLeft.add(lblBannerSize);
    }
    {
      JPanel panelRight = new JPanel();
      add(panelRight, "cell 1 0,grow");
      panelRight.setLayout(new MigLayout("insets 0 n n n, hidemode 2", "[][323px,grow]", "[][][shrink 0][][286px,grow]"));
      {
        lblTvshowTitle = new TmmLabel("", 1.33);
        panelRight.add(lblTvshowTitle, "cell 0 0 2 1");
      }
      {
        JLabel lblSeasonT = new TmmLabel(TmmResourceBundle.getString("metatag.season"));
        panelRight.add(lblSeasonT, "cell 0 1");
        TmmFontHelper.changeFont(lblSeasonT, 1.166, Font.BOLD);

        lblSeason = new JLabel("");
        panelRight.add(lblSeason, "cell 1 1");
        TmmFontHelper.changeFont(lblSeason, 1.166, Font.BOLD);
      }
      {
        panelRight.add(new JSeparator(), "cell 0 2 2 1,growx");
      }
      {
        JLabel lblEpisodelistT = new TmmLabel(TmmResourceBundle.getString("metatag.episodes"));
        panelRight.add(lblEpisodelistT, "cell 0 3 2 1");

        tableEpisodes = new TmmTable(episodeTableModel);
        tableEpisodes.setName("tvshows.seaon.episodeTable");
        TmmUILayoutStore.getInstance().install(tableEpisodes);
        JScrollPane scrollPaneEpisodes = new JScrollPane();
        tableEpisodes.configureScrollPane(scrollPaneEpisodes);
        panelRight.add(scrollPaneEpisodes, "cell 0 4 2 1,grow");
        scrollPaneEpisodes.setViewportView(tableEpisodes);
      }
    }
  }

  private void setPoster(TvShowSeason season) {
    lblTvShowPoster.clearImage();

    String posterPath = season.getArtworkFilename(MediaArtwork.MediaArtworkType.SEASON_POSTER);
    Dimension posterSize = season.getArtworkSize(MediaArtwork.MediaArtworkType.SEASON_POSTER);

    if (StringUtils.isBlank(posterPath) && TvShowModuleManager.getInstance().getSettings().isSeasonArtworkFallback()) {
      // fall back to the show
      posterPath = season.getTvShow().getArtworkFilename(MediaFileType.POSTER);
      posterSize = season.getTvShow().getArtworkDimension(MediaFileType.POSTER);
    }
    lblTvShowPoster.setImagePath(posterPath);

    if (posterSize.width > 0 && posterSize.height > 0) {
      lblPosterSize.setText(TmmResourceBundle.getString("mediafiletype.poster") + " - " + posterSize.width + "x" + posterSize.height);
    }
    else {
      lblPosterSize.setText(TmmResourceBundle.getString("mediafiletype.poster"));
    }
  }

  private void setBanner(TvShowSeason season) {
    lblTvShowBanner.clearImage();

    String bannerPath = season.getArtworkFilename(MediaArtwork.MediaArtworkType.SEASON_BANNER);
    Dimension bannerSize = season.getArtworkSize(MediaArtwork.MediaArtworkType.SEASON_BANNER);

    if (StringUtils.isBlank(bannerPath) && TvShowModuleManager.getInstance().getSettings().isSeasonArtworkFallback()) {
      // fall back to the show
      bannerPath = season.getTvShow().getArtworkFilename(MediaFileType.BANNER);
      bannerSize = season.getTvShow().getArtworkDimension(MediaFileType.BANNER);
    }

    lblTvShowBanner.setImagePath(bannerPath);
    if (bannerSize.width > 0 && bannerSize.height > 0) {
      lblBannerSize.setText(TmmResourceBundle.getString("mediafiletype.banner") + " - " + bannerSize.width + "x" + bannerSize.height);
    }
    else {
      lblBannerSize.setText(TmmResourceBundle.getString("mediafiletype.banner"));
    }
  }

  private void setThumb(TvShowSeason season) {
    lblTvShowThumb.clearImage();

    String thumbPath = season.getArtworkFilename(MediaArtwork.MediaArtworkType.SEASON_THUMB);
    Dimension thumbSize = season.getArtworkSize(MediaArtwork.MediaArtworkType.SEASON_THUMB);

    if (StringUtils.isBlank(thumbPath) && TvShowModuleManager.getInstance().getSettings().isSeasonArtworkFallback()) {
      thumbPath = season.getTvShow().getArtworkFilename(MediaFileType.FANART);
      thumbSize = season.getTvShow().getArtworkDimension(MediaFileType.FANART);
    }

    lblTvShowThumb.setImagePath(thumbPath);
    if (thumbSize.width > 0 && thumbSize.height > 0) {
      lblThumbSize.setText(TmmResourceBundle.getString("mediafiletype.thumb") + " - " + thumbSize.width + "x" + thumbSize.height);
    }
    else {
      lblThumbSize.setText(TmmResourceBundle.getString("mediafiletype.thumb"));
    }
  }

  protected void initDataBindings() {
    BeanProperty<TvShowSeasonSelectionModel, String> tvShowSeasonSelectionModelBeanProperty = BeanProperty
        .create("selectedTvShowSeason.tvShow.title");
    BeanProperty<JLabel, String> jLabelBeanProperty = BeanProperty.create("text");
    AutoBinding<TvShowSeasonSelectionModel, String, JLabel, String> autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ,
        tvShowSeasonSelectionModel, tvShowSeasonSelectionModelBeanProperty, lblTvshowTitle, jLabelBeanProperty);
    autoBinding.bind();
    //
    BeanProperty<TvShowSeasonSelectionModel, Integer> tvShowSeasonSelectionModelBeanProperty_1 = BeanProperty.create("selectedTvShowSeason.season");
    AutoBinding<TvShowSeasonSelectionModel, Integer, JLabel, String> autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ,
        tvShowSeasonSelectionModel, tvShowSeasonSelectionModelBeanProperty_1, lblSeason, jLabelBeanProperty);
    autoBinding_1.bind();

  }

  private static class EpisodeTableFormat extends TmmTableFormat<TvShowEpisode> {

    public EpisodeTableFormat() {
      /*
       * episode number
       */
      Column col = new Column(TmmResourceBundle.getString("metatag.episode"), "episode", TvShowEpisode::getEpisode, String.class);
      col.setColumnResizeable(false);
      addColumn(col);

      /*
       * episode title
       */
      col = new Column(TmmResourceBundle.getString("metatag.title"), "title", TvShowEpisode::getTitle, String.class);
      col.setColumnTooltip(TvShowEpisode::getTitle);
      addColumn(col);

      /*
       * aired date
       */
      col = new Column(TmmResourceBundle.getString("metatag.aired"), "aired", TvShowEpisode::getFirstAiredAsString, String.class);
      col.setColumnResizeable(false);
      addColumn(col);
    }
  }

  private class EpisodeTableCellRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

      Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

      // Check if the episode is a dummy one
      int dataRow = table.convertRowIndexToModel(row);
      if (episodeEventList.get(dataRow).isDummy()) {
        c.setForeground(dummyColor);
      }
      else {
        c.setForeground(defaultColor);
      }
      return c;
    }
  }
}
