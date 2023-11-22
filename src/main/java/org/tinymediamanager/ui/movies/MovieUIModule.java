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
package org.tinymediamanager.ui.movies;

import java.awt.CardLayout;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.Globals;
import org.tinymediamanager.core.PostProcess;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MoviePostProcessExecutor;
import org.tinymediamanager.license.License;
import org.tinymediamanager.thirdparty.KodiRPC;
import org.tinymediamanager.ui.AbstractTmmUIModule;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.TmmLazyMenuAdapter;
import org.tinymediamanager.ui.components.MainTabbedPane;
import org.tinymediamanager.ui.components.MenuScroller;
import org.tinymediamanager.ui.movies.actions.DebugDumpMovieAction;
import org.tinymediamanager.ui.movies.actions.MovieAddDatasourceAction;
import org.tinymediamanager.ui.movies.actions.MovieAspectRatioDetectAction;
import org.tinymediamanager.ui.movies.actions.MovieAssignMovieSetAction;
import org.tinymediamanager.ui.movies.actions.MovieBulkEditAction;
import org.tinymediamanager.ui.movies.actions.MovieChangeDatasourceAction;
import org.tinymediamanager.ui.movies.actions.MovieCleanUpFilesAction;
import org.tinymediamanager.ui.movies.actions.MovieClearImageCacheAction;
import org.tinymediamanager.ui.movies.actions.MovieCreateOfflineAction;
import org.tinymediamanager.ui.movies.actions.MovieDeleteAction;
import org.tinymediamanager.ui.movies.actions.MovieDownloadActorImagesAction;
import org.tinymediamanager.ui.movies.actions.MovieDownloadMissingArtworkAction;
import org.tinymediamanager.ui.movies.actions.MovieEditAction;
import org.tinymediamanager.ui.movies.actions.MovieExportAction;
import org.tinymediamanager.ui.movies.actions.MovieFetchImdbTop250;
import org.tinymediamanager.ui.movies.actions.MovieFetchRatingsAction;
import org.tinymediamanager.ui.movies.actions.MovieFindMissingAction;
import org.tinymediamanager.ui.movies.actions.MovieLockAction;
import org.tinymediamanager.ui.movies.actions.MovieMediaInformationAction;
import org.tinymediamanager.ui.movies.actions.MovieOpenFolderAction;
import org.tinymediamanager.ui.movies.actions.MoviePlayAction;
import org.tinymediamanager.ui.movies.actions.MovieReadNfoAction;
import org.tinymediamanager.ui.movies.actions.MovieRebuildImageCacheAction;
import org.tinymediamanager.ui.movies.actions.MovieRebuildMediainfoXmlAction;
import org.tinymediamanager.ui.movies.actions.MovieRemoveAction;
import org.tinymediamanager.ui.movies.actions.MovieRenameAction;
import org.tinymediamanager.ui.movies.actions.MovieRenamePreviewAction;
import org.tinymediamanager.ui.movies.actions.MovieResetNewFlagAction;
import org.tinymediamanager.ui.movies.actions.MovieRewriteNfoAction;
import org.tinymediamanager.ui.movies.actions.MovieSelectedScrapeAction;
import org.tinymediamanager.ui.movies.actions.MovieSelectedScrapeMetadataAction;
import org.tinymediamanager.ui.movies.actions.MovieSingleScrapeAction;
import org.tinymediamanager.ui.movies.actions.MovieSubtitleDownloadAction;
import org.tinymediamanager.ui.movies.actions.MovieSubtitleSearchAction;
import org.tinymediamanager.ui.movies.actions.MovieSyncSelectedCollectionTraktTvAction;
import org.tinymediamanager.ui.movies.actions.MovieSyncSelectedRatingTraktTvAction;
import org.tinymediamanager.ui.movies.actions.MovieSyncSelectedTraktTvAction;
import org.tinymediamanager.ui.movies.actions.MovieSyncSelectedWatchedTraktTvAction;
import org.tinymediamanager.ui.movies.actions.MovieSyncTraktTvAction;
import org.tinymediamanager.ui.movies.actions.MovieToggleWatchedFlagAction;
import org.tinymediamanager.ui.movies.actions.MovieTrailerDownloadAction;
import org.tinymediamanager.ui.movies.actions.MovieUnlockAction;
import org.tinymediamanager.ui.movies.actions.MovieUnscrapedScrapeAction;
import org.tinymediamanager.ui.movies.actions.MovieUpdateAction;
import org.tinymediamanager.ui.movies.actions.MovieUpdateDatasourceAction;
import org.tinymediamanager.ui.movies.actions.MovieUpdateSingleDatasourceAction;
import org.tinymediamanager.ui.movies.dialogs.MovieFilterDialog;
import org.tinymediamanager.ui.movies.panels.MovieArtworkPanel;
import org.tinymediamanager.ui.movies.panels.MovieCastPanel;
import org.tinymediamanager.ui.movies.panels.MovieInformationPanel;
import org.tinymediamanager.ui.movies.panels.MovieListPanel;
import org.tinymediamanager.ui.movies.panels.MovieMediaInformationPanel;
import org.tinymediamanager.ui.movies.panels.MovieTrailerPanel;
import org.tinymediamanager.ui.movies.settings.MovieSettingsNode;
import org.tinymediamanager.ui.settings.TmmSettingsNode;
import org.tinymediamanager.ui.thirdparty.KodiRPCMenu;

import net.miginfocom.swing.MigLayout;

/**
 * The class MovieUIModule is the general access point to all movie related UI operations
 *
 * @author Manuel Laggner
 */
public class MovieUIModule extends AbstractTmmUIModule {
  private static final String       ID       = "movies";
  private static MovieUIModule      instance = null;

  private final MovieListPanel      listPanel;
  private final JPanel              detailPanel;
  private final MovieSelectionModel selectionModel;
  private final MovieFilterDialog   movieFilterDialog;
  private final TmmSettingsNode     settingsNode;

  private MovieUIModule() {
    listPanel = new MovieListPanel();
    selectionModel = listPanel.getSelectionModel();

    detailPanel = new JPanel();
    detailPanel.setLayout(new MigLayout("insets 0", "[grow]", "[grow]"));

    // need this panel for layouting
    JPanel dataPanel = new JPanel();
    dataPanel.setLayout(new CardLayout());
    detailPanel.add(dataPanel, "cell 0 0, grow");

    // tabbed pane containing the movie data
    JTabbedPane tabbedPane = new MainTabbedPane() {
      @Override
      public void updateUI() {
        putClientProperty("leftBorder", "half");
        putClientProperty("bottomBorder", Boolean.FALSE);
        putClientProperty("roundEdge", Boolean.FALSE);
        super.updateUI();
      }
    };

    tabbedPane.add(TmmResourceBundle.getString("metatag.details"), new MovieInformationPanel(selectionModel));
    tabbedPane.add(TmmResourceBundle.getString("metatag.cast"), new MovieCastPanel(selectionModel));
    tabbedPane.add(TmmResourceBundle.getString("metatag.mediafiles"), new MovieMediaInformationPanel(selectionModel));
    tabbedPane.add(TmmResourceBundle.getString("metatag.artwork"), new MovieArtworkPanel(selectionModel));
    tabbedPane.add(TmmResourceBundle.getString("metatag.trailer"), new MovieTrailerPanel(selectionModel));
    dataPanel.add(tabbedPane);

    movieFilterDialog = new MovieFilterDialog(selectionModel);

    createActions();
    createMenus();
    createPopupMenu();
    registerAccelerators();

    // settings node
    settingsNode = new MovieSettingsNode();

    // further initializations
    init();
  }

  private void init() {
    // apply stored UI filters
    if (MovieModuleManager.getInstance().getSettings().isStoreUiFilters()) {
      SwingUtilities.invokeLater(() -> {
        MovieModuleManager.getInstance().getMovieList().searchDuplicates();
        selectionModel.setFilterValues(MovieModuleManager.getInstance().getSettings().getUiFilters());
      });
    }

    // init the table panel
    listPanel.init();
  }

  public static MovieUIModule getInstance() {
    if (instance == null) {
      instance = new MovieUIModule();
    }
    return instance;
  }

  public MovieSelectionModel getSelectionModel() {
    return selectionModel;
  }

  private void createActions() {
    searchAction = createAndRegisterAction(MovieSingleScrapeAction.class);
    editAction = createAndRegisterAction(MovieEditAction.class);
    updateAction = createAndRegisterAction(MovieUpdateDatasourceAction.class);
    renameAction = createAndRegisterAction(MovieRenameAction.class);
  }

  private void createMenus() {
    updateMenu = new JMenu(TmmResourceBundle.getString("Toolbar.update"));
    updateMenu.setIcon(IconManager.REFRESH);
    updateMenu.add(createAndRegisterAction(MovieUpdateDatasourceAction.class));

    JMenu datasourcesMenu = new JMenu(TmmResourceBundle.getString("metatag.datasource"));
    datasourcesMenu.setIcon(IconManager.MENU);
    MenuScroller.setScrollerFor(datasourcesMenu, 20, 50, 0, 0);
    datasourcesMenu.getPopupMenu().addPopupMenuListener(new TmmLazyMenuAdapter() {
      @Override
      protected void menuWillBecomeVisible(JMenu menu) {
        menu.removeAll();

        for (String ds : MovieModuleManager.getInstance().getSettings().getMovieDataSource()) {
          menu.add(new MovieUpdateSingleDatasourceAction(ds));
        }
      }
    });
    updateMenu.add(datasourcesMenu);

    updateMenu.addSeparator();
    updateMenu.add(createAndRegisterAction(MovieUpdateAction.class));
    updateMenu.addSeparator();
    updateMenu.add(createAndRegisterAction(MovieFindMissingAction.class));
    updateMenu.add(createAndRegisterAction(MovieCreateOfflineAction.class));
    updateMenu.addSeparator();
    updateMenu.add(createAndRegisterAction(MovieAddDatasourceAction.class));

    // search popup menu
    searchMenu = new JMenu(TmmResourceBundle.getString("Toolbar.search"));
    searchMenu.setIcon(IconManager.SEARCH);
    searchMenu.add(createAndRegisterAction(MovieSingleScrapeAction.class));
    searchMenu.add(createAndRegisterAction(MovieSelectedScrapeAction.class));
    searchMenu.add(createAndRegisterAction(MovieSelectedScrapeMetadataAction.class));
    searchMenu.add(createAndRegisterAction(MovieUnscrapedScrapeAction.class));

    // edit popup menu
    editMenu = new JMenu(TmmResourceBundle.getString("Toolbar.edit"));
    editMenu.setIcon(IconManager.EDIT);
    editMenu.add(createAndRegisterAction(MovieEditAction.class));
    editMenu.add(createAndRegisterAction(MovieBulkEditAction.class));
    editMenu.add(createAndRegisterAction(MovieLockAction.class));
    editMenu.add(createAndRegisterAction(MovieUnlockAction.class));
    editMenu.add(createAndRegisterAction(MovieToggleWatchedFlagAction.class));
    editMenu.add(createAndRegisterAction(MovieFetchRatingsAction.class));
    editMenu.add(createAndRegisterAction(MovieFetchImdbTop250.class));
    editMenu.add(createAndRegisterAction(MovieAssignMovieSetAction.class));
    editMenu.add(createAndRegisterAction(MovieChangeDatasourceAction.class));
    editMenu.add(createAndRegisterAction(MovieRewriteNfoAction.class));
    editMenu.add(createAndRegisterAction(MovieReadNfoAction.class));

    editMenu.addSeparator();
    editMenu.add(createAndRegisterAction(MovieMediaInformationAction.class));
    editMenu.add(createAndRegisterAction(MovieRebuildMediainfoXmlAction.class));
    editMenu.add(createAndRegisterAction(MovieAspectRatioDetectAction.class));

    editMenu.addSeparator();
    editMenu.add(createAndRegisterAction(MovieRebuildImageCacheAction.class));
    editMenu.add(createAndRegisterAction(MovieResetNewFlagAction.class));

    editMenu.addSeparator();
    JMenu downloadMenu = new JMenu(TmmResourceBundle.getString("tmm.download"));
    downloadMenu.setIcon(IconManager.MENU);
    downloadMenu.add(createAndRegisterAction(MovieDownloadMissingArtworkAction.class));
    downloadMenu.add(createAndRegisterAction(MovieDownloadActorImagesAction.class));
    downloadMenu.add(createAndRegisterAction(MovieTrailerDownloadAction.class));
    downloadMenu.add(createAndRegisterAction(MovieSubtitleSearchAction.class));
    downloadMenu.add(createAndRegisterAction(MovieSubtitleDownloadAction.class));
    editMenu.add(downloadMenu);

    editMenu.addSeparator();
    JMenu traktMenu = new JMenu("Trakt.tv");
    traktMenu.setIcon(IconManager.MENU);
    traktMenu.add(createAndRegisterAction(MovieSyncTraktTvAction.class));
    traktMenu.addSeparator();
    traktMenu.add(createAndRegisterAction(MovieSyncSelectedTraktTvAction.class));
    traktMenu.add(createAndRegisterAction(MovieSyncSelectedCollectionTraktTvAction.class));
    traktMenu.add(createAndRegisterAction(MovieSyncSelectedWatchedTraktTvAction.class));
    traktMenu.add(createAndRegisterAction(MovieSyncSelectedRatingTraktTvAction.class));
    editMenu.add(traktMenu);

    editMenu.addSeparator();
    editMenu.add(createAndRegisterAction(MovieExportAction.class));

    // rename popup menu
    renameMenu = new JMenu(TmmResourceBundle.getString("Toolbar.rename"));
    renameMenu.add(createAndRegisterAction(MovieRenameAction.class));
    renameMenu.add(createAndRegisterAction(MovieRenamePreviewAction.class));
    renameMenu.addSeparator();
    renameMenu.add(createAndRegisterAction(MovieCleanUpFilesAction.class));
    renameMenu.add(createAndRegisterAction(MovieClearImageCacheAction.class));
  }

  private void createPopupMenu() {
    popupMenu = new JPopupMenu();

    popupMenu.add(createAndRegisterAction(MovieSingleScrapeAction.class));
    popupMenu.add(createAndRegisterAction(MovieSelectedScrapeAction.class));
    popupMenu.add(createAndRegisterAction(MovieSelectedScrapeMetadataAction.class));
    popupMenu.add(createAndRegisterAction(MovieUnscrapedScrapeAction.class));

    popupMenu.addSeparator();
    popupMenu.add(createAndRegisterAction(MovieUpdateAction.class));
    popupMenu.add(createAndRegisterAction(MovieReadNfoAction.class));

    JMenu updateDatasourcesMenu = new JMenu(TmmResourceBundle.getString("Toolbar.update"));
    updateDatasourcesMenu.setIcon(IconManager.MENU);
    updateDatasourcesMenu.add(createAndRegisterAction(MovieUpdateDatasourceAction.class));

    JMenu datasourcesMenu = new JMenu(TmmResourceBundle.getString("metatag.datasource"));
    datasourcesMenu.setIcon(IconManager.MENU);
    MenuScroller.setScrollerFor(datasourcesMenu, 20, 50, 2, 5);
    datasourcesMenu.getPopupMenu().addPopupMenuListener(new TmmLazyMenuAdapter() {
      @Override
      protected void menuWillBecomeVisible(JMenu menu) {
        menu.removeAll();

        for (String ds : MovieModuleManager.getInstance().getSettings().getMovieDataSource()) {
          menu.add(new MovieUpdateSingleDatasourceAction(ds));
        }
      }
    });
    updateDatasourcesMenu.add(datasourcesMenu);

    updateDatasourcesMenu.addSeparator();
    updateDatasourcesMenu.add(createAndRegisterAction(MovieFindMissingAction.class));
    updateDatasourcesMenu.add(createAndRegisterAction(MovieCreateOfflineAction.class));
    updateDatasourcesMenu.addSeparator();
    updateDatasourcesMenu.add(createAndRegisterAction(MovieAddDatasourceAction.class));
    popupMenu.add(updateDatasourcesMenu);

    popupMenu.addSeparator();
    popupMenu.add(createAndRegisterAction(MovieEditAction.class));

    JMenu enhancedEditMenu = new JMenu(TmmResourceBundle.getString("edit.enhanced"));
    enhancedEditMenu.setIcon(IconManager.MENU);
    enhancedEditMenu.add(createAndRegisterAction(MovieBulkEditAction.class));
    enhancedEditMenu.add(createAndRegisterAction(MovieLockAction.class));
    enhancedEditMenu.add(createAndRegisterAction(MovieUnlockAction.class));
    enhancedEditMenu.add(createAndRegisterAction(MovieToggleWatchedFlagAction.class));
    enhancedEditMenu.add(createAndRegisterAction(MovieFetchRatingsAction.class));
    enhancedEditMenu.add(createAndRegisterAction(MovieFetchImdbTop250.class));
    enhancedEditMenu.add(createAndRegisterAction(MovieAssignMovieSetAction.class));
    enhancedEditMenu.add(createAndRegisterAction(MovieChangeDatasourceAction.class));
    enhancedEditMenu.add(createAndRegisterAction(MovieRewriteNfoAction.class));
    enhancedEditMenu.addSeparator();
    enhancedEditMenu.add(createAndRegisterAction(MovieMediaInformationAction.class));
    enhancedEditMenu.add(createAndRegisterAction(MovieRebuildMediainfoXmlAction.class));
    enhancedEditMenu.add(createAndRegisterAction(MovieAspectRatioDetectAction.class));
    enhancedEditMenu.addSeparator();
    enhancedEditMenu.add(createAndRegisterAction(MovieRebuildImageCacheAction.class));
    enhancedEditMenu.add(createAndRegisterAction(MovieResetNewFlagAction.class));
    popupMenu.add(enhancedEditMenu);

    JMenu downloadMenu = new JMenu(TmmResourceBundle.getString("tmm.download"));
    downloadMenu.setIcon(IconManager.MENU);
    downloadMenu.add(createAndRegisterAction(MovieDownloadMissingArtworkAction.class));
    downloadMenu.add(createAndRegisterAction(MovieDownloadActorImagesAction.class));
    downloadMenu.add(createAndRegisterAction(MovieTrailerDownloadAction.class));
    downloadMenu.add(createAndRegisterAction(MovieSubtitleSearchAction.class));
    downloadMenu.add(createAndRegisterAction(MovieSubtitleDownloadAction.class));
    popupMenu.add(downloadMenu);

    JMenu renameMenu = new JMenu(TmmResourceBundle.getString("Toolbar.rename"));
    renameMenu.setIcon(IconManager.MENU);
    renameMenu.add(createAndRegisterAction(MovieRenameAction.class));
    renameMenu.add(createAndRegisterAction(MovieRenamePreviewAction.class));
    renameMenu.addSeparator();
    renameMenu.add(createAndRegisterAction(MovieCleanUpFilesAction.class));
    renameMenu.add(createAndRegisterAction(MovieClearImageCacheAction.class));
    popupMenu.add(renameMenu);

    popupMenu.addSeparator();
    popupMenu.add(createAndRegisterAction(MovieExportAction.class));
    popupMenu.add(createAndRegisterAction(MoviePlayAction.class));
    popupMenu.add(createAndRegisterAction(MovieOpenFolderAction.class));

    popupMenu.addSeparator();
    JMenu traktMenu = new JMenu("Trakt.tv");
    traktMenu.setIcon(IconManager.MENU);
    traktMenu.add(createAndRegisterAction(MovieSyncTraktTvAction.class));
    traktMenu.addSeparator();
    traktMenu.add(createAndRegisterAction(MovieSyncSelectedTraktTvAction.class));
    traktMenu.add(createAndRegisterAction(MovieSyncSelectedCollectionTraktTvAction.class));
    traktMenu.add(createAndRegisterAction(MovieSyncSelectedWatchedTraktTvAction.class));
    traktMenu.add(createAndRegisterAction(MovieSyncSelectedRatingTraktTvAction.class));
    popupMenu.add(traktMenu);

    JMenu kodiRPCMenu = KodiRPCMenu.createMenuKodiMenuRightClickMovies();
    popupMenu.add(kodiRPCMenu);

    JMenu postProcessingMenu = new JMenu(TmmResourceBundle.getString("Settings.postprocessing"));
    postProcessingMenu.setIcon(IconManager.MENU);
    popupMenu.add(postProcessingMenu);

    popupMenu.addSeparator();
    popupMenu.add(createAndRegisterAction(MovieRemoveAction.class));
    popupMenu.add(createAndRegisterAction(MovieDeleteAction.class));

    if (Globals.isDebug()) {
      final JMenu debugMenu = new JMenu("Debug");
      debugMenu.add(new DebugDumpMovieAction());
      popupMenu.addSeparator();
      popupMenu.add(debugMenu);
    }

    // activate/deactivate menu items based on some status
    popupMenu.addPopupMenuListener(new PopupMenuListener() {
      @Override
      public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        if (StringUtils.isNotBlank(Settings.getInstance().getKodiHost())) {
          kodiRPCMenu.setText(KodiRPC.getInstance().getVersion());
          kodiRPCMenu.setEnabled(true);
        }
        else {
          kodiRPCMenu.setText("Kodi");
          kodiRPCMenu.setEnabled(false);
        }

        if (License.getInstance().isValidLicense() && StringUtils.isNotBlank(Settings.getInstance().getTraktAccessToken())) {
          traktMenu.setEnabled(true);
        }
        else {
          traktMenu.setEnabled(false);
        }

        // Post-processing
        postProcessingMenu.removeAll();
        for (PostProcess process : new ArrayList<>(MovieModuleManager.getInstance().getSettings().getPostProcess())) {
          JMenuItem menuItem = new JMenuItem(process.getName(), IconManager.APPLY);
          menuItem.addActionListener(pp -> new MoviePostProcessExecutor(process).execute());
          postProcessingMenu.add(menuItem);
        }
        if (postProcessingMenu.getItemCount() == 0) {
          postProcessingMenu.setEnabled(false);
        }
        else {
          postProcessingMenu.setEnabled(true);
        }
      }

      @Override
      public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
        // nothing to do
      }

      @Override
      public void popupMenuCanceled(PopupMenuEvent e) {
        // nothing to do
      }
    });

    listPanel.setPopupMenu(popupMenu);

    // update popup menu
  }

  @Override
  public String getModuleId() {
    return ID;
  }

  @Override
  public JPanel getTabPanel() {
    return listPanel;
  }

  @Override
  public String getTabTitle() {
    return TmmResourceBundle.getString("tmm.movies");
  }

  @Override
  public ImageIcon getMenuIcon() {
    return IconManager.MENU_MOVIES;
  }

  @Override
  public ImageIcon getMenuActiveIcon() {
    return IconManager.MENU_MOVIES_ACTIVE;
  }

  @Override
  public JPanel getDetailPanel() {
    return detailPanel;
  }

  @Override
  public TmmSettingsNode getSettingsNode() {
    return settingsNode;
  }

  public void setFilterDialogVisible(boolean selected) {
    movieFilterDialog.setVisible(selected);
  }
}
