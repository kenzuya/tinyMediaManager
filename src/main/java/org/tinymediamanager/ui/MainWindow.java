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
package org.tinymediamanager.ui;

import static org.tinymediamanager.TinyMediaManager.shutdownLogger;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLayer;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeListener;
import javax.swing.text.JTextComponent;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.ReleaseInfo;
import org.tinymediamanager.core.ITmmModule;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.TmmModuleManager;
import org.tinymediamanager.core.TmmProperties;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.thirdparty.MediaInfo;
import org.tinymediamanager.ui.components.MainTabbedPane;
import org.tinymediamanager.ui.components.TextFieldPopupMenu;
import org.tinymediamanager.ui.components.toolbar.ToolbarPanel;
import org.tinymediamanager.ui.dialogs.SettingsDialog;
import org.tinymediamanager.ui.images.LogoCircle;
import org.tinymediamanager.ui.movies.MovieUIModule;
import org.tinymediamanager.ui.moviesets.MovieSetUIModule;
import org.tinymediamanager.ui.panels.StatusBarPanel;
import org.tinymediamanager.ui.tvshows.TvShowUIModule;

import com.sun.jna.Platform;

import net.miginfocom.swing.MigLayout;

/**
 * The Class MainWindow.
 * 
 * @author Manuel Laggner
 */
public class MainWindow extends JFrame {

  private static final Logger     LOGGER           = LoggerFactory.getLogger(MainWindow.class);
  private static final long       serialVersionUID = 1L;

  public static final List<Image> LOGOS            = createLogos();

  private static MainWindow       instance;

  private JTabbedPane             tabbedPane;
  private JPanel                  detailPanel;

  /**
   * Gets the active instance.
   *
   * @return the active instance
   */
  public static synchronized MainWindow getInstance() {
    if (instance == null) {
      new MainWindow("tinyMediaManager / " + ReleaseInfo.getRealVersion());
    }
    return instance;
  }

  /**
   * Create the application.
   * 
   * @param name
   *          the name
   */
  private MainWindow(String name) {
    super(name);
    setName("mainWindow");
    setMinimumSize(new Dimension(1050, 700));

    instance = this; // NOSONAR

    initialize();
  }

  /**
   * load all predefined logo sizes
   * 
   * @return a list of all predefined logos
   */
  private static List<Image> createLogos() {

    return List.of(new LogoCircle(48).getImage(), new LogoCircle(64).getImage(), new LogoCircle(96).getImage(), new LogoCircle(128).getImage(),
        new LogoCircle(256).getImage());
  }

  /**
   * Initialize the contents of the frame.
   */
  private void initialize() {
    // set the logo
    setIconImages(LOGOS);
    setBounds(5, 5, 1100, 720);
    // do nothing, we have our own windowClosing() listener
    setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

    ToolbarPanel toolbarPanel = new ToolbarPanel();
    getContentPane().add(toolbarPanel, BorderLayout.NORTH);

    JPanel rootPanel = new JPanel();
    Color color = UIManager.getColor("Panel.tmmAlternateBackground");
    if (color != null) {
      rootPanel.setBackground(color);
    }
    rootPanel.setLayout(new MigLayout("insets 0", "[900lp:n,grow]", "[300lp:400lp,grow,shrink 0]0[shrink 0]"));

    // to draw the shadow beneath the toolbar, encapsulate the panel
    JLayer<JComponent> rootLayer = new JLayer<>(rootPanel, new ShadowLayerUI()); // $hide$ - do not parse this in wbpro
    getContentPane().add(rootLayer, BorderLayout.CENTER);

    JSplitPane splitPane = new JSplitPane();
    splitPane.setContinuousLayout(true);
    splitPane.setResizeWeight(0.5);
    splitPane.setOneTouchExpandable(true);
    splitPane.setName("mainWindow.splitPane");
    TmmUILayoutStore.getInstance().install(splitPane);
    rootPanel.add(splitPane, "cell 0 0, grow");

    tabbedPane = new MainTabbedPane() {
      private static final long serialVersionUID = 9041548865608767661L;

      @Override
      public void updateUI() {
        putClientProperty("rightBorder", "half");
        putClientProperty("roundEdge", Boolean.FALSE);
        super.updateUI();
      }
    };
    splitPane.setLeftComponent(tabbedPane);

    detailPanel = new JPanel();
    detailPanel.setLayout(new CardLayout(0, 0));
    splitPane.setRightComponent(detailPanel);

    JPanel panelStatusBar = new StatusBarPanel();
    rootPanel.add(panelStatusBar, "cell 0 1,grow");

    addModule(MovieUIModule.getInstance());
    toolbarPanel.setUIModule(MovieUIModule.getInstance());
    addModule(MovieSetUIModule.getInstance());
    addModule(TvShowUIModule.getInstance());

    ChangeListener changeListener = changeEvent -> {
      JTabbedPane sourceTabbedPane = (JTabbedPane) changeEvent.getSource();
      if (sourceTabbedPane.getSelectedComponent() instanceof ITmmTabItem) {
        ITmmTabItem activeTab = (ITmmTabItem) sourceTabbedPane.getSelectedComponent();
        toolbarPanel.setUIModule(activeTab.getUIModule());
        CardLayout cl = (CardLayout) detailPanel.getLayout();
        cl.show(detailPanel, activeTab.getUIModule().getModuleId());
      }
    };
    tabbedPane.addChangeListener(changeListener);

    // shutdown listener - to clean database connections safely
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        closeTmm();
      }
    });

    MessageManager.instance.addListener(TmmUIMessageCollector.instance);

    // mouse event listener for context menu
    Toolkit.getDefaultToolkit().addAWTEventListener(arg0 -> {
      if (arg0 instanceof MouseEvent && ((MouseEvent) arg0).isPopupTrigger() && arg0.getSource() instanceof JTextComponent) {
        MouseEvent me = (MouseEvent) arg0;
        JTextComponent tc = (JTextComponent) arg0.getSource();
        if (me.isPopupTrigger() && tc.getComponentPopupMenu() == null) {
          TextFieldPopupMenu.buildCutCopyPaste().show(tc, me.getX(), me.getY());
        }
      }
    }, AWTEvent.MOUSE_EVENT_MASK);

    // inform user that MI could not be loaded
    if (Platform.isLinux() && StringUtils.isBlank(MediaInfo.version())) {
      SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(MainWindow.this, TmmResourceBundle.getString("mediainfo.failed.linux")));
    }

    // inform user that something happened while loading the modules
    for (ITmmModule module : TmmModuleManager.getInstance().getModules()) {
      if (!module.getStartupMessages().isEmpty()) {
        for (String message : module.getStartupMessages()) {
          SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(MainWindow.this, message));
        }
      }
    }

    // init the settings window
    SwingUtilities.invokeLater(SettingsDialog::getInstance);
  }

  private void addModule(ITmmUIModule module) {
    tabbedPane.addTab(module.getTabTitle(), module.getTabPanel());
    detailPanel.add(module.getDetailPanel(), module.getModuleId());
  }

  public void closeTmm() {
    closeTmmAndStart(null);
  }

  public void closeTmmAndStart(ProcessBuilder pb) {
    int confirm = JOptionPane.YES_OPTION;
    // if there are some threads running, display exit confirmation
    if (TmmTaskManager.getInstance().poolRunning()) {
      Object[] options = { TmmResourceBundle.getString("Button.yes"), TmmResourceBundle.getString("Button.no") };
      confirm = JOptionPane.showOptionDialog(null, TmmResourceBundle.getString("tmm.exit.runningtasks"),
          TmmResourceBundle.getString("tmm.exit.confirmation"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, null); // $NON-NLS-1$
    }
    if (confirm == JOptionPane.YES_OPTION) {
      LOGGER.info("bye bye");
      try {
        // persist all stored properties
        TmmProperties.getInstance().writeProperties();

        // send shutdown signal
        TmmTaskManager.getInstance().shutdown();
        // save unsaved settings
        TmmModuleManager.getInstance().saveSettings();
        // hard kill
        TmmTaskManager.getInstance().shutdownNow();
        // close database connection
        TmmModuleManager.getInstance().shutDown();
      }
      catch (Exception ex) {
        LOGGER.warn("", ex);
      }
      dispose();

      // spawn our process
      if (pb != null) {
        try {
          LOGGER.info("Going to execute: {}", pb.command());
          pb.start();
        }
        catch (Exception e) {
          LOGGER.error("Cannot spawn process:", e);
        }
      }
      shutdownLogger();
      System.exit(0); // calling the method is a must
    }
  }

  /**
   * Gets the frame.
   * 
   * @return the frame
   */
  public static JFrame getFrame() {
    return instance;
  }

  public void createLightbox(String pathToFile, String urlToFile) {
    LightBox.showLightBox(instance, pathToFile, urlToFile);
  }
}
