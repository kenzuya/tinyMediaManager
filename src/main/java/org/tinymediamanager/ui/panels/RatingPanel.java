/*
 * Copyright 2012 - 2021 Manuel Laggner
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
package org.tinymediamanager.ui.panels;

import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.TmmFontHelper;

/**
 * the class {@link RatingPanel} is used to display well known ratings
 * 
 * @author Manuel Laggner
 */
public class RatingPanel extends JPanel {
  private final Locale defaultLocale = Locale.getDefault();

  public RatingPanel() {
    setLayout(new FlowLayout(FlowLayout.LEFT, 15, 0));
    setOpaque(false);
  }

  public void setRatings(Map<String, MediaRating> newRatings) {
    updateRatings(Objects.requireNonNullElse(newRatings, Collections.emptyMap()));
  }

  private void updateRatings(Map<String, MediaRating> ratings) {
    removeAll();

    List<MediaRating> addedRatings = new ArrayList<>();

    // add well known ratings in the following order

    // 1. user rating
    MediaRating rating = ratings.get(MediaRating.USER);
    if (rating != null) {
      addedRatings.add(rating);
      add(new RatingContainer(rating));
    }

    // 2. imdb rating
    rating = ratings.get(MediaMetadata.IMDB);
    if (rating != null) {
      addedRatings.add(rating);
      add(new RatingContainer(rating));
    }

    // 3. rotten tomatoes rating
    rating = ratings.get("tomatometerallcritics");
    if (rating != null) {
      addedRatings.add(rating);
      add(new RatingContainer(rating));
    }

    // 4. metacritic rating
    rating = ratings.get("metacritic");
    if (rating != null) {
      addedRatings.add(rating);
      add(new RatingContainer(rating));
    }

    // 5. tmdb rating
    rating = ratings.get(MediaMetadata.TMDB);
    if (rating != null) {
      addedRatings.add(rating);
      add(new RatingContainer(rating));
    }

    // 6. tvdb rating
    rating = ratings.get(MediaMetadata.TVDB);
    if (rating != null) {
      addedRatings.add(rating);
      add(new RatingContainer(rating));
    }

    // 7. trakt.tv rating
    rating = ratings.get(MediaMetadata.TRAKT_TV);
    if (rating != null) {
      addedRatings.add(rating);
      add(new RatingContainer(rating));
    }

    // 6. the custom rating (if it has not been added yet)
    rating = ratings.get("custom");
    if (rating != null && !addedRatings.contains(rating)) {
      addedRatings.add(rating);
      add(new RatingContainer(rating));
    }

    // 7. last but not least NFO/Default if none has been added yet
    if (addedRatings.isEmpty()) {
      rating = ratings.get(MediaRating.DEFAULT);
      if (rating == null) {
        rating = ratings.get(MediaRating.NFO);
      }
      if (rating != null) {
        addedRatings.add(rating);
        add(new RatingContainer(rating));
      }
    }

    // 8. no one added yet? just add the empty rating logo
    if (addedRatings.isEmpty()) {
      add(new RatingContainer(MediaMetadata.EMPTY_RATING));
    }

    revalidate();
    repaint();
  }

  private class RatingContainer extends JPanel {
    public RatingContainer(MediaRating rating) {
      setOpaque(false);

      JLabel logo = null;
      JLabel text = null;

      switch (rating.getId()) {
        case MediaRating.USER:
          logo = new JLabel(IconManager.RATING_USER);
          text = new JLabel(String.format("%.1f", rating.getRating()));
          break;

        case MediaMetadata.IMDB:
          logo = new JLabel(IconManager.RATING_IMDB);
          text = new JLabel(String.format("%.1f", rating.getRating()));
          break;

        case MediaMetadata.TMDB:
          logo = new JLabel(IconManager.RATING_TMDB);
          text = new JLabel(String.format("%.1f", rating.getRating()));
          break;

        case "tomatometerallcritics":
          logo = new JLabel(IconManager.RATING_ROTTEN_TOMATOES);
          text = new JLabel(String.format("%.0f%%", rating.getRating()));
          break;

        case "metacritic":
          logo = new JLabel(IconManager.RATING_METACRITIC);
          text = new JLabel(String.format("%.0f", rating.getRating()));
          break;

        case MediaMetadata.TVDB:
          logo = new JLabel(IconManager.RATING_THETVDB);
          text = new JLabel(String.format("%.1f", rating.getRating()));
          break;

        case MediaMetadata.TRAKT_TV:
          logo = new JLabel(IconManager.RATING_TRAKTTV);
          text = new JLabel(String.format("%.1f", rating.getRating()));
          break;

        case "": // empty rating
          logo = new JLabel(IconManager.RATING_EMTPY);
          text = new JLabel("?");
          break;

        case MediaRating.DEFAULT:
        case MediaRating.NFO:
        default:
          logo = new JLabel(IconManager.RATING_NEUTRAL);
          text = new JLabel(String.format("%.1f", rating.getRating()));
          break;

      }

      if (logo != null && text != null) {
        String tooltipText = createTooltipText(rating);
        logo.setToolTipText(tooltipText);
        add(logo);
        TmmFontHelper.changeFont(text, TmmFontHelper.H2, Font.BOLD);
        text.setToolTipText(tooltipText);
        add(text);
      }
    }

    private String createTooltipText(MediaRating rating) {
      String tooltipText = "";

      switch (rating.getId()) {
        case MediaRating.DEFAULT:
        case MediaRating.NFO:
          break; // no label

        case MediaRating.USER:
          tooltipText = TmmResourceBundle.getString("rating.personal") + ": ";
          break;

        case MediaMetadata.IMDB:
          tooltipText = "IMDb: ";
          break;

        case MediaMetadata.TMDB:
          tooltipText = "TMDB: ";
          break;

        case "tomatometerallcritics":
          tooltipText = "Rotten Tomatoes: ";
          break;

        case "metacritic":
          tooltipText = "Metascore: ";
          break;

        case MediaMetadata.TVDB:
          tooltipText = "TheTVDB: ";
          break;

        case MediaMetadata.TRAKT_TV:
          tooltipText = "Trakt.tv: ";
          break;

        case "": // empty rating
          tooltipText = TmmResourceBundle.getString("rating.empty");
          break;

        default:
          tooltipText = rating.getId() + ": ";
          break;
      }

      if (rating == MediaMetadata.EMPTY_RATING) {
        return tooltipText;
      }

      if (rating.getVotes() > 1) {
        return tooltipText + String.format(defaultLocale, "%.1f / %,d (%,d %s)", rating.getRating(), rating.getMaxValue(), rating.getVotes(),
            TmmResourceBundle.getString("metatag.votes"));
      }
      else {
        return tooltipText + String.format(defaultLocale, "%.1f / %,d", rating.getRating(), rating.getMaxValue());
      }
    }
  }
}
