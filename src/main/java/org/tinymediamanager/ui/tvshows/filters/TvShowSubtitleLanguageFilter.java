package org.tinymediamanager.ui.tvshows.filters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.swing.*;

import org.tinymediamanager.core.Constants;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.ui.components.TmmLabel;

import static org.tinymediamanager.core.MediaFileType.SUBTITLE;
import static org.tinymediamanager.core.MediaFileType.VIDEO;

public class TvShowSubtitleLanguageFilter extends AbstractCheckComboBoxTvShowUIFilter<String>{

    private final TvShowList tvShowList = TvShowList.getInstance();

    public TvShowSubtitleLanguageFilter() {
        super();
        checkComboBox.enableFilter((s, s2) -> String.valueOf(s).startsWith(s2.toLowerCase(Locale.ROOT)));
        buildSubtitleLanguageArray();
        tvShowList.addPropertyChangeListener(Constants.SUBTITLE_LANGUAGES, evt -> SwingUtilities.invokeLater(this::buildSubtitleLanguageArray));

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
    protected boolean accept(TvShow tvShow, List<TvShowEpisode> episodes, boolean invert) {

        List<String> selectedItems = checkComboBox.getSelectedItems();

        for (TvShowEpisode episode : episodes) {
            List<MediaFile> mfs = episode.getMediaFiles(VIDEO,SUBTITLE);
            for (MediaFile mf : mfs) {
                if (invert ^ (selectedItems.isEmpty() && mf.getSubtitleLanguagesList().isEmpty())) {
                    return true;
                }

                for(String lang : mf.getSubtitleLanguagesList()) {
                    if (invert ^ selectedItems.contains(lang)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    protected JLabel createLabel() {
        return new TmmLabel(BUNDLE.getString("metatag.subtitlelanguage"));
    }

    @Override
    public String getId() {
        return "tvShowSubtitleLanguage";
    }

    private void buildSubtitleLanguageArray() {
        List<String> subtitles = new ArrayList<>(tvShowList.getSubtitleLanguagesInEpisodes());
        Collections.sort(subtitles);
        setValues(subtitles);
    }
}
