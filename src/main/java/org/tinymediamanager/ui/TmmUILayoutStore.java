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

import static java.awt.Frame.MAXIMIZED_BOTH;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JSplitPane;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.Globals;
import org.tinymediamanager.core.TmmProperties;
import org.tinymediamanager.ui.components.table.TmmTable;

/**
 * The Class TmmUiLayoutStore. To save UI settings (like window size/positions, splitpane divider location, visible table columns, ...)
 * 
 * @author Manuel Laggner
 */
public class TmmUILayoutStore implements AWTEventListener {
  private static TmmUILayoutStore instance;

  private final TmmProperties     properties;
  private final Set<String>       componentSet;

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

    if (component instanceof JSplitPane) {
      installJSplitPane((JSplitPane) component);
    }
    else if (component instanceof TmmTable) {
      installTmmTable((TmmTable) component);
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

      if (table.getTableComparatorChooser() != null) {
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
    if (!Globals.settings.isStoreWindowPreferences()) {
      // at least display the main frame centered
      if ("mainWindow".equals(frame.getName())) {
        frame.setLocationRelativeTo(null);
      }
      return;
    }

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

        frame.setBounds(rect);

        // was the main window maximized?
        if (Boolean.TRUE.equals(properties.getPropertyAsBoolean("mainWindowMaximized"))) {
          frame.setExtendedState(frame.getExtendedState() | MAXIMIZED_BOTH);
          frame.validate();
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
    if (!Globals.settings.isStoreWindowPreferences() || StringUtils.isBlank(dialog.getName())) {
      dialog.pack();
      dialog.setLocationRelativeTo(dialog.getParent());
      return;
    }

    if (!dialog.getName().contains("dialog")) {
      Rectangle rect = getWindowBounds(dialog.getName());
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
    if (!Globals.settings.isStoreWindowPreferences()) {
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

      if (comp instanceof Container)
        saveChildren((Container) comp);
    }
  }

  private void saveComponent(Component component) {
    if (component instanceof JSplitPane) {
      saveJSplitPane((JSplitPane) component);
    }
    else if (component instanceof TmmTable) {
      saveTmmTable((TmmTable) component);
    }
  }

  private void saveJSplitPane(JSplitPane splitPane) {
    String componentName = splitPane.getName();
    addParam(componentName + ".dividerLocation", splitPane.getDividerLocation());
  }

  private void saveTmmTable(TmmTable table) {
    String componentName = table.getName();
    addParam(componentName + ".hiddenColumns", String.join(",", table.getHiddenColumns()));

    if (table.getTableComparatorChooser() != null) {
      addParam(componentName + ".sortState", table.getTableComparatorChooser().toString());
    }
  }

  /**
   * Save settings for a dialog
   * 
   * @param dialog
   *          the dialog
   */
  public void saveSettings(JDialog dialog) {
    if (!Globals.settings.isStoreWindowPreferences() || StringUtils.isBlank(dialog.getName())) {
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
    if ((rect.x + rect.width) > (screenBounds.x + screenBounds.width - scnMax.left - scnMax.right)) {
      rect.x = screenBounds.x + scnMax.left;
      rect.width = screenBounds.width - scnMax.right;
    }

    if ((rect.y + rect.height) > (screenBounds.y + screenBounds.height - scnMax.top - scnMax.bottom)) {
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

  @Override
  public void eventDispatched(AWTEvent evt) {
    ComponentEvent cev = (ComponentEvent) evt;
    // frame = mainWindow
    if (evt.getID() == WindowEvent.WINDOW_CLOSING && cev.getComponent() instanceof JFrame) {
      JFrame frame = (JFrame) cev.getComponent();
      saveSettings(frame);
    }
    // popup dialogs
    if (evt.getID() == WindowEvent.WINDOW_CLOSED && cev.getComponent() instanceof JDialog) {
      JDialog dialog = (JDialog) cev.getComponent();
      saveSettings(dialog);
    }
  }
}
