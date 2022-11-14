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
package org.tinymediamanager.ui.panels;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Comparator;
import java.util.Locale;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.TableCellRenderer;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.Message.MessageLevel;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaTrailer;
import org.tinymediamanager.license.License;
import org.tinymediamanager.scraper.util.UrlUtil;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.components.table.NullSelectionModel;
import org.tinymediamanager.ui.components.table.TmmTable;
import org.tinymediamanager.ui.components.table.TmmTableFormat;
import org.tinymediamanager.ui.components.table.TmmTableModel;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.swing.GlazedListsSwing;
import net.miginfocom.swing.MigLayout;

/**
 * The Class MovieTrailerPanel.
 *
 * @author Manuel Laggner
 */
public abstract class TrailerPanel extends JPanel {
  private static final Logger        LOGGER           = LoggerFactory.getLogger(TrailerPanel.class);

  protected TmmTable                 table;
  protected SortedList<MediaTrailer> trailerEventList;

  protected void createLayout() {
    trailerEventList = new SortedList<>(
        GlazedListsSwing.swingThreadProxyList(new ObservableElementList<>(new BasicEventList<>(), GlazedLists.beanConnector(MediaTrailer.class))));
    setLayout(new MigLayout("", "[400lp,grow]", "[250lp,grow]"));
    table = new TmmTable(new TmmTableModel<>(trailerEventList, new TrailerTableFormat())) {
      final boolean downloadEnabled = License.getInstance().isValidLicense();

      @Override
      public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        java.awt.Component comp = super.prepareRenderer(renderer, row, column);
        if (column == 0 && !downloadEnabled) {
          comp.setEnabled(false);
        }
        else {
          comp.setEnabled(true);
        }
        return comp;
      }
    };
    table.setSelectionModel(new NullSelectionModel());
    table.installComparatorChooser(trailerEventList);

    JScrollPane scrollPane = new JScrollPane();
    table.configureScrollPane(scrollPane);
    add(scrollPane, "cell 0 0,grow");
    scrollPane.setViewportView(table);

    LinkListener linkListener = new LinkListener();
    table.addMouseListener(linkListener);
    table.addMouseMotionListener(linkListener);
  }

  private static class TrailerTableFormat extends TmmTableFormat<MediaTrailer> {
    public TrailerTableFormat() {
      Comparator<String> stringComparator = new StringComparator();
      Comparator<Boolean> booleanComparator = new BooleanComparator();
      Comparator<ImageIcon> imageComparator = new ImageComparator();

      /*
       * download
       */
      Column col = new Column("", "download", trailer -> {
        if (StringUtils.isNotBlank(trailer.getUrl()) && trailer.getUrl().toLowerCase(Locale.ROOT).startsWith("http")) {
          return IconManager.DOWNLOAD;
        }
        return null;
      }, ImageIcon.class);
      col.setColumnResizeable(false);
      col.setColumnComparator(imageComparator);
      addColumn(col);

      /*
       * play
       */
      col = new Column("", "play", trailer -> IconManager.PLAY, ImageIcon.class);
      col.setColumnResizeable(false);
      col.setColumnComparator(imageComparator);
      addColumn(col);

      /*
       * nfo
       */
      col = new Column(TmmResourceBundle.getString("metatag.nfo"), "nfo", MediaTrailer::getInNfo, Boolean.class);
      col.setColumnResizeable(false);
      col.setColumnComparator(booleanComparator);
      addColumn(col);

      /*
       * name
       */
      col = new Column(TmmResourceBundle.getString("metatag.name"), "name", MediaTrailer::getName, String.class);
      col.setColumnTooltip(MediaTrailer::getName);
      col.setColumnComparator(stringComparator);
      addColumn(col);

      /*
       * source
       */
      col = new Column(TmmResourceBundle.getString("metatag.source"), "source", MediaTrailer::getProvider, String.class);
      col.setColumnTooltip(MediaTrailer::getProvider);
      col.setColumnComparator(stringComparator);
      col.setColumnResizeable(false);
      addColumn(col);

      /*
       * quality
       */
      col = new Column(TmmResourceBundle.getString("metatag.quality"), "quality", MediaTrailer::getQuality, String.class);
      col.setColumnResizeable(false);
      col.setColumnComparator(stringComparator);
      addColumn(col);

      /*
       * format
       */
      col = new Column(TmmResourceBundle.getString("metatag.format"), "format", trailer -> {
        String ext = UrlUtil.getExtension(trailer.getUrl()).toLowerCase(Locale.ROOT);
        if (!Settings.getInstance().getVideoFileType().contains("." + ext)) {
          // .php redirection scripts et all
          ext = "";
        }
        return ext;
      }, String.class);
      col.setColumnComparator(stringComparator);
      col.setColumnResizeable(false);
      addColumn(col);
    }
  }

  protected abstract void downloadTrailer(MediaTrailer trailer);

  private class LinkListener implements MouseListener, MouseMotionListener {
    @Override
    public void mouseClicked(MouseEvent e) {
      int row = table.rowAtPoint(new Point(e.getX(), e.getY()));
      int col = table.columnAtPoint(new Point(e.getX(), e.getY()));

      // click on the download button
      if (col == 0 && License.getInstance().isValidLicense()) {
        row = table.convertRowIndexToModel(row);
        MediaTrailer trailer = trailerEventList.get(row);

        if (StringUtils.isNotBlank(trailer.getUrl()) && trailer.getUrl().toLowerCase(Locale.ROOT).startsWith("http")) {
          downloadTrailer(trailer);
        }
      }

      // click on the play button
      if (col == 1) {
        // try to open the browser
        row = table.convertRowIndexToModel(row);
        MediaTrailer trailer = trailerEventList.get(row);
        String url = trailer.getUrl();
        try {
          TmmUIHelper.browseUrl(url);
        }
        catch (Exception ex) {
          LOGGER.error(ex.getMessage());
          MessageManager.instance
              .pushMessage(new Message(MessageLevel.ERROR, url, "message.erroropenurl", new String[] { ":", ex.getLocalizedMessage() }));
        }
      }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
      int col = table.columnAtPoint(new Point(e.getX(), e.getY()));
      if (col == 0 || col == 1) {
        table.setCursor(new Cursor(Cursor.HAND_CURSOR));
      }
    }

    @Override
    public void mouseExited(MouseEvent e) {
      int col = table.columnAtPoint(new Point(e.getX(), e.getY()));
      if (col != 0 && col != 1) {
        table.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
      }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
      int col = table.columnAtPoint(new Point(e.getX(), e.getY()));
      if (col != 0 && col != 1 && table.getCursor().getType() == Cursor.HAND_CURSOR) {
        table.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
      }
      if ((col == 0 || col == 1) && table.getCursor().getType() == Cursor.DEFAULT_CURSOR) {
        table.setCursor(new Cursor(Cursor.HAND_CURSOR));
      }
    }

    @Override
    public void mousePressed(MouseEvent e) {
      // do nothing
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      // do nothing
    }

    @Override
    public void mouseDragged(MouseEvent arg0) {
      // do nothing
    }
  }
}
