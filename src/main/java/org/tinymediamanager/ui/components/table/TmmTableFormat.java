package org.tinymediamanager.ui.components.table;

import java.awt.Canvas;
import java.awt.Font;
import java.awt.FontMetrics;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.Globals;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.scraper.entities.Certification;
import org.tinymediamanager.ui.IconManager;

import ca.odell.glazedlists.gui.AdvancedTableFormat;

/**
 * The abstract TmmTableFormat is a convenience wrapper for the @see com.glazedlists.AdvancedTableFormat
 *
 * @author Manuel Laggner
 */
public abstract class TmmTableFormat<E> implements AdvancedTableFormat<E> {

  protected List<Column> columns = new ArrayList<>();

  public TmmTableFormat() {
  }

  protected FontMetrics getFontMetrics() {
    Font defaultFont = UIManager.getFont("Table.font");
    if (defaultFont == null) {
      defaultFont = UIManager.getFont("Label.font");
    }
    Canvas canvas = new Canvas();
    return canvas.getFontMetrics(defaultFont);
  }

  protected void addColumn(Column column) {
    columns.add(column);
  }

  @Override
  public Class getColumnClass(int i) {
    return columns.get(i).columnClass;
  }

  @Override
  public Comparator getColumnComparator(int i) {
    return columns.get(i).columnComparator;
  }

  @Override
  public int getColumnCount() {
    return columns.size();
  }

  @Override
  public String getColumnName(int i) {
    return columns.get(i).columnTitle;
  }

  @Override
  public Object getColumnValue(E e, int i) {
    return columns.get(i).columnValue.apply(e);
  }

  public String getColumnTooltip(E e, int i) {
    if (columns.get(i).columnTooltip != null) {
      return columns.get(i).columnTooltip.apply(e);
    }
    return null;
  }

  public String getColumnIdentifier(int i) {
    return columns.get(i).columnIdentifier;
  }

  public TableCellRenderer getCellRenderer(int i) {
    return columns.get(i).cellRenderer;
  }

  public ImageIcon getHeaderIcon(int i) {
    return columns.get(i).headerIcon;
  }

  public boolean getColumnResizeable(int i) {
    return columns.get(i).columnResizeable;
  }

  public int getMinWidth(int i) {
    return columns.get(i).minWidth;
  }

  public int getMaxWidth(int i) {
    return columns.get(i).maxWidth;
  }

  protected class Column {
    private String              columnTitle;
    private String              columnIdentifier;
    private Function<E, ?>      columnValue;
    private Function<E, String> columnTooltip    = null;
    private Class               columnClass;
    private Comparator<?>       columnComparator = null;
    private TableCellRenderer   cellRenderer     = null;
    private ImageIcon           headerIcon       = null;
    private boolean             columnResizeable = true;
    private int                 minWidth         = 0;
    private int                 maxWidth         = 0;

    public Column(String title, String identifier, Function<E, ?> value, Class clazz) {
      columnTitle = title;
      columnIdentifier = identifier;
      columnValue = value;
      columnClass = clazz;
      minWidth = (int) (Globals.settings.getFontSize() * 2.3);
    }

    public void setColumnComparator(Comparator comparator) {
      columnComparator = comparator;
    }

    public void setCellRenderer(TableCellRenderer renderer) {
      cellRenderer = renderer;
    }

    public void setHeaderIcon(ImageIcon icon) {
      headerIcon = icon;
    }

    public void setColumnResizeable(boolean resizeable) {
      columnResizeable = resizeable;
    }

    public void setMinWidth(int minWidth) {
      this.minWidth = minWidth;
    }

    public void setMaxWidth(int maxWidth) {
      this.maxWidth = maxWidth;
    }

    public void setColumnTooltip(Function<E, String> tooltip) {
      this.columnTooltip = tooltip;
    }
  }

  protected ImageIcon getCheckIcon(boolean bool) {
    if (bool) {
      return IconManager.TABLE_OK;
    }
    return IconManager.TABLE_NOT_OK;
  }

  public class StringComparator implements Comparator<String> {
    @Override
    public int compare(String arg0, String arg1) {
      if (StringUtils.isBlank(arg0)) {
        return -1;
      }
      if (StringUtils.isBlank(arg1)) {
        return 1;
      }
      return arg0.toLowerCase(Locale.ROOT).compareTo(arg1.toLowerCase(Locale.ROOT));
    }
  }

  public class PathComparator implements Comparator<Path> {
    @Override
    public int compare(Path arg0, Path arg1) {
      if (arg0 == null) {
        return -1;
      }
      if (arg1 == null) {
        return 1;
      }
      return arg0.toAbsolutePath().compareTo(arg1.toAbsolutePath());
    }
  }

  public class IntegerComparator implements Comparator<Integer> {
    @Override
    public int compare(Integer arg0, Integer arg1) {
      if (arg0 == null) {
        return -1;
      }
      if (arg1 == null) {
        return 1;
      }
      return arg0 - arg1;
    }
  }

  public class FloatComparator implements Comparator<Float> {
    @Override
    public int compare(Float arg0, Float arg1) {
      return arg0.compareTo(arg1);
    }
  }

  public class ImageComparator implements Comparator<ImageIcon> {
    @Override
    public int compare(ImageIcon arg0, ImageIcon arg1) {
      if (arg0 == arg1) {
        return 0;
      }
      if (arg0 == IconManager.TABLE_OK) {
        return 1;
      }
      return -1;
    }
  }

  public class DateComparator implements Comparator<Date> {
    @Override
    public int compare(Date arg0, Date arg1) {
      return arg0.compareTo(arg1);
    }
  }

  public class VideoFormatComparator implements Comparator<String> {
    @Override
    public int compare(String arg0, String arg1) {
      return Integer.compare(MediaFile.VIDEO_FORMATS.indexOf(arg0), MediaFile.VIDEO_FORMATS.indexOf(arg1));
    }
  }

  public class FileSizeComparator implements Comparator<String> {
    Pattern pattern = Pattern.compile("(.*) (.*)");

    @Override
    public int compare(String arg0, String arg1) {
      long size0 = parseSize(arg0);
      long size1 = parseSize(arg1);

      return Long.compare(size0, size1);
    }

    private long parseSize(String sizeAsString) {
      long size = 0;

      Matcher matcher = pattern.matcher(sizeAsString);
      if (matcher.find()) {
        try {
          float value = Float.parseFloat(matcher.group(1).replace(',','.'));
          String unit = matcher.group(2);
          if ('G' == unit.charAt(0)) {
            size = (long) (value * 1024 * 1024 * 1024);
          }
          else if ('M' == unit.charAt(0)) {
            size = (long) (value * 1024 * 1024);
          }
          else if ('K' == unit.charAt(0) || 'k' == unit.charAt(0)) {
            size = (long) (value * 1024);
          }
          else {
            size = (long) value;
          }
        }
        catch (Exception ignored) {
          
        }
      }

      return size;
    }
  }

  public class CertificationComparator implements Comparator<Certification> {
    @Override
    public int compare(Certification arg0, Certification arg1) {
      if (arg0 == null) {
        return -1;
      }
      if (arg1 == null) {
        return 1;
      }
      return arg0.toString().compareTo(arg1.toString());
    }
  }
}
