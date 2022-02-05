package org.tinymediamanager.ui.tvshows.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.tvshow.TvShowHelpers;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.tvshows.TvShowUIModule;

public class TvShowTrailerDownloadAction extends TmmAction {

  public TvShowTrailerDownloadAction() {
    putValue(NAME, TmmResourceBundle.getString("tvshow.downloadtrailer"));
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("tvshow.downloadtrailer"));
    putValue(SMALL_ICON, IconManager.DOWNLOAD);
    putValue(LARGE_ICON_KEY, IconManager.DOWNLOAD);
    putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK + InputEvent.ALT_DOWN_MASK));
  }

  @Override
  protected void processAction(ActionEvent e) {
    List<TvShow> selectedTvShows = new ArrayList<>(TvShowUIModule.getInstance().getSelectionModel().getSelectedTvShows());

    if (selectedTvShows.isEmpty()) {
      JOptionPane.showMessageDialog(MainWindow.getInstance(), TmmResourceBundle.getString("tmm.nothingselected"));
      return;
    }

    // first check if there is at least one movie containing a trailer mf
    boolean existingTrailer = false;
    for (TvShow tvShow : selectedTvShows) {
      if (!tvShow.getMediaFiles(MediaFileType.TRAILER).isEmpty()) {
        existingTrailer = true;
        break;
      }
    }

    // if there is any existing trailer found, show a message dialog
    boolean overwriteTrailer = false;
    if (existingTrailer) {
      Object[] options = { TmmResourceBundle.getString("Button.yes"), TmmResourceBundle.getString("Button.no") };
      int answer = JOptionPane.showOptionDialog(MainWindow.getFrame(), TmmResourceBundle.getString("movie.overwritetrailer"),
          TmmResourceBundle.getString("tvshow.downloadtrailer"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, null);
      if (answer == JOptionPane.YES_OPTION) {
        overwriteTrailer = true;
      }
    }

    // start tasks
    for (TvShow tvShow : selectedTvShows) {
      if (!tvShow.getMediaFiles(MediaFileType.TRAILER).isEmpty() && !overwriteTrailer) {
        continue;
      }
      if (tvShow.getTrailer().isEmpty()) {
        continue;
      }
      TvShowHelpers.downloadBestTrailer(tvShow);
    }
  }
}
