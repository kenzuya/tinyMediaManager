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
package org.tinymediamanager.ui.components.table;

import static javax.swing.ScrollPaneConstants.UPPER_RIGHT_CORNER;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS;

import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.CellRendererPane;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.ui.IconManager;

import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.swing.TableComparatorChooser;

/**
 * The Class TmmTable. It's being used to draw the tables like our designer designed it
 *
 * @author Manuel Laggner
 */
public class TmmTable extends JTable {
  private static final long             serialVersionUID = 6150939811851709115L;
  private static final ResourceBundle   BUNDLE           = ResourceBundle.getBundle("messages");

  private static final CellRendererPane CELL_RENDER_PANE = new CellRendererPane();
  private TableComparatorChooser<?>     tableComparatorChooser;

  public TmmTable() {
    super();
    init();
  }

  public TmmTable(TableModel dm) {
    setModel(dm);
    init();
  }

  /**
   * Overridden to use TmmTableColumnModel as TableColumnModel.
   *
   * @see javax.swing.JTable#createDefaultColumnModel()
   */
  @Override
  protected TableColumnModel createDefaultColumnModel() {
    return new TmmTableColumnModel();
  }

  @Override
  public void addColumn(TableColumn aColumn) {
    // // disable grid in header
    // if (!(aColumn.getHeaderRenderer() instanceof BottomBorderHeaderRenderer)) {
    // aColumn.setHeaderRenderer(new BottomBorderHeaderRenderer());
    // }

    if (aColumn.getIdentifier() == null && getModel() instanceof TmmTableModel) {
      TmmTableModel tableModel = ((TmmTableModel) getModel());
      tableModel.setUpColumn(aColumn);
    }
    super.addColumn(aColumn);
  }

  private void init() {
    // setTableHeader(createTableHeader());
    getTableHeader().setReorderingAllowed(false);
    // getTableHeader().setOpaque(false);
    // setGridColor(TABLE_GRID_COLOR);
    // setIntercellSpacing(new Dimension(0, 0));
    // turn off grid painting as we'll handle this manually in order to paint grid lines over the entire viewport.
    setShowGrid(false);

    getColumnModel().addColumnModelListener(new TableColumnModelListener() {
      @Override
      public void columnAdded(TableColumnModelEvent e) {
        adjustColumnPreferredWidths(3);
      }

      @Override
      public void columnRemoved(TableColumnModelEvent e) {
        adjustColumnPreferredWidths(3);
      }

      @Override
      public void columnMoved(TableColumnModelEvent e) {
      }

      @Override
      public void columnMarginChanged(ChangeEvent e) {
      }

      @Override
      public void columnSelectionChanged(ListSelectionEvent e) {
      }
    });
  }

  public void installComparatorChooser(SortedList<?> sortedList) {
    tableComparatorChooser = TableComparatorChooser.install(this, sortedList, new MouseKeyboardSortingStrategy());
  }

  public TableComparatorChooser getTableComparatorChooser() {
    return tableComparatorChooser;
  }

  public List<String> getHiddenColumns() {
    List<String> hiddenColumns = new ArrayList<>();

    if (getColumnModel() instanceof TmmTableColumnModel) {
      List<TableColumn> cols = ((TmmTableColumnModel) getColumnModel()).getHiddenColumns();
      for (TableColumn col : cols) {
        if (col.getIdentifier() instanceof String && StringUtils.isNotBlank((String) col.getIdentifier())) {
          hiddenColumns.add((String) col.getIdentifier());
        }
      }
    }

    return hiddenColumns;
  }

  public void readHiddenColumns(List<String> hiddenColumns) {
    if (getColumnModel() instanceof TmmTableColumnModel) {
      ((TmmTableColumnModel) getColumnModel()).setHiddenColumns(hiddenColumns);
    }
  }

  public void setDefaultHiddenColumns() {
    if (getColumnModel() instanceof TmmTableColumnModel && getModel() instanceof TmmTableModel) {
      TmmTableModel tableModel = (TmmTableModel) getModel();
      TmmTableFormat tableFormat = (TmmTableFormat) tableModel.getTableFormat();

      List<String> hiddenColumns = new ArrayList<>();

      for (int i = 0; i < tableFormat.getColumnCount(); i++) {
        if (tableFormat.isColumnDefaultHidden(i)) {
          hiddenColumns.add(tableFormat.getColumnIdentifier(i));
        }
      }

      readHiddenColumns(hiddenColumns);
    }
  }

  /**
   * Set the preferred width of all columns according to its contents If a column is marked as non resizeable, the max-width is set
   *
   * @param margin
   *          the margin left and right
   */
  public void adjustColumnPreferredWidths(int margin) {
    // strategy - get max width for cells in header and column and
    // make that the preferred width
    TableColumnModel columnModel = getColumnModel();
    for (int col = 0; col < getColumnCount(); col++) {

      int maxWidth = 0;
      int minWidth = columnModel.getColumn(col).getMinWidth();

      // header
      TableCellRenderer rend = columnModel.getColumn(col).getHeaderRenderer();
      Object value = columnModel.getColumn(col).getHeaderValue();
      if (rend == null) {
        rend = getTableHeader().getDefaultRenderer();
      }
      Component comp = rend.getTableCellRendererComponent(this, value, false, false, -1, col);
      maxWidth = Math.max(comp.getPreferredSize().width + 2 * margin, maxWidth);

      // rows
      for (int row = 0; row < getRowCount(); row++) {
        rend = getCellRenderer(row, col);
        value = getValueAt(row, col);
        comp = rend.getTableCellRendererComponent(this, value, false, false, row, col);
        maxWidth = Math.max(comp.getPreferredSize().width + margin, maxWidth);
      }

      // do not set the max width below the min width
      if (maxWidth < minWidth) {
        maxWidth = minWidth;
      }

      TableColumn column = columnModel.getColumn(col);
      column.setPreferredWidth(maxWidth);
      if (!column.getResizable()) {
        column.setMinWidth(minWidth);
        column.setMaxWidth(maxWidth);
      }
    }
  }

  // @Override
  // public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
  // Component component = super.prepareRenderer(renderer, row, column);
  // // if the renderer is a JComponent and the given row isn't part of a
  // // selection, make the renderer non-opaque so that striped rows show through.
  // if (component instanceof JComponent) {
  // ((JComponent) component).setOpaque(getSelectionModel().isSelectedIndex(row));
  // }
  // return component;
  // }

  /**
   * Overridden to install special button into the upper right hand corner.
   *
   * @see javax.swing.JTable#configureEnclosingScrollPane()
   */
  @Override
  protected void configureEnclosingScrollPane() {
    super.configureEnclosingScrollPane();

    Container p = getParent();
    if (p instanceof JViewport) {
      Container parent = p.getParent();
      if (parent instanceof JScrollPane) {
        JScrollPane scrollPane = (JScrollPane) parent;
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS);

        // Make certain we are the viewPort's view and not, for
        // example, the rowHeaderView of the scrollPane -
        // an implementor of fixed columns might do this.
        JViewport viewport = scrollPane.getViewport();
        if (viewport == null || viewport.getView() != this) {
          return;
        }

        scrollPane.setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS);

        final JButton b = new JButton(IconManager.CONFIGURE) {
          @Override
          public void updateUI() {
            super.updateUI();
            setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("TableHeader.bottomSeparatorColor")));
          }
        };
        b.setContentAreaFilled(false);
        b.putClientProperty("flatButton", Boolean.TRUE);
        b.setToolTipText(BUNDLE.getString("Button.selectvisiblecolumns"));
        b.updateUI();

        b.addActionListener(evt -> TmmTableColumnSelectionPopup.showColumnSelectionPopup(b, TmmTable.this));
        b.addMouseListener(new MouseAdapter() {
          @Override
          public void mouseEntered(MouseEvent e) {
            scrollPane.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
          }

          @Override
          public void mouseExited(MouseEvent e) {
            scrollPane.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
          }

          @Override
          public void mouseClicked(MouseEvent me) {
            TmmTableColumnSelectionPopup.showColumnSelectionPopup(b, TmmTable.this);
          }
        });
        b.setFocusable(false);
        scrollPane.setCorner(UPPER_RIGHT_CORNER, b);
      }
    }
  }

  public void configureScrollPane(JScrollPane scrollPane) {
    // per default exclude the rightmost
    int[] columnsWithoutRightVerticalGrid = { getColumnCount() - 1 };
    configureScrollPane(scrollPane, columnsWithoutRightVerticalGrid);
  }

  public void configureScrollPane(JScrollPane scrollPane, int[] columnsWithoutRightVerticalGrid) {
    // if (!(scrollPane.getViewport() instanceof TmmViewport)) {
    // scrollPane.setViewport(new TmmViewport(this, columnsWithoutRightVerticalGrid));
    // scrollPane.getViewport().setView(this);
    // }
    // scrollPane.setBorder(null);
    // scrollPane.setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS);
  }

  protected static class IconHeaderRenderer extends DefaultTableCellRenderer {
    private static final long serialVersionUID = 7963585655106103415L;

    public IconHeaderRenderer() {
      setHorizontalAlignment(CENTER);
      setOpaque(true);
    }

    @Override
    public void updateUI() {
      super.updateUI();
      setBorder(BorderFactory.createEmptyBorder());
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused, int row, int column) {
      JTableHeader h = table != null ? table.getTableHeader() : null;

      if (h != null) {
        setEnabled(h.isEnabled());
        setComponentOrientation(h.getComponentOrientation());

        setForeground(h.getForeground());
        setBackground(h.getBackground());
        setFont(h.getFont());
      }
      else {
        /* Use sensible values instead of random leftover values from the last call */
        setEnabled(true);
        setComponentOrientation(ComponentOrientation.UNKNOWN);

        setForeground(UIManager.getColor("TableHeader.foreground"));
        setBackground(UIManager.getColor("TableHeader.background"));
        setFont(UIManager.getFont("TableHeader.font"));
      }

      if (value instanceof ImageIcon) {
        setIcon((ImageIcon) value);
        setText("");
      }
      else {
        setText((value == null) ? "" : value.toString());
        setIcon(null);
        setHorizontalAlignment(CENTER);
      }

      return this;
    }
  }

  /**
   * provide table cell tooltips via our table model
   * 
   * @param e
   *          the mouse event
   * @return the tooltip or null
   */
  public String getToolTipText(MouseEvent e) {
    if (!(getModel() instanceof TmmTableModel)) {
      return super.getToolTipText(e);
    }

    TmmTableModel tableModel = ((TmmTableModel) getModel());
    Point p = e.getPoint();
    int rowIndex = rowAtPoint(p);
    int colIndex = columnAtPoint(p);
    int realColumnIndex = convertColumnIndexToModel(colIndex);

    return tableModel.getTooltipAt(rowIndex, realColumnIndex);
  }

  @Override
  public Point getToolTipLocation(MouseEvent e) {
    // do not return a coordinate if the tooltip text ist empty
    if (StringUtils.isBlank(getToolTipText(e))) {
      return null;
    }

    Point p = e.getPoint();
    int rowIndex = rowAtPoint(p);
    int colIndex = columnAtPoint(p);
    Rectangle r = getCellRect(rowIndex, colIndex, false);

    Point point = new Point(r.x + 20, r.y + (int) (1.2 * r.height));
    return point;
  }
}
