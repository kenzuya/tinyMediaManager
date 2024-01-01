/*
 * Copyright 2012 - 2024 Manuel Laggner
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
import static org.tinymediamanager.ui.TmmFontHelper.L2;

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.swingbinding.JTableBinding;
import org.jdesktop.swingbinding.SwingBindings;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.TrailerQuality;
import org.tinymediamanager.core.TrailerSources;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieSettings;
import org.tinymediamanager.core.movie.filenaming.MovieTrailerNaming;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.ui.ScraperInTable;
import org.tinymediamanager.ui.TableColumnResizer;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.components.CollapsiblePanel;
import org.tinymediamanager.ui.components.DocsButton;
import org.tinymediamanager.ui.components.ReadOnlyTextPane;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.table.TmmTable;
import org.tinymediamanager.ui.panels.MediaScraperConfigurationPanel;
import org.tinymediamanager.ui.panels.ScrollablePanel;

import net.miginfocom.swing.MigLayout;

/**
 * The Class MovieTrailerSettingsPanel. To maintain trailer related settings
 *
 * @author Manuel Laggner
 */
class MovieTrailerSettingsPanel extends JPanel {
  private final MovieSettings        settings                   = MovieModuleManager.getInstance().getSettings();
  private final List<ScraperInTable> scrapers                   = new ArrayList<>();
  private final ItemListener         checkBoxListener;

  private final ButtonGroup          trailerFilenameButtonGroup = new ButtonGroup();

  private TmmTable                   tableScraperInTable;
  private JTextPane                  tpScraperDescription;
  private JComboBox<TrailerSources>  cbTrailerSource;
  private JComboBox<TrailerQuality>  cbTrailerQuality;
  private JCheckBox                  checkBox;
  private JCheckBox                  chckbxAutomaticTrailerDownload;
  private JPanel                     panelScraperOptions;
  private JCheckBox                  cbTrailerFilename1;
  private JCheckBox                  cbTrailerFilename2;
  private JCheckBox                  cbTrailerFilename3;
  private JCheckBox                  cbTrailerFilename4;
  private JLabel                     lblAutomaticTrailerDownloadHint;

  MovieTrailerSettingsPanel() {
    checkBoxListener = e -> checkChanges();

    // implement checkBoxListener for preset events
    settings.addPropertyChangeListener(evt -> {
      if ("preset".equals(evt.getPropertyName())) {
        buildCheckBoxes();
      }
    });

    // data init
    List<String> enabledTrailerProviders = settings.getTrailerScrapers();
    int selectedIndex = -1;
    int counter = 0;
    for (MediaScraper scraper : MovieModuleManager.getInstance().getMovieList().getAvailableTrailerScrapers()) {
      ScraperInTable ScraperInTable = new ScraperInTable(scraper);
      if (enabledTrailerProviders.contains(ScraperInTable.getScraperId())) {
        ScraperInTable.setActive(true);
        if (selectedIndex < 0) {
          selectedIndex = counter;
        }
      }
      scrapers.add(ScraperInTable);
      counter++;
    }

    // UI init
    initComponents();
    initDataBindings();

    // adjust table columns
    // Checkbox and Logo shall have minimal width
    TableColumnResizer.setMaxWidthForColumn(tableScraperInTable, 0, 2);
    TableColumnResizer.setMaxWidthForColumn(tableScraperInTable, 1, 10);
    TableColumnResizer.adjustColumnPreferredWidths(tableScraperInTable, 5);

    tableScraperInTable.getModel().addTableModelListener(arg0 -> {
      // click on the checkbox
      if (arg0.getColumn() == 0) {
        int row = arg0.getFirstRow();
        ScraperInTable changedScraper = scrapers.get(row);
        if (changedScraper.getActive()) {
          settings.addMovieTrailerScraper(changedScraper.getScraperId());
        }
        else {
          settings.removeMovieTrailerScraper(changedScraper.getScraperId());
        }
      }
    });

    // implement selection listener to load settings
    tableScraperInTable.getSelectionModel().addListSelectionListener(e -> {
      int index = tableScraperInTable.convertRowIndexToModel(tableScraperInTable.getSelectedRow());
      if (index > -1) {
        panelScraperOptions.removeAll();
        if (scrapers.get(index).getMediaProvider().getProviderInfo().getConfig().hasConfig()) {
          panelScraperOptions.add(new MediaScraperConfigurationPanel(scrapers.get(index).getMediaProvider()));
        }
        panelScraperOptions.revalidate();
      }
    });

    {
      // add a CSS rule to force body tags to use the default label font
      // instead of the value in javax.swing.text.html.default.csss
      Font font = UIManager.getFont("Label.font");
      Color color = UIManager.getColor("Label.foreground");
      String bodyRule = "body { font-family: " + font.getFamily() + "; " + "font-size: " + font.getSize() + "pt; color: rgb(" + color.getRed() + ","
          + color.getGreen() + "," + color.getBlue() + "); }";
      tpScraperDescription.setEditorKit(new HTMLEditorKit());
      ((HTMLDocument) tpScraperDescription.getDocument()).getStyleSheet().addRule(bodyRule);
    }

    // select default movie scraper
    if (selectedIndex < 0) {
      selectedIndex = 0;
    }
    if (counter > 0) {
      tableScraperInTable.getSelectionModel().setSelectionInterval(selectedIndex, selectedIndex);
    }

    buildCheckBoxes();
  }

  private void buildCheckBoxes() {
    cbTrailerFilename1.removeItemListener(checkBoxListener);
    cbTrailerFilename2.removeItemListener(checkBoxListener);
    cbTrailerFilename3.removeItemListener(checkBoxListener);
    cbTrailerFilename4.removeItemListener(checkBoxListener);
    clearSelection(cbTrailerFilename1, cbTrailerFilename2, cbTrailerFilename3, cbTrailerFilename4);

    // trailer filenames
    List<MovieTrailerNaming> movieTrailerFilenames = settings.getTrailerFilenames();
    if (movieTrailerFilenames.contains(MovieTrailerNaming.FILENAME_TRAILER)) {
      cbTrailerFilename1.setSelected(true);
    }
    else if (movieTrailerFilenames.contains(MovieTrailerNaming.MOVIE_TRAILER)) {
      cbTrailerFilename2.setSelected(true);
    }
    else if (movieTrailerFilenames.contains(MovieTrailerNaming.TRAILERS_FILENAME_TRAILER)) {
      cbTrailerFilename3.setSelected(true);
    }
    else if (movieTrailerFilenames.contains(MovieTrailerNaming.TRAILER)) {
      cbTrailerFilename4.setSelected(true);
    }

    cbTrailerFilename1.addItemListener(checkBoxListener);
    cbTrailerFilename2.addItemListener(checkBoxListener);
    cbTrailerFilename3.addItemListener(checkBoxListener);
    cbTrailerFilename4.addItemListener(checkBoxListener);
  }

  private void clearSelection(JCheckBox... checkBoxes) {
    for (JCheckBox checkBox : checkBoxes) {
      checkBox.setSelected(false);
    }
  }

  /**
   * check changes of checkboxes
   */
  private void checkChanges() {
    // set trailer filenames
    settings.clearTrailerFilenames();
    if (cbTrailerFilename1.isSelected()) {
      settings.addTrailerFilename(MovieTrailerNaming.FILENAME_TRAILER);
    }
    if (cbTrailerFilename2.isSelected()) {
      settings.addTrailerFilename(MovieTrailerNaming.MOVIE_TRAILER);
    }
    if (cbTrailerFilename3.isSelected()) {
      settings.addTrailerFilename(MovieTrailerNaming.TRAILERS_FILENAME_TRAILER);
    }
    if (cbTrailerFilename4.isSelected()) {
      settings.addTrailerFilename(MovieTrailerNaming.TRAILER);
    }
  }

  private void initComponents() {
    setLayout(new MigLayout("hidemode 0", "[600lp,grow]", "[][15lp!][]"));
    {
      JPanel panelScraper = new JPanel(new MigLayout("hidemode 1, insets 0", "[20lp!][grow]", "[][shrink 0][]"));

      JLabel lblScraper = new TmmLabel(TmmResourceBundle.getString("scraper.trailer"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelScraper, lblScraper, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/movies/settings#trailer"));
      add(collapsiblePanel, "cell 0 0,wmin 0,grow");
      {
        tableScraperInTable = new TmmTable() {
          @Override
          public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
            java.awt.Component comp = super.prepareRenderer(renderer, row, column);
            ScraperInTable scraper = scrapers.get(row);
            comp.setEnabled(scraper.isEnabled());
            return comp;
          }
        };
        tableScraperInTable.setRowHeight(29);
        tableScraperInTable.setShowGrid(true);
        panelScraper.add(tableScraperInTable, "cell 1 0,grow");

        JSeparator separator = new JSeparator();
        panelScraper.add(separator, "cell 1 1,growx");

        JPanel panelScraperDetails = new ScrollablePanel();
        panelScraper.add(panelScraperDetails, "cell 1 2,grow");
        panelScraperDetails.setLayout(new MigLayout("insets 0", "[grow]", "[][grow]"));

        tpScraperDescription = new ReadOnlyTextPane();
        tpScraperDescription.setEditorKit(new HTMLEditorKit());
        panelScraperDetails.add(tpScraperDescription, "cell 0 0,grow");

        panelScraperOptions = new ScrollablePanel();
        panelScraperOptions.setLayout(new FlowLayout(FlowLayout.LEFT));
        panelScraperDetails.add(panelScraperOptions, "cell 0 1,grow");
      }
    }
    {
      JPanel panelOptions = new JPanel();
      panelOptions.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "")); // 16lp ~ width of the

      JLabel lblOptionsT = new TmmLabel(TmmResourceBundle.getString("Settings.advancedoptions"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelOptions, lblOptionsT, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/movies/settings#advanced-options-2"));
      add(collapsiblePanel, "cell 0 2,growx, wmin 0");
      {
        checkBox = new JCheckBox(TmmResourceBundle.getString("Settings.trailer.preferred"));
        panelOptions.add(checkBox, "cell 1 0 2 1");

        JLabel lblTrailerSource = new JLabel(TmmResourceBundle.getString("Settings.trailer.source"));
        panelOptions.add(lblTrailerSource, "cell 2 1");

        cbTrailerSource = new JComboBox();
        cbTrailerSource.setModel(new DefaultComboBoxModel<>(TrailerSources.values()));
        panelOptions.add(cbTrailerSource, "cell 2 1");

        JLabel lblTrailerQuality = new JLabel(TmmResourceBundle.getString("Settings.trailer.quality"));
        panelOptions.add(lblTrailerQuality, "cell 2 2");

        cbTrailerQuality = new JComboBox();
        cbTrailerQuality.setModel(new DefaultComboBoxModel<>(TrailerQuality.values()));
        panelOptions.add(cbTrailerQuality, "cell 2 2");

        chckbxAutomaticTrailerDownload = new JCheckBox(TmmResourceBundle.getString("Settings.trailer.automaticdownload"));
        panelOptions.add(chckbxAutomaticTrailerDownload, "cell 2 3");

        lblAutomaticTrailerDownloadHint = new JLabel(TmmResourceBundle.getString("Settings.trailer.automaticdownload.hint"));
        panelOptions.add(lblAutomaticTrailerDownloadHint, "cell 2 4");
        TmmFontHelper.changeFont(lblAutomaticTrailerDownloadHint, L2);

        JPanel panelTrailerFilenames = new JPanel();
        panelOptions.add(panelTrailerFilenames, "cell 1 5 2 1");
        panelTrailerFilenames.setLayout(new MigLayout("insets 0", "[][]", "[][][]"));

        JLabel lblTrailerFileNaming = new JLabel(TmmResourceBundle.getString("Settings.trailerFileNaming"));
        panelTrailerFilenames.add(lblTrailerFileNaming, "cell 0 0");

        cbTrailerFilename1 = new JCheckBox(
            TmmResourceBundle.getString("Settings.moviefilename") + "-trailer." + TmmResourceBundle.getString("Settings.artwork.extension"));
        trailerFilenameButtonGroup.add(cbTrailerFilename1);
        panelTrailerFilenames.add(cbTrailerFilename1, "cell 1 0");

        cbTrailerFilename2 = new JCheckBox("movie-trailer." + TmmResourceBundle.getString("Settings.artwork.extension"));
        trailerFilenameButtonGroup.add(cbTrailerFilename2);
        panelTrailerFilenames.add(cbTrailerFilename2, "cell 1 1");

        cbTrailerFilename3 = new JCheckBox("trailers/" + TmmResourceBundle.getString("Settings.moviefilename") + "-trailer."
            + TmmResourceBundle.getString("Settings.artwork.extension"));
        trailerFilenameButtonGroup.add(cbTrailerFilename3);
        panelTrailerFilenames.add(cbTrailerFilename3, "cell 1 2");

        cbTrailerFilename4 = new JCheckBox("trailer." + TmmResourceBundle.getString("Settings.artwork.extension"));
        trailerFilenameButtonGroup.add(cbTrailerFilename4);
        panelTrailerFilenames.add(cbTrailerFilename4, "cell 1 3");
      }
    }
  }

  protected void initDataBindings() {
    JTableBinding<ScraperInTable, List<ScraperInTable>, JTable> jTableBinding = SwingBindings.createJTableBinding(UpdateStrategy.READ_WRITE, scrapers,
        tableScraperInTable);
    //
    BeanProperty<ScraperInTable, Boolean> ScraperInTableBeanProperty = BeanProperty.create("active");
    jTableBinding.addColumnBinding(ScraperInTableBeanProperty)
        .setColumnName(TmmResourceBundle.getString("Settings.active"))
        .setColumnClass(Boolean.class);
    //
    BeanProperty<ScraperInTable, Icon> ScraperInTableBeanProperty_1 = BeanProperty.create("scraperLogo");
    jTableBinding.addColumnBinding(ScraperInTableBeanProperty_1)
        .setColumnName(TmmResourceBundle.getString("mediafiletype.logo"))
        .setEditable(false)
        .setColumnClass(ImageIcon.class);
    //
    BeanProperty<ScraperInTable, String> ScraperInTableBeanProperty_2 = BeanProperty.create("scraperName");
    jTableBinding.addColumnBinding(ScraperInTableBeanProperty_2)
        .setColumnName(TmmResourceBundle.getString("metatag.name"))
        .setEditable(false)
        .setColumnClass(String.class);
    //
    jTableBinding.bind();
    //
    BeanProperty<JTable, String> jTableBeanProperty = BeanProperty.create("selectedElement.scraperDescription");
    BeanProperty<JTextPane, String> jTextPaneBeanProperty = BeanProperty.create("text");
    AutoBinding<JTable, String, JTextPane, String> autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ, tableScraperInTable,
        jTableBeanProperty, tpScraperDescription, jTextPaneBeanProperty);
    autoBinding.bind();
    //
    BeanProperty<MovieSettings, TrailerSources> movieSettingsBeanProperty = BeanProperty.create("trailerSource");
    BeanProperty<JComboBox<TrailerSources>, Object> jComboBoxBeanProperty = BeanProperty.create("selectedItem");
    AutoBinding<MovieSettings, TrailerSources, JComboBox<TrailerSources>, Object> autoBinding_1 = Bindings
        .createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty, cbTrailerSource, jComboBoxBeanProperty);
    autoBinding_1.bind();
    //
    BeanProperty<MovieSettings, TrailerQuality> movieSettingsBeanProperty_1 = BeanProperty.create("trailerQuality");
    BeanProperty<JComboBox<TrailerQuality>, Object> jComboBoxBeanProperty_1 = BeanProperty.create("selectedItem");
    AutoBinding<MovieSettings, TrailerQuality, JComboBox<TrailerQuality>, Object> autoBinding_2 = Bindings
        .createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_1, cbTrailerQuality, jComboBoxBeanProperty_1);
    autoBinding_2.bind();
    //
    BeanProperty<MovieSettings, Boolean> movieSettingsBeanProperty_2 = BeanProperty.create("useTrailerPreference");
    BeanProperty<JCheckBox, Boolean> jCheckBoxBeanProperty = BeanProperty.create("selected");
    AutoBinding<MovieSettings, Boolean, JCheckBox, Boolean> autoBinding_3 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings,
        movieSettingsBeanProperty_2, checkBox, jCheckBoxBeanProperty);
    autoBinding_3.bind();
    //
    BeanProperty<MovieSettings, Boolean> movieSettingsBeanProperty_3 = BeanProperty.create("automaticTrailerDownload");
    AutoBinding<MovieSettings, Boolean, JCheckBox, Boolean> autoBinding_4 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings,
        movieSettingsBeanProperty_3, chckbxAutomaticTrailerDownload, jCheckBoxBeanProperty);
    autoBinding_4.bind();
    //
    BeanProperty<JCheckBox, Boolean> jCheckBoxBeanProperty_1 = BeanProperty.create("enabled");
    AutoBinding<JCheckBox, Boolean, JCheckBox, Boolean> autoBinding_5 = Bindings.createAutoBinding(UpdateStrategy.READ, checkBox,
        jCheckBoxBeanProperty, chckbxAutomaticTrailerDownload, jCheckBoxBeanProperty_1);
    autoBinding_5.bind();
    //
    BeanProperty<JLabel, Boolean> jLabelBeanProperty = BeanProperty.create("enabled");
    AutoBinding<JCheckBox, Boolean, JLabel, Boolean> autoBinding_6 = Bindings.createAutoBinding(UpdateStrategy.READ, checkBox, jCheckBoxBeanProperty,
        lblAutomaticTrailerDownloadHint, jLabelBeanProperty);
    autoBinding_6.bind();
  }
}
