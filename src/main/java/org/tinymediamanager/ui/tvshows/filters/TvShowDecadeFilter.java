package org.tinymediamanager.ui.tvshows.filters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.tinymediamanager.core.Constants;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.ui.components.TmmLabel;

public class TvShowDecadeFilter extends AbstractCheckComboBoxTvShowUIFilter<String> {
    private final TvShowList tvShowList = TvShowList.getInstance();

    public TvShowDecadeFilter() {
        super();
        checkComboBox.enableFilter((s, s2) -> String.valueOf(s).startsWith(s2.toLowerCase(Locale.ROOT)));
        buildYearArray();
        tvShowList.addPropertyChangeListener(Constants.YEAR, evt -> SwingUtilities.invokeLater(this::buildYearArray));
    }


    @Override
    protected JLabel createLabel() {
        return new TmmLabel(TmmResourceBundle.getString("metatag.decade"));
    }

    @Override
    public String getId() {
        return "tvShowDecades";
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
        return invert ^ selectedItems.contains(tvShow.getDecadeShort());
    }

    private void buildYearArray() {
        Set<String> decadesSet = new HashSet<>();
        tvShowList.getTvShows().forEach(tvShow -> decadesSet.add(tvShow.getDecadeShort()));
        List<String> decades = new ArrayList<>(ListUtils.asSortedList(decadesSet));
        Collections.sort(decades);
        setValues(decades);
    }
}
