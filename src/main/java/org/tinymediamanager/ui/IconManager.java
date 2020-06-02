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
package org.tinymediamanager.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.image.BaseMultiResolutionImage;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.UIManager;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.ui.images.TmmAwesomeIcon;
import org.tinymediamanager.ui.images.TmmSvgIcon;
import org.tinymediamanager.ui.images.TmmTextIcon;

public class IconManager {
  public static final ImageIcon              EMPTY_IMAGE                 = new ImageIcon(IconManager.class.getResource("plaf/empty.png"));
  private static final Set<TmmSvgIconCache>  SVG_CACHE                   = new HashSet<>();
  private static final Set<TmmTextIconCache> TEXT_ICON_CACHE             = new HashSet<>();
  private static final Map<URI, ImageIcon>   ICON_CACHE                  = new HashMap<>();

  // toolbar icons
  public static final ImageIcon              TOOLBAR_ABOUT               = loadMultiResolutionImage("icn_about.png");
  public static final ImageIcon              TOOLBAR_ABOUT_HOVER         = loadMultiResolutionImage("icn_about_hover.png");
  public static final ImageIcon              TOOLBAR_ADD_MOVIE_SET       = loadMultiResolutionImage("icn_add_collection.png");
  public static final ImageIcon              TOOLBAR_ADD_MOVIE_SET_HOVER = loadMultiResolutionImage("icn_add_collection_hover.png");
  public static final ImageIcon              TOOLBAR_DONATE              = loadMultiResolutionImage("icn_donate.png");
  public static final ImageIcon              TOOLBAR_DONATE_HOVER        = loadMultiResolutionImage("icn_donate_hover.png");
  public static final ImageIcon              TOOLBAR_EDIT                = loadMultiResolutionImage("icn_edit.png");
  public static final ImageIcon              TOOLBAR_EDIT_HOVER          = loadMultiResolutionImage("icn_edit_hover.png");
  public static final ImageIcon              TOOLBAR_LOGO                = loadMultiResolutionImage("icn_logo_toolbar.png");
  public static final ImageIcon              TOOLBAR_REFRESH             = loadMultiResolutionImage("icn_refresh.png");
  public static final ImageIcon              TOOLBAR_REFRESH_HOVER       = loadMultiResolutionImage("icn_refresh_hover.png");
  public static final ImageIcon              TOOLBAR_RENAME              = loadMultiResolutionImage("icn_rename.png");
  public static final ImageIcon              TOOLBAR_RENAME_HOVER        = loadMultiResolutionImage("icn_rename_hover.png");
  public static final ImageIcon              TOOLBAR_SEARCH              = loadMultiResolutionImage("icn_search.png");
  public static final ImageIcon              TOOLBAR_SEARCH_HOVER        = loadMultiResolutionImage("icn_search_hover.png");
  public static final ImageIcon              TOOLBAR_SETTINGS            = loadMultiResolutionImage("icn_settings.png");
  public static final ImageIcon              TOOLBAR_SETTINGS_HOVER      = loadMultiResolutionImage("icn_settings_hover.png");
  public static final ImageIcon              TOOLBAR_TOOLS               = loadMultiResolutionImage("icn_tools.png");
  public static final ImageIcon              TOOLBAR_TOOLS_HOVER         = loadMultiResolutionImage("icn_tools_hover.png");

  // packaged icons
  public static final ImageIcon              STAR_FILLED                 = createSVGIcon("star-filled.svg", new Dimension(24, 24));
  public static final ImageIcon              STAR_EMPTY                  = createSVGIcon("star-empty.svg", new Dimension(24, 24));

  // font awesome icons for actions in the popup menu
  public static final ImageIcon              ADD                         = createMenuIcon("plus.svg");
  public static final ImageIcon              BUG                         = createMenuIcon("bug.svg");
  public static final ImageIcon              DELETE                      = createMenuIcon("times.svg");
  public static final ImageIcon              DELETE_FOREVER              = createMenuIcon("trash-alt.svg");
  public static final ImageIcon              DOWNLOAD                    = createMenuIcon("download.svg");
  public static final ImageIcon              FEEDBACK                    = createMenuIcon("envelope.svg");
  public static final ImageIcon              EDIT                        = createMenuIcon("edit.avg");
  public static final ImageIcon              EXPORT                      = createMenuIcon("share-square.svg");
  public static final ImageIcon              HINT                        = createMenuIcon("info-circle.svg");
  public static final ImageIcon              IMAGE                       = createMenuIcon("image.svg");
  public static final ImageIcon              MEDIAINFO                   = createMenuIcon("info.svg");
  public static final ImageIcon              PLAY                        = createMenuIcon("play.svg");
  public static final ImageIcon              REFRESH                     = createMenuIcon("redo.svg");
  public static final ImageIcon              REMOVE                      = createMenuIcon("minus.svg");
  public static final ImageIcon              SEARCH                      = createMenuIcon("search.svg");
  public static final ImageIcon              SUBTITLE                    = createMenuIcon("comments.svg");
  public static final ImageIcon              SYNC                        = createMenuIcon("sync.svg");

  // font awesome icons for the table/tree
  public static final ImageIcon              TABLE_OK                    = createSVGIcon("check.svg", new Color(31, 187, 0));
  public static final ImageIcon              TABLE_PROBLEM               = createSVGIcon("exclamation-triangle.svg", new Color(204, 120, 50));
  public static final ImageIcon              TABLE_NOT_OK                = createSVGIcon("times.svg", new Color(204, 2, 2));

  // font awesome icons normal
  public static final ImageIcon              CANCEL                      = createSVGIcon("times-circle.svg");
  public static final ImageIcon              CARET_UP                    = createSVGIcon("chevron-up.svg");
  public static final ImageIcon              CARET_DOWN                  = createSVGIcon("chevron-down.svg");
  public static final ImageIcon              CLEAR_GREY                  = createSVGIcon("times-circle.svg");
  public static final ImageIcon              COLLAPSED                   = createSVGIcon("chevron-square-down.svg");
  public static final ImageIcon              CONFIGURE                   = createSVGIcon("wrench.svg");
  public static final ImageIcon              DELETE_GRAY                 = createSVGIcon("trash-alt.svg");
  public static final ImageIcon              ERROR                       = createSVGIcon("times-circle.svg");
  public static final ImageIcon              EXPANDED                    = createSVGIcon("chevron-square-right.svg");
  public static final ImageIcon              WARN                        = createSVGIcon("exclamation-triangle.svg");
  public static final ImageIcon              WARN_INTENSIFIED            = createSVGIcon("exclamation-triangle.svg", Color.RED);
  public static final ImageIcon              INFO                        = createSVGIcon("info-circle.svg");
  public static final ImageIcon              HELP                        = createSVGIcon("question-circle.svg");
  public static final ImageIcon              FILTER_ACTIVE               = createSVGIcon("lightbulb-on.svg", new Color(255, 119, 0));
  public static final ImageIcon              NEW                         = createTextIcon("new", new Color(31, 187, 0));
  public static final ImageIcon              PLAY_LARGE                  = createSVGIcon("play-circle.svg", 2.33333);
  public static final ImageIcon              SEARCH_GREY                 = createSVGIcon("search.svg");
  public static final ImageIcon              STOP                        = createSVGIcon("stop-circle.svg");
  public static final ImageIcon              UNDO_GREY                   = createSVGIcon("undo.svg");

  // font awesome icons light (button usage)
  public static final ImageIcon              ADD_INV                     = createButtonIcon("plus.svg");
  public static final ImageIcon              ARROW_UP_INV                = createButtonIcon("chevron-up.svg");
  public static final ImageIcon              ARROW_DOWN_INV              = createButtonIcon("chevron-down.svg");
  public static final ImageIcon              APPLY_INV                   = createButtonIcon("check-circle.svg");
  public static final ImageIcon              BACK_INV                    = createButtonIcon("chevron-circle-left.svg");
  public static final ImageIcon              CANCEL_INV                  = createButtonIcon("times-circle.svg");
  public static final ImageIcon              CHECK_ALL                   = createButtonIcon("check-square.svg");
  public static final ImageIcon              CLEAR_ALL                   = createButtonIcon("square.svg");
  public static final ImageIcon              COPY_INV                    = createButtonIcon("clone.svg");
  public static final ImageIcon              DATE_PICKER                 = createButtonIcon("calendar-alt.svg");
  public static final ImageIcon              DELETE_INV                  = createButtonIcon("trash-alt.svg");
  public static final ImageIcon              FILE_OPEN_INV               = createButtonIcon("folder-open.svg");
  public static final ImageIcon              IMAGE_INV                   = createButtonIcon("image.svg");
  public static final ImageIcon              PLAY_INV                    = createButtonIcon("play.svg");
  public static final ImageIcon              REMOVE_INV                  = createButtonIcon("minus.svg");
  public static final ImageIcon              SEARCH_INV                  = createButtonIcon("search.svg");
  public static final ImageIcon              STOP_INV                    = createButtonIcon("stop-circle.svg");

  // font awesome icons - column headers
  public static final ImageIcon              AUDIO                       = createTableHeaderIcon("volume-up.svg");
  public static final ImageIcon              CERTIFICATION               = createTableHeaderIcon("universal-access.svg");
  public static final ImageIcon              COUNT                       = createTableHeaderIcon("hashtag.svg");
  public static final ImageIcon              DATE_ADDED                  = createTableHeaderIcon("calendar-plus.svg");
  public static final ImageIcon              EDITION                     = createTableHeaderIcon("compact-disc.svg");
  public static final ImageIcon              EDIT_HEADER                 = createTableHeaderIcon("edit.svg");
  public static final ImageIcon              EPISODES                    = createTextIcon("E", 1.5);
  public static final ImageIcon              FILE_SIZE                   = createTableHeaderIcon("save.svg");
  public static final ImageIcon              IMAGES                      = createTableHeaderIcon("images.svg");
  public static final ImageIcon              IDCARD                      = createTableHeaderIcon("id-card.svg");
  public static final ImageIcon              NFO                         = createTableHeaderIcon("file-alt.svg");
  public static final ImageIcon              RATING                      = createTableHeaderIcon("star.svg");
  public static final ImageIcon              SEASONS                     = createTextIcon("S", 1.5);
  public static final ImageIcon              SOURCE                      = createTableHeaderIcon("location");
  public static final ImageIcon              SUBTITLES                   = createTableHeaderIcon("comments.svg");
  public static final ImageIcon              TRAILER                     = createTableHeaderIcon("film.svg");
  public static final ImageIcon              VIDEO_3D                    = createTableHeaderIcon("cube.svg");
  public static final ImageIcon              VIDEO_FORMAT                = createTableHeaderIcon("expand-wide.svg");
  public static final ImageIcon              VOTES                       = createTableHeaderIcon("thumbs-up.svg");
  public static final ImageIcon              WATCHED                     = createTableHeaderIcon("play.svg");

  private static ImageIcon loadImage(String name) {
    URL file = IconManager.class.getResource("images/interface/" + name);
    if (file != null) {
      return new ImageIcon(file);
    }

    return EMPTY_IMAGE;
  }

  private static ImageIcon loadMultiResolutionImage(String name) {
    List<String> resolutions = Arrays.asList("", "@125", "@150", "@200");
    try {
      List<Image> images = new ArrayList<>();

      String extension = FilenameUtils.getExtension(name);
      String basename = FilenameUtils.getBaseName(name);

      for (String resolution : resolutions) {
        URL file = IconManager.class.getResource("images/interface/" + basename + resolution + "." + extension);
        if (file != null) {
          images.add(new ImageIcon(file).getImage());
        }
      }

      return new ImageIcon(new BaseMultiResolutionImage(images.toArray(new Image[0])));
    }
    catch (Exception ignored) {
    }

    return EMPTY_IMAGE;
  }

  /**
   * loads an image from the given url
   *
   * @param url
   *          the url pointing to the image
   * @return the image or an empty image (1x1 px transparent) if it is not loadable
   */
  public static ImageIcon loadImageFromURL(URL url) {
    URI uri = null;

    if (url == null) {
      return EMPTY_IMAGE;
    }

    try {
      uri = url.toURI();
    }
    catch (Exception e) {
      return EMPTY_IMAGE;
    }

    // read cache
    ImageIcon icon = ICON_CACHE.get(uri);

    if (icon == null) {
      try {
        icon = new ImageIcon(url);
      }
      catch (Exception ignored) {
      }

      if (icon == null) {
        icon = EMPTY_IMAGE;
      }

      ICON_CACHE.put(uri, icon);
    }

    return icon;
  }

  private static ImageIcon createMenuIcon(String name) {
    return createSVGIcon(name, "Component.focusColor", TmmFontHelper.H3);
  }

  private static ImageIcon createButtonIcon(String name) {
    return createSVGIcon(name, "Button.foreground", TmmFontHelper.H3);
  }

  private static ImageIcon createTableHeaderIcon(String name) {
    return createSVGIcon(name, "TableHeader.foreground", TmmFontHelper.H2);
  }

  private static ImageIcon createSVGIcon(String name) {
    return createSVGIcon(name, "Label.foreground", TmmFontHelper.H3);
  }

  private static ImageIcon createSVGIcon(String name, double scaleFactor) {
    return createSVGIcon(name, "Label.foreground", scaleFactor);
  }

  private static ImageIcon createSVGIcon(String name, Color color) {
    return createSVGIcon(name, color, TmmFontHelper.H2);
  }

  private static ImageIcon createSVGIcon(String name, String colorReference, double scaleFactor) {
    // create the icon
    ImageIcon icon = createSVGIcon(name, UIManager.getColor(colorReference), scaleFactor);

    // and put it to the cache (for recoloring/rescaling if needed)
    if (icon != EMPTY_IMAGE) {
      SVG_CACHE.add(new TmmSvgIconCache((TmmSvgIcon) icon, scaleFactor, colorReference));
    }
    return icon;
  }

  private static ImageIcon createSVGIcon(String name, Dimension size) {
    try {
      // create the icon
      URI uri = IconManager.class.getResource("images/svg/" + name).toURI();
      TmmSvgIcon icon = new TmmSvgIcon(uri);
      icon.setPreferredSize(size);
      return icon;
    }
    catch (Exception e) {
      return EMPTY_IMAGE;
    }
  }

  private static ImageIcon createSVGIcon(String name, Color color, double scaleFactor) {
    try {
      // create the icon
      URI uri = IconManager.class.getResource("images/svg/" + name).toURI();
      TmmAwesomeIcon icon = new TmmAwesomeIcon(uri, color);

      int size = calculateFontIconSize(scaleFactor);
      icon.setPreferredSize(new Dimension(size, size));

      SVG_CACHE.add(new TmmSvgIconCache(icon, scaleFactor, null));
      return icon;
    }
    catch (Exception e) {
      return EMPTY_IMAGE;
    }
  }

  private static int calculateFontIconSize(double scaleFactor) {
    try {
      return (int) Math.floor(UIManager.getFont("defaultFont").getSize() * scaleFactor);
    }
    catch (Exception e) {
      return (int) Math.floor(12 * scaleFactor);
    }
  }

  private static ImageIcon createTextIcon(String text, Color color) {
    return createTextIcon(text, 1, color);
  }

  private static ImageIcon createTextIcon(String text, double scaleFactor) {
    ImageIcon icon = createTextIcon(text, scaleFactor, UIManager.getColor("Label.foreground"));
    TEXT_ICON_CACHE.add(new TmmTextIconCache((TmmTextIcon) icon, scaleFactor, null));
    return icon;
  }

  private static ImageIcon createTextIcon(String text, double scaleFactor, Color color) {
    int size = calculateFontIconSize(scaleFactor);
    TmmTextIcon icon = new TmmTextIcon(text, size, color);

    TEXT_ICON_CACHE.add(new TmmTextIconCache(icon, scaleFactor, null));
    return icon;
  }

  private static class TmmSvgIconCache {
    private TmmSvgIcon   icon;
    private final double scaleFactor;
    private final String colorReference;

    public TmmSvgIconCache(TmmSvgIcon icon, double scaleFactor, String colorReference) {
      this.icon = icon;
      this.scaleFactor = scaleFactor;
      this.colorReference = colorReference;
    }
  }

  private static class TmmTextIconCache {
    private TmmTextIcon  icon;
    private final double scaleFactor;
    private final String colorReference;

    public TmmTextIconCache(TmmTextIcon icon, double scaleFactor, String colorReference) {
      this.icon = icon;
      this.scaleFactor = scaleFactor;
      this.colorReference = colorReference;
    }
  }

  static void updateIcons() {
    for (TmmSvgIconCache iconCache : SVG_CACHE) {
      if (StringUtils.isNotBlank(iconCache.colorReference)) {
        iconCache.icon.setColor(UIManager.getColor(iconCache.colorReference));
      }
      int size = calculateFontIconSize(iconCache.scaleFactor);
      iconCache.icon.setPreferredSize(new Dimension(size, size));
    }
    for (TmmTextIconCache iconCache : TEXT_ICON_CACHE) {
      if (StringUtils.isNotBlank(iconCache.colorReference)) {
        iconCache.icon.setColor(UIManager.getColor(iconCache.colorReference));
      }
      int size = calculateFontIconSize(iconCache.scaleFactor);
      iconCache.icon.setFontSize(size);
      iconCache.icon.update();
    }
  }
}
