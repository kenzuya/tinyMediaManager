package org.tinymediamanager.ui.movies.actions;

import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.tasks.MovieCleanUpTask;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.UTF8Control;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.movies.MovieUIModule;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class MovieCleanUpFilesAction extends TmmAction {

    private static final long           serialVersionUID = -2029243504238273721L;
    private static final ResourceBundle BUNDLE           = ResourceBundle.getBundle("messages", new UTF8Control()); //$NON-NLS-1$
    List<File> fileArray = new ArrayList<>();

    public MovieCleanUpFilesAction() {

        putValue(NAME, BUNDLE.getString("movie.cleanupfiles")); //$NON-NLS-1$
        putValue(SHORT_DESCRIPTION, BUNDLE.getString("movie.cleanupfiles.desc")); //$NON-NLS-1$
        putValue(SMALL_ICON, IconManager.DELETE);
        putValue(LARGE_ICON_KEY, IconManager.DELETE);

    }

    @Override
    protected void processAction(ActionEvent e) {

        List<Movie> selectedMovies = new ArrayList<>(MovieUIModule.getInstance().getSelectionModel().getSelectedMovies());

        if (selectedMovies.isEmpty()) {
            JOptionPane.showMessageDialog(MainWindow.getActiveInstance(), BUNDLE.getString("tmm.nothingselected")); //$NON-NLS-1$
            return;
        }

        //Add only files with these extension to the FileArray
        for (Movie movie : selectedMovies) {
            fileArray.addAll(getUnknownFiles(movie.getPath(),"*.{txt,url,html}"));
        }

        MovieCleanUpTask cleanUpTask = new MovieCleanUpTask(fileArray);
        cleanUpTask.start();

    }

    /**
   * Method to get a List of files with the following extensions
   * @param path Path where the files are located
   * @param extensions Extension ( *.txt ) or (*.{txt,html,url}
   * @return List of Files
   */
    private List<File> getUnknownFiles(String path, String extensions) {
        Path dir = FileSystems.getDefault().getPath(path);
        List<File> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, extensions)) {
            for (Path entry : stream) {
                files.add(entry.toFile());
            }
            return files;
        } catch (IOException x) {
            throw new RuntimeException(String.format("error reading folder %s: %s", dir, x.getMessage()), x);
        }
    }
}
