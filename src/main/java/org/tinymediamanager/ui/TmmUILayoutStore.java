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
package org.tinymediamanager.ui;

import static java.awt.Frame.MAXIMIZED_BOTH;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JSplitPane;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.TmmProperties;
import org.tinymediamanager.ui.components.table.TmmTable;

/**
 * The Class TmmUiLayoutStore. To save UI settings (like window size/positions, splitpane divider location, visible table columns, ...)
 * 
 * @author Manuel Laggner
 */
public class TmmUILayoutStore {
  private static TmmUILayoutStore instance;

  private final TmmProperties     properties;
  private final Set<String>       componentSet;

  private boolean                 skipSaving = false;

  private TmmUILayoutStore() {
    properties = TmmProperties.getInstance();
    componentSet = new HashSet<>();
  }

  /**
   * get an instance of this class
   *
   * @return an instance of this class
   */
  public static synchronized TmmUILayoutStore getInstance() {
    if (instance == null) {
      instance = new TmmUILayoutStore();
    }
    return instance;
  }

  /**
   * install the {@link TmmUILayoutStore} to the given component. Supported components for the are: <br/>
   * - JSplitPane<br/>
   * - TmmTable
   *
   * @param component
   *          the component to install the ui store
   */
  public void install(JComponent component) {
    // only if the component has a name
    if (StringUtils.isBlank(component.getName())) {
      return;
    }

    if (component instanceof JSplitPane splitPane) {
      installJSplitPane(splitPane);
    }
    else if (component instanceof TmmTable tmmTable) {
      installTmmTable(tmmTable);
    }

  }

  private void installJSplitPane(JSplitPane splitPane) {
    String componentName = splitPane.getName();

    componentSet.add(componentName);

    int dividerLocation = properties.getPropertyAsInteger(componentName + ".dividerLocation");
    if (dividerLocation > 0) {
      splitPane.setDividerLocation(dividerLocation);
    }
  }

  private void installTmmTable(TmmTable table) {
    String componentName = table.getName();

    componentSet.add(componentName);

    String hiddenColumnsAsString = properties.getProperty(componentName + ".hiddenColumns");
    if (StringUtils.isNotBlank(hiddenColumnsAsString)) {
      List<String> hiddenColumns = Arrays.asList(hiddenColumnsAsString.split(","));
      table.readHiddenColumns(hiddenColumns);

      if (table.getTableComparatorChooser() != null && StringUtils.isNotBlank(properties.getProperty(componentName + ".sortState"))) {
        table.getTableComparatorChooser().fromString(properties.getProperty(componentName + ".sortState"));
      }
    }
    else if (hiddenColumnsAsString == null) {
      // set the default hidden columns of the table model
      table.setDefaultHiddenColumns();
    }
  }

  /**
   * Load settings for a frame
   * 
   * @param frame
   *          the frame
   */
  public void loadSettings(JFrame frame) {
    // settings for main window
    if ("mainWindow".equals(frame.getName())) {
      // only set location/size if something was stored
      Rectangle rect = getWindowBounds("mainWindow");
      if (rect.width > 0) {
        GraphicsDevice ge = getScreenForBounds(rect);
        if (ge.getDefaultConfiguration() != frame.getGraphicsConfiguration()) {
          // move to another screen
          JFrame dummy = new JFrame(ge.getDefaultConfiguration());
          frame.setLocationRelativeTo(dummy);
          dummy.dispose();
        }

        // re-calculate the window rect (maybe it is bigger than the visible screen size)
        Rectangle screenBounds = ge.getDefaultConfiguration().getBounds();
        Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(ge.getDefaultConfiguration());

        if (rect.width > screenBounds.width - screenInsets.left - screenInsets.right) {
          rect.width = screenBounds.width - screenInsets.left - screenInsets.right;
        }

        if (rect.x < screenInsets.left) {
          rect.x = screenInsets.left;
        }

        if (rect.height > screenBounds.height - screenInsets.top - screenInsets.bottom) {
          rect.height = screenBounds.height - screenInsets.top - screenInsets.bottom;
        }

        if (rect.y < screenInsets.top) {
          rect.y = screenInsets.top;
        }

        frame.setBounds(rect);

        // was the main window maximized?
        // do not set this on linux (Wayland problem with missing window decorations)
        if (!SystemUtils.IS_OS_LINUX) {
          if (Boolean.TRUE.equals(properties.getPropertyAsBoolean("mainWindowMaximized"))) {
            frame.setExtendedState(frame.getExtendedState() | MAXIMIZED_BOTH);
            frame.validate();
          }
        }
      }
      else {
        frame.setLocationRelativeTo(null);
      }
    }
  }

  /**
   * Load settings for a dialog
   * 
   * @param dialog
   *          the dialog
   */
  public void loadSettings(JDialog dialog) {
    if (!StringUtils.isBlank(dialog.getName()) && !dialog.getName().contains("dialog")) {
      Rectangle rect = getWindowBounds(dialog.getName());

      if (rect.width == 0 && rect.height == 0) {
        // nothing found for that dialog
        dialog.pack();
        dialog.setLocationRelativeTo(dialog.getParent());
        return;
      }

      Dimension minimumSize = dialog.getMinimumSize();

      // re-check if the stored window size is "big" enough (the "default" size has already been set with .pack())
      if (rect.width < minimumSize.width) {
        rect.width = minimumSize.width;
      }
      if (rect.height < minimumSize.height) {
        rect.height = minimumSize.height;
      }

      if (rect.width > 0 && getVirtualBounds().contains(rect)) {
        GraphicsDevice ge = getScreenForBounds(rect);
        if (ge.getDefaultConfiguration() != dialog.getGraphicsConfiguration()) {
          // move to another screen
          JFrame dummy = new JFrame(ge.getDefaultConfiguration());
          dialog.setLocationRelativeTo(dummy);
          dummy.dispose();
        }

        dialog.setBounds(rect);
      }
      else {
        dialog.pack();
        dialog.setLocationRelativeTo(dialog.getParent());
      }
    }
    else {
      dialog.pack();
      dialog.setLocationRelativeTo(dialog.getParent());
    }
  }

  private GraphicsDevice getScreenForBounds(Rectangle rectangle) {
    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    GraphicsDevice[] gs = ge.getScreenDevices();

    for (GraphicsDevice device : gs) {
      GraphicsConfiguration gc = device.getDefaultConfiguration();
      Rectangle bounds = gc.getBounds();
      if (bounds.contains(rectangle)) {
        return device;
      }
    }

    return ge.getDefaultScreenDevice();
  }

  /**
   * Save settings for a frame
   * 
   * @param frame
   *          the frame
   */
  public void saveSettings(JFrame frame) {
    if (!Settings.getInstance().isStoreWindowPreferences() || skipSaving) {
      return;
    }

    // settings for main window
    if ("mainWindow".equals(frame.getName()) && frame instanceof MainWindow) {

      // if the frame is maximized, we simply take the screen coordinates
      if ((frame.getExtendedState() & MAXIMIZED_BOTH) == MAXIMIZED_BOTH) {
        storeWindowBounds("mainWindow", frame.getGraphicsConfiguration().getBounds());
        addParam("mainWindowMaximized", true);
      }
      else {
        storeWindowBounds("mainWindow", frame.getBounds());
        addParam("mainWindowMaximized", false);
      }
    }

    saveChildren(frame);
  }

  private void saveChildren(Container container) {
    Component[] comps = container.getComponents();

    for (Component comp : comps) {
      if (componentSet.contains(comp.getName())) {
        if (comp instanceof JComponent && componentSet.contains(comp.getName())) {
          saveComponent(comp);
        }
      }

      if (comp instanceof Container container1)
        saveChildren(container1);
    }
  }

  private void saveComponent(Component component) {
    if (component instanceof JSplitPane splitPane) {
      saveJSplitPane(splitPane);
    }
    else if (component instanceof TmmTable tmmTable) {
      saveTmmTable(tmmTable);
    }
  }

  private void saveJSplitPane(JSplitPane splitPane) {
    String componentName = splitPane.getName();
    if (splitPane.getLastDividerLocation() != -1) {
      // workaround for #2432, where the splitPane is always 7px drifting
      addParam(componentName + ".dividerLocation", splitPane.getDividerLocation());
    }
  }

  private void saveTmmTable(TmmTable table) {
    String componentName = table.getName();
    addParam(componentName + ".hiddenColumns", String.join(",", table.getHiddenColumns()));

    if (table.getTableComparatorChooser() != null) {
      addParam(componentName + ".sortState", table.getTableComparatorChooser().toString());
    }
  }

  /**
   * allow to hide a new column after upgrade
   * 
   * @param tableIdentifier
   *          the @{@link TmmTable} id to hide the column for
   * @param columnName
   *          the column id to hide
   */
  public void hideNewColumn(String tableIdentifier, String columnName) {
    if (StringUtils.isBlank(tableIdentifier)) {
      return;
    }

    String hiddenColumnsAsString = properties.getProperty(tableIdentifier + ".hiddenColumns");
    if (StringUtils.isNotBlank(hiddenColumnsAsString)) {
      List<String> hiddenColumns = new ArrayList<>(Arrays.asList(hiddenColumnsAsString.split(",")));
      if (!hiddenColumns.contains(columnName)) {
        hiddenColumns.add(columnName);
      }
      addParam(tableIdentifier + ".hiddenColumns", String.join(",", hiddenColumns));
      properties.writeProperties();
    }
  }

  /**
   * general purpose flag to skip saving
   *
   * @param skipSaving
   *          true/false
   */
  public void setSkipSaving(boolean skipSaving) {
    this.skipSaving = skipSaving;
  }

  /**
   * Save settings for a dialog
   * 
   * @param dialog
   *          the dialog
   */
  public void saveSettings(JDialog dialog) {
    if (!Settings.getInstance().isStoreWindowPreferences() || StringUtils.isBlank(dialog.getName()) || skipSaving) {
      return;
    }

    // do not save the values for "unnamed" dialogs
    if (!dialog.getName().contains("dialog")) {
      storeWindowBounds(dialog.getName(), dialog.getBounds());
    }

    saveChildren(dialog);
  }

  private void storeWindowBounds(String name, Rectangle bounds) {
    addParam(name + ".bounds", bounds.x + "," + bounds.y + "," + bounds.width + "," + bounds.height);
  }

  private Rectangle getWindowBounds(String name) {
    Rectangle rect = new Rectangle();

    String boundsAsString = properties.getProperty(name + ".bounds");

    if (StringUtils.isBlank(boundsAsString)) {
      return rect;
    }

    try {
      String[] parts = boundsAsString.split(",");
      rect.x = Integer.parseInt(parts[0]);
      rect.y = Integer.parseInt(parts[1]);
      rect.width = Integer.parseInt(parts[2]);
      rect.height = Integer.parseInt(parts[3]);
    }
    catch (Exception e) {
      return rect;
    }

    // prevent from moving out of the screen (can happen in Docker/VNC)
    if (rect.x < 0) {
      rect.x = 0;
    }
    if (rect.y < 0) {
      rect.y = 0;
    }

    // check if the stored sizes fit to any screen
    GraphicsConfiguration graphicsConfiguration = null;

    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    GraphicsDevice[] gs = ge.getScreenDevices();

    for (GraphicsDevice device : gs) {
      GraphicsConfiguration gc = device.getDefaultConfiguration();
      Rectangle bounds = gc.getBounds();
      if (bounds.contains(rect)) {
        graphicsConfiguration = gc;
        break;
      }
    }

    if (graphicsConfiguration == null) {
      graphicsConfiguration = ge.getDefaultScreenDevice().getDefaultConfiguration();
    }

    // screen insets / taskbar
    Rectangle screenBounds = graphicsConfiguration.getBounds();
    Insets scnMax = Toolkit.getDefaultToolkit().getScreenInsets(graphicsConfiguration);
    if ((rect.x - scnMax.left + rect.width) > (screenBounds.x + screenBounds.width - scnMax.right)) {
      rect.x = screenBounds.x + scnMax.left;
      rect.width = screenBounds.width - scnMax.right;
    }

    if ((rect.y - scnMax.top + rect.height) > (screenBounds.y + screenBounds.height - scnMax.bottom)) {
      rect.y = screenBounds.y + scnMax.top;
      rect.height = screenBounds.height - scnMax.bottom;
    }

    return rect;
  }

  private void addParam(String key, Object value) {
    properties.putProperty(key, value.toString());
  }

  private Rectangle getVirtualBounds() {
    Rectangle bounds = new Rectangle(0, 0, 0, 0);
    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    GraphicsDevice[] lstGDs = ge.getScreenDevices();
    for (GraphicsDevice gd : lstGDs) {
      bounds.add(gd.getDefaultConfiguration().getBounds());
    }
    return bounds;
  }
}
