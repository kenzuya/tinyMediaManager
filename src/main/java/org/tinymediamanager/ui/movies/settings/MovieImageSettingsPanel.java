/*
 * Copyright 2012 - 2018 Manuel Laggner
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
package org.tinymediamanager.ui.movies.settings;

import static org.tinymediamanager.ui.TmmFontHelper.H3;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.observablecollections.ObservableCollections;
import org.jdesktop.swingbinding.JTableBinding;
import org.jdesktop.swingbinding.SwingBindings;
import org.tinymediamanager.core.movie.MovieList;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieSettings;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.scraper.entities.MediaArtwork.FanartSizes;
import org.tinymediamanager.scraper.entities.MediaArtwork.PosterSizes;
import org.tinymediamanager.ui.ScraperInTable;
import org.tinymediamanager.ui.TableColumnResizer;
import org.tinymediamanager.ui.UTF8Control;
import org.tinymediamanager.ui.components.CollapsiblePanel;
import org.tinymediamanager.ui.components.SettingsPanelFactory;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.table.TmmTable;
import org.tinymediamanager.ui.panels.MediaScraperConfigurationPanel;
import org.tinymediamanager.ui.panels.ScrollablePanel;

import net.miginfocom.swing.MigLayout;

/**
 * The class {@link MovieImageSettingsPanel} is used to display artwork scraper settings.
 *
 * @author Manuel Laggner
 */
class MovieImageSettingsPanel extends JPanel {
  private static final long           serialVersionUID = 7312645402037806284L;
  /** @wbp.nls.resourceBundle messages */
  private static final ResourceBundle BUNDLE           = ResourceBundle.getBundle("messages", new UTF8Control()); //$NON-NLS-1$ @wbp.nls.resourceBundle

  private MovieSettings               settings         = MovieModuleManager.SETTINGS;
  private List<ScraperInTable>        scrapers         = ObservableCollections.observableList(new ArrayList<>());

  private JComboBox                   cbImagePosterSize;
  private JComboBox                   cbImageFanartSize;
  private TmmTable                    tableScraper;
  private JTextPane                   tpScraperDescription;
  private JPanel                      panelScraperOptions;

  /**
   * Instantiates a new movie image settings panel.
   */
  MovieImageSettingsPanel() {
    // UI init
    initComponents();
    initDataBindings();

    // data init
    List<String> enabledArtworkProviders = settings.getArtworkScrapers();
    int selectedIndex = -1;
    int counter = 0;
    for (MediaScraper scraper : MovieList.getInstance().getAvailableArtworkScrapers()) {
      ScraperInTable artworkScraper = new ScraperInTable(scraper);
      if (enabledArtworkProviders.contains(artworkScraper.getScraperId())) {
        artworkScraper.setActive(true);
        if (selectedIndex < 0) {
          selectedIndex = counter;
        }
      }
      scrapers.add(artworkScraper);
      counter++;
    }

    // adjust table columns
    // Checkbox and Logo shall have minimal width
    TableColumnResizer.setMaxWidthForColumn(tableScraper, 0, 2);
    TableColumnResizer.setMaxWidthForColumn(tableScraper, 1, 2);
    TableColumnResizer.adjustColumnPreferredWidths(tableScraper, 5);

    tableScraper.getModel().addTableModelListener(arg0 -> {
      // click on the checkbox
      if (arg0.getColumn() == 0) {
        int row = arg0.getFirstRow();
        ScraperInTable changedScraper = scrapers.get(row);
        if (changedScraper.getActive()) {
          settings.addMovieArtworkScraper(changedScraper.getScraperId());
        }
        else {
          settings.removeMovieArtworkScraper(changedScraper.getScraperId());
        }
      }
    });

    // implement selection checkBoxListener to load settings
    tableScraper.getSelectionModel().addListSelectionListener(e -> {
      int index = tableScraper.convertRowIndexToModel(tableScraper.getSelectedRow());
      if (index > -1) {
        panelScraperOptions.removeAll();
        if (scrapers.get(index).getMediaProvider().getProviderInfo().getConfig().hasConfig()) {
          panelScraperOptions.add(new MediaScraperConfigurationPanel(scrapers.get(index).getMediaProvider()));
        }
        panelScraperOptions.revalidate();
      }
    });

    // add a CSS rule to force body tags to use the default label font
    // instead of the value in javax.swing.text.html.default.csss
    Font font = UIManager.getFont("Label.font");
    Color color = UIManager.getColor("Label.foreground");
    String bodyRule = "body { font-family: " + font.getFamily() + "; font-size: " + font.getSize() + "pt; color: rgb(" + color.getRed() + ","
        + color.getGreen() + "," + color.getBlue() + "); }";
    tpScraperDescription.setEditorKit(new HTMLEditorKit());
    ((HTMLDocument) tpScraperDescription.getDocument()).getStyleSheet().addRule(bodyRule);

    // select default artwork scraper
    if (selectedIndex < 0) {
      selectedIndex = 0;
    }
    if (counter > 0) {
      tableScraper.getSelectionModel().setSelectionInterval(selectedIndex, selectedIndex);
    }
  }

  private void initComponents() {
    setLayout(new MigLayout("", "[400lp,grow]", "[][15lp!][]"));
    {
      JPanel panelScraper = new JPanel(new MigLayout("hidemode 1, insets 0", "[20lp!][grow]", "[100lp:200lp,grow][][200lp:300lp,grow]"));

      JLabel lblScraperT = new TmmLabel(BUNDLE.getString("scraper.artwork"), H3); //$NON-NLS-1$
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelScraper, lblScraperT, true);
      add(collapsiblePanel, "cell 0 0,wmin 0,grow");
      {
        JScrollPane scrollPaneScraper = new JScrollPane(tableScraper);
        panelScraper.add(scrollPaneScraper, "cell 1 0,grow");

        tableScraper = new TmmTable();
        tableScraper.setRowHeight(29);
        tableScraper.configureScrollPane(scrollPaneScraper);

        JSeparator separator = new JSeparator();
        panelScraper.add(separator, "cell 1 1,growx");

        JScrollPane scrollPaneScraperDetails = new JScrollPane();
        panelScraper.add(scrollPaneScraperDetails, "cell 1 2,grow");

        scrollPaneScraperDetails.setBorder(null);
        scrollPaneScraperDetails.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        JPanel panelScraperDetails = new ScrollablePanel();
        scrollPaneScraperDetails.setViewportView(panelScraperDetails);
        panelScraperDetails.setLayout(new MigLayout("insets 0", "[grow]", "[][]"));

        tpScraperDescription = new JTextPane();
        tpScraperDescription.setOpaque(false);
        tpScraperDescription.setEditorKit(new HTMLEditorKit());
        panelScraperDetails.add(tpScraperDescription, "cell 0 0,grow");

        panelScraperOptions = new JPanel();
        panelScraperOptions.setLayout(new FlowLayout(FlowLayout.LEFT));
        panelScraperDetails.add(panelScraperOptions, "cell 0 1,grow");
      }
    }
    {
      JPanel panelOptions = SettingsPanelFactory.createSettingsPanel();

      JLabel lblOptionsT = new TmmLabel(BUNDLE.getString("Settings.advancedoptions"), H3); //$NON-NLS-1$
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelOptions, lblOptionsT, true);
      add(collapsiblePanel, "cell 0 2,growx, wmin 0");
      {
        JLabel lblImageTmdbPosterSize = new JLabel(BUNDLE.getString("image.poster.size")); //$NON-NLS-1$
        panelOptions.add(lblImageTmdbPosterSize, "cell 1 0 2 1");

        cbImagePosterSize = new JComboBox(PosterSizes.values());
        panelOptions.add(cbImagePosterSize, "cell 1 0");

        JLabel lblImageTmdbFanartSize = new JLabel(BUNDLE.getString("image.fanart.size")); //$NON-NLS-1$
        panelOptions.add(lblImageTmdbFanartSize, "cell 1 1 2 1");

        cbImageFanartSize = new JComboBox(FanartSizes.values());
        panelOptions.add(cbImageFanartSize, "cell 1 1");


      }
    }
  }

  protected void initDataBindings() {
    BeanProperty<MovieSettings, PosterSizes> settingsBeanProperty_5 = BeanProperty.create("imagePosterSize");
    BeanProperty<JComboBox, Object> jComboBoxBeanProperty = BeanProperty.create("selectedItem");
    AutoBinding<MovieSettings, PosterSizes, JComboBox, Object> autoBinding_4 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings,
        settingsBeanProperty_5, cbImagePosterSize, jComboBoxBeanProperty);
    autoBinding_4.bind();
    //
    BeanProperty<MovieSettings, FanartSizes> settingsBeanProperty_6 = BeanProperty.create("imageFanartSize");
    AutoBinding<MovieSettings, FanartSizes, JComboBox, Object> autoBinding_5 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings,
        settingsBeanProperty_6, cbImageFanartSize, jComboBoxBeanProperty);
    autoBinding_5.bind();
    //
    JTableBinding<ScraperInTable, List<ScraperInTable>, JTable> jTableBinding = SwingBindings.createJTableBinding(UpdateStrategy.READ_WRITE, scrapers,
        tableScraper);
    //
    BeanProperty<ScraperInTable, Boolean> artworkScraperBeanProperty = BeanProperty.create("active");
    jTableBinding.addColumnBinding(artworkScraperBeanProperty).setColumnName("Active").setColumnClass(Boolean.class);
    //
    BeanProperty<ScraperInTable, Icon> artworkScraperBeanProperty_1 = BeanProperty.create("scraperLogo");
    jTableBinding.addColumnBinding(artworkScraperBeanProperty_1).setColumnName("Logo").setEditable(false).setColumnClass(ImageIcon.class);
    //
    BeanProperty<ScraperInTable, String> artworkScraperBeanProperty_2 = BeanProperty.create("scraperName");
    jTableBinding.addColumnBinding(artworkScraperBeanProperty_2).setColumnName("Name").setEditable(false).setColumnClass(String.class);
    //
    jTableBinding.bind();
    //
    BeanProperty<JTable, String> jTableBeanProperty = BeanProperty.create("selectedElement.scraperDescription");
    BeanProperty<JTextPane, String> jTextPaneBeanProperty = BeanProperty.create("text");
    AutoBinding<JTable, String, JTextPane, String> autoBinding_23 = Bindings.createAutoBinding(UpdateStrategy.READ, tableScraper, jTableBeanProperty,
        tpScraperDescription, jTextPaneBeanProperty);
    autoBinding_23.bind();
  }
}
