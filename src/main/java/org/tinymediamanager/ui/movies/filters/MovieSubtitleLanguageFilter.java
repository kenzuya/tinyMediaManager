package org.tinymediamanager.ui.movies.filters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.swing.*;

import org.tinymediamanager.core.Constants;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.movie.MovieList;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.ui.components.TmmLabel;

public class MovieSubtitleLanguageFilter extends AbstractCheckComboBoxMovieUIFilter<String> {

    private MovieList movieList = MovieList.getInstance();

    public MovieSubtitleLanguageFilter() {
        super();
        checkComboBox.enableFilter((s, s2) -> String.valueOf(s).startsWith(s2.toLowerCase(Locale.ROOT)));
        buildSubtitleLanguageArray();
        movieList.addPropertyChangeListener(Constants.SUBTITLE_LANGUAGES, evt -> SwingUtilities.invokeLater(this::buildSubtitleLanguageArray));
    }

    @Override
    protected String parseTypeToString(String type) throws Exception {
        return type;
    }

    @Override
    protected String parseStringToType(String string) throws Exception {
        return string;
    }

    @Override
    protected JLabel createLabel() {
        return new TmmLabel(BUNDLE.getString("metatag.subtitlelanguage"));
    }

    @Override
    public String getId() {
        return "movieSubtitleLanguage";
    }

    @Override
    public boolean accept(Movie movie) {

        List<String> selectedItems = checkComboBox.getSelectedItems();
        List<MediaFile> mediaFileList = movie.getMediaFiles(MediaFileType.VIDEO, MediaFileType.SUBTITLE);

        for(MediaFile mf : mediaFileList) {
            // check for explicit empty search
            if (selectedItems.isEmpty() && mf.getSubtitleLanguagesList().isEmpty()) {
                return true;
            }
            for (String lang : mf.getSubtitleLanguagesList()) {
                if (selectedItems.contains(lang)) {
                    return true;
                }
            }
        }

        return false;
    }

    public void buildSubtitleLanguageArray() {
        List<String> subtitleLanguages = new ArrayList<>(movieList.getSubtitleLanguagesInMovies());
        Collections.sort(subtitleLanguages);
        setValues(subtitleLanguages);
    }
}
