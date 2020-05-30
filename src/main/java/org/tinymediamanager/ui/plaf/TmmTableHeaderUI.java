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

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import com.formdev.flatlaf.ui.FlatTableHeaderUI;
import com.formdev.flatlaf.ui.FlatUIUtils;
import com.formdev.flatlaf.util.UIScale;

import ca.odell.glazedlists.swing.SortableRenderer;
import net.miginfocom.swing.MigLayout;

/**
 * the class TmmTableHeaderUI is used to render the table header including icons
 *
 * @author Manuel Laggner
 */
public class TmmTableHeaderUI extends FlatTableHeaderUI {

  public static ComponentUI createUI(JComponent c) {
    return new TmmTableHeaderUI();
  }

  @Override
  protected void installDefaults() {
    super.installDefaults();

    TableCellRenderer defaultRenderer = header.getDefaultRenderer();
    if (defaultRenderer instanceof UIResource) {
      header.setDefaultRenderer(new TmmTableCellHeaderRenderer(defaultRenderer));
    }
  }

  @Override
  protected void uninstallDefaults() {
    super.uninstallDefaults();

    // restore default renderer
    TableCellRenderer defaultRenderer = header.getDefaultRenderer();
    if (defaultRenderer instanceof TmmTableCellHeaderRenderer) {
      header.setDefaultRenderer(((TmmTableCellHeaderRenderer) defaultRenderer).delegate);
    }

    separatorColor = null;
    bottomSeparatorColor = null;
  }

  public void paint(Graphics g, JComponent c) {
    super.paint(g, c);

    TableCellRenderer defaultRenderer = this.header.getDefaultRenderer();
    boolean paintBorders = this.isSystemDefaultRenderer(defaultRenderer);
    if (!paintBorders && this.header.getColumnModel().getColumnCount() > 0) {
      Component rendererComponent = defaultRenderer.getTableCellRendererComponent(this.header.getTable(), "", false, false, -1, 0);
      paintBorders = this.isSystemDefaultRenderer(rendererComponent);
    }

    // re-paint the column borders
    if (paintBorders) {
      this.paintColumnBorders(g, c);
    }

    if (paintBorders) {
      this.paintDraggedColumnBorders(g, c);
    }
  }

  private boolean isSystemDefaultRenderer(Object headerRenderer) {
    String rendererClassName = headerRenderer.getClass().getName();
    return rendererClassName.equals("sun.swing.table.DefaultTableCellHeaderRenderer")
        || rendererClassName.equals("sun.swing.FilePane$AlignableTableHeaderRenderer")
        || rendererClassName.equals(TmmTableCellHeaderRenderer.class.getName());
  }

  private void paintColumnBorders(Graphics g, JComponent c) {
    int width = c.getWidth();
    int height = c.getHeight();
    float lineWidth = UIScale.scale(1.0F);
    float topLineIndent = lineWidth;
    float bottomLineIndent = 0;
    TableColumnModel columnModel = this.header.getColumnModel();
    int columnCount = columnModel.getColumnCount();
    Graphics2D g2 = (Graphics2D) g.create();

    try {
      FlatUIUtils.setRenderingHints(g2);

      // horizontal
      g2.setColor(this.bottomSeparatorColor);
      g2.fill(new Rectangle2D.Float(0.0F, (float) height - lineWidth, (float) width, lineWidth));
      g2.setColor(this.separatorColor);
      int sepCount = columnCount;
      if (this.header.getTable().getAutoResizeMode() != 0 && !this.isVerticalScrollBarVisible()) {
        sepCount = columnCount - 1;
      }

      // vertical
      ArrayList<Integer> colsWoRightGrid = new ArrayList<>();
      if (this.header.getTable().getClientProperty("borderNotToDraw") != null) {
        colsWoRightGrid = (ArrayList<Integer>) this.header.getTable().getClientProperty("borderNotToDraw");
      }

      int x;
      int i;
      if (this.header.getComponentOrientation().isLeftToRight()) {
        x = 0;

        for (i = 0; i < sepCount; ++i) {
          x += columnModel.getColumn(i).getWidth();

          if (colsWoRightGrid.contains(i)) {
            continue;
          }

          g2.fill(new Rectangle2D.Float((float) x - lineWidth, topLineIndent, lineWidth, (float) height - bottomLineIndent));
        }
      }
      else {
        x = width;

        for (i = 0; i < sepCount; ++i) {
          x -= columnModel.getColumn(i).getWidth();
          g2.fill(
              new Rectangle2D.Float((float) x - (i < sepCount - 1 ? lineWidth : 0.0F), topLineIndent, lineWidth, (float) height - bottomLineIndent));
        }
      }
    }
    finally {
      g2.dispose();
    }

  }

  private void paintDraggedColumnBorders(Graphics g, JComponent c) {
    TableColumn draggedColumn = this.header.getDraggedColumn();
    if (draggedColumn != null) {
      TableColumnModel columnModel = this.header.getColumnModel();
      int columnCount = columnModel.getColumnCount();
      int draggedColumnIndex = -1;

      for (int i = 0; i < columnCount; ++i) {
        if (columnModel.getColumn(i) == draggedColumn) {
          draggedColumnIndex = i;
          break;
        }
      }

      if (draggedColumnIndex >= 0) {
        float lineWidth = UIScale.scale(1.0F);
        float topLineIndent = lineWidth;
        float bottomLineIndent = lineWidth * 3.0F;
        Rectangle r = this.header.getHeaderRect(draggedColumnIndex);
        r.x += this.header.getDraggedDistance();
        Graphics2D g2 = (Graphics2D) g.create();

        try {
          FlatUIUtils.setRenderingHints(g2);
          g2.setColor(this.bottomSeparatorColor);
          g2.fill(new Rectangle2D.Float((float) r.x, (float) (r.y + r.height) - lineWidth, (float) r.width, lineWidth));
          g2.setColor(this.separatorColor);
          g2.fill(new Rectangle2D.Float((float) r.x, topLineIndent, lineWidth, (float) r.height - bottomLineIndent));
          g2.fill(new Rectangle2D.Float((float) (r.x + r.width) - lineWidth, (float) r.y + topLineIndent, lineWidth,
              (float) r.height - bottomLineIndent));
        }
        finally {
          g2.dispose();
        }

      }
    }
  }

  private boolean isVerticalScrollBarVisible() {
    JScrollPane scrollPane = this.getScrollPane();
    return scrollPane != null && scrollPane.getVerticalScrollBar() != null ? scrollPane.getVerticalScrollBar().isVisible() : false;
  }

  private JScrollPane getScrollPane() {
    Container parent = this.header.getParent();
    if (parent == null) {
      return null;
    }
    else {
      parent = parent.getParent();
      return parent instanceof JScrollPane ? (JScrollPane) parent : null;
    }
  }

  /**
   * A delegating header renderer that is only used to paint icons along with sort arrows
   */
  private static class TmmTableCellHeaderRenderer extends JPanel implements TableCellRenderer, UIResource, SortableRenderer {
    private final TableCellRenderer delegate;
    private final JLabel            labelLeft;
    private final JLabel            labelRight;

    private Icon                    sortIcon;

    TmmTableCellHeaderRenderer(TableCellRenderer delegate) {
      this.delegate = delegate;
      setLayout(new MigLayout("insets 0, hidemode 3, center", "[]", "[grow]"));
      this.labelLeft = new JLabel();

      add(this.labelLeft, "cell 0 0");
      this.labelRight = new JLabel();
      this.labelRight.setIconTextGap(0);

      add(this.labelRight, "cell 0 0, gapx 1");
    }

    @Override
    public void setSortIcon(Icon sortIcon) {
      this.sortIcon = sortIcon;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      Component c = delegate.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      if (!(c instanceof JLabel)) {
        return c;
      }

      JLabel label = (JLabel) c;
      setBorder(label.getBorder());
      setForeground(label.getForeground());
      setBackground(label.getBackground());
      setFont(label.getFont());

      labelLeft.setForeground(label.getForeground());
      labelLeft.setBackground(label.getBackground());
      labelLeft.setFont(label.getFont());

      labelRight.setForeground(label.getForeground());
      labelRight.setBackground(label.getBackground());
      labelRight.setFont(label.getFont());

      // move the sort icon to the right label
      if (sortIcon == null) {
        labelRight.setVisible(false);
      }
      else {
        labelRight.setVisible(true);
        labelRight.setIcon(sortIcon);
      }

      if (value instanceof ImageIcon) {
        labelLeft.setIcon((ImageIcon) value);
        labelLeft.setText("");
      }
      else {
        labelLeft.setText((value == null) ? "" : value.toString());
        labelLeft.setIcon(null);
      }

      return this;
    }
  }

}
