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
package org.tinymediamanager.ui.movies.settings;

import static org.tinymediamanager.core.AbstractSettings.UniversalFilterFields.ACTOR;
import static org.tinymediamanager.core.AbstractSettings.UniversalFilterFields.COUNTRY;
import static org.tinymediamanager.core.AbstractSettings.UniversalFilterFields.DIRECTOR;
import static org.tinymediamanager.core.AbstractSettings.UniversalFilterFields.FILENAME;
import static org.tinymediamanager.core.AbstractSettings.UniversalFilterFields.NOTE;
import static org.tinymediamanager.core.AbstractSettings.UniversalFilterFields.PRODUCER;
import static org.tinymediamanager.core.AbstractSettings.UniversalFilterFields.PRODUCTION_COMPANY;
import static org.tinymediamanager.core.AbstractSettings.UniversalFilterFields.SPOKEN_LANGUAGE;
import static org.tinymediamanager.core.AbstractSettings.UniversalFilterFields.TAGS;
import static org.tinymediamanager.core.AbstractSettings.UniversalFilterFields.WRITER;
import static org.tinymediamanager.ui.TmmFontHelper.H3;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import org.apache.commons.lang3.StringUtils;
import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.Property;
import org.jdesktop.swingbinding.JListBinding;
import org.jdesktop.swingbinding.SwingBindings;
import org.tinymediamanager.core.AbstractSettings;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieScraperMetadataConfig;
import org.tinymediamanager.core.movie.MovieSettings;
import org.tinymediamanager.core.movie.MovieSettingsDefaults;
import org.tinymediamanager.core.movie.MovieTextMatcherList;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.thirdparty.trakttv.MovieClearTraktTvTask;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.components.CollapsiblePanel;
import org.tinymediamanager.ui.components.DocsButton;
import org.tinymediamanager.ui.components.JHintCheckBox;
import org.tinymediamanager.ui.components.JHintLabel;
import org.tinymediamanager.ui.components.SquareIconButton;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.combobox.AutoCompleteSupport;
import org.tinymediamanager.ui.components.combobox.AutocompleteComboBox;

import net.miginfocom.swing.MigLayout;

/**
 * The class MovieSettingsPanel is used for displaying some movie related settings
 * 
 * @author Manuel Laggner
 */
public class MovieSettingsPanel extends JPanel {
  private static final long                                serialVersionUID = -4173835431245178069L;
  private static final int                                 COL_COUNT        = 7;

  private final MovieSettings                              settings         = MovieModuleManager.getInstance().getSettings();

  private JButton                                          btnClearTraktData;
  private JCheckBox                                        chckbxTraktSync;
  private JCheckBox                                        chckbxRenameAfterScrape;
  private JCheckBox                                        chckbxARDAfterScrape;
  private JCheckBox                                        chckbxAutoUpdateOnStart;
  private JCheckBox                                        chckbxBuildImageCache;
  private JCheckBox                                        chckbxExtractArtworkFromVsmeta;
  private JCheckBox                                        chckbxRuntimeFromMi;
  private JButton                                          btnPresetKodi;
  private JButton                                          btnPresetXbmc;
  private JButton                                          btnPresetMediaPortal1;
  private JButton                                          btnPresetMediaPortal2;
  private JButton                                          btnPresetPlex;
  private AutocompleteComboBox<String>                     cbRating;
  private JCheckBox                                        chckbxIncludeExternalAudioStreams;
  private JCheckBox                                        chckbxMovieTableTooltips;
  private JCheckBox                                        chckbxUseMediainfoMetadata;

  private JList                                            listRatings;
  private JButton                                          btnAddRating;
  private JButton                                          btnRemoveRating;
  private JButton                                          btnMoveRatingUp;
  private JButton                                          btnMoveRatingDown;

  private JCheckBox                                        chckbxTitle;
  private JCheckBox                                        chckbxSortableTitle;
  private JCheckBox                                        chckbxOriginalTitle;
  private JCheckBox                                        chckbxSortableOriginalTitle;
  private JCheckBox                                        chckbxSortTitle;
  private JCheckBox                                        chckbxNote;

  private JCheckBox                                        chckbxUniversalNote;
  private JCheckBox                                        chckbxUniversalFilename;
  private JCheckBox                                        chckbxUniversalTags;
  private JCheckBox                                        chckbxUniversalProductionCompany;
  private JCheckBox                                        chckbxUniversalCountry;
  private JCheckBox                                        chckbxUniversalLanguages;
  private JCheckBox                                        chckbxUniversalActors;
  private JCheckBox                                        chckbxUniversalProducers;
  private JCheckBox                                        chckbxUniversalDirectors;
  private JCheckBox                                        chckbxUniversalWriters;

  private JCheckBox                                        chckbxTraktSyncWatched;
  private JCheckBox                                        chckbxTraktSyncRating;
  private JCheckBox                                        chckbxTraktSyncCollection;
  private JCheckBox                                        chckbxStoreFilter;
  private JHintCheckBox                                    chckbxDisplayAllMissingMetadata;
  private JHintCheckBox                                    chckbxDisplayAllMissingArtwork;
  private JButton                                          btnPresetJellyfin;
  private JButton                                          btnPresetEmby;

  private final Map<MovieScraperMetadataConfig, JCheckBox> metadataCheckBoxes;
  private final Map<MovieScraperMetadataConfig, JCheckBox> artworkCheckBoxes;
  private final ItemListener                               checkBoxListener;

  public MovieSettingsPanel() {
    metadataCheckBoxes = new LinkedHashMap<>();
    artworkCheckBoxes = new LinkedHashMap<>();
    checkBoxListener = e -> checkChanges();

    // UI initializations
    initComponents();
    initDataBindings();

    // logic initializations
    btnAddRating.addActionListener(arg0 -> {
      Object selectedItem = cbRating.getSelectedItem();

      // check, if text is selected (from auto completion), in this case we just
      // remove the selection
      Component editorComponent = cbRating.getEditor().getEditorComponent();
      if (editorComponent instanceof JTextField) {
        JTextField tf = (JTextField) editorComponent;
        String selectedText = tf.getSelectedText();
        if (selectedText != null) {
          tf.setSelectionStart(0);
          tf.setSelectionEnd(0);
          tf.setCaretPosition(tf.getText().length());
          return;
        }
      }

      if (selectedItem instanceof String && StringUtils.isNotBlank((String) selectedItem)) {
        MovieModuleManager.getInstance().getSettings().addRatingSource((String) selectedItem);

        // set text combobox text input to ""
        if (editorComponent instanceof JTextField) {
          AutoCompleteSupport<String> autoCompleteSupport = cbRating.getAutoCompleteSupport();
          autoCompleteSupport.setFirstItem(null);
          cbRating.setSelectedIndex(0);
          autoCompleteSupport.removeFirstItem();
        }
      }

    });

    btnRemoveRating.addActionListener(arg0 -> {
      int row = listRatings.getSelectedIndex();
      if (row != -1) { // nothing selected
        String ratingSource = settings.getRatingSources().get(row);
        MovieModuleManager.getInstance().getSettings().removeRatingSource(ratingSource);
      }
    });

    btnMoveRatingUp.addActionListener(arg0 -> {
      int row = listRatings.getSelectedIndex();
      if (row != -1 && row != 0) {
        settings.swapRatingSources(row, row - 1);
        row = row - 1;
        listRatings.setSelectedIndex(row);
        listRatings.updateUI();
      }
    });

    btnMoveRatingDown.addActionListener(arg0 -> {
      int row = listRatings.getSelectedIndex();
      if (row != -1 && row < listRatings.getModel().getSize() - 1) {
        settings.swapRatingSources(row, row + 1);
        row = row + 1;
        listRatings.setSelectedIndex(row);
        listRatings.updateUI();
      }
    });

    btnClearTraktData.addActionListener(e -> {
      Object[] options = { TmmResourceBundle.getString("Button.yes"), TmmResourceBundle.getString("Button.no") };
      int confirm = JOptionPane.showOptionDialog(null, TmmResourceBundle.getString("Settings.trakt.clearmovies.hint"),
          TmmResourceBundle.getString("Settings.trakt.clearmovies"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, null);
      if (confirm == JOptionPane.YES_OPTION) {
        TmmTask task = new MovieClearTraktTvTask();
        TmmTaskManager.getInstance().addUnnamedTask(task);
      }
    });

    btnPresetXbmc.addActionListener(evt -> MovieSettingsDefaults.setDefaultSettingsForXbmc());
    btnPresetKodi.addActionListener(evt -> MovieSettingsDefaults.setDefaultSettingsForKodi());
    btnPresetJellyfin.addActionListener(evt -> MovieSettingsDefaults.setDefaultSettingsForJellyfin());
    btnPresetEmby.addActionListener(evt -> MovieSettingsDefaults.setDefaultSettingsForEmby());
    btnPresetPlex.addActionListener(evt -> MovieSettingsDefaults.setDefaultSettingsForPlex());
    btnPresetMediaPortal1.addActionListener(evt -> MovieSettingsDefaults.setDefaultSettingsForMediaPortal1());
    btnPresetMediaPortal2.addActionListener(evt -> MovieSettingsDefaults.setDefaultSettingsForMediaPortal2());

    buildCheckBoxes();
  }

  private void checkChanges() {
    // universal filter
    List<AbstractSettings.UniversalFilterFields> universalFilterFields = new ArrayList<>();
    if (chckbxUniversalNote.isSelected()) {
      universalFilterFields.add(NOTE);
    }
    if (chckbxUniversalFilename.isSelected()) {
      universalFilterFields.add(FILENAME);
    }
    if (chckbxUniversalTags.isSelected()) {
      universalFilterFields.add(TAGS);
    }
    if (chckbxUniversalCountry.isSelected()) {
      universalFilterFields.add(COUNTRY);
    }
    if (chckbxUniversalProductionCompany.isSelected()) {
      universalFilterFields.add(PRODUCTION_COMPANY);
    }
    if (chckbxUniversalLanguages.isSelected()) {
      universalFilterFields.add(SPOKEN_LANGUAGE);
    }
    if (chckbxUniversalActors.isSelected()) {
      universalFilterFields.add(ACTOR);
    }
    if (chckbxUniversalProducers.isSelected()) {
      universalFilterFields.add(PRODUCER);
    }
    if (chckbxUniversalWriters.isSelected()) {
      universalFilterFields.add(WRITER);
    }
    if (chckbxUniversalDirectors.isSelected()) {
      universalFilterFields.add(DIRECTOR);
    }
    settings.setUniversalFilterFields(universalFilterFields);

    // metadata
    settings.clearMovieCheckMetadata();
    for (Map.Entry<MovieScraperMetadataConfig, JCheckBox> entry : metadataCheckBoxes.entrySet()) {
      MovieScraperMetadataConfig key = entry.getKey();
      JCheckBox value = entry.getValue();
      if (value.isSelected()) {
        settings.addMovieCheckMetadata(key);
      }
    }

    // artwork
    settings.clearMovieCheckArtwork();
    for (Map.Entry<MovieScraperMetadataConfig, JCheckBox> entry : artworkCheckBoxes.entrySet()) {
      MovieScraperMetadataConfig key = entry.getKey();
      JCheckBox value = entry.getValue();
      if (value.isSelected()) {
        settings.addMovieCheckArtwork(key);
      }
    }
  }

  private void buildCheckBoxes() {
    // universal filter
    for (AbstractSettings.UniversalFilterFields filterField : settings.getUniversalFilterFields()) {
      switch (filterField) {
        case NOTE:
          chckbxUniversalNote.setSelected(true);
          break;

        case FILENAME:
          chckbxUniversalFilename.setSelected(true);
          break;

        case TAGS:
          chckbxUniversalTags.setSelected(true);
          break;

        case PRODUCTION_COMPANY:
          chckbxUniversalProductionCompany.setSelected(true);
          break;

        case COUNTRY:
          chckbxUniversalCountry.setSelected(true);
          break;

        case SPOKEN_LANGUAGE:
          chckbxUniversalLanguages.setSelected(true);
          break;

        case ACTOR:
          chckbxUniversalActors.setSelected(true);
          break;

        case DIRECTOR:
          chckbxUniversalDirectors.setSelected(true);
          break;

        case PRODUCER:
          chckbxUniversalProducers.setSelected(true);
          break;

        case WRITER:
          chckbxUniversalWriters.setSelected(true);
          break;
      }
    }

    // set the checkbox listener at the end!
    chckbxUniversalNote.addItemListener(checkBoxListener);
    chckbxUniversalFilename.addItemListener(checkBoxListener);
    chckbxUniversalTags.addItemListener(checkBoxListener);
    chckbxUniversalProductionCompany.addItemListener(checkBoxListener);
    chckbxUniversalCountry.addItemListener(checkBoxListener);
    chckbxUniversalLanguages.addItemListener(checkBoxListener);
    chckbxUniversalActors.addItemListener(checkBoxListener);
    chckbxUniversalProducers.addItemListener(checkBoxListener);
    chckbxUniversalDirectors.addItemListener(checkBoxListener);
    chckbxUniversalWriters.addItemListener(checkBoxListener);

    // metadata
    for (MovieScraperMetadataConfig value : settings.getMovieCheckMetadata()) {
      JCheckBox checkBox = metadataCheckBoxes.get(value);
      if (checkBox != null) {
        checkBox.setSelected(true);
      }
    }

    // set the checkbox listener at the end!
    for (JCheckBox checkBox : metadataCheckBoxes.values()) {
      checkBox.addItemListener(checkBoxListener);
    }

    // artwork
    for (MovieScraperMetadataConfig value : settings.getMovieCheckArtwork()) {
      JCheckBox checkBox = artworkCheckBoxes.get(value);
      if (checkBox != null) {
        checkBox.setSelected(true);
      }
    }

    // set the checkbox listener at the end!
    for (JCheckBox checkBox : artworkCheckBoxes.values()) {
      checkBox.addItemListener(checkBoxListener);
    }
  }

  private void initComponents() {
    setLayout(new MigLayout("", "[600lp,grow]", "[][15lp!][][15lp!][][15lp!][]"));
    {
      JPanel panelUiSettings = new JPanel();
      // 16lp ~ width of the checkbox
      panelUiSettings.setLayout(
          new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "[][][][][][][10lp!][][grow][][][][][][10lp!][][10lp!][][125lp,grow]"));

      JLabel lblUiSettings = new TmmLabel(TmmResourceBundle.getString("Settings.ui"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelUiSettings, lblUiSettings, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/movies/settings#ui-settings"));
      add(collapsiblePanel, "cell 0 0,growx,wmin 0");
      {
        JLabel lblMovieFilter = new JLabel(TmmResourceBundle.getString("Settings.movietitlefilter"));
        panelUiSettings.add(lblMovieFilter, "cell 1 0 2 1");

        chckbxTitle = new JCheckBox(MovieTextMatcherList.TITLE.toString());
        panelUiSettings.add(chckbxTitle, "flowx,cell 2 1");

        chckbxSortableTitle = new JCheckBox(MovieTextMatcherList.TITLE_SORTABLE.toString());
        panelUiSettings.add(chckbxSortableTitle, "cell 2 1");

        JLabel lblSortableTitleHint = new JLabel(IconManager.HINT);
        lblSortableTitleHint.setToolTipText(TmmResourceBundle.getString("Settings.movie.renamer.${titleSortable}"));
        panelUiSettings.add(lblSortableTitleHint, "cell 2 1");

        chckbxOriginalTitle = new JCheckBox(MovieTextMatcherList.ORIGINAL_TITLE.toString());
        panelUiSettings.add(chckbxOriginalTitle, "cell 2 1");

        chckbxSortableOriginalTitle = new JCheckBox(MovieTextMatcherList.ORIGINAL_TITLE_SORTABLE.toString());
        panelUiSettings.add(chckbxSortableOriginalTitle, "cell 2 1");

        JLabel lblSortableOriginalTitleHint = new JLabel(IconManager.HINT);
        lblSortableOriginalTitleHint.setToolTipText(TmmResourceBundle.getString("Settings.movie.renamer.${titleSortable}"));
        panelUiSettings.add(lblSortableOriginalTitleHint, "cell 2 1");

        chckbxSortTitle = new JCheckBox(MovieTextMatcherList.SORTED_TITLE.toString());
        panelUiSettings.add(chckbxSortTitle, "cell 2 1");

        chckbxNote = new JCheckBox(MovieTextMatcherList.NOTE.toString());
        panelUiSettings.add(chckbxNote, "cell 2 1");
      }

      {
        JHintLabel lblUniversalFilter = new JHintLabel(TmmResourceBundle.getString("filter.universal"));
        lblUniversalFilter.setHintIcon(IconManager.HINT);
        lblUniversalFilter.setToolTipText(TmmResourceBundle.getString("filter.universal.hint"));
        panelUiSettings.add(lblUniversalFilter, "cell 1 2 2 1");

        chckbxUniversalNote = new JCheckBox(TmmResourceBundle.getString("metatag.note"));
        panelUiSettings.add(chckbxUniversalNote, "cell 2 3");

        chckbxUniversalFilename = new JCheckBox(TmmResourceBundle.getString("metatag.filename"));
        panelUiSettings.add(chckbxUniversalFilename, "cell 2 3");

        chckbxUniversalTags = new JCheckBox(TmmResourceBundle.getString("metatag.tags"));
        panelUiSettings.add(chckbxUniversalTags, "cell 2 3");

        chckbxUniversalProductionCompany = new JCheckBox(TmmResourceBundle.getString("metatag.production"));
        panelUiSettings.add(chckbxUniversalProductionCompany, "cell 2 3");

        chckbxUniversalCountry = new JCheckBox(TmmResourceBundle.getString("metatag.country"));
        panelUiSettings.add(chckbxUniversalCountry, "cell 2 3");

        chckbxUniversalLanguages = new JCheckBox(TmmResourceBundle.getString("metatag.spokenlanguages"));
        panelUiSettings.add(chckbxUniversalLanguages, "cell 2 3");

        chckbxUniversalActors = new JCheckBox(TmmResourceBundle.getString("metatag.actors"));
        panelUiSettings.add(chckbxUniversalActors, "cell 2 4");

        chckbxUniversalProducers = new JCheckBox(TmmResourceBundle.getString("metatag.producers"));
        panelUiSettings.add(chckbxUniversalProducers, "cell 2 4");

        chckbxUniversalDirectors = new JCheckBox(TmmResourceBundle.getString("metatag.directors"));
        panelUiSettings.add(chckbxUniversalDirectors, "cell 2 4");

        chckbxUniversalWriters = new JCheckBox(TmmResourceBundle.getString("metatag.writers"));
        panelUiSettings.add(chckbxUniversalWriters, "cell 2 4");
      }
      {
        chckbxStoreFilter = new JCheckBox(TmmResourceBundle.getString("Settings.movie.persistuifilter"));
        panelUiSettings.add(chckbxStoreFilter, "cell 1 5 2 1");
      }
      {
        JLabel lblCheckMetadata = new JLabel(TmmResourceBundle.getString("Settings.checkmetadata"));
        panelUiSettings.add(lblCheckMetadata, "cell 1 7 2 1");

        JPanel panelCheckMetadata = new JPanel();
        panelCheckMetadata.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.ipadx = 10;

        // Metadata
        for (MovieScraperMetadataConfig value : MovieScraperMetadataConfig.values()) {
          if (value.isMetaData()) {
            addMetadataCheckbox(panelCheckMetadata, value, metadataCheckBoxes, gbc);
          }
        }

        // cast
        gbc.gridx = 0;
        gbc.gridy++;
        for (MovieScraperMetadataConfig value : MovieScraperMetadataConfig.values()) {
          if (value.isCast()) {
            addMetadataCheckbox(panelCheckMetadata, value, metadataCheckBoxes, gbc);
          }
        }
        panelUiSettings.add(panelCheckMetadata, "cell 2 8");

        chckbxDisplayAllMissingMetadata = new JHintCheckBox(TmmResourceBundle.getString("Settings.checkmetadata.displayall"));
        chckbxDisplayAllMissingMetadata.setToolTipText(TmmResourceBundle.getString("Settings.checkmetadata.displayall.desc"));
        chckbxDisplayAllMissingMetadata.setHintIcon(IconManager.HINT);
        panelUiSettings.add(chckbxDisplayAllMissingMetadata, "cell 2 9");
      }

      {
        JLabel lblCheckImages = new JLabel(TmmResourceBundle.getString("Settings.checkimages"));
        panelUiSettings.add(lblCheckImages, "cell 1 11 2 1");

        JPanel panelCheckImages = new JPanel();
        panelCheckImages.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.ipadx = 10;

        // Artwork
        for (MovieScraperMetadataConfig value : MovieScraperMetadataConfig.values()) {
          if (value.isArtwork()) {
            addMetadataCheckbox(panelCheckImages, value, artworkCheckBoxes, gbc);
          }
        }

        panelUiSettings.add(panelCheckImages, "cell 2 12");

        chckbxDisplayAllMissingArtwork = new JHintCheckBox(TmmResourceBundle.getString("Settings.checkimages.displayall"));
        chckbxDisplayAllMissingArtwork.setToolTipText(TmmResourceBundle.getString("Settings.checkimages.displayall.desc"));
        chckbxDisplayAllMissingArtwork.setHintIcon(IconManager.HINT);
        panelUiSettings.add(chckbxDisplayAllMissingArtwork, "cell 2 13");
      }
      {
        chckbxMovieTableTooltips = new JCheckBox(TmmResourceBundle.getString("Settings.movie.showtabletooltips"));
        panelUiSettings.add(chckbxMovieTableTooltips, "cell 1 15 2 1");
      }
      {
        JLabel lblRating = new JLabel(TmmResourceBundle.getString("Settings.preferredrating"));
        panelUiSettings.add(lblRating, "cell 1 17 2 1");

        JPanel panelRatingSource = new JPanel();
        panelUiSettings.add(panelRatingSource, "cell 2 18,grow");
        panelRatingSource.setLayout(new MigLayout("insets 0", "[100lp][]", "[grow][]"));
        {
          listRatings = new JList();
          listRatings.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

          JScrollPane scrollPane = new JScrollPane();
          scrollPane.setViewportView(listRatings);
          panelRatingSource.add(scrollPane, "cell 0 0,grow");

          btnMoveRatingUp = new SquareIconButton(IconManager.ARROW_UP_INV);
          btnMoveRatingUp.setToolTipText(TmmResourceBundle.getString("Button.moveup"));
          panelRatingSource.add(btnMoveRatingUp, "flowy,cell 1 0,aligny bottom");

          btnMoveRatingDown = new SquareIconButton(IconManager.ARROW_DOWN_INV);
          btnMoveRatingDown.setToolTipText(TmmResourceBundle.getString("Button.movedown"));
          panelRatingSource.add(btnMoveRatingDown, "cell 1 0,aligny bottom");

          cbRating = new AutocompleteComboBox(Arrays.asList("imdb", "tmdb", "metascore", "rottenTomatoes", "user"));
          panelRatingSource.add(cbRating, "cell 0 1,growx");

          btnRemoveRating = new SquareIconButton(IconManager.REMOVE_INV);
          btnRemoveRating.setToolTipText(TmmResourceBundle.getString("Button.remove"));
          panelRatingSource.add(btnRemoveRating, "cell 1 0");

          btnAddRating = new SquareIconButton(IconManager.ADD_INV);
          btnAddRating.setToolTipText(TmmResourceBundle.getString("Button.add"));
          panelRatingSource.add(btnAddRating, "cell 1 1");
        }
      }
    }
    {
      JPanel panelAutomaticTasks = new JPanel();
      panelAutomaticTasks.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "[][][][][][][]")); // 16lp ~ width of the

      JLabel lblAutomaticTasksT = new TmmLabel(TmmResourceBundle.getString("Settings.automatictasks"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelAutomaticTasks, lblAutomaticTasksT, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/movies/settings#automatic-tasks"));
      add(collapsiblePanel, "cell 0 2,growx,wmin 0");
      {
        chckbxRenameAfterScrape = new JCheckBox(TmmResourceBundle.getString("Settings.movie.automaticrename"));
        panelAutomaticTasks.add(chckbxRenameAfterScrape, "cell 1 0 2 1");

        JLabel lblAutomaticRenameHint = new JLabel(IconManager.HINT);
        lblAutomaticRenameHint.setToolTipText(TmmResourceBundle.getString("Settings.movie.automaticrename.desc"));
        panelAutomaticTasks.add(lblAutomaticRenameHint, "cell 1 0 2 1");

        chckbxARDAfterScrape = new JCheckBox(TmmResourceBundle.getString("Settings.movie.automaticard"));
        panelAutomaticTasks.add(chckbxARDAfterScrape, "cell 1 1 2 1");

        JLabel lblAutomaticARDHint = new JLabel(IconManager.HINT);
        lblAutomaticARDHint.setToolTipText(TmmResourceBundle.getString("Settings.movie.automaticard.desc"));
        panelAutomaticTasks.add(lblAutomaticARDHint, "cell 1 1 2 1");

        chckbxTraktSync = new JCheckBox(TmmResourceBundle.getString("Settings.trakt"));
        panelAutomaticTasks.add(chckbxTraktSync, "cell 1 2 2 1");

        btnClearTraktData = new JButton(TmmResourceBundle.getString("Settings.trakt.clearmovies"));
        panelAutomaticTasks.add(btnClearTraktData, "cell 1 2 2 1");

        chckbxTraktSyncCollection = new JCheckBox(TmmResourceBundle.getString("Settings.trakt.collection"));
        panelAutomaticTasks.add(chckbxTraktSyncCollection, "cell 2 3");

        chckbxTraktSyncWatched = new JCheckBox(TmmResourceBundle.getString("Settings.trakt.watched"));
        panelAutomaticTasks.add(chckbxTraktSyncWatched, "cell 2 4");

        chckbxTraktSyncRating = new JCheckBox(TmmResourceBundle.getString("Settings.trakt.rating"));
        panelAutomaticTasks.add(chckbxTraktSyncRating, "cell 2 5");

        chckbxAutoUpdateOnStart = new JCheckBox(TmmResourceBundle.getString("Settings.movie.automaticupdate"));
        panelAutomaticTasks.add(chckbxAutoUpdateOnStart, "cell 1 7 2 1");

        JLabel lblAutomaticUpdateHint = new JLabel(IconManager.HINT);
        lblAutomaticUpdateHint.setToolTipText(TmmResourceBundle.getString("Settings.movie.automaticupdate.desc"));
        panelAutomaticTasks.add(lblAutomaticUpdateHint, "cell 1 7 2 1");

      }
    }
    {
      JPanel panelMisc = new JPanel();
      panelMisc.setLayout(new MigLayout("hidemode 1, insets 0", "[20lp!][16lp!][grow]", "[][][][][][]")); // 16lp ~ width of the

      JLabel lblMiscT = new TmmLabel(TmmResourceBundle.getString("Settings.misc"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelMisc, lblMiscT, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/movies/settings#misc-settings"));
      add(collapsiblePanel, "cell 0 4,growx,wmin 0");

      chckbxUseMediainfoMetadata = new JCheckBox(TmmResourceBundle.getString("Settings.usemediainfometadata"));
      panelMisc.add(chckbxUseMediainfoMetadata, "cell 1 0 2 1");
      {
        chckbxExtractArtworkFromVsmeta = new JCheckBox(TmmResourceBundle.getString("Settings.extractartworkfromvsmeta"));
        panelMisc.add(chckbxExtractArtworkFromVsmeta, "cell 1 1 2 1");

        chckbxBuildImageCache = new JCheckBox(TmmResourceBundle.getString("Settings.imagecacheimport"));
        panelMisc.add(chckbxBuildImageCache, "cell 1 2 2 1");

        JLabel lblBuildImageCacheHint = new JLabel(IconManager.HINT);
        lblBuildImageCacheHint.setToolTipText(TmmResourceBundle.getString("Settings.imagecacheimporthint"));
        panelMisc.add(lblBuildImageCacheHint, "cell 1 2 2 1");

        chckbxRuntimeFromMi = new JCheckBox(TmmResourceBundle.getString("Settings.runtimefrommediafile"));
        panelMisc.add(chckbxRuntimeFromMi, "cell 1 3 2 1");

        chckbxIncludeExternalAudioStreams = new JCheckBox(TmmResourceBundle.getString("Settings.includeexternalstreamsinnfo"));
        panelMisc.add(chckbxIncludeExternalAudioStreams, "cell 1 4 2 1");
      }
    }
    {
      JPanel panelPresets = new JPanel(
          new MigLayout("hidemode 1, insets 0", "[20lp!][15lp][120lp:n][15lp!][120lp:n][15lp!][120lp:n][grow]", "[][][][]"));

      JLabel lblPresets = new TmmLabel(TmmResourceBundle.getString("Settings.preset"), H3);
      CollapsiblePanel collapsiblePanel = new CollapsiblePanel(panelPresets, lblPresets, true);
      collapsiblePanel.addExtraTitleComponent(new DocsButton("/movies/settings#media-center-presets"));
      add(collapsiblePanel, "cell 0 6,growx,wmin 0");
      {
        JLabel lblPresetHintT = new JLabel(TmmResourceBundle.getString("Settings.preset.desc"));
        panelPresets.add(lblPresetHintT, "cell 1 0 7 1");
      }
      {
        btnPresetKodi = new JButton("Kodi v17+");
        panelPresets.add(btnPresetKodi, "cell 2 1,growx");

        btnPresetXbmc = new JButton("XBMC/Kodi <v17");
        panelPresets.add(btnPresetXbmc, "cell 4 1,growx");
      }
      {
        btnPresetJellyfin = new JButton("Jellyfin");
        panelPresets.add(btnPresetJellyfin, "cell 2 2,growx");

        btnPresetEmby = new JButton("Emby");
        panelPresets.add(btnPresetEmby, "cell 4 2,growx");

        btnPresetPlex = new JButton("Plex");
        panelPresets.add(btnPresetPlex, "cell 6 2,growx");
      }
      {
        btnPresetMediaPortal1 = new JButton("MediaPortal 1.x");
        panelPresets.add(btnPresetMediaPortal1, "cell 2 3,growx");

        btnPresetMediaPortal2 = new JButton("MediaPortal 2.x");
        panelPresets.add(btnPresetMediaPortal2, "cell 4 3,growx");
      }
    }
  }

  private void addMetadataCheckbox(JPanel panel, MovieScraperMetadataConfig config, Map<MovieScraperMetadataConfig, JCheckBox> map,
      GridBagConstraints gbc) {
    JCheckBox checkBox;
    if (StringUtils.isNotBlank(config.getToolTip())) {
      checkBox = new JHintCheckBox(config.getDescription());
      checkBox.setToolTipText(config.getToolTip());
      ((JHintCheckBox) checkBox).setHintIcon(IconManager.HINT);
    }
    else {
      checkBox = new JCheckBox(config.getDescription());
    }
    map.put(config, checkBox);

    if (gbc.gridx >= COL_COUNT) {
      gbc.gridx = 0;
      gbc.gridy++;
    }
    panel.add(checkBox, gbc);

    gbc.gridx++;
  }

  protected void initDataBindings() {
    Property movieSettingsBeanProperty_1 = BeanProperty.create("renameAfterScrape");
    Property jCheckBoxBeanProperty = BeanProperty.create("selected");
    AutoBinding autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_1, chckbxRenameAfterScrape,
        jCheckBoxBeanProperty);
    autoBinding_1.bind();
    //
    Property movieSettingsBeanProperty_2 = BeanProperty.create("syncTrakt");
    AutoBinding autoBinding_2 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_2, chckbxTraktSync,
        jCheckBoxBeanProperty);
    autoBinding_2.bind();
    //
    Property movieSettingsBeanProperty_3 = BeanProperty.create("buildImageCacheOnImport");
    AutoBinding autoBinding_3 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_3, chckbxBuildImageCache,
        jCheckBoxBeanProperty);
    autoBinding_3.bind();
    //
    Property movieSettingsBeanProperty_4 = BeanProperty.create("runtimeFromMediaInfo");
    AutoBinding autoBinding_4 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_4, chckbxRuntimeFromMi,
        jCheckBoxBeanProperty);
    autoBinding_4.bind();
    //
    Property movieSettingsBeanProperty_8 = BeanProperty.create("preferredRating");
    Property autocompleteComboBoxBeanProperty = BeanProperty.create("selectedItem");
    AutoBinding autoBinding_8 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_8, cbRating,
        autocompleteComboBoxBeanProperty);
    autoBinding_8.bind();
    //
    Property movieSettingsBeanProperty_9 = BeanProperty.create("includeExternalAudioStreams");
    AutoBinding autoBinding_9 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_9,
        chckbxIncludeExternalAudioStreams, jCheckBoxBeanProperty);
    autoBinding_9.bind();
    //
    Property movieSettingsBeanProperty_11 = BeanProperty.create("extractArtworkFromVsmeta");
    AutoBinding autoBinding_11 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_11,
        chckbxExtractArtworkFromVsmeta, jCheckBoxBeanProperty);
    autoBinding_11.bind();
    //
    Property movieSettingsBeanProperty_12 = BeanProperty.create("updateOnStart");
    AutoBinding autoBinding_12 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_12,
        chckbxAutoUpdateOnStart, jCheckBoxBeanProperty);
    autoBinding_12.bind();
    //
    Property movieSettingsBeanProperty_13 = BeanProperty.create("title");
    AutoBinding autoBinding_13 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_13, chckbxTitle,
        jCheckBoxBeanProperty);
    autoBinding_13.bind();
    //
    Property movieSettingsBeanProperty_14 = BeanProperty.create("sortableTitle");
    AutoBinding autoBinding_14 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_14, chckbxSortableTitle,
        jCheckBoxBeanProperty);
    autoBinding_14.bind();
    //
    Property movieSettingsBeanProperty_15 = BeanProperty.create("originalTitle");
    AutoBinding autoBinding_15 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_15, chckbxOriginalTitle,
        jCheckBoxBeanProperty);
    autoBinding_15.bind();
    //
    Property movieSettingsBeanProperty_16 = BeanProperty.create("sortableOriginalTitle");
    AutoBinding autoBinding_16 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_16,
        chckbxSortableOriginalTitle, jCheckBoxBeanProperty);
    autoBinding_16.bind();
    //
    Property movieSettingsBeanProperty_17 = BeanProperty.create("sortTitle");
    AutoBinding autoBinding_17 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_17, chckbxSortTitle,
        jCheckBoxBeanProperty);
    autoBinding_17.bind();
    //
    Property movieSettingsBeanProperty_23 = BeanProperty.create("note");
    AutoBinding autoBinding_23 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_23, chckbxNote,
        jCheckBoxBeanProperty);
    autoBinding_23.bind();
    //
    Property movieSettingsBeanProperty = BeanProperty.create("ratingSources");
    JListBinding jListBinding = SwingBindings.createJListBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty, listRatings);
    jListBinding.bind();
    //
    Property movieSettingsBeanProperty_5 = BeanProperty.create("showMovieTableTooltips");
    AutoBinding autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_5, chckbxMovieTableTooltips,
        jCheckBoxBeanProperty);
    autoBinding.bind();
    //
    Property movieSettingsBeanProperty_6 = BeanProperty.create("useMediainfoMetadata");
    AutoBinding autoBinding_5 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_6,
        chckbxUseMediainfoMetadata, jCheckBoxBeanProperty);
    autoBinding_5.bind();
    //
    Property jCheckBoxBeanProperty_1 = BeanProperty.create("enabled");
    AutoBinding autoBinding_6 = Bindings.createAutoBinding(UpdateStrategy.READ, chckbxTraktSync, jCheckBoxBeanProperty, chckbxTraktSyncCollection,
        jCheckBoxBeanProperty_1);
    autoBinding_6.bind();
    //
    AutoBinding autoBinding_7 = Bindings.createAutoBinding(UpdateStrategy.READ, chckbxTraktSync, jCheckBoxBeanProperty, chckbxTraktSyncWatched,
        jCheckBoxBeanProperty_1);
    autoBinding_7.bind();
    //
    AutoBinding autoBinding_10 = Bindings.createAutoBinding(UpdateStrategy.READ, chckbxTraktSync, jCheckBoxBeanProperty, chckbxTraktSyncRating,
        jCheckBoxBeanProperty_1);
    autoBinding_10.bind();
    //
    Property movieSettingsBeanProperty_7 = BeanProperty.create("syncTraktRating");
    AutoBinding autoBinding_18 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_7, chckbxTraktSyncRating,
        jCheckBoxBeanProperty);
    autoBinding_18.bind();
    //
    Property movieSettingsBeanProperty_10 = BeanProperty.create("syncTraktWatched");
    AutoBinding autoBinding_19 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_10, chckbxTraktSyncWatched,
        jCheckBoxBeanProperty);
    autoBinding_19.bind();
    //
    Property movieSettingsBeanProperty_18 = BeanProperty.create("syncTraktCollection");
    AutoBinding autoBinding_20 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_18,
        chckbxTraktSyncCollection, jCheckBoxBeanProperty);
    autoBinding_20.bind();
    //
    Property movieSettingsBeanProperty_19 = BeanProperty.create("storeUiFilters");
    AutoBinding autoBinding_21 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_19, chckbxStoreFilter,
        jCheckBoxBeanProperty);
    autoBinding_21.bind();
    //
    Property movieSettingsBeanProperty_22 = BeanProperty.create("ardAfterScrape");
    AutoBinding autoBinding_22 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_22, chckbxARDAfterScrape,
        jCheckBoxBeanProperty);
    autoBinding_22.bind();
    //
    Property movieSettingsBeanProperty_20 = BeanProperty.create("movieDisplayAllMissingMetadata");
    AutoBinding autoBinding_24 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_20,
        chckbxDisplayAllMissingMetadata, jCheckBoxBeanProperty);
    autoBinding_24.bind();
    //
    Property movieSettingsBeanProperty_21 = BeanProperty.create("movieDisplayAllMissingArtwork");
    AutoBinding autoBinding_25 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, settings, movieSettingsBeanProperty_21,
        chckbxDisplayAllMissingArtwork, jCheckBoxBeanProperty);
    autoBinding_25.bind();
  }
}
