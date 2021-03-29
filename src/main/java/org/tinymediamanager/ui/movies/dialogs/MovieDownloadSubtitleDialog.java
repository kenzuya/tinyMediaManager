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
package org.tinymediamanager.ui.movies.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.MovieList;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.components.ReadOnlyTextArea;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.combobox.MediaScraperCheckComboBox;
import org.tinymediamanager.ui.components.combobox.TmmCheckComboBox;
import org.tinymediamanager.ui.dialogs.TmmDialog;

import net.miginfocom.swing.MigLayout;

/**
 * The Class MovieDownloadSubtitleDialog. Download subtitles via file hash
 * 
 * @author Manuel Laggner
 */
public class MovieDownloadSubtitleDialog extends TmmDialog {
  private static final long         serialVersionUID = 3826984454317879241L;

  private final MediaScraperCheckComboBox        cbSubtitleScraper;
  private final TmmCheckComboBox<MediaLanguages> cbLanguage;

  private boolean                   startDownload    = false;

  public MovieDownloadSubtitleDialog(String title) {
    super(title, "downloadSubtitle");
    setMinimumSize(new Dimension(getWidth(), getHeight()));
    {

      JPanel panelScraper = new JPanel();
      getContentPane().add(panelScraper, BorderLayout.CENTER);
      panelScraper.setLayout(new MigLayout("", "[][300lp]", "[][][20lp:n][]"));

      JLabel lblScraper = new TmmLabel(TmmResourceBundle.getString("scraper"));
      panelScraper.add(lblScraper, "cell 0 0,alignx right");

      cbSubtitleScraper = new MediaScraperCheckComboBox(MovieList.getInstance().getAvailableSubtitleScrapers());
      panelScraper.add(cbSubtitleScraper, "cell 1 0,growx");

      JLabel lblLanguage = new TmmLabel(TmmResourceBundle.getString("metatag.language"));
      panelScraper.add(lblLanguage, "cell 0 1,alignx right");

      cbLanguage = new TmmCheckComboBox<>(MediaLanguages.valuesSorted());
      panelScraper.add(cbLanguage, "cell 1 1,growx");

      JTextArea taHint = new ReadOnlyTextArea(TmmResourceBundle.getString("movie.download.subtitles.hint"));
      taHint.setOpaque(false);
      panelScraper.add(taHint, "cell 0 3 2 1,grow");
    }

    {
      JButton btnCancel = new JButton(TmmResourceBundle.getString("Button.cancel"));
      btnCancel.setIcon(IconManager.CANCEL_INV);
      btnCancel.addActionListener(e -> {
        startDownload = false;
        setVisible(false);
      });

      addButton(btnCancel);

      JButton btnStart = new JButton(TmmResourceBundle.getString("scraper.start"));
      btnStart.setIcon(IconManager.APPLY_INV);
      btnStart.addActionListener(e -> {
        startDownload = true;
        setVisible(false);
      });
      addDefaultButton(btnStart);
    }
    // set data

    // scraper
    List<MediaScraper> selectedSubtitleScrapers = new ArrayList<>();
    for (MediaScraper subtitleScraper : MovieList.getInstance().getAvailableSubtitleScrapers()) {
      if (MovieModuleManager.SETTINGS.getSubtitleScrapers().contains(subtitleScraper.getId())) {
        selectedSubtitleScrapers.add(subtitleScraper);
      }
    }
    if (!selectedSubtitleScrapers.isEmpty()) {
      cbSubtitleScraper.setSelectedItems(selectedSubtitleScrapers);
    }

    cbLanguage.setSelectedItems(Collections.singletonList(MovieModuleManager.SETTINGS.getSubtitleScraperLanguage()));
  }

  /**
   * Get the selected scrapers
   * 
   * @return the selected subtitle scrapers
   */
  public List<MediaScraper> getSubtitleScrapers() {
    // scrapers
    return new ArrayList<>(cbSubtitleScraper.getSelectedItems());
  }

  /**
   * Get the selected Languages
   *
   * @return the selected languages
   */
  public List<MediaLanguages> getLanguages() {
    return cbLanguage.getSelectedItems();
  }

  /**
   * Should start download.
   * 
   * @return true, if successful
   */
  public boolean shouldStartDownload() {
    return startDownload;
  }
}
