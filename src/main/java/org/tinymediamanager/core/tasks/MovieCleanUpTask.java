package org.tinymediamanager.core.tasks;

import java.awt.BorderLayout;
import java.awt.Container;
import java.io.File;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Vector;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.ui.UTF8Control;

import ca.odell.glazedlists.EventList;

public class MovieCleanUpTask {

    private final static Logger         LOGGER        = LoggerFactory.getLogger(MovieCleanUpTask.class);
    private static final ResourceBundle BUNDLE        = ResourceBundle.getBundle("messages", new UTF8Control()); //$NON-NLS-1$
    private List<File>                  fileList;
    private final static String         TABLETITLE    = "Unknown Files";
    EventList<File>                     eventList     = null;
    private JTable                      cleanUpTable  = null;
    private JButton                     cleanSelected = null;
    private JFrame                      cleanUpFrame  = null;
    private DefaultTableModel           model;

    public MovieCleanUpTask(List<File> fileList) {
        this.fileList = fileList;
    }

    @SuppressWarnings({ "deprecation", "serial" })
    public void start() {

        model = new DefaultTableModel();
        model.addColumn(TABLETITLE);
        model.addRow(getPathList());

        cleanUpTable = new JTable(model) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        //Popup Menu for further options
        JPopupMenu popupmenu = new JPopupMenu();
        JMenuItem selectAll = new JMenuItem(BUNDLE.getString("Button.selectall"));
        JMenuItem selectNone = new JMenuItem(BUNDLE.getString("Button.selectnone"));
        selectAll.addActionListener(e -> cleanUpTable.selectAll());
        popupmenu.add(selectAll);
        popupmenu.add(selectNone);

        cleanUpTable.setComponentPopupMenu(popupmenu);


        cleanSelected = new JButton(BUNDLE.getString("movie.cleanupfiles"));

        cleanSelected.addActionListener(actionEvent -> cleanFiles(cleanUpTable.getSelectedRows()));

        cleanUpFrame = new JFrame();

        Container content = cleanUpFrame.getContentPane();
        JScrollPane scrollPane = new JScrollPane(cleanUpTable);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        content.add(scrollPane, BorderLayout.CENTER);
        content.add(cleanSelected, BorderLayout.SOUTH);

        cleanUpFrame.pack();
        cleanUpFrame.setVisible(true);

    }

    /**
     * Die Liste der Files beziehen
     *
     * @return
     */
    private Vector<String> getPathList() {

        Vector<String> pathList = new Vector<>();

        for (File file : fileList) {
            pathList.add(file.getAbsolutePath());
        }
        return pathList;
    }

    private void cleanFiles(int[] rows) {

        /**
         * von der Table die richtige Zeile selektieren ( getSelectedRow ) und dieses File dann löschen!
         */

        for (int row : rows) {

            try {
                File file = new File((String) cleanUpTable.getValueAt(0, row));
                file.delete();
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            LOGGER.info("Deleted File" + fileList.toString());
            model.removeRow(row);
            LOGGER.info("Removed Row");

        }

    }

}
