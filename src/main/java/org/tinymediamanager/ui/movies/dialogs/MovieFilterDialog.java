/*
 * Copyright 2012 - 2020 Manuel Laggner
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

import static org.tinymediamanager.ui.TmmFontHelper.L1;

import java.awt.*;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

import javax.swing.*;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.AbstractSettings.UIFilters;
import org.tinymediamanager.core.TmmProperties;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.TmmUILayoutStore;
import org.tinymediamanager.ui.components.FlatButton;
import org.tinymediamanager.ui.components.NoBorderScrollPane;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.TmmTabbedPane;
import org.tinymediamanager.ui.dialogs.FilterSaveDialog;
import org.tinymediamanager.ui.dialogs.TmmDialog;
import org.tinymediamanager.ui.movies.MovieSelectionModel;
import org.tinymediamanager.ui.movies.filters.*;

import net.miginfocom.swing.MigLayout;

public class MovieFilterDialog extends TmmDialog {
  private static final long                      serialVersionUID = 2298540526428945319L;
  /** @wbp.nls.resourceBundle messages */
  protected static final ResourceBundle          BUNDLE           = ResourceBundle.getBundle("messages");

  private final MovieSelectionModel              selectionModel;

  // map for storing which filter is in which panel
  private final Map<JPanel, Set<IMovieUIFilter>> filterMap;
  private final Set<IMovieUIFilter>              filters;

  private final JTabbedPane                      tabbedPane;
  private JComboBox<String>                      cbPreset;

  public MovieFilterDialog(MovieSelectionModel selectionModel) {
    super(BUNDLE.getString("movieextendedsearch.options"), "movieFilter");
    setModalityType(ModalityType.MODELESS);
    setMinimumSize(new Dimension(400, 0));

    this.selectionModel = selectionModel;
    this.filterMap = new HashMap<>();
    this.filters = new HashSet<>();
    this.selectionModel.addPropertyChangeListener("filterChanged", evt -> filterChanged());

    ActionListener actionListener = e -> {
      String filterName = (String) cbPreset.getSelectedItem();
      if (StringUtils.isNotBlank(filterName)) {
        selectionModel.setFilterValues(MovieModuleManager.SETTINGS.getMovieUiFilterPresets().get(filterName));
      }
      else {
        selectionModel.setFilterValues(Collections.emptyList());
      }
    };

    {
      tabbedPane = new TmmTabbedPane();
      getContentPane().add(tabbedPane, BorderLayout.CENTER);

      {
        // panel Main
        JPanel panelMain = new JPanel(new MigLayout("", "[][][50lp:150lp,grow]", "[]"));
        JScrollPane scrollPaneMain = new NoBorderScrollPane(panelMain);
        scrollPaneMain.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        tabbedPane.addTab(BUNDLE.getString("metatag.details"), scrollPaneMain);

        panelMain.add(new TmmLabel(BUNDLE.getString("movieextendedsearch.filterby")), "cell 0 0 3 1, growx, aligny top, wrap");

        addFilter(new MovieNewMoviesFilter(), panelMain);
        addFilter(new MovieDuplicateFilter(), panelMain);
        addFilter(new MovieWatchedFilter(), panelMain);
        addFilter(new MovieGenreFilter(), panelMain);
        addFilter(new MovieCertificationFilter(), panelMain);
        addFilter(new MovieYearFilter(), panelMain);
        addFilter(new MovieCastFilter(), panelMain);
        addFilter(new MovieCountryFilter(), panelMain);
        addFilter(new MovieLanguageFilter(), panelMain);
        addFilter(new MovieProductionCompanyFilter(), panelMain);
        addFilter(new MovieTagFilter(), panelMain);
        addFilter(new MovieEditionFilter(), panelMain);
        addFilter(new MovieInMovieSetFilter(), panelMain);
        addFilter(new MovieDifferentRuntimeFilter(), panelMain);
      }

      {
        // panel media data
        JPanel panelMediaData = new JPanel(new MigLayout("", "[][][50lp:150lp,grow]", "[]"));
        JScrollPane scrollPaneMediaData = new NoBorderScrollPane(panelMediaData);

        scrollPaneMediaData.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        tabbedPane.addTab(BUNDLE.getString("metatag.mediainformation"), scrollPaneMediaData);
        panelMediaData.add(new TmmLabel(BUNDLE.getString("movieextendedsearch.filterby")), "cell 0 0 3 1, growx, aligny top, wrap");

        addFilter(new MovieMediaSourceFilter(), panelMediaData);
        addFilter(new MovieMediaFilesFilter(), panelMediaData);
        addFilter(new MovieFilenameFilter(), panelMediaData);
        addFilter(new MovieVideoFormatFilter(), panelMediaData);
        addFilter(new MovieVideoCodecFilter(), panelMediaData);
        addFilter(new MovieAspectRatioFilter(), panelMediaData);
        addFilter(new MovieFrameRateFilter(), panelMediaData);
        addFilter(new MovieVideo3DFilter(), panelMediaData);
        addFilter(new MovieVideoContainerFilter(), panelMediaData);
        addFilter(new MovieAudioCodecFilter(), panelMediaData);
        addFilter(new MovieAudioChannelFilter(), panelMediaData);
        addFilter(new MovieCountAudioStreamFilter(), panelMediaData);
        addFilter(new MovieCountSubtitleFilter(), panelMediaData);
        addFilter(new MovieDatasourceFilter(), panelMediaData);
        addFilter(new MovieVideoExtrasFilter(), panelMediaData);
        addFilter(new MovieMissingMetadataFilter(), panelMediaData);
        addFilter(new MovieMissingArtworkFilter(), panelMediaData);
        addFilter(new MovieMissingSubtitlesFilter(), panelMediaData);
        addFilter(new MovieAudioLanguageFilter(),panelMediaData);
        addFilter(new MovieSubtitleLanguageFilter(),panelMediaData);
      }

      {
        // filter preset panel
        JPanel panelFilterPreset = new JPanel();
        panelFilterPreset.setLayout(new MigLayout("insets n 0 n 0", "[5lp!][10lp][150lp,grow][5lp!]", "[]"));

        JSeparator separator = new JSeparator();
        panelFilterPreset.add(separator, "cell 0 1 4 1,growx,aligny top");

        JLabel lblEnableAllT = new TmmLabel(BUNDLE.getString("filter.enableall"));
        panelFilterPreset.add(lblEnableAllT, "cell 1 2, alignx trailing");

        JCheckBox chkbxEnableAll = new JCheckBox();
        chkbxEnableAll.setSelected(true);
        chkbxEnableAll.addActionListener(e -> selectionModel.setFiltersActive(chkbxEnableAll.isSelected()));
        panelFilterPreset.add(chkbxEnableAll, "cell 2 2");

        JLabel lblFilterPresetT = new TmmLabel(BUNDLE.getString("filter.presets"));
        panelFilterPreset.add(lblFilterPresetT, "cell 1 3, alignx trailing");

        cbPreset = new JComboBox<>();
        cbPreset.addActionListener(e -> {
          String filterName = (String) cbPreset.getSelectedItem();
          if (StringUtils.isNotBlank(filterName)) {
            selectionModel.setFilterValues(MovieModuleManager.SETTINGS.getMovieUiFilterPresets().get(filterName));
          }
          else {
            selectionModel.setFilterValues(Collections.emptyList());
          }
        });
        panelFilterPreset.add(cbPreset, "cell 2 3");

        JButton btnSavePreset = new FlatButton(IconManager.SAVE);
        btnSavePreset.setToolTipText(BUNDLE.getString("filter.savepreset"));
        btnSavePreset.addActionListener(e -> {
          Set<UIFilters> activeUiFilters = getActiveUiFilters();
          if (!activeUiFilters.isEmpty()) {
            Map<String, List<UIFilters>> movieUiFilters = new HashMap<>(MovieModuleManager.SETTINGS.getMovieUiFilterPresets());
            FilterSaveDialog saveDialog = new FilterSaveDialog(MovieFilterDialog.this, activeUiFilters, movieUiFilters);
            saveDialog.setVisible(true);

            String savedPreset = saveDialog.getSavedPreset();
            if (StringUtils.isNotBlank(savedPreset)) {
              cbPreset.removeActionListener(actionListener);
              MovieModuleManager.SETTINGS.setMovieUiFilterPresets(movieUiFilters);
              loadPresets();
              cbPreset.setSelectedItem(savedPreset);
              cbPreset.addActionListener(actionListener);
            }
          }
        });
        panelFilterPreset.add(btnSavePreset, "cell 2 3");

        JButton btnDeletePreset = new FlatButton(IconManager.DELETE_GRAY);
        btnDeletePreset.setToolTipText(BUNDLE.getString("filter.remove"));
        btnDeletePreset.addActionListener(e -> {
          String filterName = (String) cbPreset.getSelectedItem();
          if (StringUtils.isBlank(filterName)) {
            return;
          }

          // display warning and ask the user again
          if (!TmmProperties.getInstance().getPropertyAsBoolean("movie.hidefilterhint")) {
            JCheckBox checkBox = new JCheckBox(BUNDLE.getString("tmm.donotshowagain"));
            TmmFontHelper.changeFont(checkBox, L1);
            checkBox.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

            Object[] options = { BUNDLE.getString("Button.yes"), BUNDLE.getString("Button.no") };
            Object[] params = { BUNDLE.getString("filter.remove"), checkBox };
            int answer = JOptionPane.showOptionDialog(MainWindow.getInstance(), params, BUNDLE.getString("filter.remove"),
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, null);

            // the user don't want to show this dialog again
            if (checkBox.isSelected()) {
              TmmProperties.getInstance().putProperty("movie.hidefilterhint", String.valueOf(checkBox.isSelected()));
            }

            if (answer != JOptionPane.YES_OPTION) {
              return;
            }

            Map<String, List<UIFilters>> movieUiFilters = new HashMap<>(MovieModuleManager.SETTINGS.getMovieUiFilterPresets());
            if (movieUiFilters.remove(filterName) != null) {
              cbPreset.removeActionListener(actionListener);
              MovieModuleManager.SETTINGS.setMovieUiFilterPresets(movieUiFilters);
              loadPresets();
              cbPreset.addActionListener(actionListener);
            }
          }
        });
        panelFilterPreset.add(btnDeletePreset, "cell 2 3");

        getContentPane().add(panelFilterPreset, BorderLayout.SOUTH);
      }
    }

    {
      // init
      loadPresets();

      cbPreset.addActionListener(actionListener);
    }
  }

  private Set<UIFilters> getActiveUiFilters() {
    return new HashSet<>(IMovieUIFilter.morphToUiFilters(filters));
  }

  private void loadPresets() {
    String preset = (String) cbPreset.getSelectedItem();

    cbPreset.removeAllItems();
    cbPreset.addItem("");
    MovieModuleManager.SETTINGS.getMovieUiFilterPresets().keySet().stream().sorted().forEach(key -> cbPreset.addItem(key));

    if (StringUtils.isNotBlank(preset)) {
      cbPreset.setSelectedItem(preset);
    }
  }

  /**
   * add a new filter to the panel and selection model
   *
   * @param filter
   *          the filter to be added
   * @param panel
   *          the panel to add the filter to
   */
  private void addFilter(IMovieUIFilter filter, JPanel panel) {
    panel.add(filter.getCheckBox(), "");
    panel.add(filter.getLabel(), "");

    if (filter.getFilterComponent() != null) {
      panel.add(filter.getFilterComponent(), "wmin 100, grow, wrap");
    }
    else {
      panel.add(Box.createGlue(), "wrap");
    }

    filterMap.computeIfAbsent(panel, k -> new HashSet<>()).add(filter);
    filters.add(filter);

    selectionModel.addFilter(filter);
  }

  /**
   * re-calculate if the active filter icon should be displayed
   */
  private void filterChanged() {
    for (Map.Entry<JPanel, Set<IMovieUIFilter>> entry : filterMap.entrySet()) {
      boolean active = false;
      for (IMovieUIFilter filter : entry.getValue()) {
        switch (filter.getFilterState()) {
          case ACTIVE:
          case ACTIVE_NEGATIVE:
            active = true;
            break;

          default:
            break;
        }

        if (active) {
          break;
        }
      }

      for (int i = 0; i < tabbedPane.getTabCount(); i++) {
        if (SwingUtilities.isDescendingFrom(entry.getKey(), tabbedPane.getComponentAt(i))) {
          if (active) {
            tabbedPane.setIconAt(i, IconManager.FILTER_ACTIVE);
          }
          else {
            tabbedPane.setIconAt(i, null);
          }

          break;
        }
      }
    }
  }

  @Override
  protected void initBottomPanel() {
    // no bottom line needed
  }

  @Override
  public void dispose() {
    // do not dispose (singleton), but save the size/position
    TmmUILayoutStore.getInstance().saveSettings(this);
  }
}
