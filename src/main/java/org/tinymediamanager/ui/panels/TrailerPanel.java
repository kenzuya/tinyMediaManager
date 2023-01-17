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

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.tinymediamanager.core.entities.MediaTrailer;
import org.tinymediamanager.ui.components.MediaTrailerTable;
import org.tinymediamanager.ui.components.table.NullSelectionModel;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.swing.GlazedListsSwing;
import net.miginfocom.swing.MigLayout;

/**
 * The class {@link TrailerPanel} is used to display trailers
 *
 * @author Manuel Laggner
 */
public abstract class TrailerPanel extends JPanel {
  protected MediaTrailerTable        table;
  protected SortedList<MediaTrailer> trailerEventList;

  protected void createLayout() {
    trailerEventList = new SortedList<>(
        GlazedListsSwing.swingThreadProxyList(new ObservableElementList<>(new BasicEventList<>(), GlazedLists.beanConnector(MediaTrailer.class))));
    setLayout(new MigLayout("", "[400lp,grow]", "[250lp,grow]"));
    table = new MediaTrailerTable(trailerEventList, false) {
      @Override
      protected void downloadTrailer(MediaTrailer mediaTrailer) {
        TrailerPanel.this.downloadTrailer(mediaTrailer);
      }
    };
    table.setSelectionModel(new NullSelectionModel());
    table.installComparatorChooser(trailerEventList);

    JScrollPane scrollPane = new JScrollPane();
    table.configureScrollPane(scrollPane);
    add(scrollPane, "cell 0 0,grow");
    scrollPane.setViewportView(table);
  }

  protected abstract void downloadTrailer(MediaTrailer trailer);

  protected abstract String refreshUrlFromId(MediaTrailer trailer);

  private class LinkListener implements MouseListener, MouseMotionListener {
    @Override
    public void mouseClicked(MouseEvent e) {
      int row = table.rowAtPoint(new Point(e.getX(), e.getY()));
      int col = table.columnAtPoint(new Point(e.getX(), e.getY()));

      // click on the download button
      if (col == 0 && License.getInstance().isValidLicense()) {
        row = table.convertRowIndexToModel(row);
        MediaTrailer trailer = trailerEventList.get(row);

        if ((StringUtils.isNotBlank(trailer.getUrl()) && trailer.getUrl().toLowerCase(Locale.ROOT).startsWith("http"))
            || !trailer.getId().isEmpty()) {
          downloadTrailer(trailer);
        }
      }

      // click on the play button
      if (col == 1) {
        // try to open the browser
        row = table.convertRowIndexToModel(row);
        MediaTrailer trailer = trailerEventList.get(row);
        String url = trailer.getUrl();
        if (url.isEmpty()) {
          url = refreshUrlFromId(trailer);
        }
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
