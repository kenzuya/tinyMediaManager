/*
 * Copyright 2012 - 2022 Manuel Laggner
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
package org.tinymediamanager.ui.tvshows;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.scraper.entities.MediaEpisodeGroup;
import org.tinymediamanager.scraper.entities.MediaEpisodeNumber;
import org.tinymediamanager.ui.panels.IModalPopupPanel;

import net.miginfocom.swing.MigLayout;

/**
 * the class {@link TvShowEpisodeNumberEditorPanel} is used to edit TV show episode numbers
 * 
 * @author Manuel Laggner
 */
public class TvShowEpisodeNumberEditorPanel extends JPanel implements IModalPopupPanel {
  private final JComboBox<MediaEpisodeGroup.EpisodeGroup> cbEpisodeGroup;
  private final JSpinner                                  spEpisode;
  private final JSpinner                                  spSeason;

  private final JButton                                   btnClose;
  private final JButton                                   btnCancel;

  private boolean                                         cancel = false;
  private MediaEpisodeNumber                              episodeNumber;

  public TvShowEpisodeNumberEditorPanel(MediaEpisodeNumber episodeNumber) {
    super();

    {
      setLayout(new MigLayout("", "[][]", "[][][]"));
      {
        JLabel episodeGroupT = new JLabel(TmmResourceBundle.getString("metatag.episode.group"));
        add(episodeGroupT, "cell 0 0,alignx trailing");

        cbEpisodeGroup = new JComboBox<>(MediaEpisodeGroup.EpisodeGroup.values());
        add(cbEpisodeGroup, "cell 1 0,growx");
      }
      {
        JLabel seasonT = new JLabel(TmmResourceBundle.getString("metatag.season"));
        add(seasonT, "cell 0 1,alignx trailing");

        spSeason = new JSpinner();
        spSeason.setModel(new SpinnerNumberModel(-1, -1, Integer.MAX_VALUE, 1));

        add(spSeason, "cell 1 1,growx");
      }
      {
        JLabel episodeT = new JLabel(TmmResourceBundle.getString("metatag.episode"));
        add(episodeT, "cell 0 2,alignx trailing");

        spEpisode = new JSpinner();
        spEpisode.setModel(new SpinnerNumberModel(-1, -1, Integer.MAX_VALUE, 1));

        add(spEpisode, "cell 1 2,growx");
      }
      {
        btnCancel = new JButton(TmmResourceBundle.getString("Button.cancel"));
        btnCancel.addActionListener(e -> {
          cancel = true;
          setVisible(false);
        });

        btnClose = new JButton(TmmResourceBundle.getString("Button.save"));
        btnClose.addActionListener(e -> {
          if (cbEpisodeGroup.getSelectedItem()instanceof MediaEpisodeGroup.EpisodeGroup episodeGroup) {
            this.episodeNumber = new MediaEpisodeNumber(episodeGroup, (Integer) spSeason.getValue(), (Integer) spEpisode.getValue());
          }

          setVisible(false);
        });
      }
    }

    // set all existing data
    cbEpisodeGroup.setSelectedItem(episodeNumber.episodeGroup());
    spSeason.setValue(episodeNumber.season());
    spEpisode.setValue(episodeNumber.episode());

    // set focus to the first combobox
    SwingUtilities.invokeLater(cbEpisodeGroup::requestFocus);
  }

  @Override
  public JComponent getContent() {
    return this;
  }

  @Override
  public JButton getCloseButton() {
    return btnClose;
  }

  @Override
  public JButton getCancelButton() {
    return btnCancel;
  }

  @Override
  public boolean isCancelled() {
    return cancel;
  }

  public MediaEpisodeNumber getEpisodeNumber() {
    return episodeNumber;
  }
}
