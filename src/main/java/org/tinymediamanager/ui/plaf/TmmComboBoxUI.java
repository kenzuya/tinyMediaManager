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
package org.tinymediamanager.ui.plaf;

import static com.formdev.flatlaf.util.UIScale.scale;

import java.awt.Dimension;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;

import com.formdev.flatlaf.ui.FlatComboBoxUI;
import com.formdev.flatlaf.ui.FlatUIUtils;

/**
 * Provides the Flat LaF UI delegate for {@link javax.swing.JComboBox}.
 *
 * <!-- BasicComboBoxUI -->
 *
 * @uiDefault ComboBox.font Font
 * @uiDefault ComboBox.background Color
 * @uiDefault ComboBox.foreground Color
 * @uiDefault ComboBox.border Border
 * @uiDefault ComboBox.padding Insets
 * @uiDefault ComboBox.squareButton boolean default is true
 *
 *            <!-- FlatComboBoxUI -->
 *
 * @uiDefault ComboBox.minimumWidth int
 * @uiDefault ComboBox.editorColumns int
 * @uiDefault ComboBox.maximumRowCount int
 * @uiDefault ComboBox.buttonStyle String auto (default), button or none
 * @uiDefault Component.arrowType String triangle (default) or chevron
 * @uiDefault Component.isIntelliJTheme boolean
 * @uiDefault Component.borderColor Color
 * @uiDefault Component.disabledBorderColor Color
 * @uiDefault ComboBox.editableBackground Color optional; defaults to ComboBox.background
 * @uiDefault ComboBox.disabledBackground Color
 * @uiDefault ComboBox.disabledForeground Color
 * @uiDefault ComboBox.buttonBackground Color
 * @uiDefault ComboBox.buttonEditableBackground Color
 * @uiDefault ComboBox.buttonArrowColor Color
 * @uiDefault ComboBox.buttonDisabledArrowColor Color
 * @uiDefault ComboBox.buttonHoverArrowColor Color
 *
 * @author Manuel Laggner
 */
public class TmmComboBoxUI extends FlatComboBoxUI {

  public static ComponentUI createUI(JComponent c) {
    return new TmmComboBoxUI();
  }

  @Override
  protected JButton createArrowButton() {
    JButton arrowButton = new TmmComboBoxButton();
    // just make is look like a square button on "default" comboboxes, but do not stretch on multi line comboboxes
    arrowButton.setBorder(BorderFactory.createEmptyBorder(0, 1, 0, 2));
    return arrowButton;
  }

  /**
   * The minimum size is the size of the display area plus insets plus the button.
   */
  @Override
  public Dimension getMinimumSize(JComponent c) {
    if (!isMinimumSizeDirty) {
      return new Dimension(cachedMinimumSize);
    }
    Dimension size = getDisplaySize();
    Insets insets = getInsets();
    Insets arrowInsets = arrowButton.getInsets();
    // calculate the width and height of the button
    int buttonHeight = size.height;
    int buttonWidth = squareButton ? buttonHeight : arrowButton.getPreferredSize().width + arrowInsets.left + arrowInsets.right;
    // adjust the size based on the button width
    size.height += insets.top + insets.bottom;
    size.width += insets.left + insets.right + buttonWidth;

    cachedMinimumSize.setSize(size.width, size.height);
    isMinimumSizeDirty = false;

    size.width = Math.max(size.width, scale(FlatUIUtils.minimumWidth(c, minimumWidth)));
    return new Dimension(size);
  }

  private class TmmComboBoxButton extends FlatComboBoxButton {
    // needed to have access to the constructor
  }
}
