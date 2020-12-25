package org.tinymediamanager.ui.tvshows.filters;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.ui.components.TmmLabel;

import javax.swing.JComponent;
import javax.swing.JLabel;
import java.util.List;

public class TvShowUncategorizedEpisodesFilter extends AbstractTvShowUIFilter {

  @Override
  protected boolean accept(TvShow tvShow, List<TvShowEpisode> episodes, boolean invert) {

    for (TvShowEpisode episode : episodes) {
      if (episode.isDummy()) {
        continue;
      }

      if (invert ^ episode.isUncategorized()) {
        return true;
      }
    }

    return false;
  }

  @Override
  protected JLabel createLabel() {
    return new TmmLabel(TmmResourceBundle.getString("tvshow.uncategorized"));
  }

  @Override
  protected JComponent createFilterComponent() {
    return null;
  }

  @Override
  public String getId() {
    return "uncategorizedEpisodes";
  }

  @Override
  public String getFilterValueAsString() {
    return null;
  }

  @Override
  public void setFilterValue(Object value) {

  }
}
