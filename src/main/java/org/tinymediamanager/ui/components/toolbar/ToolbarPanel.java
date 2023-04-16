/*
 * Copyright 2012 - 2022 Manuel Laggner
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
package org.tinymediamanager.ui.components.toolbar;

import static org.tinymediamanager.ui.TmmUIHelper.shouldCheckForUpdate;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.Timer;
import javax.swing.UIManager;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.Globals;
import org.tinymediamanager.TinyMediaManager;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.Message.MessageLevel;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.WolDevice;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.license.License;
import org.tinymediamanager.thirdparty.KodiRPC;
import org.tinymediamanager.ui.ITmmUIModule;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.TmmLazyMenuAdapter;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.actions.AboutAction;
import org.tinymediamanager.ui.actions.BugReportAction;
import org.tinymediamanager.ui.actions.CheckForUpdateAction;
import org.tinymediamanager.ui.actions.ClearHttpCacheAction;
import org.tinymediamanager.ui.actions.ClearImageCacheAction;
import org.tinymediamanager.ui.actions.CreateDesktopFileAction;
import org.tinymediamanager.ui.actions.DeleteTrashAction;
import org.tinymediamanager.ui.actions.DocsAction;
import org.tinymediamanager.ui.actions.ExportLogAction;
import org.tinymediamanager.ui.actions.FaqAction;
import org.tinymediamanager.ui.actions.FeedbackAction;
import org.tinymediamanager.ui.actions.ForumAction;
import org.tinymediamanager.ui.actions.HomepageAction;
import org.tinymediamanager.ui.actions.RebuildImageCacheAction;
import org.tinymediamanager.ui.actions.SettingsAction;
import org.tinymediamanager.ui.actions.ShowChangelogAction;
import org.tinymediamanager.ui.actions.UnlockAction;
import org.tinymediamanager.ui.dialogs.FullLogDialog;
import org.tinymediamanager.ui.dialogs.LogDialog;
import org.tinymediamanager.ui.dialogs.MessageHistoryDialog;
import org.tinymediamanager.ui.thirdparty.KodiRPCMenu;
import org.tinymediamanager.updater.UpdateCheck;

import net.miginfocom.swing.MigLayout;

/**
 * The Class ToolbarPanel.
 *
 * @author Manuel Laggner
 */
public class ToolbarPanel extends JPanel {
  private static final Logger LOGGER = LoggerFactory.getLogger(ToolbarPanel.class); // $NON-NLS-1$

  private final ToolbarButton btnSearch;
  private final ToolbarButton btnEdit;
  private final ToolbarButton btnUpdate;
  private final ToolbarButton btnRename;
  private final ToolbarButton btnUnlock;
  private final ToolbarButton btnRenewLicense;
  private final ToolbarButton btnUpdateFound;

  private final ToolbarMenu   menuUpdate;
  private final ToolbarMenu   menuSearch;
  private final ToolbarMenu   menuEdit;
  private final ToolbarMenu   menuRename;
  private final ToolbarMenu   menuTools;
  private final ToolbarMenu   menuInfo;

  private final ToolbarLabel  lblUnlock;
  private final ToolbarLabel  lblRenewLicense;
  private final ToolbarLabel  lblUpdateFound;

  public ToolbarPanel() {
    setLayout(new BorderLayout());

    JPanel panelCenter = new JPanel();
    add(panelCenter, BorderLayout.CENTER);
    panelCenter.setOpaque(false);
    panelCenter.setLayout(
        new MigLayout("insets 0, hidemode 3", "[15lp:n][]20lp[]20lp[]20lp[]20lp[][grow][]15lp[]15lp[]15lp[][][][][15lp:n]", "[50lp]1lp[]5lp"));

    panelCenter.add(new JLabel(IconManager.TOOLBAR_LOGO), "cell 1 0 1 2,center");

    btnUpdate = new ToolbarButton(IconManager.TOOLBAR_REFRESH, IconManager.TOOLBAR_REFRESH_HOVER);
    panelCenter.add(btnUpdate, "cell 2 0,grow, center");

    btnSearch = new ToolbarButton(IconManager.TOOLBAR_SEARCH, IconManager.TOOLBAR_SEARCH_HOVER);
    panelCenter.add(btnSearch, "cell 3 0,grow, center");

    btnEdit = new ToolbarButton(IconManager.TOOLBAR_EDIT, IconManager.TOOLBAR_EDIT_HOVER);
    panelCenter.add(btnEdit, "cell 4 0,grow, center");

    btnRename = new ToolbarButton(IconManager.TOOLBAR_RENAME, IconManager.TOOLBAR_RENAME_HOVER);
    panelCenter.add(btnRename, "cell 5 0,grow, center");

    JButton btnSettings = new ToolbarButton(IconManager.TOOLBAR_SETTINGS, IconManager.TOOLBAR_SETTINGS_HOVER);
    Action settingsAction = new SettingsAction();
    btnSettings.setAction(settingsAction);
    panelCenter.add(btnSettings, "cell 8 0,growx, alignx center,aligny bottom");

    JPopupMenu toolsPopupMenu = buildToolsMenu();
    JButton btnTools = new ToolbarButton(IconManager.TOOLBAR_TOOLS, IconManager.TOOLBAR_TOOLS_HOVER, toolsPopupMenu);
    panelCenter.add(btnTools, "cell 9 0,alignx center,aligny bottom");

    JPopupMenu infoPopupMenu = buildInfoMenu();
    JButton btnInfo = new ToolbarButton(IconManager.TOOLBAR_ABOUT, IconManager.TOOLBAR_ABOUT_HOVER, infoPopupMenu);
    panelCenter.add(btnInfo, "cell 10 0,alignx center,aligny bottom");

    btnUnlock = new ToolbarButton(IconManager.TOOLBAR_UPGRADE, IconManager.TOOLBAR_UPGRADE);
    Action unlockAction = new UnlockAction();
    btnUnlock.setAction(unlockAction);
    panelCenter.add(btnUnlock, "cell 11 0, alignx center,aligny bottom");

    btnRenewLicense = new ToolbarButton(IconManager.TOOLBAR_RENEW, IconManager.TOOLBAR_RENEW);
    btnRenewLicense.setAction(unlockAction);
    btnRenewLicense.setToolTipText(TmmResourceBundle.getString("Toolbar.renewlicense.desc"));
    panelCenter.add(btnRenewLicense, "cell 12 0, alignx center,aligny bottom, gap 10lp");

    btnUpdateFound = new ToolbarButton(IconManager.TOOLBAR_DOWNLOAD, IconManager.TOOLBAR_DOWNLOAD);
    btnUpdateFound.setAction(new CheckForUpdateAction());
    btnUpdateFound.setToolTipText(TmmResourceBundle.getString("tmm.update.message.toolbar"));
    panelCenter.add(btnUpdateFound, "cell 13 0, alignx center,aligny bottom, gap 10lp");

    menuUpdate = new ToolbarMenu(TmmResourceBundle.getString("Toolbar.update"));
    panelCenter.add(menuUpdate, "cell 2 1,alignx center, wmin 0");

    menuSearch = new ToolbarMenu(TmmResourceBundle.getString("Toolbar.search"));
    panelCenter.add(menuSearch, "cell 3 1,alignx center, wmin 0");

    menuEdit = new ToolbarMenu(TmmResourceBundle.getString("Toolbar.edit"));
    panelCenter.add(menuEdit, "cell 4 1,alignx center, wmin 0");

    menuRename = new ToolbarMenu(TmmResourceBundle.getString("Toolbar.rename"));
    panelCenter.add(menuRename, "cell 5 1,alignx center, wmin 0");

    JLabel lblSettings = new ToolbarLabel(TmmResourceBundle.getString("Toolbar.settings"), settingsAction);
    panelCenter.add(lblSettings, "cell 8 1,alignx center, wmin 0");

    menuTools = new ToolbarMenu(TmmResourceBundle.getString("Toolbar.tools"), toolsPopupMenu);
    panelCenter.add(menuTools, "cell 9 1,alignx center, wmin 0");

    menuInfo = new ToolbarMenu(TmmResourceBundle.getString("Toolbar.help"), infoPopupMenu);
    panelCenter.add(menuInfo, "cell 10 1,alignx center, wmin 0");

    lblUnlock = new ToolbarLabel(TmmResourceBundle.getString("Toolbar.upgrade"), unlockAction);
    lblUnlock.setToolTipText(TmmResourceBundle.getString("Toolbar.upgrade.desc"));
    panelCenter.add(lblUnlock, "cell 11 1,alignx center, gap 10lp, wmin 0");

    lblRenewLicense = new ToolbarLabel(TmmResourceBundle.getString("Toolbar.renewlicense"), unlockAction);
    lblRenewLicense.setToolTipText(TmmResourceBundle.getString("Toolbar.renewlicense.desc"));
    panelCenter.add(lblRenewLicense, "cell 12 1,alignx center, gap 10lp, wmin 0");

    lblUpdateFound = new ToolbarLabel(TmmResourceBundle.getString("tmm.update.message.toolbar"));
    lblUpdateFound.setToolTipText(TmmResourceBundle.getString("tmm.update.message"));
    panelCenter.add(lblUpdateFound, "cell 13 1,alignx center, gap 10lp, wmin 0");

    License.getInstance().addEventListener(this::showHideUnlock);

    showHideUnlock();

    initUpgradeCheck();
  }

  private void showHideUnlock() {
    if (License.getInstance().isValidLicense()) {
      btnUnlock.setVisible(false);
      lblUnlock.setVisible(false);

      LocalDate validUntil = License.getInstance().validUntil();
      if (validUntil != null && validUntil.minus(14, ChronoUnit.DAYS).isBefore(LocalDate.now())) {
        btnRenewLicense.setVisible(true);
        lblRenewLicense.setVisible(true);
      }
      else {
        btnRenewLicense.setVisible(false);
        lblRenewLicense.setVisible(false);
      }
    }
    else {
      btnUnlock.setVisible(true);
      lblUnlock.setVisible(true);
      btnRenewLicense.setVisible(false);
      lblRenewLicense.setVisible(false);
    }
  }

  private void initUpgradeCheck() {
    btnUpdateFound.setVisible(false);
    lblUpdateFound.setVisible(false);

    if (!Settings.getInstance().isNewConfig()) {
      // if the wizard is not run, check for an update
      // this has a simple reason: the wizard lets you do some settings only once: if you accept the update WHILE the wizard is
      // showing, the
      // wizard will no more appear
      // the same goes for the scraping AFTER the wizard has been started.. in this way the update check is only being done at the
      // next startup
      if (Settings.getInstance().isEnableAutomaticUpdate() && !Boolean.parseBoolean(System.getProperty("tmm.noupdate"))) {
        // only update if the last update check is more than the specified interval ago
        if (shouldCheckForUpdate()) {
          Runnable runnable = () -> {
            try {
              UpdateCheck updateCheck = new UpdateCheck();
              if (updateCheck.isUpdateAvailable()) {
                btnUpdateFound.setVisible(true);
                lblUpdateFound.setVisible(true);
                LOGGER.info("update available");
              }
            }
            catch (Exception e) {
              LOGGER.warn("Update check failed - {}", e.getMessage());
            }
          };

          // update task start a few secs after GUI...
          Timer timer = new Timer(10 * 1000, e -> TmmTaskManager.getInstance().addUnnamedTask(runnable));
          timer.setRepeats(false);
          timer.start();
        }
      }
    }
  }

  @Override
  public void updateUI() {
    super.updateUI();
    setBackground(UIManager.getColor("Tmm.toolbar.background"));
  }

  public void setUIModule(ITmmUIModule module) {
    btnUpdate.setAction(module.getUpdateAction());
    btnUpdate.setIcons(module.getSearchButtonIcon(), module.getSearchButtonHoverIcon());
    menuUpdate.setPopupMenu(module.getUpdateMenu());

    btnSearch.setAction(module.getSearchAction());
    menuSearch.setPopupMenu(module.getSearchMenu());

    btnEdit.setAction(module.getEditAction());
    menuEdit.setPopupMenu(module.getEditMenu());

    btnRename.setAction(module.getRenameAction());
    menuRename.setPopupMenu(module.getRenameMenu());
  }

  private JPopupMenu buildToolsMenu() {
    JPopupMenu menu = new JPopupMenu();

    menu.add(new ClearImageCacheAction());
    menu.add(new RebuildImageCacheAction());
    menu.add(new ClearHttpCacheAction());

    menu.addSeparator();

    JMenuItem tmmLogs = new JMenuItem(TmmResourceBundle.getString("tmm.errorlogs"));
    menu.add(tmmLogs);
    tmmLogs.addActionListener(arg0 -> {
      JDialog logDialog = new LogDialog();
      logDialog.setLocationRelativeTo(MainWindow.getInstance());
      logDialog.setVisible(true);
    });

    JMenuItem tmmMessages = new JMenuItem(TmmResourceBundle.getString("tmm.messages"));
    tmmMessages.setMnemonic(KeyEvent.VK_L);
    menu.add(tmmMessages);
    tmmMessages.addActionListener(arg0 -> {
      JDialog messageDialog = MessageHistoryDialog.getInstance();
      messageDialog.setVisible(true);
    });

    JMenuItem tmmFolder = new JMenuItem(TmmResourceBundle.getString("tmm.gotoinstalldir"));
    menu.add(tmmFolder);
    tmmFolder.setToolTipText(TmmResourceBundle.getString("tmm.gotoinstalldir.desc"));
    tmmFolder.addActionListener(arg0 -> openFolder(Paths.get(System.getProperty("user.dir"))));

    JMenuItem dataFolder = new JMenuItem(TmmResourceBundle.getString("tmm.gotodatadir"));
    menu.add(dataFolder);
    dataFolder.setToolTipText(TmmResourceBundle.getString("tmm.gotodatadir.desc"));
    dataFolder.addActionListener(arg0 -> openFolder(Paths.get(Globals.DATA_FOLDER)));

    JMenuItem logFolder = new JMenuItem(TmmResourceBundle.getString("tmm.gotologdir"));
    menu.add(logFolder);
    logFolder.setToolTipText(TmmResourceBundle.getString("tmm.gotologdir.desc"));
    logFolder.addActionListener(arg0 -> openFolder(Paths.get(Globals.LOG_FOLDER)));

    JMenuItem tmpFolder = new JMenuItem(TmmResourceBundle.getString("tmm.gototmpdir"));
    menu.add(tmpFolder);
    tmpFolder.addActionListener(arg0 -> openFolder(Paths.get(Utils.getTempFolder())));

    menu.add(new DeleteTrashAction());
    menu.addSeparator();

    final JMenu menuWakeOnLan = new JMenu(TmmResourceBundle.getString("tmm.wakeonlan"));
    menuWakeOnLan.setMnemonic(KeyEvent.VK_W);
    menuWakeOnLan.addMenuListener(new TmmLazyMenuAdapter() {
      @Override
      protected void menuWillBecomeVisible(JMenu menu) {
        menu.removeAll();
        for (final WolDevice device : Settings.getInstance().getWolDevices()) {
          JMenuItem item = new JMenuItem(device.getName());
          item.addActionListener(arg01 -> Utils.sendWakeOnLanPacket(device.getMacAddress()));
          menu.add(item);
        }
      }
    });
    menu.add(menuWakeOnLan);

    final JMenu kodiRPCMenu = KodiRPCMenu.createKodiMenuTop();
    menu.add(kodiRPCMenu);

    // activate/deactivate menu items based on some status
    menu.addPopupMenuListener(new TmmLazyMenuAdapter() {
      @Override
      protected void menuWillBecomeVisible(JMenu menu) {
        if (!Settings.getInstance().getWolDevices().isEmpty()) {
          menu.setEnabled(true);
        }
        else {
          menu.setEnabled(false);
        }

        JMenu kodiRPCMenu = null;
        for (Component comp : menu.getMenuComponents()) {
          if (comp instanceof JMenu subMenu && subMenu.getIcon() == IconManager.KODI) {
            kodiRPCMenu = subMenu;
            break;
          }
        }

        if (kodiRPCMenu != null) {
          if (StringUtils.isNotBlank(Settings.getInstance().getKodiHost())) {
            kodiRPCMenu.setText(KodiRPC.getInstance().getVersion());
            kodiRPCMenu.setEnabled(true);
          }
          else {
            kodiRPCMenu.setText("Kodi");
            kodiRPCMenu.setEnabled(false);
          }
        }
      }
    });

    if (Globals.isSelfUpdatable() || Globals.isDebug()) {
      menu.addSeparator();
    }

    if (Globals.isSelfUpdatable()) {
      menu.add(new CheckForUpdateAction());
    }

    // debug menu
    if (Globals.isDebug()) {
      final JMenu debugMenu = new JMenu("Debug");

      JMenuItem trace = new JMenuItem("set Console Logger to TRACE");
      trace.addActionListener(arg0 -> {
        System.setProperty("tmm.consoleloglevel", "TRACE");
        TinyMediaManager.setConsoleLogLevel();
        MessageManager.instance.pushMessage(new Message("Trace levels set!", "Test"));
        LOGGER.trace("if you see that, we're now on TRACE logging level ;)");
      });
      debugMenu.add(trace);

      JMenuItem traceLogs = new JMenuItem("Show all logs from this session");
      debugMenu.add(traceLogs);
      traceLogs.addActionListener(arg0 -> {
        JDialog logDialog = new FullLogDialog();
        logDialog.setLocationRelativeTo(MainWindow.getInstance());
        logDialog.setVisible(true);
      });

      menu.addSeparator();
      menu.add(debugMenu);
    }

    menu.addSeparator();
    menu.add(new BugReportAction());
    menu.add(new ExportLogAction());

    if (SystemUtils.IS_OS_LINUX) {
      menu.addSeparator();
      menu.add(new CreateDesktopFileAction());
    }

    return menu;
  }

  private static void openFolder(Path path) {
    try {
      // check whether this location exists
      if (Files.exists(path)) {
        TmmUIHelper.openFile(path);
      }
    }
    catch (Exception ex) {
      LOGGER.error("open filemanager", ex);
      MessageManager.instance
          .pushMessage(new Message(MessageLevel.ERROR, path, "message.erroropenfolder", new String[] { ":", ex.getLocalizedMessage() }));
    }
  }

  private JPopupMenu buildInfoMenu() {
    JPopupMenu menu = new JPopupMenu();

    menu.add(new FaqAction());
    menu.add(new DocsAction());
    menu.add(new ForumAction());
    menu.add(new ShowChangelogAction());
    menu.addSeparator();

    menu.add(new BugReportAction());
    menu.add(new FeedbackAction());

    menu.addSeparator();
    if (!License.getInstance().isValidLicense()) {
      menu.add(new UnlockAction());
    }
    menu.add(new HomepageAction());
    menu.add(new AboutAction());

    return menu;
  }

  public JPopupMenu getToolsMenu() {
    return menuTools.getPopupMenu();
  }

  public JPopupMenu getInfoMenu() {
    return menuInfo.getPopupMenu();
  }
}
