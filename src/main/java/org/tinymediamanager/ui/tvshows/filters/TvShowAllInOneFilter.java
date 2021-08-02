package org.tinymediamanager.ui.tvshows.filters;

import java.util.List;

import javax.swing.*;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.ui.components.TmmLabel;

public class TvShowAllInOneFilter extends AbstractTextTvShowUIFilter {

    @Override
    protected JLabel createLabel() {
        return new TmmLabel(TmmResourceBundle.getString("filter.universal"));
    }

    @Override
    public String getId() {
        return "tvShowAllInOne";
    }

    @Override
    protected boolean accept(TvShow tvShow, List<TvShowEpisode> episodes, boolean invert) {

        //TvShowCastFilter
        TvShowCastFilter tvShowCastFilter = new TvShowCastFilter();
        setFields(tvShowCastFilter);
        if (tvShowCastFilter.accept(tvShow,episodes,invert)) {
            return true;
        }

        //TvShowCountryFilter
        TvShowCountryFilter tvShowCountryFilter = new TvShowCountryFilter();
        setFields(tvShowCountryFilter);
        if (tvShowCountryFilter.accept(tvShow,episodes,invert)) {
            return true;
        }

        //TvShowFilenameFilter
        TvShowFilenameFilter tvShowFilenameFilter = new TvShowFilenameFilter();
        setFields(tvShowFilenameFilter);
        if (tvShowFilenameFilter.accept(tvShow,episodes,invert)) {
            return true;
        }

        //TvShowNoteFilter
        TvShowNoteFilter tvShowNoteFilter = new TvShowNoteFilter();
        setFields(tvShowNoteFilter);
        if (tvShowNoteFilter.accept(tvShow,episodes,invert)) {
            return true;
        }

        //TvShowStudioFilter
        TvShowStudioFilter tvShowStudioFilter = new TvShowStudioFilter();
        setFields(tvShowStudioFilter);
        if (tvShowStudioFilter.accept(tvShow,episodes,invert)) {
            return true;
        }

        return false;
    }


    private void setFields(AbstractTextTvShowUIFilter filter) {
        filter.textField = this.textField;
        filter.filterPattern = this.filterPattern;
        filter.normalizedFilterText = this.normalizedFilterText;
    }
}
