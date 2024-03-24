package org.tinymediamanager.ui.movies.dialogs;

import static org.tinymediamanager.ui.TmmFontHelper.H2;

import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.Property;
import org.jetbrains.annotations.NotNull;
import org.tinymediamanager.core.AbstractModelObject;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieRenamer;
import org.tinymediamanager.core.movie.MovieSettings;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.tvshow.TvShowRenamer;
import org.tinymediamanager.scraper.DynaEnum;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.components.MainTabbedPane;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.TmmRoundTextArea;
import org.tinymediamanager.ui.components.table.TmmTable;
import org.tinymediamanager.ui.components.table.TmmTableFormat;
import org.tinymediamanager.ui.components.table.TmmTableModel;
import org.tinymediamanager.ui.dialogs.TmmDialog;
import org.tinymediamanager.ui.movies.MovieUIModule;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.swing.GlazedListsSwing;
import net.miginfocom.swing.MigLayout;

public class MovieTokenPreviewDialog extends TmmDialog implements HierarchyListener {

  private JComboBox                         cbMovieForPreview;
  private final MovieSettings               settings     = MovieModuleManager.getInstance().getSettings();
  private JTextArea                         tfMovieTokens;
  JLabel                                    lblExample   = new TmmLabel("", H2);
  JPanel                                    contentPanel = new JPanel(new MigLayout("wrap,insets 10", "[20%][80%]", "[min!][min!][][][]"));

  private TmmTable                          tableExamples;

  private final EventList<MovieRenamerTab1> movieRenamerTab1EventList;

  public MovieTokenPreviewDialog() {
    super(TmmResourceBundle.getString("movie.edit"), "movieBulkEditor");

    this.movieRenamerTab1EventList = GlazedLists
        .threadSafeList(new ObservableElementList<>(new BasicEventList<>(), GlazedLists.beanConnector(MovieRenamerTab1.class)));

    initComponents();
    initDataBindings();

    // data init
    DocumentListener documentListener = new DocumentListener() {
      @Override
      public void removeUpdate(DocumentEvent arg0) {
        createRenamerExample();
      }

      @Override
      public void insertUpdate(DocumentEvent arg0) {
        createRenamerExample();
      }

      @Override
      public void changedUpdate(DocumentEvent arg0) {
        createRenamerExample();
      }
    };

    tfMovieTokens.getDocument().addDocumentListener(documentListener);

    ActionListener actionCreateRenamerExample = e -> createRenamerExample();
    cbMovieForPreview.addActionListener(actionCreateRenamerExample);

    movieRenamerTab1EventList.add(new MovieRenamerTab1());

  }

  private void initComponents() {
    {
      // Movie / Pattern Input
      {
        contentPanel.add(new JLabel(TmmResourceBundle.getString("tmm.movie")));
        cbMovieForPreview = new JComboBox();
        contentPanel.add(cbMovieForPreview, "grow, wrap");

        contentPanel.add(new JLabel(TmmResourceBundle.getString("Settings.renamer.folder")));
        tfMovieTokens = new TmmRoundTextArea();
        tfMovieTokens.setBorder(UIManager.getBorder("ScrollPane.border"));
        contentPanel.add(tfMovieTokens, "grow,wrap");
      }

      // Result
      {
        contentPanel.add(lblExample, "span, align center");
      }

      // Examples
      {
        JTabbedPane tabbedPane = new MainTabbedPane();
        tabbedPane.add("Props Movies", createPropsMoviePanel());
        tabbedPane.add("Renderer", createMovieRendererPanel());
        tabbedPane.add("Props Entities", createPropsEntityPanel());
        contentPanel.add(tabbedPane, "span, grow, push");
      }
      JButton btnDone = new JButton(TmmResourceBundle.getString("Button.close"));
      btnDone.setIcon(IconManager.APPLY_INV);
      btnDone.addActionListener(e -> setVisible(false));
      addDefaultButton(btnDone);
    }
    add(contentPanel);
  }

  private void buildAndInstallMovieArray() {
    cbMovieForPreview.removeAllItems();
    List<Movie> allMovies = new ArrayList<>(MovieModuleManager.getInstance().getMovieList().getMovies());
    Movie sel = MovieUIModule.getInstance().getSelectionModel().getSelectedMovie();
    allMovies.sort(new MovieComparator());
    for (Movie movie : allMovies) {
      MoviePreviewContainer container = new MoviePreviewContainer();
      container.movie = movie;
      cbMovieForPreview.addItem(container);
      if (sel != null && movie.equals(sel)) {
        cbMovieForPreview.setSelectedItem(container);
      }
    }
  }

  private void createRenamerExample() {
    Movie movie = null;

    if (cbMovieForPreview.getSelectedItem() instanceof MovieTokenPreviewDialog.MoviePreviewContainer) {
      MovieTokenPreviewDialog.MoviePreviewContainer container = (MovieTokenPreviewDialog.MoviePreviewContainer) cbMovieForPreview.getSelectedItem();
      movie = container.movie;
    }

    if (movie != null) {
      String filename = "";

      if (StringUtils.isNotBlank(tfMovieTokens.getText())) {
        List<MediaFile> mediaFiles = movie.getMediaFiles(MediaFileType.VIDEO);
        if (!mediaFiles.isEmpty()) {
          String extension = FilenameUtils.getExtension(mediaFiles.get(0).getFilename());
          filename = MovieRenamer.getTokenValue(movie, tfMovieTokens.getText());
          // patterns are always w/o extension, but when having the originalFilename, it will be there.
          if (!filename.endsWith(extension)) {
            filename += "." + extension;
          }
        }
      }
      else {
        filename = movie.getMediaFiles(MediaFileType.VIDEO).get(0).getFilename();
      }

      try {
        String result = filename;
        lblExample.setText(result);
        lblExample.setToolTipText(result);
      }
      catch (Exception e) {
        // not changing on errors
      }
    }
    else {
      lblExample.setText(TmmResourceBundle.getString("Settings.movie.renamer.nomovie"));
      lblExample.setToolTipText(null);
    }
  }

  private JPanel createPropsMoviePanel() {
    JPanel panel = new JPanel(new MigLayout("wrap,insets 10", "[]", "[]"));
    TmmTable table = new TmmTable(
        new TmmTableModel<>(GlazedListsSwing.swingThreadProxyList(movieRenamerTab1EventList), new MovieRenamerTab1TableFormat()));
    panel.add(table, "span, grow, push");
    return panel;
  }

  private JPanel createMovieRendererPanel() {
    return new JPanel();
  }

  private JPanel createPropsEntityPanel() {
    return new JPanel();
  }

  @Override
  public void hierarchyChanged(HierarchyEvent e) {
    if (isShowing()) {
      buildAndInstallMovieArray();
    }
  }

  @Override
  public void addNotify() {
    super.addNotify();
    addHierarchyListener(this);
  }

  @Override
  public void removeNotify() {
    removeHierarchyListener(this);
    super.removeNotify();
  }

  /*****************************************************************************
   * helper classes
   *****************************************************************************/
  private static class MoviePreviewContainer {
    Movie movie;

    @Override
    public String toString() {
      return movie.getTitle();
    }
  }

  private static class MovieComparator implements Comparator<Movie> {
    @Override
    public int compare(Movie arg0, Movie arg1) {
      return arg0.getTitle().compareTo(arg1.getTitle());
    }
  }

  protected void initDataBindings() {
    Property settingsBeanProperty_1 = BeanProperty.create("renamerToken");
    Property jTextFieldBeanProperty_1 = BeanProperty.create("text");
    AutoBinding autoBinding_1 = Bindings.createAutoBinding(AutoBinding.UpdateStrategy.READ_WRITE, settings, settingsBeanProperty_1, tfMovieTokens,
        jTextFieldBeanProperty_1);
    autoBinding_1.bind();
  }

  private static class MovieRenamerTab1 extends AbstractModelObject {
    private final String title    = "Titel";
    private final String shortcut = "Shortcut";
    private final String result   = "Result";
  }

  private static class MovieRenamerTab1TableFormat extends TmmTableFormat<MovieRenamerTab1> {
    public MovieRenamerTab1TableFormat() {
      Column title = new Column(TmmResourceBundle.getString("Titel"), "title", movieRenamerTab1 -> movieRenamerTab1.title, String.class);
      addColumn(title);

      Column shortcut = new Column(TmmResourceBundle.getString("Shortcut"), "shortcut", movieRenamerTab1 -> movieRenamerTab1.shortcut, String.class);
      addColumn(shortcut);

      Column result = new Column(TmmResourceBundle.getString("Result"), "result", movieRenamerTab1 -> movieRenamerTab1.result, String.class);
      addColumn(result);
    }
  }

  private void getToken(Class<?> clazz, String prefix) throws Exception {

    // access properties as Map
    BeanInfo info = Introspector.getBeanInfo(clazz);
    PropertyDescriptor[] pds = info.getPropertyDescriptors();

    for (PropertyDescriptor descriptor : pds) {
      if ("class".equals(descriptor.getDisplayName())) {
        continue;
      }

      if ("declaringClass".equals(descriptor.getDisplayName())) {
        continue;
      }

      if (descriptor.getReadMethod() != null) {
        String shortToken = getShort(prefix, descriptor.getDisplayName());
        String fullToken = getFull(prefix, descriptor.getDisplayName());
        String description = descriptor.getShortDescription();
      }
    }
  }

  private String getFull(String prefix, String name) {
    String fullToken = name;
    if (prefix.length() > 3) {
      fullToken = "${" + prefix + fullToken + "}";
    }
    return fullToken;
  }


  private String getShort(String prefix, String name) {
    String shortToken = MovieRenamer.getTokenMapReversed().get(prefix + name);
    if (shortToken == null) {
      shortToken = TvShowRenamer.getTokenMapReversed().get(prefix + name);
    }
    if (shortToken != null && prefix.length() > 3) {
      shortToken = "${" + shortToken + "} |";
    }
    else {
      shortToken = " | ";
    }
    return shortToken;
  }

  private String getTypeName(Class<?> clazz) {
    String typeAsString;

    if (clazz.isEnum()) {
      typeAsString = "String";
    }
    else if (DynaEnum.class.isAssignableFrom(clazz)) {
      typeAsString = "String";
    }
    else {
      typeAsString = clazz.getSimpleName();
    }
    return typeAsString;
  }
}
