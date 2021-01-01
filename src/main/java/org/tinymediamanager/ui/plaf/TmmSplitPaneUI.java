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

package org.tinymediamanager.ui.plaf;

import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

import com.formdev.flatlaf.ui.FlatArrowButton;
import com.formdev.flatlaf.util.UIScale;

/**
 * The class TmmSplitPaneUI
 *
 * @author Manuel Laggner
 */
public class TmmSplitPaneUI extends BasicSplitPaneUI {
  protected String arrowType;
  private int      oneTouchSize;
  private int      oneTouchOffset;
  private boolean  centerOneTouchButtons;
  private Boolean  continuousLayout;
  private Color    oneTouchArrowColor;
  private Color    oneTouchHoverArrowColor;

  protected void installDefaults() {
    this.oneTouchSize = UIManager.getInt("SplitPane.oneTouchButtonSize");
    this.oneTouchOffset = UIManager.getInt("SplitPane.oneTouchButtonOffset");
    this.centerOneTouchButtons = UIManager.getBoolean("SplitPane.centerOneTouchButtons");
    this.arrowType = UIManager.getString("Component.arrowType");
    this.oneTouchArrowColor = UIManager.getColor("SplitPaneDivider.oneTouchArrowColor");
    this.oneTouchHoverArrowColor = UIManager.getColor("SplitPaneDivider.oneTouchHoverArrowColor");
    super.installDefaults();
    this.continuousLayout = (Boolean) UIManager.get("SplitPane.continuousLayout");
  }

  public boolean isContinuousLayout() {
    return super.isContinuousLayout() || this.continuousLayout != null && Boolean.TRUE.equals(this.continuousLayout);
  }

  public static ComponentUI createUI(JComponent c) {
    return new TmmSplitPaneUI();
  }

  public BasicSplitPaneDivider createDefaultDivider() {
    return new TmmSplitPaneDivider(this);
  }

  public class TmmSplitPaneDivider extends BasicSplitPaneDivider {

    public TmmSplitPaneDivider(BasicSplitPaneUI ui) {
      super(ui);
      setLayout(new TmmDividerLayout());
    }

    public void setDividerSize(int newSize) {
      super.setDividerSize(UIScale.scale(newSize));
    }

    protected JButton createLeftOneTouchButton() {
      return new TmmSplitPaneDivider.FlatOneTouchButton(true);
    }

    protected JButton createRightOneTouchButton() {
      return new TmmSplitPaneDivider.FlatOneTouchButton(false);
    }

    protected class TmmDividerLayout extends DividerLayout {
      @Override
      public void layoutContainer(Container c) {
        if (leftButton != null && rightButton != null && c == TmmSplitPaneDivider.this) {
          if (splitPane.isOneTouchExpandable()) {
            Insets insets = getInsets();

            if (orientation == JSplitPane.VERTICAL_SPLIT) {
              int blockSize = getHeight();

              int x = (c.getSize().width - blockSize) / 2;
              int extraX = (insets != null) ? insets.left : 0;

              if (insets != null) {
                blockSize -= (insets.top + insets.bottom);
                blockSize = Math.max(blockSize, 0);
              }
              blockSize = Math.min(blockSize, oneTouchSize);

              int y = (c.getSize().height - blockSize) / 2;

              if (!centerOneTouchButtons) {
                y = (insets != null) ? insets.top : 0;
                extraX = 0;
              }
              leftButton.setBounds(x + extraX + oneTouchOffset, y, blockSize * 2, blockSize);
              rightButton.setBounds(x + extraX + oneTouchOffset + oneTouchSize * 2, y, blockSize * 2, blockSize);
            }
            else {
              int blockSize = getWidth();

              int y = (c.getSize().height - blockSize) / 2;
              int extraY = (insets != null) ? insets.top : 0;

              if (insets != null) {
                blockSize -= (insets.left + insets.right);
                blockSize = Math.max(blockSize, 0);
              }
              blockSize = Math.min(blockSize, oneTouchSize);

              int x = (c.getSize().width - blockSize) / 2;

              if (!centerOneTouchButtons) {
                x = (insets != null) ? insets.left : 0;
                extraY = 0;
              }

              leftButton.setBounds(x, y + extraY + oneTouchOffset, blockSize, blockSize * 2);
              rightButton.setBounds(x, y + extraY + oneTouchOffset + oneTouchSize * 2, blockSize, blockSize * 2);
            }
          }
          else {
            leftButton.setBounds(-5, -5, 1, 1);
            rightButton.setBounds(-5, -5, 1, 1);
          }
        }
      }
    }

    private class FlatOneTouchButton extends FlatArrowButton {
      private final boolean left;

      public FlatOneTouchButton(boolean left) {
        super(1, TmmSplitPaneUI.this.arrowType, TmmSplitPaneUI.this.oneTouchArrowColor, (Color) null, TmmSplitPaneUI.this.oneTouchHoverArrowColor,
            (Color) null);
        this.setCursor(Cursor.getPredefinedCursor(0));
        this.left = left;
      }

      public int getDirection() {
        return TmmSplitPaneDivider.this.orientation == 0 ? (this.left ? 1 : 5) : (this.left ? 7 : 3);
      }
    }
  }
}
