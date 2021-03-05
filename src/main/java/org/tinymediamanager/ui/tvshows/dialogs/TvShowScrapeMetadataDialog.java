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
package org.tinymediamanager.ui.tvshows.dialogs;

import static java.util.Locale.ROOT;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import org.tinymediamanager.core.ScraperMetadataConfig;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.tvshow.TvShowEpisodeScraperMetadataConfig;
import org.tinymediamanager.core.tvshow.TvShowEpisodeSearchAndScrapeOptions;
import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.TvShowScraperMetadataConfig;
import org.tinymediamanager.core.tvshow.TvShowSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.scraper.kodi.KodiTvShowMetadataProvider;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.combobox.MediaScraperCheckComboBox;
import org.tinymediamanager.ui.components.combobox.MediaScraperComboBox;
import org.tinymediamanager.ui.components.combobox.ScraperMetadataConfigCheckComboBox;
import org.tinymediamanager.ui.dialogs.TmmDialog;

import net.miginfocom.swing.MigLayout;

/**
 * The Class TvShowScrapeMetadataDialog.
 * 
 * @author Manuel Laggner
 */
public class TvShowScrapeMetadataDialog extends TmmDialog {
  private static final long                                                      serialVersionUID = 6120530120703772160L;

  private boolean                                                                startScrape      = false;

  /** UI components */
  private JComboBox                                                              cbLanguage;
  private MediaScraperComboBox                                                   cbMetadataScraper;
  private MediaScraperCheckComboBox                                              cbArtworkScraper;
  private MediaScraperCheckComboBox                                              cbTrailerScraper;
  private ScraperMetadataConfigCheckComboBox<TvShowScraperMetadataConfig>        cbTvShowScraperConfig;
  private ScraperMetadataConfigCheckComboBox<TvShowEpisodeScraperMetadataConfig> cbEpisodeScraperConfig;

  /**
   * create the scraper dialog with displaying just set fields
   * 
   * @param title
   *          the title to display
   * @param artworkOnly
   *          only for artwork scraping?
   * @param tvShowMetadata
   *          show the TV show metadata config block?
   */
  private TvShowScrapeMetadataDialog(String title, boolean artworkOnly, boolean tvShowMetadata) {
    super(title, "tvShowUpdateMetadata");

    JPanel panelContent = new JPanel();
    getContentPane().add(panelContent, BorderLayout.CENTER);
    panelContent.setLayout(new MigLayout("hidemode 3", "[][600lp:800lp,grow]", "[][][][][shrink 0][200lp:n, grow]"));

    JLabel lblLanguageT = new TmmLabel(TmmResourceBundle.getString("metatag.language"));
    panelContent.add(lblLanguageT, "cell 0 0,alignx trailing");

    cbLanguage = new JComboBox(MediaLanguages.valuesSorted());
    cbLanguage.setSelectedItem(TvShowModuleManager.SETTINGS.getScraperLanguage());
    panelContent.add(cbLanguage, "cell 1 0,growx");

    if (!artworkOnly) {
      JLabel lblMetadataScraperT = new TmmLabel(TmmResourceBundle.getString("scraper.metadata"));
      panelContent.add(lblMetadataScraperT, "cell 0 1,alignx trailing");

      cbMetadataScraper = new MediaScraperComboBox(TvShowList.getInstance()
          .getAvailableMediaScrapers()
          .stream()
          .filter(scraper -> !(scraper.getMediaProvider() instanceof KodiTvShowMetadataProvider))
          .collect(Collectors.toList()));
      panelContent.add(cbMetadataScraper, "cell 1 1,growx");
    }

    JLabel lblArtworkScraper = new TmmLabel(TmmResourceBundle.getString("scraper.artwork"));
    panelContent.add(lblArtworkScraper, "cell 0 2,alignx trailing");

    cbArtworkScraper = new MediaScraperCheckComboBox(TvShowList.getInstance().getAvailableArtworkScrapers());
    panelContent.add(cbArtworkScraper, "cell 1 2,growx");

    if (!artworkOnly) {
      JLabel lblTrailerScraper = new TmmLabel(TmmResourceBundle.getString("scraper.trailer"));
      panelContent.add(lblTrailerScraper, "cell 0 3,alignx right");

      cbTrailerScraper = new MediaScraperCheckComboBox(TvShowList.getInstance().getAvailableTrailerScrapers());
      panelContent.add(cbTrailerScraper, "cell 1 3,growx");
    }
    {
      JSeparator separator = new JSeparator();
      panelContent.add(separator, "cell 0 4 2 1,growx");
    }
    JPanel panelScraperConfig = new JPanel();
    panelContent.add(panelScraperConfig, "cell 0 5 2 1,grow");
    panelScraperConfig.setLayout(new MigLayout("hidemode 3", "[][300lp:500lp,grow]", "[][][]"));
    {
      JLabel lblScrapeFollowingItems = new TmmLabel(TmmResourceBundle.getString("chooser.scrape"));
      panelScraperConfig.add(lblScrapeFollowingItems, "cell 0 0 2 1");
    }
    if (tvShowMetadata) {
      JLabel lblTvShowsT = new TmmLabel(TmmResourceBundle.getString("metatag.tvshows"));
      panelScraperConfig.add(lblTvShowsT, "cell 0 1,alignx trailing");

      if (artworkOnly) {
        cbTvShowScraperConfig = new ScraperMetadataConfigCheckComboBox(TvShowScraperMetadataConfig.valuesForType(ScraperMetadataConfig.Type.ARTWORK));
        cbTvShowScraperConfig.enableFilter(
            (movieScraperMetadataConfig, s) -> movieScraperMetadataConfig.getDescription().toLowerCase(ROOT).startsWith(s.toLowerCase(ROOT)));
      }
      else {
        cbTvShowScraperConfig = new ScraperMetadataConfigCheckComboBox(TvShowScraperMetadataConfig.values());
        cbTvShowScraperConfig.enableFilter(
            (movieScraperMetadataConfig, s) -> movieScraperMetadataConfig.getDescription().toLowerCase(ROOT).startsWith(s.toLowerCase(ROOT)));
      }
      panelScraperConfig.add(cbTvShowScraperConfig, "cell 1 1,grow, wmin 0");
    }

    JLabel lblEpisodesT = new TmmLabel(TmmResourceBundle.getString("metatag.episodes"));
    panelScraperConfig.add(lblEpisodesT, "cell 0 2,alignx trailing");

    if (artworkOnly) {
      cbEpisodeScraperConfig = new ScraperMetadataConfigCheckComboBox(
          TvShowEpisodeScraperMetadataConfig.valuesForType(ScraperMetadataConfig.Type.ARTWORK));
      cbEpisodeScraperConfig.enableFilter(
          (movieScraperMetadataConfig, s) -> movieScraperMetadataConfig.getDescription().toLowerCase(ROOT).startsWith(s.toLowerCase(ROOT)));
    }
    else {
      cbEpisodeScraperConfig = new ScraperMetadataConfigCheckComboBox(TvShowEpisodeScraperMetadataConfig.values());
      cbEpisodeScraperConfig.enableFilter(
          (movieScraperMetadataConfig, s) -> movieScraperMetadataConfig.getDescription().toLowerCase(ROOT).startsWith(s.toLowerCase(ROOT)));
    }
    panelScraperConfig.add(cbEpisodeScraperConfig, "cell 1 2,grow, wmin 0");

    {
      JButton btnCancel = new JButton(TmmResourceBundle.getString("Button.cancel"));
      btnCancel.setIcon(IconManager.CANCEL_INV);
      btnCancel.addActionListener(e -> {
        startScrape = false;
        setVisible(false);
      });
      addButton(btnCancel);

      JButton btnStart = new JButton(TmmResourceBundle.getString("scraper.start"));
      btnStart.setIcon(IconManager.APPLY_INV);
      btnStart.addActionListener(e -> {
        startScrape = true;
        setVisible(false);
      });
      addDefaultButton(btnStart);
    }
    // set data

    // metadataprovider
    if (cbMetadataScraper != null) {
      MediaScraper defaultScraper = TvShowList.getInstance().getDefaultMediaScraper();
      cbMetadataScraper.setSelectedItem(defaultScraper);
    }

    // artwork scraper
    if (cbArtworkScraper != null) {
      List<MediaScraper> selectedArtworkScrapers = new ArrayList<>();
      for (MediaScraper artworkScraper : TvShowList.getInstance().getAvailableArtworkScrapers()) {
        if (TvShowModuleManager.SETTINGS.getArtworkScrapers().contains(artworkScraper.getId())) {
          selectedArtworkScrapers.add(artworkScraper);
        }
      }
      if (!selectedArtworkScrapers.isEmpty()) {
        cbArtworkScraper.setSelectedItems(selectedArtworkScrapers);
      }
    }

    // trailer scraper
    if (cbTrailerScraper != null) {
      List<MediaScraper> selectedTrailerScrapers = new ArrayList<>();
      for (MediaScraper trailerScraper : TvShowList.getInstance().getAvailableTrailerScrapers()) {
        if (TvShowModuleManager.SETTINGS.getTrailerScrapers().contains(trailerScraper.getId())) {
          selectedTrailerScrapers.add(trailerScraper);
        }
      }
      if (!selectedTrailerScrapers.isEmpty()) {
        cbTrailerScraper.setSelectedItems(selectedTrailerScrapers);
      }
    }

    // pre-set configs
    if (cbTvShowScraperConfig != null) {
      List<TvShowScraperMetadataConfig> config = new ArrayList<>(TvShowModuleManager.SETTINGS.getTvShowScraperMetadataConfig());
      // only take artwork if only artwork has been requested
      if (artworkOnly) {
        config = config.stream().filter(ScraperMetadataConfig::isArtwork).collect(Collectors.toList());
      }
      cbTvShowScraperConfig.setSelectedItems(config);
    }
    if (cbEpisodeScraperConfig != null) {
      List<TvShowEpisodeScraperMetadataConfig> config = new ArrayList<>(TvShowModuleManager.SETTINGS.getEpisodeScraperMetadataConfig());
      // only take artwork if only artwork has been requested
      if (artworkOnly) {
        config = config.stream().filter(ScraperMetadataConfig::isArtwork).collect(Collectors.toList());
      }
      cbEpisodeScraperConfig.setSelectedItems(config);
    }
  }

  public void setMetadataScraper(MediaScraper metadataScraper) {
    cbMetadataScraper.setSelectedItem(metadataScraper);
  }

  public void setLanguage(MediaLanguages language) {
    cbLanguage.setSelectedItem(language);
  }

  /**
   * Pass the tv show search and scrape config to the caller.
   * 
   * @return the tv show search and scrape config
   */
  public TvShowSearchAndScrapeOptions getTvShowSearchAndScrapeOptions() {
    TvShowSearchAndScrapeOptions tvShowSearchAndScrapeConfig = new TvShowSearchAndScrapeOptions();
    tvShowSearchAndScrapeConfig.setCertificationCountry(TvShowModuleManager.SETTINGS.getCertificationCountry());
    tvShowSearchAndScrapeConfig.setReleaseDateCountry(TvShowModuleManager.SETTINGS.getReleaseDateCountry());

    // language
    tvShowSearchAndScrapeConfig.setLanguage((MediaLanguages) cbLanguage.getSelectedItem());

    // metadata provider
    if (cbMetadataScraper != null) {
      tvShowSearchAndScrapeConfig.setMetadataScraper((MediaScraper) cbMetadataScraper.getSelectedItem());
    }

    // artwork scrapers
    if (cbArtworkScraper != null) {
      tvShowSearchAndScrapeConfig.setArtworkScraper(cbArtworkScraper.getSelectedItems());
    }

    // trailer scrapers
    if (cbTrailerScraper != null) {
      tvShowSearchAndScrapeConfig.setTrailerScraper(cbTrailerScraper.getSelectedItems());
    }

    return tvShowSearchAndScrapeConfig;
  }

  /**
   * Pass the episode search and scrape config to the caller.
   *
   * @return the episode search and scrape config
   */
  public TvShowEpisodeSearchAndScrapeOptions getTvShowEpisodeSearchAndScrapeOptions() {
    TvShowEpisodeSearchAndScrapeOptions episodeSearchAndScrapeOptions = new TvShowEpisodeSearchAndScrapeOptions();
    episodeSearchAndScrapeOptions.setCertificationCountry(TvShowModuleManager.SETTINGS.getCertificationCountry());
    episodeSearchAndScrapeOptions.setReleaseDateCountry(TvShowModuleManager.SETTINGS.getReleaseDateCountry());

    // language
    episodeSearchAndScrapeOptions.setLanguage((MediaLanguages) cbLanguage.getSelectedItem());

    // metadata provider
    episodeSearchAndScrapeOptions.setMetadataScraper((MediaScraper) cbMetadataScraper.getSelectedItem());

    // artwork scrapers
    episodeSearchAndScrapeOptions.setArtworkScraper(cbArtworkScraper.getSelectedItems());

    return episodeSearchAndScrapeOptions;
  }

  /**
   * pass the tv show meta data config to the caller
   * 
   * @return a list of meta data config
   */
  public List<TvShowScraperMetadataConfig> getTvShowScraperMetadataConfig() {
    return cbTvShowScraperConfig.getSelectedItems();
  }

  /**
   * pass the episode meta data config to the caller
   * 
   * @return a list of meta data config
   */
  public List<TvShowEpisodeScraperMetadataConfig> getTvShowEpisodeScraperMetadataConfig() {
    return cbEpisodeScraperConfig.getSelectedItems();
  }

  /**
   * Should start scrape.
   * 
   * @return true, if successful
   */
  public boolean shouldStartScrape() {
    return startScrape;
  }

  /**
   * create a new instance of the {@link TvShowScrapeMetadataDialog} for artwork scraping
   *
   * @param title
   *          the title for the dialog
   * @return a new instance of {@link TvShowScrapeMetadataDialog}
   */
  public static TvShowScrapeMetadataDialog createArtworkScrapeDialog(String title) {
    return new TvShowScrapeMetadataDialog(title, true, true);
  }

  /**
   * create a new instance of the {@link TvShowScrapeMetadataDialog} for TV show and episode scraping
   *
   * @param title
   *          the title for the dialog
   * @return a new instance of {@link TvShowScrapeMetadataDialog}
   */
  public static TvShowScrapeMetadataDialog createScrapeDialog(String title) {
    return new TvShowScrapeMetadataDialog(title, false, true);
  }

  /**
   * create a new instance of the {@link TvShowScrapeMetadataDialog} for episode only scraping
   *
   * @param title
   *          the title for the dialog
   * @return a new instance of {@link TvShowScrapeMetadataDialog}
   */
  public static TvShowScrapeMetadataDialog createEpisodeScrapeDialog(String title) {
    return new TvShowScrapeMetadataDialog(title, false, false);
  }
}
