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
package org.tinymediamanager.ui.movies;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.scraper.util.StrgUtils;

import ca.odell.glazedlists.matchers.AbstractMatcherEditor;
import ca.odell.glazedlists.matchers.Matcher;

/**
 * a text field based filter for filtering on movie titles
 *
 * @author Manuel Laggner
 */
public class MovieTextMatcherEditor extends AbstractMatcherEditor<Movie> {
  private final JTextComponent textComponent;
  private String               normalizedFilterText;
  private Pattern              filterPattern;

  public MovieTextMatcherEditor(JTextComponent textComponent) {
    this.textComponent = textComponent;
    this.textComponent.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void removeUpdate(DocumentEvent e) {
        refilter();
      }

      @Override
      public void insertUpdate(DocumentEvent e) {
        refilter();
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
        refilter();
      }
    });
  }

  private void refilter() {
    normalizedFilterText = StrgUtils.normalizeString(textComponent.getText());
    try {
      filterPattern = Pattern.compile(normalizedFilterText, Pattern.CASE_INSENSITIVE);
    } catch (PatternSyntaxException ignore) {}

    fireChanged(new MovieMatcher());
  }

  /*
   * helper class for running this filter against the given movie
   */
  private class MovieMatcher implements Matcher<Movie> {
    @Override
    public boolean matches(Movie movie) {
      if (StringUtils.isBlank(normalizedFilterText)) {
        return true;
      }

      if (StringUtils.isNotEmpty(movie.getTitleSortable())) {
        java.util.regex.Matcher matcher = filterPattern.matcher(StrgUtils.normalizeString(movie.getTitleSortable()));
        if (matcher.find()) {
          return true;
        }
      }

      if (StringUtils.isNotEmpty(movie.getOriginalTitleSortable())) {
        java.util.regex.Matcher matcher = filterPattern.matcher(StrgUtils.normalizeString(movie.getOriginalTitleSortable()));
        if (matcher.find()) {
          return true;
        }
      }

      return false;
    }
  }
}
