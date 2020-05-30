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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.CellRendererPane;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.plaf.ComponentUI;
import javax.swing.table.TableColumn;

import com.formdev.flatlaf.ui.FlatTableUI;
import com.formdev.flatlaf.ui.FlatUIUtils;

/**
 * the class TmmTableUI is used to render the JTable in our way
 *
 * @author Manuel Laggner
 */
public class TmmTableUI extends FlatTableUI {

  private boolean paintTmmGrid;
  private Color   gridColor;
  private Color   gridColor2;
  private Color   selectedGridColor;
  private Border  defaultTableCellBorder;

  public static ComponentUI createUI(JComponent c) {
    return new TmmTableUI();
  }

  @Override
  public void installUI(JComponent c) {
    super.installUI(c);

    paintTmmGrid = UIManager.getBoolean("Table.paintTmmGrid");
    if (paintTmmGrid) {
      gridColor = UIManager.getColor("Table.gridColor");
      gridColor2 = UIManager.getColor("Table.gridColor2");
      selectedGridColor = UIManager.getColor("Table.selectedGridColor");
      defaultTableCellBorder = UIManager.getBorder("Table.cellNoFocusBorder");

      table.remove(rendererPane);
      rendererPane = createCustomCellRendererPane();
      table.add(rendererPane);
    }
  }

  @Override
  public void paint(Graphics g, JComponent c) {
    // paint the grid lines ourself
    if (paintTmmGrid) {
      paintHorizontalGridLines(g, c);
      paintVerticalGridLines(g, c);
    }

    super.paint(g, c);
  }

  private void paintHorizontalGridLines(Graphics g, JComponent c) {
    Graphics2D g2 = (Graphics2D) g.create();
    try {
      FlatUIUtils.setRenderingHints(g2);

      Rectangle clip = g2.getClipBounds();
      Rectangle bounds = table.getBounds();

      // account for the fact that the graphics has already been translated
      // into the table's bounds
      bounds.x = bounds.y = 0;

      // compute the visible part of table which needs to be painted
      Rectangle visibleBounds = clip.intersection(bounds);
      Point upperLeft = visibleBounds.getLocation();

      // get the row index at the top of the clip bounds (the first row to paint).
      int rowAtPoint = table.rowAtPoint(upperLeft);

      // get the y coordinate of the first row to paint. if there are no
      // rows in the table, start painting at the top of the supplied clipping bounds.
      int topY = rowAtPoint < 0 ? g2.getClipBounds().y : table.getCellRect(rowAtPoint, 0, true).y;

      // create a counter variable to hold the current row. if there are no
      // rows in the table, start the counter at 0.
      int currentRow = Math.max(rowAtPoint, 0);
      while (topY < g.getClipBounds().y + g2.getClipBounds().height) {
        int bottomY = topY + table.getRowHeight(currentRow);
        g2.setColor(gridColor);
        g2.drawLine(5, bottomY - 1, bounds.width, bottomY - 1);
        if (gridColor2 != null) {
          g2.setColor(gridColor2);
          g2.drawLine(5, bottomY, bounds.width, bottomY);
        }
        topY = bottomY;
        currentRow++;
      }
    }
    finally {
      g2.dispose();
    }
  }

  private void paintVerticalGridLines(Graphics g, JComponent c) {
    Graphics2D g2 = (Graphics2D) g.create();
    try {
      FlatUIUtils.setRenderingHints(g2);

      Rectangle clip = g2.getClipBounds();
      Rectangle bounds = table.getBounds();

      // account for the fact that the graphics has already been translated
      // into the table's bounds
      bounds.x = bounds.y = 0;

      // compute the visible part of table which needs to be painted
      Rectangle visibleBounds = clip.intersection(bounds);
      Point upperLeft = visibleBounds.getLocation();

      int drawColumnCountOffset = 0;

      ArrayList<Integer> colsWoRightGrid = new ArrayList<>();
      if (table.getClientProperty("borderNotToDraw") != null) {
        colsWoRightGrid = (ArrayList<Integer>) table.getClientProperty("borderNotToDraw");
      }

      int x = 0;
      for (int i = 0; i < table.getColumnCount() - drawColumnCountOffset; i++) {
        TableColumn column = table.getColumnModel().getColumn(i);
        // increase the x position by the width of the current column.
        x += column.getWidth();

        if (colsWoRightGrid.contains(i)) {
          continue;
        }

        if (x >= 0) {
          g2.setColor(gridColor);
          // draw the grid line (not sure what the -1 is for, but BasicTableUI also does it.
          g2.drawLine(x - 1, g2.getClipBounds().y, x - 1, bounds.height);
        }
      }
    }
    finally {
      g2.dispose();
    }
  }

  /**
   * Creates a custom {@link CellRendererPane} that sets the renderer component to be non-opaque if the associated row isn't selected. This custom
   * {@code CellRendererPane} is needed because a table UI delegate has no prepare renderer like {@link JTable} has.
   */
  private CellRendererPane createCustomCellRendererPane() {
    return new CellRendererPane() {
      private static final long serialVersionUID = 7146435127995900923L;

      @SuppressWarnings({ "unchecked", "rawtypes" })
      @Override
      public void paintComponent(Graphics graphics, Component component, Container container, int x, int y, int w, int h, boolean shouldValidate) {
        // figure out what row we're rendering a cell for.
        Point point = new Point(x, y);
        int rowAtPoint = table.rowAtPoint(point);
        int columnAtPoint = table.columnAtPoint(point);

        boolean isSelected = table.isRowSelected(rowAtPoint);

        // look if there are any non drawable borders defined
        Object prop = table.getClientProperty("borderNotToDraw");
        List<Integer> colsNotToDraw = new ArrayList<>();
        if (prop instanceof List<?>) {
          try {
            colsNotToDraw.addAll((List) prop);
          }
          catch (Exception ignored) {
          }
        }

        if (component instanceof JCheckBox) {
          // prevent the checkbox from clearing the horizontal lines
          ((JCheckBox) component).setContentAreaFilled(false);
        }

        // if the component to render is a JComponent, add our tweaks.
        if (component instanceof JComponent) {
          JComponent jcomponent = (JComponent) component;
          jcomponent.setOpaque(isSelected);

          if (isSelected && !colsNotToDraw.contains(columnAtPoint) && columnAtPoint != table.getColumnCount() - 1) {
            jcomponent.setBorder(BorderFactory.createCompoundBorder(new RightSideBorder(selectedGridColor), jcomponent.getBorder()));
          }
        }

        super.paintComponent(graphics, component, container, x, y, w, h, shouldValidate);
      }
    };
  }

  private static class RightSideBorder extends AbstractBorder {

    private static final int THICKNESS = 1;

    private final Color      color;

    public RightSideBorder(Color color) {
      this.color = color;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
      Graphics2D g2d = (Graphics2D) g.create();
      g2d.setColor(this.color);
      g2d.setStroke(new BasicStroke(THICKNESS));
      g2d.drawLine(width - 1, 0, width - 1, height - 1);
      g2d.dispose();
    }
  }
}
