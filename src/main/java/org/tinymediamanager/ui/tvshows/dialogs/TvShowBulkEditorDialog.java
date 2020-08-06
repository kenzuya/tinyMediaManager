/*
 * Copyright 2012 - 2020 Manuel Laggner
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
package org.tinymediamanager.ui.tvshows.dialogs;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import org.tinymediamanager.core.MediaSource;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.thirdparty.trakttv.SyncTraktTvTask;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.components.ReadOnlyTextArea;
import org.tinymediamanager.ui.components.SquareIconButton;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.TmmTabbedPane;
import org.tinymediamanager.ui.components.combobox.AutocompleteComboBox;
import org.tinymediamanager.ui.dialogs.TmmDialog;

import net.miginfocom.swing.MigLayout;

/**
 * The Class TvShowBulkEditorDialog.
 * 
 * @author Manuel Laggner
 */
public class TvShowBulkEditorDialog extends TmmDialog {
  private static final long   serialVersionUID = 3527478264068979388L;

  private TvShowList          tvShowList       = TvShowList.getInstance();
  private List<TvShow>        tvShowsToEdit;
  private List<TvShowEpisode> tvShowEpisodesToEdit;
  private boolean             episodesChanged  = false;
  private boolean             tvShowsChanged   = false;

  /**
   * Instantiates a new movie batch editor.
   * 
   * @param tvShows
   *          the tv shows
   * @param episodes
   *          the episodes
   */
  public TvShowBulkEditorDialog(final List<TvShow> tvShows, final List<TvShowEpisode> episodes) {
    super(BUNDLE.getString("tvshow.bulkedit"), "tvShowBulkEditor");

    tvShowsToEdit = tvShows;
    tvShowEpisodesToEdit = episodes;

    initComponents();

  }

  private void initComponents() {
    TmmTabbedPane tabbedPane = new TmmTabbedPane();
    getContentPane().add(tabbedPane, BorderLayout.CENTER);

    {
      JPanel panelContent = new JPanel();
      panelContent.setLayout(new MigLayout("", "[][200lp:350lp,grow][]", "[][][][][][grow]"));
      tabbedPane.add(BUNDLE.getString("metatag.tvshow"), panelContent);
      {
        JLabel lblGenres = new TmmLabel(BUNDLE.getString("metatag.genre"));
        panelContent.add(lblGenres, "cell 0 0,alignx right");

        JComboBox<MediaGenres> cbGenres = new AutocompleteComboBox(MediaGenres.values());
        panelContent.add(cbGenres, "cell 1 0,growx,wmin 0");
        cbGenres.setEditable(true);

        JButton btnAddGenre = new SquareIconButton(IconManager.ADD_INV);
        panelContent.add(btnAddGenre, "flowx,cell 2 0");
        btnAddGenre.addActionListener(e -> {
          tvShowsChanged = true;
          setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          MediaGenres genre = null;
          Object item = cbGenres.getSelectedItem();

          // genre
          if (item instanceof MediaGenres) {
            genre = (MediaGenres) item;
          }

          // newly created genre?
          if (item instanceof String) {
            genre = MediaGenres.getGenre((String) item);
          }

          if (genre != null) {
            for (TvShow tvShow : tvShowsToEdit) {
              tvShow.addGenre(genre);
            }
          }
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        });

        JButton btnRemoveGenre = new SquareIconButton(IconManager.REMOVE_INV);
        panelContent.add(btnRemoveGenre, "cell 2 0");
        btnRemoveGenre.addActionListener(e -> {
          tvShowsChanged = true;
          setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          MediaGenres genre = (MediaGenres) cbGenres.getSelectedItem();
          for (TvShow tvShow : tvShowsToEdit) {
            tvShow.removeGenre(genre);
          }
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        });
      }

      {
        JLabel lblTags = new TmmLabel(BUNDLE.getString("metatag.tags"));
        panelContent.add(lblTags, "cell 0 1,alignx right");

        JComboBox<String> cbTags = new AutocompleteComboBox<>(ListUtils.asSortedList(tvShowList.getTagsInTvShows()));
        panelContent.add(cbTags, "cell 1 1,growx,wmin 0");
        cbTags.setEditable(true);

        JButton btnAddTag = new SquareIconButton(IconManager.ADD_INV);
        panelContent.add(btnAddTag, "flowx,cell 2 1");
        btnAddTag.addActionListener(e -> {
          tvShowsChanged = true;
          setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          String tag = (String) cbTags.getSelectedItem();
          for (TvShow tvShow : tvShowsToEdit) {
            tvShow.addToTags(tag);
          }
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        });

        JButton btnRemoveTag = new SquareIconButton(IconManager.REMOVE_INV);
        panelContent.add(btnRemoveTag, "cell 2 1");
        btnRemoveTag.addActionListener(e -> {
          tvShowsChanged = true;
          setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          String tag = (String) cbTags.getSelectedItem();
          for (TvShow tvShow : tvShowsToEdit) {
            tvShow.removeFromTags(tag);
          }
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        });
      }

      {
        JLabel lblCountry = new TmmLabel(BUNDLE.getString("metatag.country"));
        panelContent.add(lblCountry, "cell 0 2,alignx right");

        JTextField tfCountry = new JTextField();
        panelContent.add(tfCountry, "cell 1 2,growx,wmin 0");

        JButton btnChgCountry = new SquareIconButton(IconManager.APPLY_INV);
        panelContent.add(btnChgCountry, "flowx,cell 2 2");
        btnChgCountry.addActionListener(e -> {
          tvShowsChanged = true;
          setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          for (TvShow tvShow : tvShowsToEdit) {
            tvShow.setCountry(tfCountry.getText());
          }
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        });
      }

      {
        JLabel lblStudio = new TmmLabel(BUNDLE.getString("metatag.studio"));
        panelContent.add(lblStudio, "cell 0 3,alignx right");

        JTextField tfStudio = new JTextField();
        panelContent.add(tfStudio, "cell 1 3,growx,wmin 0");

        JButton btnChgStudio = new SquareIconButton(IconManager.APPLY_INV);
        panelContent.add(btnChgStudio, "flowx,cell 2 3");
        btnChgStudio.addActionListener(e -> {
          tvShowsChanged = true;
          setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          for (TvShow tvShow : tvShowsToEdit) {
            tvShow.setProductionCompany(tfStudio.getText());
          }
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        });
      }

      {
        JLabel lblTvShowNoteT = new TmmLabel(BUNDLE.getString("metatag.note"));
        panelContent.add(lblTvShowNoteT, "cell 0 4,alignx right");

        JTextField tfTvShowNote = new JTextField();
        panelContent.add(tfTvShowNote, "cell 1 4,growx");

        JButton btnTvShowNote = new SquareIconButton(IconManager.APPLY_INV);
        btnTvShowNote.addActionListener(e -> {
          tvShowsChanged = true;
          setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          for (TvShow tvShow : tvShowsToEdit) {
            tvShow.setNote(tfTvShowNote.getText());
          }
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        });
        panelContent.add(btnTvShowNote, "cell 2 4");
      }
    }
    {
      JPanel panelContent = new JPanel();
      panelContent.setLayout(new MigLayout("", "[][200lp:350lp,grow][]", "[][][][][][][][grow]"));
      tabbedPane.add(BUNDLE.getString("metatag.episode"), panelContent);

      JTextArea textArea = new ReadOnlyTextArea(BUNDLE.getString("tvshow.bulkedit.episodesfromshows"));
      textArea.setWrapStyleWord(true);
      panelContent.add(textArea, "cell 0 0 2 1,wmin 0,grow");

      {
        JLabel lblWatched = new TmmLabel(BUNDLE.getString("metatag.watched"));
        panelContent.add(lblWatched, "cell 0 1,alignx right");

        JCheckBox chckbxWatched = new JCheckBox("");
        panelContent.add(chckbxWatched, "cell 1 1");

        JButton btnWatched = new SquareIconButton(IconManager.APPLY_INV);
        panelContent.add(btnWatched, "cell 2 1");
        btnWatched.addActionListener(e -> {
          episodesChanged = true;
          setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          for (TvShowEpisode episode : tvShowEpisodesToEdit) {
            episode.setWatched(chckbxWatched.isSelected());
          }
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        });
      }

      {
        JLabel lblSeason = new TmmLabel(BUNDLE.getString("metatag.season"));
        panelContent.add(lblSeason, "cell 0 2,alignx right");

        JSpinner spSeason = new JSpinner();
        spSeason.setModel(new SpinnerNumberModel(0, -1, 9999, 1));
        panelContent.add(spSeason, "cell 1 2");

        JButton btnSeason = new SquareIconButton(IconManager.APPLY_INV);
        panelContent.add(btnSeason, "cell 2 2");
        btnSeason.addActionListener(arg0 -> {
          episodesChanged = true;
          setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          for (TvShowEpisode episode : tvShowEpisodesToEdit) {
            Integer season = (Integer) spSeason.getValue();
            episode.setSeason(season);
          }
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        });
      }

      {
        JLabel lblDvdOrder = new TmmLabel(BUNDLE.getString("metatag.dvdorder"));
        panelContent.add(lblDvdOrder, "cell 0 3,alignx right");

        final JCheckBox cbDvdOrder = new JCheckBox("");
        panelContent.add(cbDvdOrder, "cell 1 3");

        JButton btnDvdOrder = new SquareIconButton(IconManager.APPLY_INV);
        panelContent.add(btnDvdOrder, "cell 2 3");
        btnDvdOrder.addActionListener(arg0 -> {
          episodesChanged = true;
          setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          for (TvShowEpisode episode : tvShowEpisodesToEdit) {
            episode.setDvdOrder(cbDvdOrder.isSelected());
          }
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        });
      }

      {

        JLabel lblTagsEpisode = new TmmLabel(BUNDLE.getString("metatag.tags"));
        panelContent.add(lblTagsEpisode, "cell 0 4,alignx right");

        JComboBox<String> cbTagsEpisode = new AutocompleteComboBox(tvShowList.getTagsInEpisodes().toArray());
        panelContent.add(cbTagsEpisode, "cell 1 4,growx,wmin 0");
        cbTagsEpisode.setEditable(true);

        JButton btnAddTagEpisode = new SquareIconButton(IconManager.ADD_INV);
        panelContent.add(btnAddTagEpisode, "flowx,cell 2 4");
        btnAddTagEpisode.addActionListener(e -> {
          episodesChanged = true;
          setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          String tag = (String) cbTagsEpisode.getSelectedItem();
          for (TvShowEpisode episode : tvShowEpisodesToEdit) {
            episode.addToTags(tag);
          }
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        });

        JButton btnRemoveTagEpisode = new SquareIconButton(IconManager.REMOVE_INV);
        panelContent.add(btnRemoveTagEpisode, "cell 2 4");
        btnRemoveTagEpisode.addActionListener(e -> {
          episodesChanged = true;
          setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          String tag = (String) cbTagsEpisode.getSelectedItem();
          for (TvShowEpisode episode : tvShowEpisodesToEdit) {
            episode.removeFromTags(tag);
          }
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        });
      }

      {
        JLabel lblMediasourceEpisode = new TmmLabel(BUNDLE.getString("metatag.source"));
        panelContent.add(lblMediasourceEpisode, "cell 0 5,alignx right");

        JComboBox<MediaSource> cbMediaSourceEpisode = new JComboBox(MediaSource.values());
        panelContent.add(cbMediaSourceEpisode, "cell 1 5,growx,wmin 0");

        JButton btnMediaSourceEpisode = new SquareIconButton(IconManager.APPLY_INV);
        panelContent.add(btnMediaSourceEpisode, "cell 2 5");
        btnMediaSourceEpisode.addActionListener(e -> {
          episodesChanged = true;
          Object obj = cbMediaSourceEpisode.getSelectedItem();
          if (obj instanceof MediaSource) {
            MediaSource mediaSource = (MediaSource) obj;
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            for (TvShowEpisode episode : tvShowEpisodesToEdit) {
              episode.setMediaSource(mediaSource);
            }
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
          }
        });
      }

      {
        JLabel lblEpisodeNoteT = new TmmLabel(BUNDLE.getString("metatag.note"));
        panelContent.add(lblEpisodeNoteT, "cell 0 6,alignx right");

        JTextField tfEpisodeNote = new JTextField();
        panelContent.add(tfEpisodeNote, "cell 1 6,growx");

        JButton btnEpisodeNote = new SquareIconButton(IconManager.APPLY_INV);
        btnEpisodeNote.addActionListener(e -> {
          episodesChanged = true;
          setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          for (TvShowEpisode episode : tvShowEpisodesToEdit) {
            episode.setNote(tfEpisodeNote.getText());
          }
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        });
        panelContent.add(btnEpisodeNote, "cell 2 6");
      }

      {
        JButton btnClose = new JButton(BUNDLE.getString("Button.close"));
        btnClose.setIcon(IconManager.APPLY_INV);
        btnClose.addActionListener(arg0 -> {
          // rewrite tv show if anything changed
          if (tvShowsChanged) {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            for (TvShow tvShow : tvShowsToEdit) {
              tvShow.writeNFO();
              tvShow.saveToDb();
            }
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
          }

          // rewrite episodes if anything changed
          if (episodesChanged) {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            for (TvShowEpisode episode : tvShowEpisodesToEdit) {
              episode.writeNFO();
              episode.saveToDb();
            }
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
          }

          if (TvShowModuleManager.SETTINGS.getSyncTrakt()) {
            Set<TvShow> tvShows1 = new HashSet<>();
            for (TvShowEpisode episode : tvShowEpisodesToEdit) {
              tvShows1.add(episode.getTvShow());
            }
            tvShows1.addAll(tvShowsToEdit);
            TmmTask task = new SyncTraktTvTask(null, new ArrayList<>(tvShows1));
            TmmTaskManager.getInstance().addUnnamedTask(task);
          }

          setVisible(false);
        });
        addDefaultButton(btnClose);

        // add window listener to write changes (if the window close button "X" is pressed)
        addWindowListener(new WindowAdapter() {
          @Override
          public void windowClosing(WindowEvent e) {
            // rewrite tv show if anything changed
            if (tvShowsChanged) {
              setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
              for (TvShow tvShow : tvShowsToEdit) {
                tvShow.writeNFO();
                tvShow.saveToDb();
              }
              setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }

            // rewrite episodes if anything changed
            if (episodesChanged) {
              setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
              for (TvShowEpisode episode : tvShowEpisodesToEdit) {
                episode.writeNFO();
                episode.saveToDb();
              }
              setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
          }
        });
      }
    }
  }
}
