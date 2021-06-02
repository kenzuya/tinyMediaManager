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

import static org.tinymediamanager.ui.TmmFontHelper.L1;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.AbstractSettings;
import org.tinymediamanager.core.TmmProperties;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.TmmUILayoutStore;
import org.tinymediamanager.ui.components.FlatButton;
import org.tinymediamanager.ui.components.NoBorderScrollPane;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.TmmTabbedPane;
import org.tinymediamanager.ui.components.tree.TmmTreeNode;
import org.tinymediamanager.ui.components.treetable.TmmTreeTable;
import org.tinymediamanager.ui.dialogs.FilterSaveDialog;
import org.tinymediamanager.ui.dialogs.TmmDialog;
import org.tinymediamanager.ui.tvshows.filters.ITvShowUIFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowAspectRatioFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowAudioChannelFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowAudioCodecFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowAudioLanguageFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowAudioStreamCountFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowCastFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowCertificationFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowCountryFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowDatasourceFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowDateAddedFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowDuplicateEpisodesFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowEmptyFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowFilenameFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowFrameRateFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowGenreFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowHDRFormatFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowMediaFilesFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowMediaSourceFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowMissingArtworkFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowMissingEpisodesFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowMissingMetadataFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowMissingSubtitlesFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowNewEpisodesFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowNoteFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowStatusFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowStudioFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowSubtitleCountFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowSubtitleLanguageFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowTagFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowUncategorizedEpisodesFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowVideoCodecFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowVideoContainerFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowVideoFormatFilter;
import org.tinymediamanager.ui.tvshows.filters.TvShowWatchedFilter;

import net.miginfocom.swing.MigLayout;

public class TvShowFilterDialog extends TmmDialog {
  private static final long                          serialVersionUID = 2298540526328945319L;

  protected static final ResourceBundle              BUNDLE           = ResourceBundle.getBundle("messages");

  private final TmmTreeTable                         treeTable;

  // map for storing which filter is in which panel
  private final Map<JPanel, Set<ITvShowUIFilter<?>>> filterMap;
  private final Set<ITvShowUIFilter<?>>              filters;

  private final JTabbedPane                          tabbedPane;
  private JComboBox<String>                          cbPreset;

  public TvShowFilterDialog(TmmTreeTable treeTable) {
    super(TmmResourceBundle.getString("movieextendedsearch.options"), "tvShowFilter");
    setModalityType(ModalityType.MODELESS);

    this.treeTable = treeTable;
    this.filterMap = new HashMap<>();
    this.filters = new HashSet<>();
    this.treeTable.addPropertyChangeListener("filterChanged", evt -> filterChanged());

    ActionListener actionListener = e -> {
      String filterName = (String) cbPreset.getSelectedItem();
      if (StringUtils.isNotBlank(filterName)) {
        treeTable.setFilterValues(TvShowModuleManager.getInstance().getSettings().getUiFilterPresets().get(filterName));
      }
      else {
        treeTable.setFilterValues(Collections.emptyList());
      }
    };

    {
      tabbedPane = new TmmTabbedPane();
      getContentPane().add(tabbedPane, BorderLayout.CENTER);

      {
        // panel Main
        JPanel panelMain = new JPanel(new MigLayout("", "[][][100lp:150lp,grow]", "[]"));
        JScrollPane scrollPaneMain = new NoBorderScrollPane(panelMain);
        scrollPaneMain.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        tabbedPane.addTab(TmmResourceBundle.getString("metatag.details"), scrollPaneMain);

        panelMain.add(new TmmLabel(TmmResourceBundle.getString("movieextendedsearch.filterby")), "cell 0 0 3 1, growx, aligny top, wrap");

        addFilter(new TvShowNewEpisodesFilter(), panelMain);
        addFilter(new TvShowDateAddedFilter(), panelMain);
        addFilter(new TvShowDuplicateEpisodesFilter(), panelMain);
        addFilter(new TvShowWatchedFilter(), panelMain);
        addFilter(new TvShowStatusFilter(), panelMain);
        addFilter(new TvShowGenreFilter(), panelMain);
        addFilter(new TvShowCertificationFilter(), panelMain);
        addFilter(new TvShowStudioFilter(), panelMain);
        addFilter(new TvShowCastFilter(), panelMain);
        addFilter(new TvShowCountryFilter(), panelMain);
        addFilter(new TvShowTagFilter(), panelMain);
        addFilter(new TvShowEmptyFilter(), panelMain);
        addFilter(new TvShowUncategorizedEpisodesFilter(), panelMain);
        addFilter(new TvShowMissingEpisodesFilter(), panelMain);
        addFilter(new TvShowNoteFilter(), panelMain);
      }

      {
        // panel media data
        JPanel panelMediaData = new JPanel(new MigLayout("", "[][][150lp:150lp,grow]", "[]"));
        JScrollPane scrollPaneMediaData = new NoBorderScrollPane(panelMediaData);

        scrollPaneMediaData.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        tabbedPane.addTab(TmmResourceBundle.getString("metatag.mediainformation"), scrollPaneMediaData);
        panelMediaData.add(new TmmLabel(TmmResourceBundle.getString("movieextendedsearch.filterby")), "cell 0 0 3 1, growx, aligny top, wrap");

        addFilter(new TvShowDatasourceFilter(), panelMediaData);
        addFilter(new TvShowMediaFilesFilter(), panelMediaData);
        addFilter(new TvShowFilenameFilter(), panelMediaData);
        addFilter(new TvShowVideoFormatFilter(), panelMediaData);
        addFilter(new TvShowVideoCodecFilter(), panelMediaData);
        addFilter(new TvShowAspectRatioFilter(), panelMediaData);
        addFilter(new TvShowFrameRateFilter(), panelMediaData);
        addFilter(new TvShowVideoContainerFilter(), panelMediaData);
        addFilter(new TvShowAudioCodecFilter(), panelMediaData);
        addFilter(new TvShowAudioChannelFilter(), panelMediaData);
        addFilter(new TvShowAudioStreamCountFilter(), panelMediaData);
        addFilter(new TvShowAudioLanguageFilter(), panelMediaData);
        addFilter(new TvShowSubtitleCountFilter(), panelMediaData);
        addFilter(new TvShowSubtitleLanguageFilter(), panelMediaData);
        addFilter(new TvShowMediaSourceFilter(), panelMediaData);
        addFilter(new TvShowMissingMetadataFilter(), panelMediaData);
        addFilter(new TvShowMissingArtworkFilter(), panelMediaData);
        addFilter(new TvShowMissingSubtitlesFilter(), panelMediaData);
        addFilter(new TvShowHDRFormatFilter(), panelMediaData);
      }

      {
        // filter preset panel
        JPanel panelFilterPreset = new JPanel();
        panelFilterPreset.setLayout(new MigLayout("insets n 0 n 0", "[5lp!][10lp][150lp,grow][5lp!]", "[]"));

        JSeparator separator = new JSeparator();
        panelFilterPreset.add(separator, "cell 0 1 4 1,growx,aligny top");

        JLabel lblEnableAllT = new TmmLabel(TmmResourceBundle.getString("filter.enableall"));
        panelFilterPreset.add(lblEnableAllT, "cell 1 2, alignx trailing");

        JCheckBox chkbxEnableAll = new JCheckBox();
        chkbxEnableAll.setSelected(true);
        chkbxEnableAll.addActionListener(e -> treeTable.setFiltersActive(chkbxEnableAll.isSelected()));
        panelFilterPreset.add(chkbxEnableAll, "cell 2 2");

        JLabel lblFilterPresetT = new TmmLabel(TmmResourceBundle.getString("filter.presets"));
        panelFilterPreset.add(lblFilterPresetT, "cell 1 3, alignx trailing");

        cbPreset = new JComboBox<>();
        panelFilterPreset.add(cbPreset, "cell 2 3");

        JButton btnSavePreset = new FlatButton(IconManager.SAVE);
        btnSavePreset.setToolTipText(TmmResourceBundle.getString("filter.savepreset"));
        btnSavePreset.addActionListener(e -> {
          Set<AbstractSettings.UIFilters> activeUiFilters = getActiveUiFilters();
          if (!activeUiFilters.isEmpty()) {
            Map<String, List<AbstractSettings.UIFilters>> tvShowUiFilters = new HashMap<>(
                TvShowModuleManager.getInstance().getSettings().getUiFilterPresets());
            FilterSaveDialog saveDialog = new FilterSaveDialog(TvShowFilterDialog.this, activeUiFilters, tvShowUiFilters);
            saveDialog.setVisible(true);

            String savedPreset = saveDialog.getSavedPreset();
            if (StringUtils.isNotBlank(savedPreset)) {
              cbPreset.removeActionListener(actionListener);
              TvShowModuleManager.getInstance().getSettings().setUiFilterPresets(tvShowUiFilters);
              loadPresets();
              cbPreset.setSelectedItem(savedPreset);
              cbPreset.addActionListener(actionListener);
            }
          }
        });
        panelFilterPreset.add(btnSavePreset, "cell 2 3");

        JButton btnDeletePreset = new FlatButton(IconManager.DELETE_GRAY);
        btnDeletePreset.setToolTipText(TmmResourceBundle.getString("filter.remove"));
        btnDeletePreset.addActionListener(e -> {
          String filterName = (String) cbPreset.getSelectedItem();
          if (StringUtils.isBlank(filterName)) {
            return;
          }

          // display warning and ask the user again
          if (!TmmProperties.getInstance().getPropertyAsBoolean("tvshow.hidefilterhint")) {
            JCheckBox checkBox = new JCheckBox(TmmResourceBundle.getString("tmm.donotshowagain"));
            TmmFontHelper.changeFont(checkBox, L1);
            checkBox.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

            Object[] options = { TmmResourceBundle.getString("Button.yes"), TmmResourceBundle.getString("Button.no") };
            Object[] params = { TmmResourceBundle.getString("filter.remove"), checkBox };
            int answer = JOptionPane.showOptionDialog(MainWindow.getInstance(), params, TmmResourceBundle.getString("filter.remove"),
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, null);

            // the user don't want to show this dialog again
            if (checkBox.isSelected()) {
              TmmProperties.getInstance().putProperty("tvshow.hidefilterhint", String.valueOf(checkBox.isSelected()));
            }

            if (answer != JOptionPane.YES_OPTION) {
              return;
            }

            Map<String, List<AbstractSettings.UIFilters>> tvShowUiFilters = new HashMap<>(
                TvShowModuleManager.getInstance().getSettings().getUiFilterPresets());
            if (tvShowUiFilters.remove(filterName) != null) {
              cbPreset.removeActionListener(actionListener);
              TvShowModuleManager.getInstance().getSettings().setUiFilterPresets(tvShowUiFilters);
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

  private Set<AbstractSettings.UIFilters> getActiveUiFilters() {
    return new HashSet<>(ITvShowUIFilter.morphToUiFilters(filters));
  }

  private void loadPresets() {
    String preset = (String) cbPreset.getSelectedItem();

    cbPreset.removeAllItems();
    cbPreset.addItem("");
    TvShowModuleManager.getInstance().getSettings().getUiFilterPresets().keySet().stream().sorted().forEach(key -> cbPreset.addItem(key));

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
  private void addFilter(ITvShowUIFilter<TmmTreeNode> filter, JPanel panel) {
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

    treeTable.addFilter(filter);
  }

  /**
   * re-calculate if the active filter icon should be displayed
   */
  private void filterChanged() {
    for (Map.Entry<JPanel, Set<ITvShowUIFilter<?>>> entry : filterMap.entrySet()) {
      boolean active = false;
      if (treeTable.isFiltersActive()) {
        for (ITvShowUIFilter<?> filter : entry.getValue()) {
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
