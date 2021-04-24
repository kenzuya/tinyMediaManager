package org.tinymediamanager.ui.movies.actions;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.tasks.MovieARDetectorTask;
import org.tinymediamanager.core.tasks.ARDetectorTask;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.movies.MovieUIModule;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

public class MovieAspectRatioDetectAction extends TmmAction {

  private static final long           serialVersionUID = 2040242768614719459L;

  public MovieAspectRatioDetectAction() {
    putValue(NAME, TmmResourceBundle.getString("movie.ard"));
    putValue(LARGE_ICON_KEY, IconManager.SEARCH);
    putValue(SMALL_ICON, IconManager.SEARCH);
  }

  @Override
  protected void processAction(ActionEvent e) {
    final List<Movie> selectedMovies = new ArrayList<>(MovieUIModule.getInstance().getSelectionModel().getSelectedMovies());

    if (selectedMovies.isEmpty()) {
      JOptionPane.showMessageDialog(MainWindow.getInstance(), TmmResourceBundle.getString("tmm.nothingselected"));
      return;
    }

    ARDetectorTask task = new MovieARDetectorTask(selectedMovies);
    TmmTaskManager.getInstance().addUnnamedTask(task);
  }
}
