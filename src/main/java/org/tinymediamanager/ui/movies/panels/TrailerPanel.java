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
package org.tinymediamanager.ui.movies.panels;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeListener;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.DefaultListSelectionModel;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionListener;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.Globals;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.Message.MessageLevel;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.entities.MediaTrailer;
import org.tinymediamanager.core.movie.MovieHelpers;
import org.tinymediamanager.core.tvshow.TvShowHelpers;
import org.tinymediamanager.scraper.util.UrlUtil;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.TmmUILayoutStore;
import org.tinymediamanager.ui.components.table.TmmTable;
import org.tinymediamanager.ui.components.table.TmmTableFormat;
import org.tinymediamanager.ui.components.table.TmmTableModel;
import org.tinymediamanager.ui.movies.MovieSelectionModel;
import org.tinymediamanager.ui.tvshows.TvShowSelectionModel;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.swing.GlazedListsSwing;
import net.miginfocom.swing.MigLayout;

/**
 * The Class MovieTrailerPanel.
 *
 * @author Manuel Laggner
 */
public class TrailerPanel extends JPanel {
  private static final long           serialVersionUID = 2506465845096043845L;
  /**
   * @wbp.nls.resourceBundle messages
   */
  private static final ResourceBundle BUNDLE           = ResourceBundle.getBundle("messages");
  private static final Logger         LOGGER           = LoggerFactory.getLogger(TrailerPanel.class);

  private MovieSelectionModel         movieSelectionModel;
  private TvShowSelectionModel        tvShowSelectionModel;
  private TmmTable                    table;
  private EventList<MediaTrailer>     trailerEventList;

  /**
   * Instantiates a new movie details panel.
   *
   * @param model
   *          the model
   */
  public TrailerPanel(MovieSelectionModel model) {
    this.movieSelectionModel = model;

    createLayout();

    table.setName("movies.trailerTable");
    TmmUILayoutStore.getInstance().install(table);

    // install the propertychangelistener
    PropertyChangeListener propertyChangeListener = propertyChangeEvent -> {
      String property = propertyChangeEvent.getPropertyName();
      Object source = propertyChangeEvent.getSource();

      if (source.getClass() != MovieSelectionModel.class) {
        return;
      }

      // react on selection of a movie and change of a trailer
      if ("selectedMovie".equals(property) || "trailer".equals(property)) {
        // this does sometimes not work. simply wrap it
        try {
          trailerEventList.getReadWriteLock().writeLock().lock();
          trailerEventList.clear();
          trailerEventList.addAll(movieSelectionModel.getSelectedMovie().getTrailer());
        }
        catch (Exception ignored) {
          // ignored
        }
        finally {
          trailerEventList.getReadWriteLock().writeLock().unlock();
          table.adjustColumnPreferredWidths(7);
        }
      }
    };

    movieSelectionModel.addPropertyChangeListener(propertyChangeListener);

  }

  public TrailerPanel(TvShowSelectionModel model) {
    this.tvShowSelectionModel = model;

    createLayout();

    table.setName("movies.trailerTable");
    TmmUILayoutStore.getInstance().install(table);

    // install the propertychangelistener
    PropertyChangeListener propertyChangeListener = propertyChangeEvent -> {
      String property = propertyChangeEvent.getPropertyName();
      Object source = propertyChangeEvent.getSource();

      if (source.getClass() != TvShowSelectionModel.class) {
        return;
      }

      // react on selection of a movie and change of a trailer
      if ("selectedTvShow".equals(property) || "trailer".equals(property)) {
        // this does sometimes not work. simply wrap it
        try {
          trailerEventList.getReadWriteLock().writeLock().lock();
          trailerEventList.clear();
          trailerEventList.addAll(tvShowSelectionModel.getSelectedTvShow().getTrailer());
        }
        catch (Exception ignored) {
          // ignored
        }
        finally {
          trailerEventList.getReadWriteLock().writeLock().unlock();
          table.adjustColumnPreferredWidths(7);
        }
      }
    };

    tvShowSelectionModel.addPropertyChangeListener(propertyChangeListener);

  }

  private void createLayout() {
    trailerEventList = new ObservableElementList<>(GlazedLists.threadSafeList(new BasicEventList<>()), GlazedLists.beanConnector(MediaTrailer.class));
    setLayout(new MigLayout("", "[400lp,grow]", "[250lp,grow]"));
    table = new TmmTable(new TmmTableModel<>(GlazedListsSwing.swingThreadProxyList(trailerEventList), new TrailerTableFormat()));
    table.setSelectionModel(new NullSelectionModel());

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
      addColumn(col);

      /*
       * play
       */
      col = new Column("", "play", trailer -> IconManager.PLAY, ImageIcon.class);
      col.setColumnResizeable(false);
      addColumn(col);

      /*
       * nfo
       */
      col = new Column(BUNDLE.getString("metatag.nfo"), "nfo", MediaTrailer::getInNfo, Boolean.class);
      col.setColumnResizeable(false);
      addColumn(col);

      /*
       * name
       */
      col = new Column(BUNDLE.getString("metatag.name"), "name", MediaTrailer::getName, String.class);
      col.setColumnTooltip(MediaTrailer::getName);
      addColumn(col);

      /*
       * source
       */
      col = new Column(BUNDLE.getString("metatag.source"), "source", MediaTrailer::getProvider, String.class);
      col.setColumnTooltip(MediaTrailer::getProvider);
      col.setColumnResizeable(false);
      addColumn(col);

      /*
       * quality
       */
      col = new Column(BUNDLE.getString("metatag.quality"), "quality", MediaTrailer::getQuality, String.class);
      col.setColumnResizeable(false);
      addColumn(col);

      /*
       * format
       */
      col = new Column(BUNDLE.getString("metatag.format"), "format", trailer -> {
        String ext = UrlUtil.getExtension(trailer.getUrl()).toLowerCase(Locale.ROOT);
        if (!Globals.settings.getVideoFileType().contains("." + ext)) {
          // .php redirection scripts et all
          ext = "";
        }
        return ext;
      }, String.class);
      col.setColumnResizeable(false);
      addColumn(col);
    }
  }

  private class LinkListener implements MouseListener, MouseMotionListener {
    @Override
    public void mouseClicked(MouseEvent e) {
      JTable table = (JTable) e.getSource();
      int row = table.rowAtPoint(new Point(e.getX(), e.getY()));
      int col = table.columnAtPoint(new Point(e.getX(), e.getY()));

      // click on the download button
      if (col == 0) {
        row = table.convertRowIndexToModel(row);
        MediaTrailer trailer = trailerEventList.get(row);

        if (StringUtils.isNotBlank(trailer.getUrl()) && trailer.getUrl().toLowerCase(Locale.ROOT).startsWith("http")) {
          if (movieSelectionModel != null) {
            MovieHelpers.downloadTrailer(movieSelectionModel.getSelectedMovie(), trailer);
          }
          if (tvShowSelectionModel != null) {
            TvShowHelpers.downloadTrailer(tvShowSelectionModel.getSelectedTvShow(), trailer);
          }
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
      JTable table = (JTable) e.getSource();
      int col = table.columnAtPoint(new Point(e.getX(), e.getY()));
      if (col == 0 || col == 1) {
        table.setCursor(new Cursor(Cursor.HAND_CURSOR));
      }
    }

    @Override
    public void mouseExited(MouseEvent e) {
      JTable table = (JTable) e.getSource();
      int col = table.columnAtPoint(new Point(e.getX(), e.getY()));
      if (col != 0 && col != 1) {
        table.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
      }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
      JTable table = (JTable) e.getSource();
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
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent arg0) {
    }
  }

  private static class NullSelectionModel extends DefaultListSelectionModel {
    private static final long serialVersionUID = -1956483331520197616L;

    @Override
    public boolean isSelectionEmpty() {
      return true;
    }

    @Override
    public boolean isSelectedIndex(int index) {
      return false;
    }

    @Override
    public int getMinSelectionIndex() {
      return -1;
    }

    @Override
    public int getMaxSelectionIndex() {
      return -1;
    }

    @Override
    public int getLeadSelectionIndex() {
      return -1;
    }

    @Override
    public int getAnchorSelectionIndex() {
      return -1;
    }

    @Override
    public void setSelectionInterval(int index0, int index1) {
      // nothing to do
    }

    @Override
    public void setLeadSelectionIndex(int index) {
      // nothing to do
    }

    @Override
    public void setAnchorSelectionIndex(int index) {
      // nothing to do
    }

    @Override
    public void addSelectionInterval(int index0, int index1) {
      // nothing to do
    }

    @Override
    public void insertIndexInterval(int index, int length, boolean before) {
      // nothing to do
    }

    @Override
    public void clearSelection() {
      // nothing to do
    }

    @Override
    public void removeSelectionInterval(int index0, int index1) {
      // nothing to do
    }

    @Override
    public void removeIndexInterval(int index0, int index1) {
      // nothing to do
    }

    @Override
    public void setSelectionMode(int selectionMode) {
      // nothing to do
    }

    @Override
    public int getSelectionMode() {
      return SINGLE_SELECTION;
    }

    @Override
    public void addListSelectionListener(ListSelectionListener lsl) {
      // nothing to do
    }

    @Override
    public void removeListSelectionListener(ListSelectionListener lsl) {
      // nothing to do
    }

    @Override
    public void setValueIsAdjusting(boolean valueIsAdjusting) {
      // nothing to do
    }

    @Override
    public boolean getValueIsAdjusting() {
      return false;
    }
  }
}
