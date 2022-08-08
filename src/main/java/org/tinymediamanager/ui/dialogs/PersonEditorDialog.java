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

package org.tinymediamanager.ui.dialogs;

import java.awt.Dialog;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.scraper.ScraperType;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.components.MediaIdTable;
import org.tinymediamanager.ui.components.MediaIdTable.MediaId;
import org.tinymediamanager.ui.components.SquareIconButton;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.table.TmmTable;

import ca.odell.glazedlists.EventList;
import net.miginfocom.swing.MigLayout;

/**
 * this dialog is used for editing information of a person
 *
 * @author Manuel Laggner
 */
public class PersonEditorDialog extends TmmDialog {
  private static final long        serialVersionUID = 535326891112742179L;

  private final Person             personToEdit;
  private final EventList<MediaId> ids;

  private JTextField               tfName;
  private JTextField               tfRole;
  private JTextField               tfImageUrl;
  private JTextField               tfProfileUrl;
  private TmmTable                 tableIds;

  private boolean                  savePressed      = false;

  public PersonEditorDialog(Window owner, String title, Person person) {
    super(owner, title, "personEditor");
    personToEdit = person;
    this.ids = MediaIdTable.convertIdMapToEventList(personToEdit.getIds());

    initComponents();

    tfName.setText(personToEdit.getName());
    tfRole.setText(personToEdit.getRole());
    tfImageUrl.setText(personToEdit.getThumbUrl());
    tfProfileUrl.setText(personToEdit.getProfileUrl());
  }

  private void initComponents() {
    {
      JPanel panelContent = new JPanel();
      getContentPane().add(panelContent);
      panelContent.setLayout(new MigLayout("", "[][500lp:n,grow][]", "[][][][][][][][20lp]"));
      {
        JLabel lblNameT = new JLabel(TmmResourceBundle.getString("metatag.name"));
        panelContent.add(lblNameT, "cell 0 0,alignx trailing");

        tfName = new JTextField();
        panelContent.add(tfName, "cell 1 0,growx");
        tfName.setColumns(10);
      }
      {
        JLabel lblRoleT = new JLabel(TmmResourceBundle.getString("metatag.role"));
        panelContent.add(lblRoleT, "cell 0 1,alignx trailing");

        tfRole = new JTextField();
        panelContent.add(tfRole, "cell 1 1,growx");
        tfRole.setColumns(10);
      }
      {
        JLabel lblImageUrlT = new JLabel(TmmResourceBundle.getString("image.url"));
        panelContent.add(lblImageUrlT, "cell 0 2,alignx trailing");

        tfImageUrl = new JTextField();
        panelContent.add(tfImageUrl, "cell 1 2,growx");
        tfImageUrl.setColumns(10);
      }
      {
        JButton btnShowImage = new JButton(IconManager.IMAGE_INV);
        btnShowImage.setToolTipText(TmmResourceBundle.getString("image.show"));
        btnShowImage.addActionListener(e -> {
          if (StringUtils.isNotBlank(tfImageUrl.getText())) {
            // check for valid url
            try {
              URL url = new URL(tfImageUrl.getText());
              Dialog dialog = new ImagePreviewDialog(url.toExternalForm());
              dialog.setVisible(true);
            }
            catch (Exception ignored) {
              // ignored
            }
          }
        });
        panelContent.add(btnShowImage, "cell 2 2");
      }
      {
        JLabel lblProfileUrlT = new JLabel(TmmResourceBundle.getString("profile.url"));
        panelContent.add(lblProfileUrlT, "cell 0 3,alignx trailing");

        tfProfileUrl = new JTextField();
        panelContent.add(tfProfileUrl, "cell 1 3,growx");
        tfProfileUrl.setColumns(10);
      }
      {
        JLabel lblIds = new TmmLabel(TmmResourceBundle.getString("metatag.ids"));
        panelContent.add(lblIds, "cell 0 4,alignx trailing");

        JScrollPane scrollPaneIds = new JScrollPane();
        panelContent.add(scrollPaneIds, "cell 1 4 1 4,grow");

        tableIds = new MediaIdTable(ids);
        tableIds.configureScrollPane(scrollPaneIds);

        JButton btnAddId = new SquareIconButton(new AddIdAction());
        panelContent.add(btnAddId, "cell 0 5,alignx right,aligny top");
      }

      JButton btnRemoveId = new SquareIconButton(new RemoveIdAction());
      panelContent.add(btnRemoveId, "cell 0 6,alignx right,aligny top");
    }
    {
      JButton btnCancel = new JButton(TmmResourceBundle.getString("Button.cancel"));
      btnCancel.addActionListener(e -> setVisible(false));
      addButton(btnCancel);

      JButton btnOk = new JButton(TmmResourceBundle.getString("Button.save"));
      btnOk.addActionListener(e -> {
        personToEdit.setName(tfName.getText());
        personToEdit.setRole(tfRole.getText());
        personToEdit.setThumbUrl(tfImageUrl.getText());
        personToEdit.setProfileUrl(tfProfileUrl.getText());

        // sync of media ids
        // first round -> add existing ids
        for (MediaId id : ids) {
          // first try to cast it into an Integer
          try {
            Integer value = Integer.parseInt(id.value);
            // cool, it is an Integer
            personToEdit.setId(id.key, value);
          }
          catch (NumberFormatException ex) {
            // okay, we set it as a String
            personToEdit.setId(id.key, id.value);
          }
        }
        // second round -> remove deleted ids
        List<String> removeIds = new ArrayList<>();
        for (Entry<String, Object> entry : personToEdit.getIds().entrySet()) {
          MediaId id = new MediaId(entry.getKey());
          if (!ids.contains(id)) {
            removeIds.add(entry.getKey());
          }
        }
        for (String id : removeIds) {
          // set a null value causes to fire the right events
          personToEdit.setId(id, null);
        }

        savePressed = true;

        setVisible(false);
      });
      addDefaultButton(btnOk);
    }
  }

  public boolean isSavePressed() {
    return savePressed;
  }

  private class AddIdAction extends AbstractAction {
    private static final long serialVersionUID = 2903255414553349267L;

    public AddIdAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("id.add"));
      putValue(SMALL_ICON, IconManager.ADD_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      MediaId mediaId = new MediaId();
      IdEditorDialog dialog = new IdEditorDialog(SwingUtilities.getWindowAncestor(tableIds), TmmResourceBundle.getString("id.add"), mediaId,
          ScraperType.MOVIE);
      dialog.setVisible(true);

      if (StringUtils.isNoneBlank(mediaId.key, mediaId.value)) {
        ids.add(mediaId);
      }
    }
  }

  private class RemoveIdAction extends AbstractAction {
    private static final long serialVersionUID = -7079826950827356996L;

    public RemoveIdAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("id.remove"));
      putValue(SMALL_ICON, IconManager.REMOVE_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      int row = tableIds.getSelectedRow();
      if (row > -1) {
        row = tableIds.convertRowIndexToModel(row);
        ids.remove(row);
      }
    }
  }
}
