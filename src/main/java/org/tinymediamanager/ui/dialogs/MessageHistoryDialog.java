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

import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;

import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.TmmUIMessageCollector;
import org.tinymediamanager.ui.components.NoBorderScrollPane;
import org.tinymediamanager.ui.panels.MessagePanel;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import net.miginfocom.swing.MigLayout;

/**
 * The class MessageHistoryDialog is used to display a history of all messages in a window
 * 
 * @author Manuel Laggner
 */
public class MessageHistoryDialog extends TmmDialog implements ListEventListener<Message> {
  private static MessageHistoryDialog instance;

  private final Map<Message, JPanel>  messageMap;
  private final JPanel                messagesPanel;

  private MessageHistoryDialog() {
    super(MainWindow.getInstance(), TmmResourceBundle.getString("summarywindow.title"), "messageSummary");

    setModal(false);
    setModalityType(ModalityType.MODELESS);

    messageMap = new HashMap<>();

    JPanel panelContent = new JPanel();
    getContentPane().add(panelContent, BorderLayout.CENTER);

    messagesPanel = new JPanel();
    messagesPanel.setOpaque(false);
    messagesPanel.setLayout(new BoxLayout(messagesPanel, BoxLayout.PAGE_AXIS));

    JScrollPane scrollPane = new NoBorderScrollPane();
    scrollPane.setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);
    scrollPane.setViewportView(messagesPanel);
    scrollPane.getVerticalScrollBar().setUnitIncrement(16);
    panelContent.setLayout(new MigLayout("", "[300lp:600lp,grow]", "[200lp:400lp,grow]"));
    panelContent.add(scrollPane, "cell 0 0,grow, wmin 0");

    JButton btnClose = new JButton(TmmResourceBundle.getString("Button.close"));
    btnClose.addActionListener(arg0 -> setVisible(false));
    addDefaultButton(btnClose);

    TmmUIMessageCollector.instance.getMessages().addListEventListener(this);
    updatePanel();
  }

  public static MessageHistoryDialog getInstance() {
    if (instance == null) {
      instance = new MessageHistoryDialog();
    }
    return instance;
  }

  @Override
  public void setVisible(boolean visible) {
    TmmUIMessageCollector.instance.resetNewMessageCount();
    super.setVisible(visible);
  }

  @Override
  public void listChanged(ListEvent<Message> listChanges) {
    updatePanel();
  }

  private void updatePanel() {
    EventList<Message> list = TmmUIMessageCollector.instance.getMessages();
    list.getReadWriteLock().readLock().lock();
    try {
      for (Message message : list) {
        if (!messageMap.containsKey(message)) {
          MessagePanel panel = new MessagePanel(message);
          messageMap.put(message, panel);
          messagesPanel.add(panel);
        }
      }
      messagesPanel.revalidate();
      messagesPanel.repaint();
    }
    finally {
      list.getReadWriteLock().readLock().unlock();
    }
  }
}
