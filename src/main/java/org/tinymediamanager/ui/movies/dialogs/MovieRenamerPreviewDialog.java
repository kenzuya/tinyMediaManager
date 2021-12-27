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
package org.tinymediamanager.ui.movies.dialogs;

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.tinymediamanager.core.AbstractModelObject;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.movie.MovieComparator;
import org.tinymediamanager.core.movie.MovieRenamerPreview;
import org.tinymediamanager.core.movie.MovieRenamerPreviewContainer;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.tasks.MovieRenameTask;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.threading.TmmThreadPool;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.table.TmmTable;
import org.tinymediamanager.ui.components.table.TmmTableFormat;
import org.tinymediamanager.ui.components.table.TmmTableModel;
import org.tinymediamanager.ui.dialogs.TmmDialog;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import ca.odell.glazedlists.swing.GlazedListsSwing;
import net.miginfocom.swing.MigLayout;

/**
 * The class MovieRenamerPreviewDialog. generate a preview which movies have to be renamed.
 * 
 * @author Manuel Laggner
 */
public class MovieRenamerPreviewDialog extends TmmDialog {
  private static final long                       serialVersionUID = -8162631708278089277L;

  private EventList<MovieRenamerPreviewContainer> results;
  private ResultSelectionModel                    resultSelectionModel;
  private EventList<MediaFileContainer>           oldMediaFileEventList;
  private EventList<MediaFileContainer>           newMediaFileEventList;

  /** UI components */
  private TmmTable                                tableMovies;
  private JLabel                                  lblTitle;
  private JLabel                                  lblDatasource;
  private JLabel                                  lblFolderOld;
  private JLabel                                  lblFolderNew;

  public MovieRenamerPreviewDialog(final List<Movie> selectedMovies) {
    super(TmmResourceBundle.getString("movie.renamerpreview"), "movieRenamerPreview");

    oldMediaFileEventList = GlazedLists.eventList(new ArrayList<>());
    newMediaFileEventList = GlazedLists.eventList(new ArrayList<>());

    results = GlazedListsSwing.swingThreadProxyList(GlazedLists.threadSafeList(new BasicEventList<>()));
    {
      JPanel panelContent = new JPanel();
      getContentPane().add(panelContent, BorderLayout.CENTER);
      panelContent.setLayout(new MigLayout("", "[950lp,grow]", "[600lp,grow]"));
      {
        JSplitPane splitPane = new JSplitPane();
        splitPane.setResizeWeight(0.4);
        panelContent.add(splitPane, "cell 0 0,grow");
        {
          TmmTableModel<MovieRenamerPreviewContainer> movieTableModel = new TmmTableModel<>(GlazedListsSwing.swingThreadProxyList(results),
              new ResultTableFormat());
          tableMovies = new TmmTable(movieTableModel);

          DefaultEventSelectionModel<MovieRenamerPreviewContainer> tableSelectionModel = new DefaultEventSelectionModel<>(results);
          resultSelectionModel = new ResultSelectionModel();
          tableSelectionModel.addListSelectionListener(resultSelectionModel);
          resultSelectionModel.selectedResults = tableSelectionModel.getSelected();
          tableMovies.setSelectionModel(tableSelectionModel);

          movieTableModel.addTableModelListener(arg0 -> {
            // select first movie if nothing is selected
            ListSelectionModel selectionModel = tableMovies.getSelectionModel();
            if (selectionModel.isSelectionEmpty() && movieTableModel.getRowCount() > 0) {
              selectionModel.setSelectionInterval(0, 0);
            }
            if (selectionModel.isSelectionEmpty() && movieTableModel.getRowCount() == 0) {
              resultSelectionModel.setSelectedResult(null);
            }
          });

          JScrollPane scrollPaneMovies = new JScrollPane();
          tableMovies.configureScrollPane(scrollPaneMovies);
          splitPane.setLeftComponent(scrollPaneMovies);
        }
        {
          JPanel panelDetails = new JPanel();
          splitPane.setRightComponent(panelDetails);
          panelDetails.setLayout(new MigLayout("", "[][][grow]", "[][][][][][][][grow]"));
          {
            lblTitle = new JLabel("");
            TmmFontHelper.changeFont(lblTitle, 1.33, Font.BOLD);
            panelDetails.add(lblTitle, "cell 0 0 3 1,growx");
          }
          {
            JLabel lblDatasourceT = new TmmLabel(TmmResourceBundle.getString("metatag.datasource"));
            panelDetails.add(lblDatasourceT, "cell 0 2");

            lblDatasource = new JLabel("");
            panelDetails.add(lblDatasource, "cell 2 2,growx,aligny center");
          }
          {
            JLabel lblFolderOldT = new TmmLabel(TmmResourceBundle.getString("renamer.oldfolder"));
            panelDetails.add(lblFolderOldT, "cell 0 4");

            lblFolderOld = new JLabel("");
            panelDetails.add(lblFolderOld, "cell 2 4,growx,aligny center");
          }
          {
            JLabel lblFolderNewT = new TmmLabel(TmmResourceBundle.getString("renamer.newfolder"));
            panelDetails.add(lblFolderNewT, "cell 0 5");

            lblFolderNew = new JLabel("");
            panelDetails.add(lblFolderNew, "cell 2 5,growx,aligny center");
          }
          {
            JPanel panelMediaFiles = new JPanel();
            panelDetails.add(panelMediaFiles, "cell 0 7 3 1,grow");
            panelMediaFiles.setLayout(new MigLayout("", "[grow][grow]", "[15px][grow]"));
            {
              JLabel lblOldfilesT = new TmmLabel(TmmResourceBundle.getString("renamer.oldfiles"));
              panelMediaFiles.add(lblOldfilesT, "cell 0 0,alignx center");

              JLabel lblNewfilesT = new TmmLabel(TmmResourceBundle.getString("renamer.newfiles"));
              panelMediaFiles.add(lblNewfilesT, "cell 1 0,alignx center");
            }
            {
              TmmTable tableMediaFilesOld = new TmmTable(
                  new TmmTableModel<>(GlazedListsSwing.swingThreadProxyList(oldMediaFileEventList), new MediaFileTableFormat()));
              JScrollPane scrollPaneMediaFilesOld = new JScrollPane();
              tableMediaFilesOld.configureScrollPane(scrollPaneMediaFilesOld);
              panelMediaFiles.add(scrollPaneMediaFilesOld, "cell 0 1,grow");
              tableMediaFilesOld.getColumnModel().getColumn(0).setMaxWidth(40);
            }
            {
              TmmTable tableMediaFilesNew = new TmmTable(
                  new TmmTableModel<>(GlazedListsSwing.swingThreadProxyList(newMediaFileEventList), new MediaFileTableFormat()));
              JScrollPane scrollPaneMediaFilesNew = new JScrollPane(tableMediaFilesNew);
              tableMediaFilesNew.configureScrollPane(scrollPaneMediaFilesNew);
              panelMediaFiles.add(scrollPaneMediaFilesNew, "cell 1 1,grow");
              tableMediaFilesNew.getColumnModel().getColumn(0).setMaxWidth(40);
            }
          }
        }
      }
    }
    {
      JButton btnRename = new JButton(TmmResourceBundle.getString("Button.rename"));
      btnRename.setToolTipText(TmmResourceBundle.getString("movie.rename"));
      btnRename.addActionListener(arg0 -> {
        List<Movie> selectedMovies1 = new ArrayList<>();
        List<MovieRenamerPreviewContainer> selectedResults = new ArrayList<>(resultSelectionModel.selectedResults);
        for (MovieRenamerPreviewContainer result : selectedResults) {
          selectedMovies1.add(result.getMovie());
        }

        // rename
        TmmThreadPool renameTask = new MovieRenameTask(selectedMovies1);
        TmmTaskManager.getInstance().addMainTask(renameTask);
        results.removeAll(selectedResults);
      });
      addButton(btnRename);

      JButton btnClose = new JButton(TmmResourceBundle.getString("Button.close"));
      btnClose.addActionListener(arg0 -> setVisible(false));
      addDefaultButton(btnClose);
    }

    // start calculation of the preview
    MoviePreviewWorker worker = new MoviePreviewWorker(selectedMovies);
    worker.execute();
  }

  /**********************************************************************
   * helper classes
   *********************************************************************/
  private static class ResultTableFormat extends TmmTableFormat<MovieRenamerPreviewContainer> {

    public ResultTableFormat() {
      /*
       * movie title
       */
      Column col = new Column(TmmResourceBundle.getString("metatag.movie"), "title", container -> container.getMovie().getTitleSortable(),
          String.class);
      col.setColumnTooltip(container -> container.getMovie().getTitleSortable());
      addColumn(col);
    }
  }

  private class MediaFileTableFormat extends TmmTableFormat<MediaFileContainer> {
    public MediaFileTableFormat() {
      /*
       * indicator
       */
      Column col = new Column("", "indicator", container -> container.icon, ImageIcon.class);
      addColumn(col);

      /*
       * filename
       */
      col = new Column(TmmResourceBundle.getString("metatag.filename"), "filename", container -> container.mediaFile.getFilename(), String.class);
      col.setColumnTooltip(container -> container.mediaFile.getFilename());
      addColumn(col);
    }
  }

  private class MoviePreviewWorker extends SwingWorker<Void, Void> {
    private final List<Movie> moviesToProcess;

    private MoviePreviewWorker(List<Movie> movies) {
      this.moviesToProcess = new ArrayList<>(movies);
    }

    @Override
    protected Void doInBackground() {
      // sort movies
      moviesToProcess.sort(new MovieComparator());
      // rename them
      for (Movie movie : moviesToProcess) {
        MovieRenamerPreviewContainer container = MovieRenamerPreview.renameMovie(movie);
        if (container.isNeedsRename()) {
          results.add(container);
        }
      }

      SwingUtilities.invokeLater(() -> {
        if (results.isEmpty()) { // check has to be in here, since it needs some time to propagate
          JOptionPane.showMessageDialog(MovieRenamerPreviewDialog.this, TmmResourceBundle.getString("movie.renamerpreview.nothingtorename"));
        }
      });

      return null;
    }
  }

  private class ResultSelectionModel extends AbstractModelObject implements ListSelectionListener {
    private final MovieRenamerPreviewContainer emptyResult;

    private MovieRenamerPreviewContainer       selectedResult;
    private List<MovieRenamerPreviewContainer> selectedResults;

    ResultSelectionModel() {
      emptyResult = new MovieRenamerPreviewContainer(new Movie());
    }

    synchronized void setSelectedResult(MovieRenamerPreviewContainer newValue) {
      if (newValue == null) {
        selectedResult = emptyResult;
      }
      else {
        selectedResult = newValue;
      }

      lblTitle.setText(selectedResult.getMovie().getTitleSortable());
      lblDatasource.setText(selectedResult.getMovie().getDataSource());

      // the empty result does not have any valid Path
      if (selectedResult != emptyResult) {
        lblFolderOld.setText(selectedResult.getOldPath().toString());
        lblFolderNew.setText(selectedResult.getNewPath().toString());
      }
      else {
        lblFolderOld.setText("");
        lblFolderNew.setText("");
      }

      // set Mfs
      try {
        oldMediaFileEventList.getReadWriteLock().writeLock().lock();
        oldMediaFileEventList.clear();
        for (MediaFile mf : selectedResult.getOldMediaFiles()) {
          boolean found = false;
          MediaFileContainer container = new MediaFileContainer();
          container.mediaFile = mf;

          for (MediaFile mf2 : selectedResult.getNewMediaFiles()) {
            if (mf2 != null && mf.getFilename().equals(mf2.getFilename())) {
              found = true;
              break;
            }
          }

          if (!found) {
            container.icon = IconManager.REMOVE;
          }
          oldMediaFileEventList.add(container);
        }

        newMediaFileEventList.getReadWriteLock().writeLock().lock();
        newMediaFileEventList.clear();
        for (MediaFile mf : selectedResult.getNewMediaFiles()) {
          boolean found = false;
          MediaFileContainer container = new MediaFileContainer();
          container.mediaFile = mf;

          for (MediaFile mf2 : selectedResult.getOldMediaFiles()) {
            if (mf.getFilename().equals(mf2.getFilename())) {
              found = true;
              break;
            }
          }

          if (!found) {
            container.icon = IconManager.ADD;
          }
          newMediaFileEventList.add(container);
        }
      }
      catch (Exception ignored) {
      }
      finally {
        oldMediaFileEventList.getReadWriteLock().writeLock().unlock();
        newMediaFileEventList.getReadWriteLock().writeLock().unlock();
      }
    }

    @Override
    public void valueChanged(ListSelectionEvent arg0) {
      if (arg0.getValueIsAdjusting()) {
        return;
      }

      // display first selected result
      if (!selectedResults.isEmpty() && selectedResult != selectedResults.get(0)) {
        setSelectedResult(selectedResults.get(0));
      }

      // display empty result
      if (selectedResults.isEmpty()) {
        setSelectedResult(emptyResult);
      }
    }
  }

  private static class MediaFileContainer {
    ImageIcon icon = null;
    MediaFile mediaFile;
  }
}
