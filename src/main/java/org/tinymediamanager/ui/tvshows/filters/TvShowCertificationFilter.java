package org.tinymediamanager.ui.tvshows.filters;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.swing.JLabel;

import org.tinymediamanager.core.Constants;
import org.tinymediamanager.core.MediaCertification;
import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.ui.components.TmmLabel;

public class TvShowCertificationFilter extends AbstractCheckComboBoxTvShowUIFilter<MediaCertification> {

  private TvShowList tvShowList = TvShowList.getInstance();

  public TvShowCertificationFilter() {
    super();
    checkComboBox.enableFilter((s, s2) -> s.toString().toLowerCase(Locale.ROOT).startsWith(s2.toLowerCase(Locale.ROOT)));
    buildAndInstallCertificationArray();
    PropertyChangeListener propertyChangeListener = evt -> buildAndInstallCertificationArray();
    tvShowList.addPropertyChangeListener(Constants.CERTIFICATION, propertyChangeListener);
  }

  @Override
  protected String parseTypeToString(MediaCertification type) throws Exception {
    return type.name();
  }

  @Override
  protected MediaCertification parseStringToType(String string) throws Exception {
    return MediaCertification.valueOf(string);
  }

  @Override
  protected boolean accept(TvShow tvShow, List<TvShowEpisode> episodes, boolean invert) {
    List<MediaCertification> selectedItems = checkComboBox.getSelectedItems();
    if (invert) {
      return !selectedItems.contains(tvShow.getCertification());
    }
    else {
      return selectedItems.contains(tvShow.getCertification());
    }

  }

  @Override
  protected JLabel createLabel() {
    return new TmmLabel(BUNDLE.getString("metatag.certification"));
  }

  @Override
  public String getId() {
    return "tvShowCertification";
  }

  private void buildAndInstallCertificationArray() {
    List<MediaCertification> certifications = new ArrayList<>(tvShowList.getCertification());
    Collections.sort(certifications);
    setValues(certifications);
  }
}
